package social.entourage.android.map;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.ui.IconGenerator;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import social.entourage.android.BackPressable;
import social.entourage.android.Constants;
import social.entourage.android.DrawerActivity;
import social.entourage.android.EntourageApplication;
import social.entourage.android.EntourageComponent;
import social.entourage.android.EntourageLocation;
import social.entourage.android.R;
import social.entourage.android.api.model.Message;
import social.entourage.android.api.model.Newsfeed;
import social.entourage.android.api.model.PushNotificationContent;
import social.entourage.android.api.model.TimestampedObject;
import social.entourage.android.api.model.TourTransportMode;
import social.entourage.android.api.model.TourType;
import social.entourage.android.api.model.User;
import social.entourage.android.api.model.map.BaseEntourage;
import social.entourage.android.api.model.map.Encounter;
import social.entourage.android.api.model.map.Entourage;
import social.entourage.android.api.model.map.Tour;
import social.entourage.android.api.model.map.TourPoint;
import social.entourage.android.api.model.map.TourUser;
import social.entourage.android.api.tape.Events;
import social.entourage.android.api.tape.Events.OnEncounterCreated;
import social.entourage.android.api.tape.Events.OnBetterLocationEvent;
import social.entourage.android.api.tape.Events.OnCheckIntentActionEvent;
import social.entourage.android.api.tape.Events.OnLocationPermissionGranted;
import social.entourage.android.api.tape.Events.OnUserChoiceEvent;
import social.entourage.android.base.EntouragePagination;
import social.entourage.android.map.choice.ChoiceFragment;
import social.entourage.android.map.confirmation.ConfirmationActivity;
import social.entourage.android.map.encounter.CreateEncounterActivity;
import social.entourage.android.map.filter.MapFilterFragment;
import social.entourage.android.map.permissions.NoLocationPermissionFragment;
import social.entourage.android.map.tour.TourService;
import social.entourage.android.map.tour.information.TourInformationFragment;
import social.entourage.android.map.tour.join.TourJoinRequestFragment;
import social.entourage.android.newsfeed.NewsfeedAdapter;
import social.entourage.android.tools.BusProvider;

public class MapEntourageFragment extends Fragment implements BackPressable, TourService.TourServiceListener {

    // ----------------------------------
    // CONSTANTS
    // ----------------------------------

    public static final float ZOOM_REDRAW_LIMIT = 1.1f;
    private static final int REDRAW_LIMIT = 300;
    private static final int PERMISSIONS_REQUEST_LOCATION = 1;
    private static final int MAX_TOUR_HEADS_DISPLAYED = 10;

    private static final long REFRESH_TOURS_INTERVAL = 60000; //1 minute in ms

    private static final float ENTOURAGE_HEATMAP_SIZE = 100; //meters

    private static final int MAX_SCROLL_DELTA_Y = 20;

    // ----------------------------------
    // ATTRIBUTES
    // ----------------------------------

    @Inject
    MapPresenter presenter;

    private int userId;
    private boolean userHistory;

    private View toReturn;

    private SupportMapFragment mapFragment;
    private GoogleMap map;

    private LatLng previousCoordinates;
    private Location previousCameraLocation;
    private LatLng longTapCoordinates;
    private float previousCameraZoom = 1.0f;

    private TourService tourService;
    private ServiceConnection connection = new ServiceConnection();
    private ProgressDialog loaderStop;
    private ProgressDialog loaderSearchTours;
    private boolean isBound;
    private boolean isMapLoaded;
    private boolean isFollowing = true;

    private long currentTourId = -1;
    private int color;
    private int displayedTourHeads = 0;

    private List<Polyline> currentTourLines;
    private Map<Long, Polyline> drawnToursMap;
    private Map<Long, Polyline> drawnUserHistory;
    private Map<String, Object> markersMap;
    private Map<Long, Tour> retrievedHistory;

    private LayoutInflater inflater;

    private float originalMapLayoutWeight;

    private int isRequestingToJoin = 0;

    @Bind(R.id.fragment_map_pin)
    View mapPin;

    @Bind(R.id.fragment_map_gps_layout)
    LinearLayout gpsLayout;

    @Bind(R.id.fragment_map_follow_button)
    View centerButton;

    @Bind(R.id.layout_map_launcher)
    View mapLauncherLayout;

    @Bind(R.id.launcher_tour_go)
    ImageView buttonLaunchTour;

    @Bind(R.id.launcher_tour_transport_mode)
    RadioGroup radioGroupTransportMode;

    @Bind(R.id.launcher_tour_type)
    RadioGroup radioGroupType;

    @Bind(R.id.fragment_map_tours_view)
    RecyclerView toursListView;

    NewsfeedAdapter newsfeedAdapter;

    @Bind(R.id.layout_map)
    FrameLayout layoutMapMain;

    @Bind(R.id.fragment_map_main_layout)
    LinearLayout layoutMain;

    @Bind(R.id.map_display_type)
    RadioGroup mapDisplayTypeRadioGroup;

    @Bind(R.id.layout_map_longclick)
    RelativeLayout mapLongClickView;

    @Bind(R.id.map_longclick_buttons)
    RelativeLayout mapLongClickButtonsView;

    @Bind(R.id.tour_stop_button)
    FloatingActionButton tourStopButton;

    FloatingActionMenu mapOptionsMenu;

    Timer refreshToursTimer;
    TimerTask refreshToursTimerTask;
    final Handler refreshToursHandler = new Handler();

    //pagination
    private EntouragePagination pagination = new EntouragePagination(Constants.ITEMS_PER_PAGE);
    private int scrollDeltaY;
    private OnScrollListener scrollListener = new OnScrollListener();

