package social.entourage.android.map.entourage.category;

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

import social.entourage.android.R;
import social.entourage.android.api.tape.Events;
import social.entourage.android.tools.BusProvider;
import social.entourage.android.user.edit.partner.UserEditPartnerAdapter;

/**
 * Created by Mihai Ionescu on 21/09/2017.
 */

public class EntourageCategoriesAdapter extends BaseExpandableListAdapter {

    public static class EntourageCategoryViewHolder {

        ImageView mIcon;
        TextView mLabel;
        CheckBox mCheckbox;

        public EntourageCategoryViewHolder(View v, OnCheckedChangeListener checkboxListener) {
            mIcon = (ImageView)v.findViewById(R.id.entourage_category_item_icon);
            mLabel = (TextView)v.findViewById(R.id.entourage_category_item_label);
            mCheckbox = (CheckBox) v.findViewById(R.id.entourage_category_item_checkbox);

            mCheckbox.setOnCheckedChangeListener(checkboxListener);

        }

    }

    private class OnCheckedChangeListener implements CompoundButton.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(final CompoundButton compoundButton, final boolean isChecked) {
            // if no tag, exit
            if (compoundButton.getTag() == null) {
                return;
            }
            // flag to check if we need to refresh the list view
            boolean needsRefresh = false;
            // get the category
            EntourageCategory category = (EntourageCategory) compoundButton.getTag();
            // unset the previously selected partner, if different than the current
            if (EntourageCategoriesAdapter.this.selectedCategory != category) {
                if (EntourageCategoriesAdapter.this.selectedCategory != null) {
                    EntourageCategoriesAdapter.this.selectedCategory.setDefault(false);
                    needsRefresh = true;
                }
            }

            // save the state
            category.setDefault(isChecked);
            if (EntourageCategoriesAdapter.this.selectedCategory != category) {
                EntourageCategoriesAdapter.this.selectedCategory = category;
            } else {
                EntourageCategoriesAdapter.this.selectedCategory = category.isDefault() ? category : null;
            }

            // refresh the listview, if needed
            if (needsRefresh) {
                EntourageCategoriesAdapter.this.notifyDataSetChanged();
            }
        }
    }

    private Context context;
    private List<String> entourageTypeList;
    private HashMap<String, List<EntourageCategory>> entourageCategoryHashMap;

    public EntourageCategory selectedCategory = null;
    private OnCheckedChangeListener onCheckedChangeListener = new OnCheckedChangeListener();

    public EntourageCategoriesAdapter(Context context, List<String> entourageTypeList, HashMap<String, List<EntourageCategory>> entourageCategoryHashMap, EntourageCategory selectedCategory) {
        this.context = context;
        this.entourageTypeList = entourageTypeList;
        this.entourageCategoryHashMap = entourageCategoryHashMap;
        this.selectedCategory = selectedCategory;
    }

    @Override
    public int getChildrenCount(final int groupPosition) {
        return this.entourageCategoryHashMap.get(this.entourageTypeList.get(groupPosition)).size();
    }

    @Override
    public Object getChild(final int groupPosition, final int childPosition) {
        return this.entourageCategoryHashMap.get(this.entourageTypeList.get(groupPosition))
                .get(childPosition);
    }

    @Override
    public long getChildId(final int groupPosition, final int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(final int groupPosition, final int childPosition, final boolean isLastChild, View convertView, final ViewGroup parent) {
        EntourageCategoryViewHolder viewHolder;
        if (convertView == null) {
            // create the child view
            LayoutInflater layoutInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.layout_entourage_category_item, null);
            viewHolder = new EntourageCategoryViewHolder(convertView, onCheckedChangeListener);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (EntourageCategoryViewHolder)convertView.getTag();
        }
        // populate the child view
        EntourageCategory category = (EntourageCategory)getChild(groupPosition, childPosition);
        if (category != null) {
            viewHolder.mIcon.setImageResource(category.getIconRes());
            viewHolder.mIcon.clearColorFilter();
            viewHolder.mIcon.setColorFilter(ContextCompat.getColor(context, category.getTypeColorRes()), PorterDuff.Mode.SRC_IN);
            viewHolder.mLabel.setText(category.getTitle());

            // set the tag to null so that oncheckedchangelistener exits when populating the view
            viewHolder.mCheckbox.setTag(null);
            // set the check state
            viewHolder.mCheckbox.setChecked(category.isDefault());
            // set the tag to the item position
            viewHolder.mCheckbox.setTag(category);
        }

        return convertView;
    }

    @Override
    public int getGroupCount() {
        return this.entourageTypeList.size();
    }

    @Override
    public Object getGroup(final int groupPosition) {
        return this.entourageTypeList.get(groupPosition);
    }

    @Override
    public long getGroupId(final int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(final int groupPosition, final boolean isExpanded, View convertView, final ViewGroup parent) {
        boolean _isExpanded = isExpanded;
        if (convertView == null) {
            // create the group view
            LayoutInflater layoutInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.layout_entourage_category_group, null);
            // expand it
            ((ExpandableListView)parent).expandGroup(groupPosition);
            _isExpanded = true;
        }
        // populate the group
        TextView label = (TextView)convertView.findViewById(R.id.entourage_category_group_label);
        ImageView arrow = (ImageView)convertView.findViewById(R.id.entourage_category_group_arrow);
        label.setText( EntourageCategory.getEntourageTypeDescription((String)getGroup(groupPosition)) );
        if (_isExpanded) {
            arrow.setRotation(-90.0f);
        } else {
            arrow.setRotation(0.0f);
        }

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(final int groupPosition, final int childPosition) {
        return true;
    }
}