package social.entourage.android.user.edit.partner;

import android.graphics.Typeface;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import social.entourage.android.R;
import social.entourage.android.api.model.Partner;
import social.entourage.android.api.tape.Events;
import social.entourage.android.tools.BusProvider;

/**
 * Created by mihaiionescu on 16/01/2017.
 */

public class UserEditPartnerAdapter extends BaseAdapter {

    public static class PartnerViewHolder {

        public TextView mPartnerName;
        public ImageView mPartnerLogo;
        public CheckBox mCheckbox;

        public Partner partner;

        public PartnerViewHolder(View v, final OnCheckedChangeListener checkboxListener) {
            mPartnerName = (TextView) v.findViewById(R.id.partner_name);
            mPartnerLogo = (ImageView) v.findViewById(R.id.partner_logo);
            mCheckbox = (CheckBox) v.findViewById(R.id.partner_checkbox);

            mCheckbox.setOnCheckedChangeListener(checkboxListener);

            mPartnerName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    if (checkboxListener != null) {
                        mCheckbox.setChecked(!mCheckbox.isChecked());
                        checkboxListener.onCheckedChanged(mCheckbox, mCheckbox.isChecked());
                    }
                }
            });

            mPartnerLogo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    if (partner != null) {
                        BusProvider.getInstance().post(new Events.OnPartnerViewRequestedEvent(partner));
                    }
                }
            });
        }

    }


    public int selectedPartnerPosition = AdapterView.INVALID_POSITION;

    private OnCheckedChangeListener onCheckedChangeListener = new OnCheckedChangeListener();

    private List<Partner> partnerList;

    public List<Partner> getPartnerList() {
        return partnerList;
    }

    public void setPartnerList(final List<Partner> partnerList) {
        this.partnerList = partnerList;
        this.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return partnerList == null ? 0 : partnerList.size();
    }

    @Override
    public View getView(final int position, View view, final ViewGroup viewGroup) {
        PartnerViewHolder viewHolder;
        if (view == null) {
            view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_edit_partner, viewGroup, false);
            viewHolder = new PartnerViewHolder(view, onCheckedChangeListener);
            view.setTag(viewHolder);
        } else {
            viewHolder = (PartnerViewHolder) view.getTag();
        }

        // Populate the view
        Partner partner = getItem(position);
        if (partner != null) {
            viewHolder.mPartnerName.setText(partner.getName());
            if (partner.isDefault()) {
                viewHolder.mPartnerName.setTypeface(viewHolder.mPartnerName.getTypeface(), Typeface.BOLD);
            } else {
                viewHolder.mPartnerName.setTypeface(Typeface.create(viewHolder.mPartnerName.getTypeface(), Typeface.NORMAL));
            }

            String partnerLogo = partner.getLargeLogoUrl();
            if (partnerLogo != null) {
                Picasso.with(viewGroup.getContext())
                        .load(Uri.parse(partnerLogo))
                        .placeholder(R.drawable.partner_placeholder)
                        .into(viewHolder.mPartnerLogo);
            } else {
                viewHolder.mPartnerLogo.setImageResource(R.drawable.partner_placeholder);
            }

            // set the tag to null so that oncheckedchangelistener exits when populating the view
            viewHolder.mCheckbox.setTag(null);
            // set the check state
            viewHolder.mCheckbox.setChecked(partner.isDefault());
            // set the tag to the item position
            viewHolder.mCheckbox.setTag(position);

            // set the partner id
            viewHolder.partner = partner;
        }

        return view;
    }

    @Override
    public long getItemId(final int position) {
        return position;
    }

    @Override
    public Partner getItem(final int position) {
        if (partnerList == null || position < 0 || position >= partnerList.size()) {
            return null;
        }
        return partnerList.get(position);
    }

    private class OnCheckedChangeListener implements CompoundButton.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(final CompoundButton compoundButton, final boolean isChecked) {
            // if no tag, exit
            if (compoundButton.getTag() == null) {
                return;
            }
            // get the position
            int position = (Integer) compoundButton.getTag();
            // unset the previously selected partner, if different than the current
            if (UserEditPartnerAdapter.this.selectedPartnerPosition != position) {
                Partner oldPartner = UserEditPartnerAdapter.this.getItem(UserEditPartnerAdapter.this.selectedPartnerPosition);
                if (oldPartner != null) {
                    oldPartner.setDefault(false);
                }
            }

            // save the state
            Partner partner = UserEditPartnerAdapter.this.getItem(position);
            if (partner != null) {
                partner.setDefault(isChecked);
                if (UserEditPartnerAdapter.this.selectedPartnerPosition != position) {
                    UserEditPartnerAdapter.this.selectedPartnerPosition = position;
                } else {
                    UserEditPartnerAdapter.this.selectedPartnerPosition = partner.isDefault() ? position : AdapterView.INVALID_POSITION;
                }
            }

            // refresh the list view
            UserEditPartnerAdapter.this.notifyDataSetChanged();
        }
    }
}