    // ----------------------------------
    // LIFECYCLE
    // ----------------------------------

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isBound) {
            doBindService();
        }

        BusProvider.getInstance().register(this);

        currentTourLines = new ArrayList<>();
        drawnToursMap = new TreeMap<>();
        drawnUserHistory = new TreeMap<>();
        markersMap = new TreeMap<>();
        retrievedHistory = new TreeMap<>();

        checkPermission();
        FlurryAgent.logEvent(Constants.EVENT_OPEN_TOURS_FROM_MENU);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        previousCameraLocation = EntourageLocation.cameraPositionToLocation(null, EntourageLocation.getInstance().getLastCameraPosition());
        if (toReturn == null) {
            toReturn = inflater.inflate(R.layout.fragment_map, container, false);
        }
        ButterKnife.bind(this, toReturn);
        this.inflater = inflater;
        return toReturn;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupComponent(EntourageApplication.get(getActivity()).getEntourageComponent());
        presenter.start();
        initializeMap();
        initializeFloatingMenu();
        initializeToursListView();
    }

    protected void setupComponent(EntourageComponent entourageComponent) {
        DaggerMapComponent.builder()
                .entourageComponent(entourageComponent)
                .mapModule(new MapModule(this))
                .build()
                .inject(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQUEST_CREATE_ENCOUNTER) {
            if (resultCode == Constants.RESULT_CREATE_ENCOUNTER_OK) {
                Encounter encounter = (Encounter) data.getExtras().getSerializable(CreateEncounterActivity.BUNDLE_KEY_ENCOUNTER);
                addEncounter(encounter);
                presenter.loadEncounterOnMap(encounter);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
            for (int index = 0; index < permissions.length; index++) {
                if (permissions[index].equalsIgnoreCase(Manifest.permission.ACCESS_FINE_LOCATION) && grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                    checkPermission();
                } else {
                    BusProvider.getInstance().post(new OnLocationPermissionGranted(true));
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onStart() {
        super.onStart();

        toursListView.addOnScrollListener(scrollListener);
    }

    @Override
    public void onStop() {
        super.onStop();

        toursListView.removeOnScrollListener(scrollListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            getActivity().setTitle(R.string.activity_tours_title);
            if (isMapLoaded) {
                BusProvider.getInstance().post(new OnCheckIntentActionEvent());
            }
        }
        timerStart();
    }

    @Override
    public void onPause() {
        super.onPause();

        timerStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        BusProvider.getInstance().unregister(this);
        if (isBound && tourService != null) {
            tourService.unregister(MapEntourageFragment.this);
            doUnbindService();
        }
    }

    @Override
    public boolean onBackPressed() {
        if (mapLauncherLayout.getVisibility() == View.VISIBLE) {
            mapLauncherLayout.setVisibility(View.GONE);
            mapOptionsMenu.setVisibility(View.VISIBLE);
            return true;
        }
        if (mapOptionsMenu.isOpened()) {
            mapOptionsMenu.toggle(true);
            return true;
        }
        if (mapLongClickView.getVisibility() == View.VISIBLE) {
            mapLongClickView.setVisibility(View.GONE);
            mapOptionsMenu.setVisibility(View.VISIBLE);
            if (tourService != null && tourService.isRunning()) {
                tourStopButton.setVisibility(View.VISIBLE);
            }
            return true;
        }
        //before closing the fragment, send the cached tour points to server (if applicable)
        if (tourService != null) {
            tourService.updateOngoingTour();
        }
        return false;
    }

    // ----------------------------------
    // PUBLIC METHODS
    // ----------------------------------

    public void onNotificationExtras(int id, boolean choice) {
        userId = id;
        userHistory = choice;
    }

    public void setOnMarkerClickListener(MapPresenter.OnEntourageMarkerClickListener onMarkerClickListener) {
        if (map != null) {
            map.setOnMarkerClickListener(onMarkerClickListener);
        }
    }

    public void putEncounterOnMap(Encounter encounter,
                                  MapPresenter.OnEntourageMarkerClickListener onClickListener) {
        double encounterLatitude = encounter.getLatitude();
        double encounterLongitude = encounter.getLongitude();
        LatLng encounterPosition = new LatLng(encounterLatitude, encounterLongitude);
        BitmapDescriptor encounterIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_encounter);

        MarkerOptions markerOptions = new MarkerOptions().position(encounterPosition)
                .icon(encounterIcon);

        if (map != null) {
            map.addMarker(markerOptions);
            onClickListener.addEncounterMarker(encounterPosition, encounter);
        }
    }

    public void initializeMapZoom() {
        centerMap(EntourageLocation.getInstance().getLastCameraPosition());
    }

    public void displayChosenTour(long tourId) {
        //check if we are not already displaying the tour
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        TourInformationFragment tourInformationFragment = (TourInformationFragment) fragmentManager.findFragmentByTag(TourInformationFragment.TAG);
        if (tourInformationFragment != null && tourInformationFragment.getTourId() == tourId) {
            return;
        }
        //display the tour
        Tour tour = (Tour)newsfeedAdapter.findCard(TimestampedObject.TOUR_CARD, tourId);
        if (tour != null) {
            displayChosenTour(tour);
        }
        else {
            if (presenter != null) {
                presenter.openTour(tourId);
            }
        }
    }

    public void displayChosenTour(Tour tour) {
        if (presenter != null) {
            presenter.openTour(tour);
        }
    }

    public void act(TimestampedObject timestampedObject) {
        if (tourService != null) {
            isRequestingToJoin++;
            if (timestampedObject.getType() == TimestampedObject.TOUR_CARD) {
                tourService.requestToJoinTour((Tour)timestampedObject);
            }
            else if (timestampedObject.getType() == TimestampedObject.ENTOURAGE_CARD) {
                tourService.requestToJoinEntourage((Entourage)timestampedObject);
            }
            else {
                isRequestingToJoin--;
            }
        }
        else {
            Toast.makeText(getContext(), R.string.tour_join_request_error, Toast.LENGTH_SHORT).show();
        }
    }

    public void createEntourage(String entourageType) {
        mapLongClickView.setVisibility(View.GONE);
        if (mapOptionsMenu.isOpened()) {
            mapOptionsMenu.toggle(false);
        }
        mapOptionsMenu.setVisibility(View.VISIBLE);
        if (tourService != null && tourService.isRunning()) {
            tourStopButton.setVisibility(View.VISIBLE);
        }
        LatLng location = EntourageLocation.getInstance().getLastCameraPosition().target;
        if (longTapCoordinates != null) {
            location = longTapCoordinates;
            longTapCoordinates = null;
        }
        if (presenter != null) {
            presenter.createEntourage(entourageType, location);
        }
    }

    public void checkAction(String action, Tour actionTour) {
        if (getActivity() != null && isBound) {
            // 1 : Check if should Resume tour
            if (action != null && ConfirmationActivity.KEY_RESUME_TOUR.equals(action)) {
                resumeTour(actionTour);
            }
            // 2 : Check if should End tour
            else if (action != null && ConfirmationActivity.KEY_END_TOUR.equals(action)) {
                stopTour(actionTour);
            }
            // 3 : Check if tour is already paused
            else if (tourService.isPaused()) {
                launchConfirmationActivity();
            }
            // 4 : Check if should pause tour
            else if (action != null && TourService.KEY_NOTIFICATION_PAUSE_TOUR.equals(action)) {
                launchConfirmationActivity();
            }
            // 5 : Check if should stop tour
            else if (action != null && TourService.KEY_NOTIFICATION_STOP_TOUR.equals(action)) {
                tourService.endTreatment();
            }
        }
    }

    // ----------------------------------
    // BUS LISTENERS
    // ----------------------------------

    @Subscribe
    public void onUserChoiceChanged(OnUserChoiceEvent event) {
        userHistory = event.isUserHistory();
        if (userHistory) {
            tourService.updateUserHistory(userId, 1, 500);
        }
        if (userHistory) {
            showUserHistory();
        } else {
            hideUserHistory();
        }
    }

    @Subscribe
    public void onBetterLocation(OnBetterLocationEvent event) {
        if (event.getLocation() != null) {
            centerMap(event.getLocation());
        }
    }

    @Subscribe
    public void onEncounterCreated(OnEncounterCreated event) {
        Encounter encounter = event.getEncounter();
        if (encounter != null) {
            addEncounter(encounter);
            presenter.loadEncounterOnMap(encounter);
        }
        mapOptionsMenu.setVisibility(View.VISIBLE);
        if (tourService != null) {
            tourStopButton.setVisibility(tourService.isRunning() ? View.VISIBLE : View.GONE);
        }
    }

    @Subscribe
    public void onEntourageCreated(Events.OnEntourageCreated event) {
        Entourage entourage = event.getEntourage();
        if (entourage == null) return;
        drawNearbyEntourage(entourage, false);
        addNewsfeedCard(entourage);
        toursListView.scrollToPosition(0);
    }

    @Subscribe
    public void onMapFilterChanged(Events.OnMapFilterChanged event) {
        if (tourService != null) {
            clearAll();
            tourService.updateNewsfeed(pagination);
        }
    }

    // ----------------------------------
    // SERVICE BINDING METHODS
    // ----------------------------------

    void doBindService() {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), TourService.class);
            getActivity().startService(intent);
            getActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }
    }

    void doUnbindService() {
        if (getActivity() != null && isBound) {
            getActivity().unbindService(connection);
            isBound = false;
        }
    }

    // ----------------------------------
    // SERVICE INTERFACE METHODS
    // ----------------------------------

    @Override
    public void onTourCreated(boolean created, long tourId) {
        buttonLaunchTour.setEnabled(true);
        if (getActivity() != null) {
            if (created) {
                isFollowing = true;
                currentTourId = tourId;
                presenter.incrementUserToursCount();
                mapLauncherLayout.setVisibility(View.GONE);
                if (toursListView.getVisibility() == View.VISIBLE) {
                    hideToursList();
                }
                addTourCard(tourService.getCurrentTour());
                //mapPin.setVisibility(View.VISIBLE);
                mapOptionsMenu.setVisibility(View.VISIBLE);
                updateFloatingMenuOptions();
                tourStopButton.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(getActivity(), R.string.tour_creation_fail, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onTourUpdated(LatLng newPoint) {
        drawCurrentLocation(newPoint);
    }

    @Override
    public void onTourResumed(List<TourPoint> pointsToDraw, String tourType, Date startDate) {
        if (!pointsToDraw.isEmpty()) {
            drawCurrentTour(pointsToDraw, tourType, startDate);
            previousCoordinates = pointsToDraw.get(pointsToDraw.size() - 1).getLocation();

            Location currentLocation = EntourageLocation.getInstance().getCurrentLocation();
            centerMap(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
            isFollowing = true;
        }
        tourStopButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLocationUpdated(LatLng location) {
        centerMap(location);
    }

    @Override
    public void onRetrieveToursNearby(List<Tour> tours) {
        //check if there are tours to add or update
        int previousToursCount = newsfeedAdapter.getItemCount();
        tours = removeRedundantTours(tours, false);
        Collections.sort(tours, new Tour.TourComparatorOldToNew());
        for (Tour tour : tours) {
            if (currentTourId != tour.getId()) {
                //drawNearbyTour(tour, false);
                addTourCard(tour);
            }
        }
        //recreate the map if needed
        if (tours.size() > 0 && map != null) {
            map.clear();
            for (TimestampedObject timestampedObject : newsfeedAdapter.getItems()) {
                if (timestampedObject.getType() == TimestampedObject.TOUR_CARD) {
                    if (currentTourId != timestampedObject.getId()) {
                        drawNearbyTour((Tour)timestampedObject, false);
                    }
                }
            }
            if (tourService != null && currentTourId != -1) {
                PolylineOptions line = new PolylineOptions();
                for (Polyline polyline : currentTourLines) {
                    line.addAll(polyline.getPoints());
                }
                line.zIndex(2f);
                line.width(15);
                line.color(color);
                map.addPolyline(line);

                Tour currentTour = tourService.getCurrentTour();
                if (currentTour != null) {
                    if (currentTour.getEncounters() != null) {
                        for (Encounter encounter : currentTour.getEncounters()) {
                            presenter.loadEncounterOnMap(encounter);
                        }
                    }
                }
            }
        }

        //show the map if no tours
        if (newsfeedAdapter.getItemCount() == 0) {
            hideToursList();
        }
        else if (previousToursCount == 0) {
            showToursList();
        }
        //scroll to latest
        if (newsfeedAdapter.getItemCount() > 0) {
            toursListView.scrollToPosition(0);
        }
    }

    @Override
    public void onRetrieveToursByUserId(List<Tour> tours) {
        tours = removeRedundantTours(tours, true);
        tours = removeRecentTours(tours);
        Collections.sort(tours, new Tour.TourComparatorOldToNew());
        for (Tour tour : tours) {
            if (currentTourId != tour.getId()) {
                drawNearbyTour(tour, true);
            }
        }
    }

    @Override
    public void onUserToursFound(Map<Long, Tour> tours) {
    }

    @Override
    public void onToursFound(Map<Long, Tour> tours) {
        if (loaderSearchTours != null) {
            loaderSearchTours.dismiss();
            loaderSearchTours = null;
        }
        if (getActivity() != null) {
            if (tours.isEmpty()) {
                Toast.makeText(getActivity(), tourService.getString(R.string.tour_info_text_nothing_found), Toast.LENGTH_SHORT).show();
            } else {
                if (tours.size() > 1) {
                    List<Tour> tempList = new ArrayList<>();
                    for (Map.Entry<Long, Tour> entry : tours.entrySet()) {
                        tempList.add(entry.getValue());
                    }
                    Tour.Tours toursList = new Tour.Tours(tempList);
                    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                    ChoiceFragment choiceFragment = ChoiceFragment.newInstance(toursList);
                    choiceFragment.show(fragmentManager, "fragment_choice");
                } else {
                    TreeMap<Long, Tour> toursTree = new TreeMap<>(tours);
                    presenter.openTour(toursTree.firstEntry().getValue());
                }
            }
        }
    }

    @Override
    public void onTourClosed(boolean closed, Tour tour) {
        if (getActivity() != null) {
            if (closed) {

                 clearAll();

                //mapPin.setVisibility(View.GONE);
                if (tour.getId() == currentTourId) {
                    mapOptionsMenu.setVisibility(View.VISIBLE);
                    updateFloatingMenuOptions();
                    tourStopButton.setVisibility(View.GONE);

                    currentTourId = -1;
                }
                else {
                    tourService.notifyListenersTourResumed();
                }

                tourService.updateNewsfeed(pagination);
                if (userHistory) {
                    tourService.updateUserHistory(userId, 1, 1);
                }

                @StringRes int tourStatusStringId =  R.string.local_service_stopped;
                if (tour.getTourStatus().equals(Tour.TOUR_FREEZED)) {
                    tourStatusStringId = R.string.tour_freezed;
                }

                Toast.makeText(getActivity(), tourStatusStringId, Toast.LENGTH_SHORT).show();

            } else {
                @StringRes int tourClosedFailedId = R.string.tour_close_fail;
                if (tour.getTourStatus().equals(Tour.TOUR_FREEZED)) {
                    tourClosedFailedId = R.string.tour_freezed;
                }
                Toast.makeText(getActivity(), tourClosedFailedId, Toast.LENGTH_SHORT).show();
            }
            if (loaderStop != null) {
                loaderStop.dismiss();
                loaderStop = null;
            }
        }
    }

    @Override
    public void onGpsStatusChanged(boolean active) {
        if (gpsLayout != null) {
            if (active) {
                gpsLayout.setVisibility(View.GONE);
            } else {
                gpsLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onUserStatusChanged(TourUser user, BaseEntourage baseEntourage) {
        if (user == null) {
            //error changing the status
            if (isRequestingToJoin > 0) {
                Toast.makeText(getContext(), R.string.tour_join_request_error, Toast.LENGTH_SHORT).show();
            }
        }
        else {
            if (baseEntourage.getType() == TimestampedObject.TOUR_CARD) {
                Tour tour = (Tour)baseEntourage;
                tour.setJoinStatus(user.getStatus());
                if (user.getStatus().equals(Tour.JOIN_STATUS_PENDING)) {
                    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                    TourJoinRequestFragment tourJoinRequestFragment = TourJoinRequestFragment.newInstance(tour);
                    tourJoinRequestFragment.show(fragmentManager, TourJoinRequestFragment.TAG);
                }
            }
            else if (baseEntourage.getType() == TimestampedObject.ENTOURAGE_CARD) {
                ((Entourage)baseEntourage).setJoinStatus(user.getStatus());
            }
            updateNewsfeedJoinStatus(baseEntourage);
        }
        isRequestingToJoin--;
    }

    @Override
    public void onRetrieveNewsfeed(List<Newsfeed> newsfeedList) {
        int previousToursCount = newsfeedAdapter.getItemCount();
        if (newsfeedList != null) {
            newsfeedList = removeRedundantNewsfeed(newsfeedList, false);
//        Collections.sort(tours, new Tour.TourComparatorOldToNew());
            if (map != null) {
                //add or update the received newsfeed
                for (Newsfeed newsfeed : newsfeedList) {
                    Object newsfeedData = newsfeed.getData();
                    if (newsfeedData != null && (newsfeedData instanceof TimestampedObject)) {
                        addNewsfeedCard((TimestampedObject) newsfeedData);
                        //drawNearbyNewsfeed(newsfeed, false);
                    }
                }
                pagination.loadedItems(newsfeedList.size());
                if (newsfeedList.size() > 0) {
                    //redraw the map
                    map.clear();
                    markersMap.clear();
                    //redraw the whole newsfeed
                    for (TimestampedObject timestampedObject : newsfeedAdapter.getItems()) {
                        if (timestampedObject.getType() == TimestampedObject.TOUR_CARD) {
                            Tour tour = (Tour) timestampedObject;
                            if (currentTourId == tour.getId()) {
                                continue;
                            }
                            drawNearbyTour(tour, false);
                        } else if (timestampedObject.getType() == TimestampedObject.ENTOURAGE_CARD) {
                            drawNearbyEntourage((Entourage) timestampedObject, false);
                        }
                    }
                    //redraw the current ongoing tour, if any
                    if (tourService != null && currentTourId != -1) {
                        PolylineOptions line = new PolylineOptions();
                        for (Polyline polyline : currentTourLines) {
                            line.addAll(polyline.getPoints());
                        }
                        line.zIndex(2f);
                        line.width(15);
                        line.color(color);
                        map.addPolyline(line);

                        Tour currentTour = tourService.getCurrentTour();
                        if (currentTour != null) {
                            if (currentTour.getEncounters() != null) {
                                for (Encounter encounter : currentTour.getEncounters()) {
                                    presenter.loadEncounterOnMap(encounter);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (newsfeedAdapter.getItemCount() == 0) {
            hideToursList();
        }
        else if (previousToursCount == 0) {
            showToursList();
        }
        /*
        if (newsfeedAdapter.getItemCount() > 0) {
            toursListView.scrollToPosition(0);
        }
        */

        pagination.isLoading = false;
        pagination.isRefreshing = false;
    }

    // ----------------------------------
    // CLICK CALLBACKS
    // ----------------------------------

    @OnClick(R.id.fragment_map_gps_layout)
    void displayGeolocationPreferences() {
        if (getActivity() != null) {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        }
    }

    @OnClick(R.id.fragment_map_follow_button)
    void onFollowGeolocation() {
        Location currentLocation = EntourageLocation.getInstance().getCurrentLocation();
        if (currentLocation != null) {
            centerMap(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
        }
        isFollowing = true;
    }

    @OnClick(R.id.launcher_tour_go)
    void onStartNewTour() {
        buttonLaunchTour.setEnabled(false);
        TourTransportMode tourTransportMode = TourTransportMode.findByRessourceId(radioGroupTransportMode.getCheckedRadioButtonId());
        TourType tourType = TourType.findByRessourceId(radioGroupType.getCheckedRadioButtonId());
        startTour(tourTransportMode.getName(), tourType.getName());
        FlurryAgent.logEvent(Constants.EVENT_START_TOUR);
    }

    @OnClick(R.id.tour_stop_button)
    public void onStartStopConfirmation() {
        pauseTour();
        if (getActivity() != null) {
            launchConfirmationActivity();
        }
    }

    @OnClick(R.id.map_longclick_button_create_encounter)
    public void onAddEncounter() {
        if (getActivity() != null) {
            mapLongClickView.setVisibility(View.GONE);
            if (mapOptionsMenu.isOpened()) {
                mapOptionsMenu.toggle(false);
            }
            Intent intent = new Intent(getActivity(), CreateEncounterActivity.class);
            saveCameraPosition();
            Bundle args = new Bundle();
            args.putLong(CreateEncounterActivity.BUNDLE_KEY_TOUR_ID, currentTourId);
            if (longTapCoordinates != null) {
                //if ongoing tour, show only if the point is in the current tour
                if (tourService != null && tourService.isRunning()) {
                    if (!tourService.isLocationInTour(longTapCoordinates)) {
                        longTapCoordinates = null;
                        mapOptionsMenu.setVisibility(View.VISIBLE);
                        tourStopButton.setVisibility(View.VISIBLE);
                        Toast.makeText(getActivity().getApplicationContext(), R.string.tour_encounter_too_far, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                args.putDouble(CreateEncounterActivity.BUNDLE_KEY_LATITUDE, longTapCoordinates.latitude);
                args.putDouble(CreateEncounterActivity.BUNDLE_KEY_LONGITUDE, longTapCoordinates.longitude);
                longTapCoordinates = null;
            }
            else {
                args.putDouble(CreateEncounterActivity.BUNDLE_KEY_LATITUDE, EntourageLocation.getInstance().getLastCameraPosition().target.latitude);
                args.putDouble(CreateEncounterActivity.BUNDLE_KEY_LONGITUDE, EntourageLocation.getInstance().getLastCameraPosition().target.longitude);
            }
            intent.putExtras(args);
            //startActivityForResult(intent, Constants.REQUEST_CREATE_ENCOUNTER);
            startActivity(intent);
        }
    }

    @OnClick(R.id.map_display_type_list)
    public void onDisplayTypeList() {
        showToursList();
    }

    @OnClick(R.id.map_longclick_button_entourage_demand)
    protected void onCreateEntourageDemand() {
        createEntourage(Entourage.TYPE_DEMAND);
    }

    @OnClick(R.id.map_longclick_button_entourage_contribution)
    protected void onCreateEntourageContribution() {
        createEntourage(Entourage.TYPE_CONTRIBUTION);
    }

    @OnClick(R.id.fragment_map_filter_button)
    protected void onShowFilter() {
        MapFilterFragment mapFilterFragment = MapFilterFragment.newInstance(tourService != null ? tourService.isRunning() : false);
        mapFilterFragment.show(getFragmentManager(), MapFilterFragment.TAG);
    }

    // ----------------------------------
    // Map Options handler
    // ----------------------------------

    private void initializeFloatingMenu() {
        mapOptionsMenu = ((DrawerActivity)getActivity()).mapOptionsMenu;
        mapOptionsMenu.setClosedOnTouchOutside(true);
        updateFloatingMenuOptions();
    }

    private void updateFloatingMenuOptions() {
        if (tourService != null && tourService.isRunning()) {
            mapOptionsMenu.findViewById(R.id.button_add_tour_encounter).setVisibility(View.VISIBLE);
            mapOptionsMenu.findViewById(R.id.button_start_tour_launcher).setVisibility(View.GONE);
        }
        else {
            User me = EntourageApplication.me(getActivity());
            boolean isPro = ( me != null ? me.isPro() : true );

            mapOptionsMenu.findViewById(R.id.button_add_tour_encounter).setVisibility(View.GONE);
            mapOptionsMenu.findViewById(R.id.button_start_tour_launcher).setVisibility(isPro ? View.VISIBLE : View.GONE);
        }
    }

    @OnClick(R.id.map_longclick_button_start_tour_launcher)
    public void onStartTourLauncher() {
        if (tourService != null) {
            if (!tourService.isRunning()) {
                FlurryAgent.logEvent(Constants.EVENT_OPEN_TOUR_LAUNCHER_FROM_MAP);
                if (mapOptionsMenu.isOpened()) {
                    mapOptionsMenu.toggle(false);
                }
                mapOptionsMenu.setVisibility(View.GONE);
                mapLongClickView.setVisibility(View.GONE);
                mapLauncherLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    // ----------------------------------
    // Long clicks on map handler
    // ----------------------------------

    private void showLongClickOnMapOptions(LatLng latLng) {
        //only show when map is in full screen and not visible
        if (toursListView.getVisibility() == View.VISIBLE || mapLongClickView.getVisibility() == View.VISIBLE) {
            return;
        }
        //save the tap coordinates
        longTapCoordinates = latLng;
        //hide the FAB menu
        mapOptionsMenu.setVisibility(View.GONE);
        tourStopButton.setVisibility(View.GONE);
        //get the click point
        Point clickPoint = map.getProjection().toScreenLocation(latLng);
        //update the visible buttons
        boolean isTourRunning = tourService != null && tourService.isRunning();
        User me = EntourageApplication.me(getActivity());
        boolean isPro = ( me != null ? me.isPro() : true );
        mapLongClickButtonsView.findViewById(R.id.map_longclick_button_start_tour_launcher).setVisibility( isTourRunning ? View.INVISIBLE : (isPro ? View.VISIBLE : View.GONE) );
        mapLongClickButtonsView.findViewById(R.id.map_longclick_button_create_encounter).setVisibility(isTourRunning?View.VISIBLE:View.GONE);
        if (!isPro) {
            ImageView entourageContribution = (ImageView)mapLongClickButtonsView.findViewById(R.id.map_longclick_button_entourage_contribution);
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) entourageContribution.getLayoutParams();
            layoutParams.setMargins(10, 0, 0, 0);
            entourageContribution.setLayoutParams(layoutParams);
        }
        mapLongClickButtonsView.requestLayout();
        //adjust the buttons holder layout
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point screenSize = new Point();
        display.getSize(screenSize);
        mapLongClickButtonsView.measure(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        int bW = mapLongClickButtonsView.getMeasuredWidth();
        int bH = mapLongClickButtonsView.getMeasuredHeight();
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mapLongClickButtonsView.getLayoutParams();
        int marginLeft = clickPoint.x - bW/2;
        if (marginLeft + bW > screenSize.x) {
            marginLeft -= bW/2;
        }
        if (marginLeft < 0) marginLeft = 0;
        int marginTop = clickPoint.y - bH;
        if (marginTop < 0) marginTop = clickPoint.y;
        lp.setMargins(marginLeft, marginTop, 0, 0);
        mapLongClickButtonsView.setLayoutParams(lp);
        //show the view
        mapLongClickView.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.layout_map_longclick)
    void hideLongClickView() {
        onBackPressed();
    }

    // ----------------------------------
    // PRIVATE METHODS (lifecycle)
    // ----------------------------------

    private void checkPermission() {
        if (getActivity() != null) {
            if (PermissionChecker.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.map_permission_title)
                            .setMessage(R.string.map_permission_description)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    requestPermissions(new String[]{ Manifest.permission.ACCESS_FINE_LOCATION }, PERMISSIONS_REQUEST_LOCATION);
                                }
                            })
                            .setNegativeButton(R.string.map_permission_refuse, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(final DialogInterface dialog, final int i) {
                                    NoLocationPermissionFragment noLocationPermissionFragment = new NoLocationPermissionFragment();
                                    noLocationPermissionFragment.show(getActivity().getSupportFragmentManager(), "fragment_no_location_permission");
                                }
                            })
                            .show();
                } else {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_LOCATION);
                }
                return;
            }
        }
    }

    private void initializeMap() {
        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.fragment_map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final GoogleMap googleMap) {
                map = googleMap;
                if ((PermissionChecker.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) || (PermissionChecker.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                    googleMap.setMyLocationEnabled(true);
                }
                googleMap.getUiSettings().setMyLocationButtonEnabled(false);
                googleMap.getUiSettings().setMapToolbarEnabled(false);

                initializeMapZoom();
                setOnMarkerClickListener(presenter.getOnClickListener());
                map.setOnGroundOverlayClickListener(presenter.getOnGroundOverlayClickListener());

                googleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                    @Override
                    public void onCameraChange(CameraPosition cameraPosition) {
                        EntourageLocation.getInstance().saveCurrentCameraPosition(cameraPosition);
                        Location currentLocation = EntourageLocation.getInstance().getCurrentLocation();
                        Location newLocation = EntourageLocation.cameraPositionToLocation(null, cameraPosition);
                        float newZoom = cameraPosition.zoom;

                        if (tourService != null && (newZoom / previousCameraZoom >= ZOOM_REDRAW_LIMIT || newLocation.distanceTo(previousCameraLocation) >= REDRAW_LIMIT)) {
                            previousCameraZoom = newZoom;
                            previousCameraLocation = newLocation;
                            newsfeedAdapter.removeAll();
                            pagination = new EntouragePagination();
                            tourService.updateNewsfeed(pagination);
                            if (userHistory) {
                                tourService.updateUserHistory(userId, 1, 500);
                            }
                        }

                        if (isFollowing && currentLocation != null) {
                            if (currentLocation.distanceTo(newLocation) > 1) {
                                isFollowing = false;
                            }
                        }
                    }
                });

                googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng latLng) {
                        if (getActivity() != null) {
                            if (toursListView.getVisibility() == View.VISIBLE) {
                                hideToursList();
                            } else {
                                loaderSearchTours = ProgressDialog.show(getActivity(), getActivity().getString(R.string.loader_title_tour_search), getActivity().getString(R.string.button_loading), true);
                                loaderSearchTours.setCancelable(true);
                                tourService.searchToursFromPoint(latLng, userHistory, userId, 1, 500);
                            }
                        }
                    }
                });

                googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                    @Override
                    public void onMapLongClick(final LatLng latLng) {
                        if (getActivity() != null) {
                            showLongClickOnMapOptions(latLng);
                        }
                    }
                });

                googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                    @Override
                    public void onMapLoaded() {
                        isMapLoaded = true;
                        BusProvider.getInstance().post(new OnCheckIntentActionEvent());
                    }
                });
            }
        });
    }

    private void initializeToursListView() {
        if (newsfeedAdapter == null) {
            toursListView.setLayoutManager(new LinearLayoutManager(getContext()));
            newsfeedAdapter = new NewsfeedAdapter();
            toursListView.setAdapter(newsfeedAdapter);
        }
    }

    // ----------------------------------
    // PRIVATE METHODS (tours events)
    // ----------------------------------

    private Tour getCurrentTour() {
        return tourService != null ? tourService.getCurrentTour() : null;
    }

    private void startTour(String transportMode, String type) {
        if (tourService != null && !tourService.isRunning()) {
            color = getTrackColor(false, type, new Date());
            tourService.beginTreatment(transportMode, type);
        }
    }

    private void pauseTour() {
        if (tourService != null && tourService.isRunning()) {
            tourService.pauseTreatment();
        }
    }

    public void pauseTour(Tour tour) {
        if (tourService != null && tourService.isRunning()) {
            if (tourService.getCurrentTourId() == tour.getId()) {
                tourService.pauseTreatment();
            }
        }
    }

    public void saveOngoingTour() {
        if (tourService != null) {
            tourService.updateOngoingTour();
        }
    }

    private void resumeTour() {
        if (tourService.isRunning()) {
            tourService.resumeTreatment();
            //buttonStartLauncher.setVisibility(View.GONE);
            //mapPin.setVisibility(View.VISIBLE);
        }
    }

    private void resumeTour(Tour tour) {
        if (tourService != null) {
            if (tour != null && tourService.getCurrentTourId() == tour.getId() && tourService.isRunning()) {
                tourService.resumeTreatment();
            }
        }
    }

    public void stopTour(Tour tour) {
        if (getActivity() != null) {
            if (tourService != null) {
                if (tour != null && tourService.getCurrentTourId() != tour.getId()) {
                    loaderStop = ProgressDialog.show(getActivity(), getActivity().getString(R.string.loader_title_tour_finish), getActivity().getString(R.string.button_loading), true);
                    loaderStop.setCancelable(true);
                    FlurryAgent.logEvent(Constants.EVENT_STOP_TOUR);
                    tourService.stopOtherTour(tour);
                    return;
                }
                else {
                    if (tourService.isRunning()) {
                        loaderStop = ProgressDialog.show(getActivity(), getActivity().getString(R.string.loader_title_tour_finish), getActivity().getString(R.string.button_loading), true);
                        loaderStop.setCancelable(true);
                        tourService.endTreatment();
                        FlurryAgent.logEvent(Constants.EVENT_STOP_TOUR);
                    }
                }
            }
        }
    }

    public void freezeTour(Tour tour) {
        if (getActivity() != null) {
            if (tourService != null) {
                tourService.freezeTour(tour);
            }
        }
    }

    public void userStatusChanged(PushNotificationContent content, String status) {
        if (tourService != null) {
            TimestampedObject timestampedObject = null;
            if (content.isTourRelated()) {
                timestampedObject = newsfeedAdapter.findCard(TimestampedObject.TOUR_CARD, content.getJoinableId());
            }
            else if (content.isEntourageRelated()) {
                timestampedObject = newsfeedAdapter.findCard(TimestampedObject.ENTOURAGE_CARD, content.getJoinableId());
            }
            if (timestampedObject != null) {
                TourUser user = new TourUser();
                user.setUserId(userId);
                user.setStatus(status);
                tourService.notifyListenersUserStatusChanged(user, (BaseEntourage)timestampedObject);
            }
        }
    }

    public void removeUserFromNewsfeedCard(TimestampedObject card, int userId) {
        if (tourService != null) {
            if (card.getType() == TimestampedObject.TOUR_CARD) {
                tourService.removeUserFromTour((Tour)card, userId);
            }
            else if (card.getType() == TimestampedObject.ENTOURAGE_CARD) {
                //TODO Remove user from entourage
            }
        }
    }

    private void launchConfirmationActivity() {
        pauseTour();
        //buttonStartLauncher.setVisibility(View.GONE);
        Bundle args = new Bundle();
        args.putSerializable(Tour.KEY_TOUR, getCurrentTour());
        Intent confirmationIntent = new Intent(getActivity(), ConfirmationActivity.class);
        confirmationIntent.putExtras(args);
        getActivity().startActivity(confirmationIntent);
    }

    private void addEncounter(Encounter encounter) {
        tourService.addEncounter(encounter);
    }

    // ----------------------------------
    // PRIVATE METHODS (views)
    // ----------------------------------

    private List<Tour> removeRedundantTours(List<Tour> tours, boolean isHistory) {
        Iterator iteratorTours = tours.iterator();
        while (iteratorTours.hasNext()) {
            Tour tour = (Tour) iteratorTours.next();
            if (!isHistory) {
                Tour retrievedTour = (Tour)newsfeedAdapter.findCard(tour);
                if (retrievedTour.isSame(tour)) {
                    iteratorTours.remove();
                }
            } else {
                if (drawnUserHistory.containsKey(tour.getId())) {
                    iteratorTours.remove();
                }
            }
        }
        return tours;
    }

    private List<Newsfeed> removeRedundantNewsfeed(List<Newsfeed> newsfeedList, boolean isHistory) {
        Iterator iteratorNewsfeed = newsfeedList.iterator();
        while (iteratorNewsfeed.hasNext()) {
            Newsfeed newsfeed = (Newsfeed) iteratorNewsfeed.next();
            if (!isHistory) {
                Object card = newsfeed.getData();
                if (card == null || !(card instanceof TimestampedObject)) {
                    iteratorNewsfeed.remove();
                }
                TimestampedObject retrievedCard = newsfeedAdapter.findCard((TimestampedObject)card);
                if (retrievedCard != null) {
                    if (Tour.NEWSFEED_TYPE.equals(newsfeed.getType())) {
                        if (((Tour)retrievedCard).isSame((Tour)card)) {
                            iteratorNewsfeed.remove();
                        }
                    }
                    else if (Entourage.NEWSFEED_TYPE.equals(newsfeed.getType())) {
                        if (((Entourage)retrievedCard).isSame((Entourage) card)) {
                            iteratorNewsfeed.remove();
                        }
                    }
                }
            } else {
                if (drawnUserHistory.containsKey(newsfeed.getId())) {
                    iteratorNewsfeed.remove();
                }
            }
        }
        return newsfeedList;
    }

    private List<Tour> removeRecentTours(List<Tour> tours) {
        Iterator iteratorTours = tours.iterator();
        while (iteratorTours.hasNext()) {
            Tour tour = (Tour) iteratorTours.next();
            if (newsfeedAdapter.findCard(tour) != null) {
                iteratorTours.remove();
            }
        }
        return tours;
    }

    private int getTrackColor(boolean isHistory, String type, Date date) {
        int color = Color.GRAY;
        if (TourType.MEDICAL.getName().equals(type)) {
            color = Color.RED;
        }
        else if (TourType.ALIMENTARY.getName().equals(type)) {
            color = Color.GREEN;
        }
        else if (TourType.BARE_HANDS.getName().equals(type)) {
            color = Color.BLUE;
        }
        if (!isToday(date)) {
            color = getTransparentColor(color);
        }
        if (isHistory) {
            if (!userHistory) {
                return Color.argb(0, Color.red(color), Color.green(color), Color.blue(color));
            } else {
                return Color.argb(255, Color.red(color), Color.green(color), Color.blue(color));
            }
        }
        return color;
    }

    public static int getTransparentColor(int color) {
        return Color.argb(100, Color.red(color), Color.green(color), Color.blue(color));
    }

    private void hideUserHistory() {
        Iterator iteratorTours = retrievedHistory.entrySet().iterator();
        while (iteratorTours.hasNext()) {
            Map.Entry pair = (Map.Entry) iteratorTours.next();
            Tour tour = (Tour) pair.getValue();
            Polyline line = drawnUserHistory.get(tour.getId());
            line.setColor(getTrackColor(true, tour.getTourType(), tour.getStartTime()));
        }
    }

    private void showUserHistory() {
        Iterator iteratorLines = drawnUserHistory.entrySet().iterator();
        while (iteratorLines.hasNext()) {
            Map.Entry pair = (Map.Entry) iteratorLines.next();
            Tour tour = retrievedHistory.get(pair.getKey());
            Polyline line = (Polyline) pair.getValue();
            line.setColor(getTrackColor(true, tour.getTourType(), tour.getStartTime()));
        }
    }

    public static boolean isToday(Date date) {
        if (date == null) return false;
        Date today = new Date();
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(today);
        cal2.setTime(date);
        return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR));
    }

    private void centerMap(LatLng latLng) {
        CameraPosition cameraPosition = new CameraPosition(latLng, EntourageLocation.getInstance().getLastCameraPosition().zoom, 0, 0);
        centerMap(cameraPosition);
    }

    private void centerMap(CameraPosition cameraPosition) {
        if(map != null && isFollowing) {
            map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            saveCameraPosition();
        }
    }

    public void saveCameraPosition() {
        if(map != null) {
            EntourageLocation.getInstance().saveLastCameraPosition(map.getCameraPosition());
        }
    }

    private void addCurrentTourEncounters() {
        List<Encounter> encounters = tourService.getCurrentTour().getEncounters();
        if (!encounters.isEmpty()) {
            for (Encounter encounter : encounters) {
                presenter.loadEncounterOnMap(encounter);
            }
        }
    }

    private void drawCurrentLocation(LatLng location) {
        if (previousCoordinates != null) {
            PolylineOptions line = new PolylineOptions();
            line.add(previousCoordinates, location);
            line.zIndex(2f);
            line.width(15);
            line.color(color);
            currentTourLines.add(map.addPolyline(line));
        }
        previousCoordinates = location;
    }

    private void drawCurrentTour(List<TourPoint> pointsToDraw, String tourType, Date startDate) {
        if (!pointsToDraw.isEmpty()) {
            PolylineOptions line = new PolylineOptions();
            color = getTrackColor(true, tourType, startDate);
            line.zIndex(2f);
            line.width(15);
            line.color(color);
            for (TourPoint tourPoint : pointsToDraw) {
                line.add(tourPoint.getLocation());
            }
            currentTourLines.add(map.addPolyline(line));
        }
    }

    private void drawNearbyTour(Tour tour, boolean isHistory) {
        if (map != null && drawnToursMap != null && drawnUserHistory != null && tour != null && !tour.getTourPoints().isEmpty()) {
            PolylineOptions line = new PolylineOptions();
            if (isToday(tour.getStartTime())) {
                line.zIndex(1f);
            } else {
                line.zIndex(0f);
            }
            line.width(15);
            line.color(getTrackColor(isHistory, tour.getTourType(), tour.getStartTime()));
            for (TourPoint tourPoint : tour.getTourPoints()) {
                line.add(tourPoint.getLocation());
            }
            DrawerActivity activity = null;
            if (getActivity() instanceof DrawerActivity) {
                activity = (DrawerActivity) getActivity();
                tour.setBadgeCount(activity.getPushNotificationsCountForTour(tour.getId()));
            }
            boolean existingTour = drawnToursMap.containsKey(tour.getId());
            if (isHistory) {
                retrievedHistory.put(tour.getId(), tour);
                drawnUserHistory.put(tour.getId(), map.addPolyline(line));
            } else {
                drawnToursMap.put(tour.getId(), map.addPolyline(line));
                //addTourCard(tour);
            }
            if (tour.getTourStatus() == null) {
                tour.setTourStatus(Tour.TOUR_CLOSED);
            }
            if (Tour.TOUR_ON_GOING.equalsIgnoreCase(tour.getTourStatus()) && !existingTour) {
                addTourHead(tour);
            }
        }
    }

    private void drawNearbyEntourage(Entourage entourage, boolean isHistory) {
        if (map != null && markersMap != null && entourage != null) {
            if (entourage.getLocation() != null) {
                if (markersMap.get(entourage.hashString()) == null) {
                    LatLng position = entourage.getLocation().getLocation();
                    BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.heat_zone);
                    GroundOverlayOptions groundOverlayOptions = new GroundOverlayOptions()
                            .image(icon)
                            .position(position, ENTOURAGE_HEATMAP_SIZE, ENTOURAGE_HEATMAP_SIZE)
                            .clickable(true)
                            .anchor(0.5f, 0.5f);

                    markersMap.put(entourage.hashString(), map.addGroundOverlay(groundOverlayOptions));
                    if (presenter != null) {
                        presenter.getOnGroundOverlayClickListener().addEntourageGroundOverlay(position, entourage);
                    }
                }
            }

            if (isHistory) {
//                retrievedHistory.put(tour.getId(), tour);
//                drawnUserHistory.put(tour.getId(), map.addPolyline(line));
            } else {
                //addNewsfeedCard(entourage);
            }
        }
    }

    private void drawNearbyNewsfeed(Newsfeed newsfeed, boolean isHistory) {
        if (Tour.NEWSFEED_TYPE.equals(newsfeed.getType())) {
            if (currentTourId != newsfeed.getId()) {
                drawNearbyTour((Tour) newsfeed.getData(), isHistory);
            }
        }
        else if (Entourage.NEWSFEED_TYPE.equals(newsfeed.getType())) {
            drawNearbyEntourage((Entourage)newsfeed.getData(), isHistory);
        }
    }

    private void addTourCard(Tour tour) {
        if (newsfeedAdapter.findCard(tour) != null) {
            newsfeedAdapter.updateCard(tour);
        } else {
            newsfeedAdapter.addCardInfo(tour);
        }
    }

    private void addNewsfeedCard(TimestampedObject card) {
        if (newsfeedAdapter.findCard(card) != null) {
            newsfeedAdapter.updateCard(card);
        } else {
            newsfeedAdapter.addCardInfoBeforeTimestamp(card);
        }
    }

    private void updateNewsfeedJoinStatus(TimestampedObject timestampedObject) {
        newsfeedAdapter.updateCard(timestampedObject);
    }

    private void addTourHead(Tour tour) {
        if (displayedTourHeads >= MAX_TOUR_HEADS_DISPLAYED) {
            return;
        }
        displayedTourHeads++;
        TourPoint lastPoint = tour.getTourPoints().get(tour.getTourPoints().size() - 1);
        double latitude = lastPoint.getLatitude();
        double longitude = lastPoint.getLongitude();
        LatLng position = new LatLng(latitude, longitude);

        BitmapDescriptor icon = null;
        /*
        if (tour.getTourVehicleType().equals(TourTransportMode.FEET.getName())) {
            icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_feet_active);
        }
        else if (tour.getTourVehicleType().equals(TourTransportMode.CAR.getName())) {
            icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_car_active);
        }
        */
        IconGenerator iconGenerator = new IconGenerator(getContext());
        iconGenerator.setTextAppearance(R.style.OngoingTourMarker);
        icon = BitmapDescriptorFactory.fromBitmap(iconGenerator.makeIcon(tour.getOrganizationName()));

        MarkerOptions markerOptions = new MarkerOptions()
                .position(position)
                .icon(icon)
                .anchor(0.5f, 1.0f);

        if (map != null) {
            markersMap.put(tour.hashString(), map.addMarker(markerOptions));
            presenter.getOnClickListener().addTourMarker(position, tour);
        }
    }

    private void clearAll() {
        map.clear();

        currentTourLines.clear();
        drawnToursMap.clear();
        drawnUserHistory.clear();

        displayedTourHeads = 0;

        newsfeedAdapter.removeAll();
        pagination = new EntouragePagination(Constants.ITEMS_PER_PAGE);

        previousCoordinates = null;
    }

    private void hideToursList() {
        if (toursListView.getVisibility() == View.GONE) {
            return;
        }
        toursListView.setVisibility(View.GONE);

        mapDisplayTypeRadioGroup.check(R.id.map_display_type_carte);
        mapDisplayTypeRadioGroup.setVisibility(View.VISIBLE);

        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) layoutMapMain.getLayoutParams();
        originalMapLayoutWeight = lp.weight;
//        lp.weight = layoutMain.getWeightSum();
//        layoutMapMain.setLayoutParams(lp);
//        layoutMain.forceLayout();

        final float targetHeight = layoutMain.getWeightSum();
        ValueAnimator anim = ValueAnimator.ofFloat(originalMapLayoutWeight, targetHeight);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float val = (Float) valueAnimator.getAnimatedValue();
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) layoutMapMain.getLayoutParams();
                layoutParams.weight = val;
                layoutMapMain.setLayoutParams(layoutParams);
                layoutMain.forceLayout();
            }


        });
        anim.start();

    }

    private void showToursList() {
        if (toursListView.getVisibility() == View.VISIBLE) {
            return;
        }
        toursListView.setVisibility(View.VISIBLE);

        mapDisplayTypeRadioGroup.setVisibility(View.GONE);

        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) layoutMapMain.getLayoutParams();
