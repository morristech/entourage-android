package social.entourage.android.map.filter;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Switch;

import com.flurry.android.FlurryAgent;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import social.entourage.android.Constants;
import social.entourage.android.R;
import social.entourage.android.api.tape.Events;
import social.entourage.android.tools.BusProvider;

public class MapFilterFragment extends DialogFragment {

    // ----------------------------------
    // Constants
    // ----------------------------------

    public static final String TAG = "social.entourage_android.MapFilterFragment";

    private static final String KEY_PRO_USER = "social,entourage.android.KEY_PRO_USER";

    // ----------------------------------
    // Attributes
    // ----------------------------------

    private boolean isProUser = false;

    @Bind(R.id.map_filter_tour_type_layout)
    LinearLayout tourTypeLayout;

    @Bind(R.id.map_filter_entourage_tours)
    View showToursLayout;

    @Bind(R.id.map_filter_tour_medical_switch)
    Switch tourMedicalSwitch;

    @Bind(R.id.map_filter_tour_social_switch)
    Switch tourSocialSwitch;

    @Bind(R.id.map_filter_tour_distributive_switch)
    Switch tourDistributiveSwitch;

    @Bind(R.id.map_filter_entourage_demand_switch)
    Switch entourageDemandSwitch;

    @Bind(R.id.map_filter_entourage_contribution_switch)
    Switch entourageContributionSwitch;

    @Bind(R.id.map_filter_entourage_tours_switch)
    Switch showToursSwitch;

    @Bind(R.id.map_filter_entourage_user_only_switch)
    Switch onlyMyEntouragesSwitch;

    @Bind(R.id.map_filter_time_days_1)
    RadioButton days1RB;

    @Bind(R.id.map_filter_time_days_2)
    RadioButton days2RB;

    @Bind(R.id.map_filter_time_days_3)
    RadioButton days3RB;

    // ----------------------------------
    // Lifecycle
    // ----------------------------------

    public MapFilterFragment() {
        // Required empty public constructor
    }

    public static MapFilterFragment newInstance(boolean isProUser) {
        MapFilterFragment fragment = new MapFilterFragment();
        Bundle args = new Bundle();
        args.putBoolean(KEY_PRO_USER, isProUser);
        fragment.setArguments(args);

        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        super.onCreateView(inflater, container, savedInstanceState);
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_map_filter, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            isProUser = args.getBoolean(KEY_PRO_USER, false);
        }
        initializeView();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow().getAttributes().windowAnimations = R.style.CustomDialogFragmentSlide;
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.background)));
    }

    // ----------------------------------
    // Buttons handling
    // ----------------------------------

    @OnClick(R.id.map_filter_close_button)
    protected void onCloseClicked() {
        FlurryAgent.logEvent(Constants.EVENT_MAP_FILTER_CLOSE);
        dismiss();
    }

    @OnClick(R.id.map_filter_validate_button)
    protected void onValidateClicked() {
        // save the values to the filter
        MapFilter mapFilter = MapFilter.getInstance();

        mapFilter.tourTypeMedical = tourMedicalSwitch.isChecked();
        mapFilter.tourTypeSocial = tourSocialSwitch.isChecked();
        mapFilter.tourTypeDistributive = tourDistributiveSwitch.isChecked();

        mapFilter.entourageTypeDemand = entourageDemandSwitch.isChecked();
        mapFilter.entourageTypeContribution = entourageContributionSwitch.isChecked();
        mapFilter.showTours = showToursSwitch.isChecked();
        mapFilter.onlyMyEntourages = onlyMyEntouragesSwitch.isChecked();

        if (days1RB.isChecked()) mapFilter.timeframe = MapFilter.DAYS_1;
        else if (days2RB.isChecked()) mapFilter.timeframe = MapFilter.DAYS_2;
        else if (days3RB.isChecked()) mapFilter.timeframe = MapFilter.DAYS_3;

        // inform the map screen to refresh the newsfeed
        BusProvider.getInstance().post(new Events.OnMapFilterChanged());

        // flurry event
        FlurryAgent.logEvent(Constants.EVENT_MAP_FILTER_SUBMIT);

        // dismiss the dialog
        dismiss();
    }

    @OnClick(R.id.map_filter_tour_medical_switch)
    protected void onMedicalSwitch() {
        FlurryAgent.logEvent(Constants.EVENT_MAP_FILTER_ONLY_MEDICAL_TOURS);
    }

    @OnClick(R.id.map_filter_tour_social_switch)
    protected void onSocialSwitch() {
        FlurryAgent.logEvent(Constants.EVENT_MAP_FILTER_ONLY_SOCIAL_TOURS);
    }

    @OnClick(R.id.map_filter_tour_distributive_switch)
    protected void onDistributiveSwitch() {
        FlurryAgent.logEvent(Constants.EVENT_MAP_FILTER_ONLY_DISTRIBUTION_TOURS);
    }

    @OnClick(R.id.map_filter_entourage_demand_switch)
    protected void onDemandSwitch() {
        FlurryAgent.logEvent(Constants.EVENT_MAP_FILTER_ONLY_ASK);
    }

    @OnClick(R.id.map_filter_entourage_contribution_switch)
    protected void onContributionSwitch() {
        FlurryAgent.logEvent(Constants.EVENT_MAP_FILTER_ONLY_OFFERS);
    }

    @OnClick(R.id.map_filter_entourage_tours_switch)
    protected void onOnlyToursSwitch() {
        FlurryAgent.logEvent(Constants.EVENT_MAP_FILTER_ONLY_TOURS);
    }

    @OnClick(R.id.map_filter_entourage_user_only_switch)
    protected void onOnlyMineSwitch() {
        FlurryAgent.logEvent(Constants.EVENT_MAP_FILTER_ONLY_MINE);
    }

    @OnClick(R.id.map_filter_time_days_1)
    protected void onDays1Click() {
        FlurryAgent.logEvent(Constants.EVENT_MAP_FILTER_FILTER1);
    }

    @OnClick(R.id.map_filter_time_days_2)
    protected void onDays2Click() {
        FlurryAgent.logEvent(Constants.EVENT_MAP_FILTER_FILTER2);
    }

    @OnClick(R.id.map_filter_time_days_3)
    protected void onDays3Click() {
        FlurryAgent.logEvent(Constants.EVENT_MAP_FILTER_FILTER3);
    }

    // ----------------------------------
    // Private methods
    // ----------------------------------

    private void initializeView() {
        tourTypeLayout.setVisibility(isProUser ? View.VISIBLE : View.GONE);
        showToursLayout.setVisibility(isProUser ? View.VISIBLE : View.GONE);

        MapFilter mapFilter = MapFilter.getInstance();

        tourMedicalSwitch.setChecked(mapFilter.tourTypeMedical);
        tourSocialSwitch.setChecked(mapFilter.tourTypeSocial);
        tourDistributiveSwitch.setChecked(mapFilter.tourTypeDistributive);

        entourageDemandSwitch.setChecked(mapFilter.entourageTypeDemand);
        entourageContributionSwitch.setChecked(mapFilter.entourageTypeContribution);
        showToursSwitch.setChecked(mapFilter.showTours);
        onlyMyEntouragesSwitch.setChecked(mapFilter.onlyMyEntourages);

        switch (mapFilter.timeframe) {
            case MapFilter.DAYS_1:
                days1RB.setChecked(true);
                break;
            case MapFilter.DAYS_3:
                days3RB.setChecked(true);
                break;
            case MapFilter.DAYS_2:
            default:
                days2RB.setChecked(true);
                break;
        }
    }

}
