package social.entourage.android.map.entourage.category;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import social.entourage.android.Constants;
import social.entourage.android.DrawerActivity;
import social.entourage.android.base.EntourageDialogFragment;
import social.entourage.android.R;
import social.entourage.android.base.EntourageLinkMovementMethod;
import social.entourage.android.map.entourage.CreateEntourageListener;
import social.entourage.android.view.HtmlTextView;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link social.entourage.android.map.entourage.CreateEntourageListener} interface
 * to handle interaction events.
 * Use the {@link EntourageCategoryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EntourageCategoryFragment extends EntourageDialogFragment {

    // ----------------------------------
    // Constants
    // ----------------------------------

    public static final String TAG = EntourageCategoryFragment.class.getSimpleName();

    // ----------------------------------
    // Attributes
    // ----------------------------------

    public static final String KEY_ENTOURAGE_CATEGORY = "ENTOURAGE_CATEGORY";

    private EntourageCategory category;

    private CreateEntourageListener mListener;

    @BindView(R.id.entourage_category_listview)
    ExpandableListView listView;

    private EntourageCategoriesAdapter adapter;

    public EntourageCategoryFragment() {
        // Required empty public constructor
    }

    public static EntourageCategoryFragment newInstance(EntourageCategory category) {
        EntourageCategoryFragment fragment = new EntourageCategoryFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_ENTOURAGE_CATEGORY, category);
        fragment.setArguments(args);
        return fragment;
    }

    public static EntourageCategoryFragment newInstance() {
        EntourageCategoryFragment fragment = new EntourageCategoryFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_entourage_category, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            category = (EntourageCategory)getArguments().getSerializable(KEY_ENTOURAGE_CATEGORY);
            if (category != null) {
                category.setDefault(true);
            }
        }

        initializeView();
    }

    @Override
    public void dismiss() {
        if (adapter != null && adapter.selectedCategory != null) {
            // Reset the flag so consequent fragment shows will not appear broken
            adapter.selectedCategory.setDefault(false);
        }
        super.dismiss();
    }

    public void setListener(final CreateEntourageListener mListener) {
        this.mListener = mListener;
    }

    private void initializeView() {
        initializeListView();
        initializeHelpHtmlView();
    }

    private void initializeListView() {
        HashMap<String, List<EntourageCategory>> entourageCategoryHashMap = EntourageCategoryManager.getInstance().getEntourageCategories();
        List<String> entourageTypeList = EntourageCategoryManager.getInstance().getEntourageTypes();
        adapter = new EntourageCategoriesAdapter(getContext(), entourageTypeList, entourageCategoryHashMap, category);
        listView.setAdapter(adapter);
        int count = adapter.getGroupCount();
        for (int position = 0; position < count; position++) {
            listView.expandGroup(position);
        }
    }

    private void initializeHelpHtmlView() {
        HtmlTextView helpHtmlTextView = getView().findViewById(R.id.entourage_category_help_link);
        if (helpHtmlTextView != null) {
            if (getActivity() != null && getActivity() instanceof DrawerActivity) {
                String goalLink = ((DrawerActivity) getActivity()).getLink(Constants.GOAL_LINK_ID);
                helpHtmlTextView.setHtmlString(getString(R.string.entourage_create_help_text, goalLink), EntourageLinkMovementMethod.getInstance());
            }
        }
    }

    // ----------------------------------
    // Interactions handling
    // ----------------------------------

    @OnClick(R.id.title_close_button)
    void onCloseClicked() {
        mListener = null;
        dismiss();
    }

    @OnClick(R.id.title_action_button)
    void onValidateClicked() {
        if (mListener != null) {
            mListener.onCategoryChosen(adapter.selectedCategory);
        }
        mListener = null;
        dismiss();
    }

}