//        lp.weight = originalMapLayoutWeight;
//        layoutMapMain.setLayoutParams(lp);

        float targetHeight = layoutMain.getWeightSum();
        ValueAnimator anim = ValueAnimator.ofFloat(lp.weight, originalMapLayoutWeight);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float val = (Float) valueAnimator.getAnimatedValue();
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) layoutMapMain.getLayoutParams();
                layoutParams.weight = val;
                layoutMapMain.setLayoutParams(layoutParams);
                layoutMain.forceLayout();
            }


        });
        anim.start();
    }

    // ----------------------------------
    // Push handling
    // ----------------------------------

    public void onPushNotificationReceived(Message message) {
        //refresh the newsfeed
        if (tourService != null) {
            tourService.updateNewsfeed(pagination);
        }
        //update the badge count on tour card
        PushNotificationContent content = message.getContent();
        if (content == null) return;
        if (newsfeedAdapter == null) return;
        long joinableId = content.getJoinableId();
        if (content.isTourRelated()) {
            Tour tour = (Tour) newsfeedAdapter.findCard(TimestampedObject.TOUR_CARD, joinableId);
            if (tour == null) return;
            tour.increaseBadgeCount();
            newsfeedAdapter.updateCard(tour);
        }
        else if (content.isEntourageRelated()) {
            Entourage entourage = (Entourage) newsfeedAdapter.findCard(TimestampedObject.ENTOURAGE_CARD, joinableId);
            if (entourage == null) return;
            entourage.increaseBadgeCount();
            newsfeedAdapter.updateCard(entourage);
        }
    }

    public void onPushNotificationConsumedForTour(long tourId) {
        if (newsfeedAdapter == null) return;
        Tour tour = (Tour)newsfeedAdapter.findCard(TimestampedObject.TOUR_CARD, tourId);
        if (tour == null) return;
        tour.setBadgeCount(0);
        newsfeedAdapter.updateCard(tour);
    }

    // ----------------------------------
    // Refresh tours timer handling
    // ----------------------------------

    private void timerStart() {
        //create the timer
        refreshToursTimer = new Timer();
        //create the task
        refreshToursTimerTask = new TimerTask() {
            @Override
            public void run() {
                refreshToursHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (tourService != null) {
                            EntouragePagination firstPageRefresh = new EntouragePagination();
                            firstPageRefresh.isRefreshing = true;
                            tourService.updateNewsfeed(firstPageRefresh);
                        }
                    }
                });
            }
        };
        //schedule the timer
        refreshToursTimer.schedule(refreshToursTimerTask, 0, REFRESH_TOURS_INTERVAL);
    }

    private void timerStop() {
        if (refreshToursTimer != null) {
            refreshToursTimer.cancel();
            refreshToursTimer = null;
        }
    }

    // ----------------------------------
    // INNER CLASSES
    // ----------------------------------

    private class ServiceConnection implements android.content.ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (getActivity() != null) {
                tourService = ((TourService.LocalBinder) service).getService();
                tourService.register(MapEntourageFragment.this);

                boolean isRunning = tourService != null && tourService.isRunning();
                if (isRunning) {
                    updateFloatingMenuOptions();

                    currentTourId = tourService.getCurrentTourId();
                    //mapPin.setVisibility(View.VISIBLE);

                    addCurrentTourEncounters();
                }

                tourService.updateNewsfeed(pagination);
                if (userHistory) {
                    tourService.updateUserHistory(userId, 1, 500);
                }
            }
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            tourService.unregister(MapEntourageFragment.this);
            tourService = null;
        }
    }

    private class OnScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrolled(final RecyclerView recyclerView, final int dx, final int dy) {

            scrollDeltaY += dy;
            if (dy > 0 && scrollDeltaY > MAX_SCROLL_DELTA_Y) {
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                int position = linearLayoutManager.findLastVisibleItemPosition();
                if (position == recyclerView.getAdapter().getItemCount()-1) {
                    if (tourService != null) {
                        tourService.updateNewsfeed(pagination);
                    }
                }

                scrollDeltaY = 0;
            }
        }
        @Override
        public void onScrollStateChanged(final RecyclerView recyclerView, final int newState) {
        }
    }
}
