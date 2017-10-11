package social.entourage.android;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.IdRes;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.crashlytics.android.Crashlytics;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.HashSet;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import social.entourage.android.about.AboutFragment;
import social.entourage.android.api.model.Message;
import social.entourage.android.api.model.Partner;
import social.entourage.android.api.model.PushNotificationContent;
import social.entourage.android.api.model.TimestampedObject;
import social.entourage.android.api.model.User;
import social.entourage.android.api.model.map.Encounter;
import social.entourage.android.api.model.map.Entourage;
import social.entourage.android.api.model.map.FeedItem;
import social.entourage.android.api.model.map.Tour;
import social.entourage.android.api.tape.Events.OnCheckIntentActionEvent;
import social.entourage.android.api.tape.Events.OnFeedItemCloseRequestEvent;
import social.entourage.android.api.tape.Events.OnFeedItemInfoViewRequestedEvent;
import social.entourage.android.api.tape.Events.OnGCMTokenObtainedEvent;
import social.entourage.android.api.tape.Events.OnPushNotificationReceived;
import social.entourage.android.api.tape.Events.OnTourEncounterViewRequestedEvent;
import social.entourage.android.api.tape.Events.OnUnauthorizedEvent;
import social.entourage.android.api.tape.Events.OnUserActEvent;
import social.entourage.android.api.tape.Events.OnUserInfoUpdatedEvent;
import social.entourage.android.api.tape.Events.OnUserViewRequestedEvent;
import social.entourage.android.authentication.AuthenticationController;
import social.entourage.android.authentication.login.LoginActivity;
import social.entourage.android.badge.BadgeView;
import social.entourage.android.base.AmazonS3Utils;
import social.entourage.android.guide.GuideMapEntourageFragment;
import social.entourage.android.map.MapEntourageFragment;
import social.entourage.android.map.choice.ChoiceFragment;
import social.entourage.android.map.confirmation.ConfirmationActivity;
import social.entourage.android.map.encounter.CreateEncounterActivity;
import social.entourage.android.map.encounter.EncounterDisclaimerFragment;
import social.entourage.android.map.encounter.ReadEncounterActivity;
import social.entourage.android.map.entourage.EntourageDisclaimerFragment;
import social.entourage.android.map.entourage.my.MyEntouragesFragment;
import social.entourage.android.map.tour.TourService;
import social.entourage.android.map.tour.information.TourInformationFragment;
import social.entourage.android.map.tour.my.MyToursFragment;
import social.entourage.android.message.push.PushNotificationService;
import social.entourage.android.message.push.RegisterGCMService;
import social.entourage.android.newsfeed.FeedItemOptionsFragment;
import social.entourage.android.sidemenu.SideMenuItemView;
import social.entourage.android.tools.BusProvider;
import social.entourage.android.tools.CropCircleTransformation;
import social.entourage.android.user.UserFragment;
import social.entourage.android.user.edit.UserEditFragment;
import social.entourage.android.user.edit.photo.PhotoChooseInterface;
import social.entourage.android.user.edit.photo.PhotoChooseSourceFragment;
import social.entourage.android.user.edit.photo.PhotoEditFragment;
import social.entourage.android.view.PartnerLogoImageView;

public class DrawerActivity extends EntourageSecuredActivity
    implements TourInformationFragment.OnTourInformationFragmentFinish,
    ChoiceFragment.OnChoiceFragmentFinish,
    MyToursFragment.OnFragmentInteractionListener,
    EntourageDisclaimerFragment.OnFragmentInteractionListener,
    EncounterDisclaimerFragment.OnFragmentInteractionListener,
    PhotoChooseInterface {

    // ----------------------------------
    // CONSTANTS
    // ----------------------------------

    // ----------------------------------
    // ATTRIBUTES
    // ----------------------------------

    @Inject
    DrawerPresenter presenter;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.drawer_layout)
    DrawerLayout drawerLayout;

    @BindView(R.id.navigation_view)
    NavigationView navigationView;

    @BindView(R.id.content_view)
    View contentView;

    @BindView(R.id.drawer_header_user_name)
    TextView userName;

    @BindView(R.id.drawer_header_user_photo)
    ImageView userPhoto;

    @BindView(R.id.drawer_header_user_partner_logo)
    PartnerLogoImageView userPartnerLogo;

    @BindView(R.id.drawer_header_edit_profile)
    TextView userEditProfileTextView;

    @BindView(R.id.toolbar_discussion)
    BadgeView discussionBadgeView;

    @BindView(R.id.map_fab_menu)
    public FloatingActionMenu mapOptionsMenu;

    private Fragment mainFragment;
    private MapEntourageFragment mapEntourageFragment;
    private GuideMapEntourageFragment guideMapEntourageFragment;
    private UserFragment userFragment;

    private SharedPreferences gcmSharedPreferences;
    private String intentAction;
    private Tour intentTour;

    @IdRes int selectedSidemenuAction;

    // ----------------------------------
    // LIFECYCLE
    // ----------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer);
        ButterKnife.bind(this);

        configureToolbar();
        configureNavigationItem();

        selectItem(R.id.action_tours);

        gcmSharedPreferences = getApplicationContext().getSharedPreferences(RegisterGCMService.SHARED_PREFERENCES_FILE_GCM, Context.MODE_PRIVATE);

        intentAction = getIntent().getAction();

        User user = getAuthenticationController().getUser();
        if (user != null) {
            userName.setText(user.getDisplayName());
            String avatarURL = user.getAvatarURL();
            if (avatarURL != null) {
                Picasso.with(this)
                    .load(Uri.parse(avatarURL))
                    .placeholder(R.drawable.ic_user_photo_small)
                    .transform(new CropCircleTransformation())
                    .into(userPhoto);
            } else {
                userPhoto.setImageResource(R.drawable.ic_user_photo_small);
            }
            // Show partner logo
            String partnerURL = null;
            Partner partner = user.getPartner();
            if (partner != null) {
                partnerURL = partner.getSmallLogoUrl();
            }
            if (partnerURL != null) {
                Picasso.with(this)
                        .load(Uri.parse(partnerURL))
                        .placeholder(R.drawable.partner_placeholder)
                        .transform(new CropCircleTransformation())
                        .into(userPartnerLogo);
            } else {
                userPartnerLogo.setImageDrawable(null);
            }

            //refresh the user info from the server
            Location location = EntourageLocation.getInstance().getCurrentLocation();
            presenter.updateUser(null, null, null, (location != null ? location : null));
            //initialize the push notifications
            initializePushNotifications();

            Crashlytics.setUserIdentifier(String.valueOf(user.getId()));
            Crashlytics.setUserName(user.getDisplayName());
        }
    }

    @Override
    protected void setupComponent(EntourageComponent entourageComponent) {
        DaggerDrawerComponent.builder()
            .entourageComponent(entourageComponent)
            .drawerModule(new DrawerModule(this))
            .build()
            .inject(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                EntourageEvents.logEvent(Constants.EVENT_FEED_MENU);
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        this.setIntent(intent);
        getIntentAction(intent);
        if (mainFragment != null) {
            switchToMapFragment();
            if (intentAction != null) {
                switch (intentAction) {
                    case ConfirmationActivity.KEY_RESUME_TOUR:
                        break;
                    case ConfirmationActivity.KEY_END_TOUR:
                        break;
                    case TourService.KEY_NOTIFICATION_STOP_TOUR:
                    case TourService.KEY_NOTIFICATION_PAUSE_TOUR:
                        sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                        break;
                    case TourService.KEY_LOCATION_PROVIDER_DISABLED:
                        displayLocationProviderDisabledAlert();
                        sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                    default:
                        break;
                }
            }
            sendMapFragmentExtras();
        }
    }

    @Override
    public void onBackPressed() {
        if (mainFragment instanceof BackPressable) {
            BackPressable backPressable = (BackPressable) mainFragment;
            if (!backPressable.onBackPressed()) {
                finish();
            }
        } else {
            finish();
        }
    }

    @Override
    protected void onStart() {
        BusProvider.getInstance().register(this);
        presenter.checkForUpdate();

        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        highlightCurrentMenuItem();

        String action = getIntent().getAction();
        if (action != null) {
            if (TourService.KEY_LOCATION_PROVIDER_DISABLED.equals(action)) {
                displayLocationProviderDisabledAlert();
                sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            } else if (TourService.KEY_NOTIFICATION_PAUSE_TOUR.equals(action) || TourService.KEY_NOTIFICATION_STOP_TOUR.equals(action)) {
                sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            }
        }
        EntourageApplication.get().getMixpanel().getPeople().showNotificationIfAvailable(this);

        refreshBadgeCount();
    }

    @Override
    protected void onStop() {
        BusProvider.getInstance().unregister(this);
        super.onStop();
    }

    // ----------------------------------
    // PRIVATE METHODS
    // ----------------------------------

    private void displayLocationProviderDisabledAlert() {
        new AlertDialog.Builder(this)
            .setMessage(getString(R.string.error_dialog_disabled))
            .setCancelable(false)
            .setPositiveButton("Oui", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                }
            })
            .setNegativeButton("Non", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            })
            .create()
            .show();
    }

    private void sendMapFragmentExtras() {
        AuthenticationController authenticationController = getAuthenticationController();
        if (authenticationController == null || authenticationController.getUser() == null) return;
        int userId = authenticationController.getUser().getId();
        boolean choice = authenticationController.isUserToursOnly();
        mapEntourageFragment.onNotificationExtras(userId, choice);
    }

    private void getIntentAction(Intent intent) {
        String action = intent.getAction();
        Bundle args = intent.getExtras();
        if (args != null) {
            intentTour = (Tour) args.getSerializable(Tour.KEY_TOUR);
            if (args.getBoolean(ConfirmationActivity.KEY_RESUME_TOUR, false)) {
                intentAction = ConfirmationActivity.KEY_RESUME_TOUR;
            } else if (args.getBoolean(ConfirmationActivity.KEY_END_TOUR, false)) {
                intentAction = ConfirmationActivity.KEY_END_TOUR;
            } else if (PushNotificationContent.TYPE_NEW_CHAT_MESSAGE.equals(action)) {
                intentAction = PushNotificationContent.TYPE_NEW_CHAT_MESSAGE;
            } else if (PushNotificationContent.TYPE_JOIN_REQUEST_ACCEPTED.equals(action)) {
                intentAction = PushNotificationContent.TYPE_JOIN_REQUEST_ACCEPTED;
            } else if (PushNotificationContent.TYPE_ENTOURAGE_INVITATION.equals(action)) {
                intentAction = PushNotificationContent.TYPE_ENTOURAGE_INVITATION;
            } else if (PushNotificationContent.TYPE_INVITATION_STATUS.equals(action)) {
                intentAction = PushNotificationContent.TYPE_INVITATION_STATUS;
            }
        } else if (action != null) {
            getIntent().setAction(null);
            if (TourService.KEY_LOCATION_PROVIDER_DISABLED.equals(action)) {
                intentAction = TourService.KEY_LOCATION_PROVIDER_DISABLED;
            } else if (TourService.KEY_NOTIFICATION_PAUSE_TOUR.equals(action)) {
                intentAction = TourService.KEY_NOTIFICATION_PAUSE_TOUR;
            } else if (TourService.KEY_NOTIFICATION_STOP_TOUR.equals(action)) {
                intentAction = TourService.KEY_NOTIFICATION_STOP_TOUR;
            } else if (PushNotificationContent.TYPE_NEW_CHAT_MESSAGE.equals(action)) {
                intentAction = PushNotificationContent.TYPE_NEW_CHAT_MESSAGE;
            } else if (PushNotificationContent.TYPE_JOIN_REQUEST_ACCEPTED.equals(action)) {
                intentAction = PushNotificationContent.TYPE_JOIN_REQUEST_ACCEPTED;
            } else if (PushNotificationContent.TYPE_ENTOURAGE_INVITATION.equals(action)) {
                intentAction = PushNotificationContent.TYPE_ENTOURAGE_INVITATION;
            } else if (PushNotificationContent.TYPE_INVITATION_STATUS.equals(action)) {
                intentAction = PushNotificationContent.TYPE_INVITATION_STATUS;
            }
        }
    }

    private void switchToMapFragment() {
        if (mainFragment instanceof MapEntourageFragment) {
            mapEntourageFragment = (MapEntourageFragment) mainFragment;
        } else {
            loadFragmentWithExtras();
        }
    }

    private void highlightCurrentMenuItem() {
        if (mainFragment instanceof MapEntourageFragment) {
            navigationView.setCheckedItem(R.id.action_tours);
        } else if (mainFragment instanceof GuideMapEntourageFragment) {
            navigationView.setCheckedItem(R.id.action_guide);
        } else if (mainFragment instanceof UserFragment) {
            navigationView.setCheckedItem(R.id.action_user);
        }
    }

    private void configureToolbar() {
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        discussionBadgeView = (BadgeView) toolbar.findViewById(R.id.toolbar_discussion);
        if (discussionBadgeView != null) {
            discussionBadgeView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    EntourageEvents.logEvent(Constants.EVENT_SCREEN_17_2);
                    presenter.displayMyEntourages();
                }
            });
        }

    }

    private void configureNavigationItem() {
        //make the navigation view full screen
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        DrawerLayout.LayoutParams params = (DrawerLayout.LayoutParams) navigationView.getLayoutParams();
        params.width = metrics.widthPixels;
        navigationView.setLayoutParams(params);

        //add listener to back button
        ImageView backView = (ImageView) navigationView.findViewById(R.id.drawer_header_back);
        backView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                drawerLayout.closeDrawers();
            }
        });

        //add navigationitemlistener
        drawerLayout.setDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                selectItem(selectedSidemenuAction);
            }
        });

        //add listener to user photo and name, that opens the user profile screen
        userPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                selectedSidemenuAction = R.id.action_user;
                drawerLayout.closeDrawers();
            }
        });
        userName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                selectedSidemenuAction = R.id.action_user;
                drawerLayout.closeDrawers();
            }
        });
        //add listener to modify profile text view
        userEditProfileTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                selectedSidemenuAction = R.id.action_edit_user;
                drawerLayout.closeDrawers();
            }
        });

        //add listeners to side menu items
        LinearLayout sideMenuItemsLayout = (LinearLayout) navigationView.findViewById(R.id.sidemenuitems_layout);
        if (sideMenuItemsLayout != null) {
            int itemsCount = sideMenuItemsLayout.getChildCount();
            for (int j = 0; j < itemsCount; j++) {
                View child = sideMenuItemsLayout.getChildAt(j);
                if (child instanceof SideMenuItemView) {
                    child.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            selectedSidemenuAction = v.getId();
                            drawerLayout.closeDrawers();
                        }
                    });
                }
            }
        }
    }

    public void selectItem(@IdRes int menuId) {
        if (menuId == 0) {
            return;
        }
        switch (menuId) {
            case R.id.action_tours:
                loadFragmentWithExtras();
                break;
            case R.id.action_guide:
                if (mainFragment instanceof MapEntourageFragment) {
                    showSolidarityGuide();
                    EntourageEvents.logEvent(Constants.EVENT_OPEN_GUIDE_FROM_SIDEMENU);
                }
                break;
            case R.id.action_user:
                EntourageEvents.logEvent(Constants.EVENT_MENU_TAP_MY_PROFILE);
                userFragment = (UserFragment) getSupportFragmentManager().findFragmentByTag(UserFragment.TAG);
                if (userFragment == null) {
                    userFragment = UserFragment.newInstance(getAuthenticationController().getUser().getId());
                }
                //loadFragment(userFragment, TAG_FRAGMENT_USER);
                userFragment.show(getSupportFragmentManager(), UserFragment.TAG);
                break;
            case R.id.action_edit_user:
                UserEditFragment fragment = new UserEditFragment();
                fragment.show(getSupportFragmentManager(), UserEditFragment.TAG);
                break;
            case R.id.action_logout:
                EntourageEvents.logEvent(Constants.EVENT_MENU_LOGOUT);
                if (mapEntourageFragment != null) {
                    mapEntourageFragment.saveOngoingTour();
                }
                logout();
                break;
            case R.id.action_settings:
                Toast.makeText(this, R.string.error_not_yet_implemented, Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_about:
                EntourageEvents.logEvent(Constants.EVENT_MENU_ABOUT);
                AboutFragment aboutFragment = new AboutFragment();
                aboutFragment.show(getSupportFragmentManager(), AboutFragment.TAG);
                break;
            case R.id.action_blog:
                EntourageEvents.logEvent(Constants.EVENT_MENU_BLOG);
                Intent blogIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.SCB_URL));
                try {
                    startActivity(blogIntent);
                } catch (Exception ex) {
                    Toast.makeText(this, R.string.no_browser_error, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.action_charte:
                EntourageEvents.logEvent(Constants.EVENT_MENU_CHART);
                boolean isPro = false;
                User me = getAuthenticationController().getUser();
                if (me != null) {
                    isPro = me.isPro();
                }
                Intent charteIntent;
                if (isPro) {
                    charteIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.disclaimer_link_pro)));
                } else {
                    charteIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.disclaimer_link_public)));
                }
                try {
                    startActivity(charteIntent);
                } catch (Exception ex) {
                    Toast.makeText(this, R.string.no_browser_error, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.action_goal:
                EntourageEvents.logEvent(Constants.EVENT_MENU_GOAL);
                Intent goalIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.GOAL_URL));
                try {
                    startActivity(goalIntent);
                } catch (Exception ex) {
                    Toast.makeText(this, R.string.no_browser_error, Toast.LENGTH_SHORT).show();
                }

                break;
            case R.id.action_faq:
                EntourageEvents.logEvent(Constants.EVENT_MENU_FAQ);
                Intent userGuideIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.FAQ_URL));
                try {
                    startActivity(userGuideIntent);
                } catch (Exception ex) {
                    Toast.makeText(this, R.string.no_browser_error, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.action_atd:
                EntourageEvents.logEvent(Constants.EVENT_MENU_ATD);
                Intent atdIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.ATD_URL));
                try {
                    startActivity(atdIntent);
                } catch (Exception ex) {
                    Toast.makeText(this, R.string.no_browser_error, Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                //Snackbar.make(contentView, getString(R.string.drawer_error, menuItem.getTitle()), Snackbar.LENGTH_LONG).show();
                Toast.makeText(this, R.string.error_not_yet_implemented, Toast.LENGTH_SHORT).show();
        }
        selectedSidemenuAction = 0;
    }

    private void loadFragment(Fragment newFragment, String tag) {
        mainFragment = newFragment;
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.main_fragment, mainFragment, tag);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commitAllowingStateLoss();
    }

    private void loadFragmentWithExtras() {
        mapEntourageFragment = (MapEntourageFragment) getSupportFragmentManager().findFragmentByTag(MapEntourageFragment.TAG);
        if (mapEntourageFragment == null) {
            mapEntourageFragment = new MapEntourageFragment();
        }
        loadFragment(mapEntourageFragment, MapEntourageFragment.TAG);
        if (getAuthenticationController().getUser() != null) {
            final int userId = getAuthenticationController().getUser().getId();
            final boolean choice = getAuthenticationController().isUserToursOnly();
            if (mainFragment instanceof MapEntourageFragment) {
                Handler handler = new Handler();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        MapEntourageFragment mapEntourageFragment = (MapEntourageFragment) mainFragment;
                        mapEntourageFragment.onNotificationExtras(userId, choice);
                    }
                });
            }
        }
    }

    private void showSolidarityGuide() {
        // Change the Guide Option text
        FloatingActionButton button = (FloatingActionButton) mapOptionsMenu.findViewById(R.id.button_poi_launcher);
        button.setLabelText(getString(R.string.map_poi_close_button));
        // Make the 'Propose POI' button visible
        FloatingActionButton proposePOIButton = (FloatingActionButton) mapOptionsMenu.findViewById(R.id.button_poi_propose);
        if (proposePOIButton != null) {
            proposePOIButton.setVisibility(View.VISIBLE);
        }
        // Hide the overlay
        if (mapOptionsMenu.isOpened()) {
            mapOptionsMenu.close(false);
        }
        // Inform the map that the guide will be shown
        if (mapEntourageFragment != null) {
            mapEntourageFragment.onGuideWillShow();
        }
        // Show the fragment
        guideMapEntourageFragment = (GuideMapEntourageFragment) getSupportFragmentManager().findFragmentByTag(GuideMapEntourageFragment.TAG);
        if (guideMapEntourageFragment == null) {
            guideMapEntourageFragment = new GuideMapEntourageFragment();
        }
        loadFragment(guideMapEntourageFragment, GuideMapEntourageFragment.TAG);
    }

    private void hideSolidarityGuide() {
        FloatingActionButton button = (FloatingActionButton) mapOptionsMenu.findViewById(R.id.button_poi_launcher);
        button.setLabelText(getString(R.string.map_poi_launcher_button));
        // Make the 'Propose POI' button gone
        FloatingActionButton proposePOIButton = (FloatingActionButton) mapOptionsMenu.findViewById(R.id.button_poi_propose);
        if (proposePOIButton != null) {
            proposePOIButton.setVisibility(View.GONE);
        }
        // Hide the overlay
        if (mapOptionsMenu.isOpened()) {
            mapOptionsMenu.close(false);
        }
        // Show the map screen
        selectItem(R.id.action_tours);
    }

    private void initializePushNotifications() {
        final SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(RegisterGCMService.SHARED_PREFERENCES_FILE_GCM, Context.MODE_PRIVATE);
        boolean notificationsEnabled = sharedPreferences.getBoolean(RegisterGCMService.KEY_NOTIFICATIONS_ENABLED, false);
        if (notificationsEnabled) {
            startService(new Intent(this, RegisterGCMService.class));
        } else {
            presenter.updateApplicationInfo("");
        }

        User user = getAuthenticationController().getUser();
        MixpanelAPI mixpanel = EntourageApplication.get().getMixpanel();
        mixpanel.identify(String.valueOf(user.getId()));
        MixpanelAPI.People people = mixpanel.getPeople();
        people.identify(String.valueOf(user.getId()));

        people.setOnce("$email", user.getEmail());
        people.setOnce("EntouragePartner", user.getPartner());
        people.setOnce("EntourageUserType", user.isPro()?"Pro":"Public");
        people.setOnce("EntourageLanguage", Locale.getDefault().getISO3Country());
            /*people.setOnce("EntourageGeolocEnable", user.getPartner());*/
        people.setOnce("EntourageNotifEnable", notificationsEnabled ?"YES":"NO");

        //mixpanel.getPeople().initPushHandling(RegisterGCMService.GCM_SENDER_ID);
        if(notificationsEnabled) {
            people.setPushRegistrationId(sharedPreferences.getString(RegisterGCMService.KEY_REGISTRATION_ID, null));
        }
    }

    @Override
    protected void logout(){
        //remove the tutorial flag
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        HashSet<String> loggedNumbers = (HashSet) sharedPreferences.getStringSet(LoginActivity.KEY_TUTORIAL_DONE, new HashSet<String>());
        loggedNumbers.remove(EntourageApplication.me(getApplicationContext()).getPhone());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.putStringSet(LoginActivity.KEY_TUTORIAL_DONE, loggedNumbers);
        editor.commit();

        //TODO: do a proper DELETE not an UPDATE
        //presenter.deleteApplicationInfo(getDeviceID());
        presenter.updateApplicationInfo("");

        gcmSharedPreferences.edit().remove(RegisterGCMService.KEY_REGISTRATION_ID).commit();

        super.logout();
    }

    // ----------------------------------
    // BUS LISTENERS
    // ----------------------------------

    @Subscribe
    public void GCMTokenObtained(OnGCMTokenObtainedEvent event) {
        if (event.getRegistrationId() != null) {
            presenter.updateApplicationInfo(event.getRegistrationId());
        }
        /*
        else {
            presenter.updateApplicationInfo("");
        }
        */
    }

    @Subscribe
    public void checkIntentAction(OnCheckIntentActionEvent event) {
        switchToMapFragment();
        Intent intent = getIntent();
        if (intent == null || intent.getExtras() == null) {
            intentAction = null;
            intentTour = null;
            return;
        }
        mapEntourageFragment.checkAction(intentAction, intentTour);
        Message message = (Message) intent.getExtras().getSerializable(PushNotificationService.PUSH_MESSAGE);
        if (message != null) {
            PushNotificationContent content = message.getContent();
            if (content != null) {
                if (PushNotificationContent.TYPE_NEW_CHAT_MESSAGE.equals(intentAction) || PushNotificationContent.TYPE_JOIN_REQUEST_ACCEPTED.equals(intentAction)) {
                    if (content.isTourRelated()) {
                        mapEntourageFragment.displayChosenFeedItem(content.getJoinableId(), TimestampedObject.TOUR_CARD);
                    } else if (content.isEntourageRelated()) {
                        mapEntourageFragment.displayChosenFeedItem(content.getJoinableId(), TimestampedObject.ENTOURAGE_CARD);
                    }
                } else if (PushNotificationContent.TYPE_ENTOURAGE_INVITATION.equals(intentAction)) {
                    PushNotificationContent.Extra extra = content.extra;
                    if (extra != null) {
                        mapEntourageFragment.displayChosenFeedItem(extra.entourageId, TimestampedObject.ENTOURAGE_CARD, extra.invitationId);
                    }
                } else if (PushNotificationContent.TYPE_INVITATION_STATUS.equals(intentAction)) {
                    PushNotificationContent.Extra extra = content.extra;
                    if (extra != null && (content.isEntourageRelated() || content.isTourRelated())) {
                        mapEntourageFragment.displayChosenFeedItem(extra.joinableId, content.isTourRelated() ? TimestampedObject.TOUR_CARD : TimestampedObject.ENTOURAGE_CARD);
                    }
                }
            }
            EntourageApplication application = EntourageApplication.get(getApplicationContext());
            if (application != null) {
                application.removePushNotification(message);
            }
            refreshBadgeCount();
        }
        intentAction = null;
        intentTour = null;
    }

    @Subscribe
    public void userInfoUpdated(OnUserInfoUpdatedEvent event) {
        User user = getAuthenticationController().getUser();
        if (user != null) {
            userName.setText(user.getDisplayName());
            String avatarURL = user.getAvatarURL();
            if (avatarURL != null) {
                Picasso.with(this)
                    .load(Uri.parse(avatarURL))
                    .placeholder(R.drawable.ic_user_photo_small)
                    .transform(new CropCircleTransformation())
                    .into(userPhoto);
            } else {
                userPhoto.setImageResource(R.drawable.ic_user_photo_small);
            }
            // Show partner logo
            String partnerURL = null;
            Partner partner = user.getPartner();
            if (partner != null) {
                partnerURL = partner.getSmallLogoUrl();
            }
            if (partnerURL != null) {
                Picasso.with(this)
                        .load(Uri.parse(partnerURL))
                        .placeholder(R.drawable.partner_placeholder)
                        .transform(new CropCircleTransformation())
                        .into(userPartnerLogo);
            } else {
                userPartnerLogo.setImageDrawable(null);
            }
        }
    }

    @Subscribe
    public void userViewRequested(OnUserViewRequestedEvent event) {
        EntourageEvents.logEvent(Constants.EVENT_FEED_USERPROFILE);
        UserFragment fragment = UserFragment.newInstance(event.getUserId());
        fragment.show(getSupportFragmentManager(), UserFragment.TAG);
    }

    @Subscribe
    public void feedItemViewRequested(OnFeedItemInfoViewRequestedEvent event) {
        if (mapEntourageFragment != null && event != null) {
            FeedItem feedItem = event.getFeedItem();
            if (feedItem != null) {
                mapEntourageFragment.displayChosenFeedItem(feedItem, event.getfeedRank());
                //refresh badge count
                refreshBadgeCount();
                // update the newsfeed card
                mapEntourageFragment.onPushNotificationConsumedForFeedItem(feedItem);
                // update the my entourages card, if necessary
                MyEntouragesFragment myEntouragesFragment = (MyEntouragesFragment) getSupportFragmentManager().findFragmentByTag(MyEntouragesFragment.TAG);
                if (myEntouragesFragment != null) {
                    myEntouragesFragment.onPushNotificationConsumedForFeedItem(feedItem);
                }
            } else {
                //check if we are receiving feed type and id
                int feedItemType = event.getFeedItemType();
                long feedItemId = event.getFeedItemId();
                if (feedItemType == 0 || feedItemId == 0) {
                    return;
                }
                mapEntourageFragment.displayChosenFeedItem(feedItemId, feedItemType, event.getInvitationId());
            }
        }
    }

    @Subscribe
    public void userActRequested(final OnUserActEvent event) {
        if (OnUserActEvent.ACT_JOIN.equals(event.getAct())) {
            if (mapEntourageFragment != null) {
                mapEntourageFragment.act(event.getFeedItem());
            }
        } else if (OnUserActEvent.ACT_QUIT.equals(event.getAct())) {
            FeedItem feedItem = event.getFeedItem();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            int titleId = R.string.tour_info_quit_tour_title;
            int messageId = R.string.tour_info_quit_tour_description;
            if (feedItem.getType() == TimestampedObject.ENTOURAGE_CARD) {
                titleId = R.string.entourage_info_quit_entourage_title;
                messageId = R.string.entourage_info_quit_entourage_description;
            }
            builder.setTitle(titleId)
                .setMessage(messageId)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        if (mapEntourageFragment == null) {
                            Toast.makeText(DrawerActivity.this, R.string.tour_info_quit_tour_error, Toast.LENGTH_SHORT).show();
                        } else {
                            User me = EntourageApplication.me(DrawerActivity.this);
                            if (me == null) {
                                Toast.makeText(DrawerActivity.this, R.string.tour_info_quit_tour_error, Toast.LENGTH_SHORT).show();
                            } else {
                                FeedItem item = event.getFeedItem();
                                if (item != null && FeedItem.JOIN_STATUS_PENDING.equals(item.getJoinStatus())) {
                                    EntourageEvents.logEvent(Constants.EVENT_FEED_CANCEL_JOIN_REQUEST);
                                }
                                mapEntourageFragment.removeUserFromNewsfeedCard(item, me.getId());
                            }
                        }
                    }
                })
                .setNegativeButton(R.string.no, null);
            builder.create().show();
        }
    }

    @Subscribe
    public void tourEncounterViewRequested(OnTourEncounterViewRequestedEvent event) {
        Encounter encounter = event.getEncounter();
        if (encounter == null) {
            return;
        }
        if (encounter.isReadOnly()) {
            Intent intent = new Intent(this, ReadEncounterActivity.class);
            Bundle extras = new Bundle();
            extras.putSerializable(ReadEncounterActivity.BUNDLE_KEY_ENCOUNTER, encounter);
            intent.putExtras(extras);
            this.startActivity(intent);
        } else {
            Intent intent = new Intent(this, CreateEncounterActivity.class);
            Bundle extras = new Bundle();
            extras.putSerializable(CreateEncounterActivity.BUNDLE_KEY_ENCOUNTER, encounter);
            intent.putExtras(extras);
            this.startActivity(intent);
        }
    }

    @Subscribe
    public void feedItemCloseRequested(OnFeedItemCloseRequestEvent event) {
        FeedItem feedItem = event.getFeedItem();
        if (feedItem == null) {
            return;
        }
        if (mapEntourageFragment != null) {
            if (event.isShowUI()) {
                EntourageEvents.logEvent(Constants.EVENT_FEED_ACTIVE_CLOSE_OVERLAY);
                FeedItemOptionsFragment feedItemOptionsFragment = FeedItemOptionsFragment.newInstance(feedItem);
                feedItemOptionsFragment.show(getSupportFragmentManager(), FeedItemOptionsFragment.TAG);
                return;
            }
            // Only the author can close entourages/tours
            User me = EntourageApplication.me(this);
            if (me == null || feedItem.getAuthor() == null) {
                return;
            }
            int myId = me.getId();
            if (feedItem.getAuthor().getUserID() != myId) {
                return;
            }

            if (!feedItem.isClosed()) {
                // close
                mapEntourageFragment.stopFeedItem(feedItem);
            } else {
                if (feedItem.getType() == TimestampedObject.TOUR_CARD && !feedItem.isFreezed()) {
                    // freeze
                    Tour tour = (Tour) feedItem;
                    mapEntourageFragment.freezeTour(tour);
                }
            }
        }
    }

    @Subscribe
    public void onUnauthorized(OnUnauthorizedEvent event) {
        logout();
    }

    @Override
    public void closeTourInformationFragment(TourInformationFragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().remove(fragment).commit();
    }

    @Override
    public void showStopTourActivity(Tour tour) {
        mapEntourageFragment.pauseTour(tour);
        //buttonStartLauncher.setVisibility(View.GONE);
        Bundle args = new Bundle();
        args.putSerializable(Tour.KEY_TOUR, tour);
        Intent confirmationIntent = new Intent(this, ConfirmationActivity.class);
        confirmationIntent.putExtras(args);
        startActivity(confirmationIntent);
    }

    @Override
    public void closeChoiceFragment(ChoiceFragment fragment, Tour tour) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().remove(fragment).commit();
        if (tour != null) {
            if (mainFragment instanceof MapEntourageFragment) {
                MapEntourageFragment mapEntourageFragment = (MapEntourageFragment) mainFragment;
                mapEntourageFragment.displayChosenFeedItem(tour, 0);
            }
        }
    }

    @Override
    public void onShowTourInfo(final Tour tour) {

    }

    @Override
    public void onEntourageDisclaimerAccepted(final EntourageDisclaimerFragment fragment) {
        // Save the entourage disclaimer shown flag
        User me = EntourageApplication.me(this);
        me.setEntourageDisclaimerShown(true);
        getAuthenticationController().saveUser(me);

        // Dismiss the disclaimer fragment
        fragment.dismiss();

        // Show the create entourage fragment
        if (mainFragment instanceof MapEntourageFragment) {
            ((MapEntourageFragment) mainFragment).createEntourage();
        }
    }

    @Override
    public void onEncounterDisclaimerAccepted(EncounterDisclaimerFragment fragment) {
        // Save the entourage disclaimer shown flag
        User me = EntourageApplication.me(this);
        me.setEncounterDisclaimerShown(true);
        getAuthenticationController().saveUser(me);

        // Dismiss the disclaimer fragment
        fragment.dismiss();

        // Show the create encounter fragment
        if (mainFragment instanceof MapEntourageFragment) {
            ((MapEntourageFragment) mainFragment).addEncounter();
        }
    }

    @Override
    public void onPhotoBack() {
        // Do nothing
    }

    @Override
    public void onPhotoIgnore() {
        // Do nothing
    }

    @Override
    public void onPhotoChosen(final Uri photoUri, int photoSource) {

        if (photoSource == PhotoChooseSourceFragment.TAKE_PHOTO_REQUEST) {
            EntourageEvents.logEvent(Constants.EVENT_PHOTO_SUBMIT);
        }

        //Upload the photo to Amazon S3
        showProgressDialog(R.string.user_photo_uploading);

        final String objectKey = "user_" + authenticationController.getUser().getId() + ".jpg";
        TransferUtility transferUtility = AmazonS3Utils.getTransferUtility(this);
        TransferObserver transferObserver = transferUtility.upload(
            BuildConfig.AWS_BUCKET,
            BuildConfig.AWS_FOLDER + objectKey,
            new File(photoUri.getPath()),
            CannedAccessControlList.PublicRead
        );
        transferObserver.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(final int id, final TransferState state) {
                if (state == TransferState.COMPLETED) {
                    if (presenter != null) {
                        presenter.updateUserPhoto(objectKey);
                    } else {
                        Toast.makeText(DrawerActivity.this, R.string.user_photo_error_not_saved, Toast.LENGTH_SHORT).show();
                        dismissProgressDialog();
                        PhotoEditFragment photoEditFragment = (PhotoEditFragment) getSupportFragmentManager().findFragmentByTag(PhotoEditFragment.TAG);
                        if (photoEditFragment != null) {
                            photoEditFragment.onPhotoSent(false);
                        }
                    }
                    // Delete the temporary file
                    File tmpImageFile = new File(photoUri.getPath());
                    if (!tmpImageFile.delete()) {
                        // Failed to delete the file
                        Log.d("EntouragePhoto", "Failed to delete the temporary photo file");
                    }
                }
            }

            @Override
            public void onProgressChanged(final int id, final long bytesCurrent, final long bytesTotal) {

            }

            @Override
            public void onError(final int id, final Exception ex) {
                Toast.makeText(DrawerActivity.this, R.string.user_photo_error_not_saved, Toast.LENGTH_SHORT).show();
                dismissProgressDialog();
                PhotoEditFragment photoEditFragment = (PhotoEditFragment) getSupportFragmentManager().findFragmentByTag(PhotoEditFragment.TAG);
                if (photoEditFragment != null) {
                    photoEditFragment.onPhotoSent(false);
                }
            }
        });
    }

    // ----------------------------------
    // Floating Action Buttons handling
    // ----------------------------------

    @OnClick(R.id.button_start_tour_launcher)
    public void onStartTourClicked() {
        EntourageEvents.logEvent(Constants.EVENT_FEED_TOUR_CREATE_CLICK);
        if (mainFragment instanceof MapEntourageFragment) {
            mapEntourageFragment.onStartTourLauncher();
        } else {
            onPOILauncherClicked();
            mapEntourageFragment.onStartTourLauncher();
        }
    }

    @OnClick(R.id.button_add_tour_encounter)
    public void onAddTourEncounterClicked() {
        EntourageEvents.logEvent(Constants.EVENT_CREATE_ENCOUNTER_CLICK);
        if (mainFragment instanceof MapEntourageFragment) {
            mapEntourageFragment.onAddEncounter();
        } else {
            onPOILauncherClicked();
            mapEntourageFragment.onAddEncounter();
        }
    }

    @OnClick(R.id.button_create_entourage)
    public void onCreateEntourageClicked() {
        EntourageEvents.logEvent(Constants.EVENT_FEED_OFFER_CREATE_CLICK);
        if (mainFragment instanceof MapEntourageFragment) {
            mapEntourageFragment.displayEntouragePopupWhileTour(Entourage.TYPE_CONTRIBUTION);
        } else {
            onPOILauncherClicked();
            mapEntourageFragment.displayEntouragePopupWhileTour(Entourage.TYPE_CONTRIBUTION);
        }
    }

    @OnClick(R.id.button_poi_propose)
    protected void onPOIProposeClicked() {
        if (isGuideShown()) {
            GuideMapEntourageFragment guideMapEntourageFragment = (GuideMapEntourageFragment) mainFragment;
            guideMapEntourageFragment.proposePOI();
        }
    }

    @OnClick(R.id.button_poi_launcher)
    public void onPOILauncherClicked() {
        if (mainFragment instanceof MapEntourageFragment) {
            EntourageEvents.logEvent(Constants.EVENT_OPEN_GUIDE_FROM_PLUS);
            // Show the guide screen
            showSolidarityGuide();
        } else {
            EntourageEvents.logEvent(Constants.EVENT_SCREEN_06_1);
            // Change the Guide Option text
            hideSolidarityGuide();
        }
    }

    // ----------------------------------
    // Logo icon click handling
    // ----------------------------------

    @OnClick(R.id.toolbar_entourage_logo)
    protected void onToolbarLogoClicked() {
        if (isGuideShown()) {
            // switch to map mode
            onPOILauncherClicked();
        } else {
            if (mapEntourageFragment.isToursListVisible()) {
                // make the map visible
                mapEntourageFragment.ensureMapVisible();
            } else {
                // switch to list view
                mapEntourageFragment.toggleToursList();
            }
        }
    }

    // ----------------------------------
    // PUSH NOTIFICATION HANDLING
    // ----------------------------------

    @Subscribe
    public void onPushNotificationReceived(OnPushNotificationReceived event) {
        final Message message = event.getMessage();
        if (message != null && message.getContent() != null && message.getContent().getJoinableId() != 0) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    PushNotificationContent content = message.getContent();
                    if (content != null) {
                        String contentType = content.getType();
                        if (contentType == null) {
                            return;
                        }
                        if (contentType.equals(PushNotificationContent.TYPE_NEW_CHAT_MESSAGE)) {
                            if (!onPushNotificationChatMessageReceived(message)) {
                                addPushNotification(message);
                            } else {
                                EntourageApplication application = EntourageApplication.get(getApplicationContext());
                                if (application != null) {
                                    if (content.isTourRelated()) {
                                        application.removePushNotification(content.getJoinableId(), TimestampedObject.TOUR_CARD, content.getUserId(), PushNotificationContent.TYPE_NEW_CHAT_MESSAGE);
                                    }
                                    else if (content.isEntourageRelated()) {
                                        application.removePushNotification(content.getJoinableId(), TimestampedObject.ENTOURAGE_CARD, content.getUserId(), PushNotificationContent.TYPE_NEW_CHAT_MESSAGE);
                                    }
                                }
                            }
                        }
                        else if (contentType.equals(PushNotificationContent.TYPE_JOIN_REQUEST_CANCELED)) {
                            EntourageApplication application = EntourageApplication.get(getApplicationContext());
                            if (application != null) {
                                if (content.isTourRelated()) {
                                    application.removePushNotification(content.getJoinableId(), TimestampedObject.TOUR_CARD, content.getUserId(), PushNotificationContent.TYPE_NEW_JOIN_REQUEST);
                                }
                                else if (content.isEntourageRelated()) {
                                    application.removePushNotification(content.getJoinableId(), TimestampedObject.ENTOURAGE_CARD, content.getUserId(), PushNotificationContent.TYPE_NEW_JOIN_REQUEST);
                                }
                            }
                        }
                        else {
                            addPushNotification(message);
                            if (contentType.equals(PushNotificationContent.TYPE_JOIN_REQUEST_ACCEPTED)) {
                                if (mapEntourageFragment != null) {
                                    mapEntourageFragment.userStatusChanged(content, Tour.JOIN_STATUS_ACCEPTED);
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    private boolean onPushNotificationChatMessageReceived(Message message) {
        TourInformationFragment fragment = (TourInformationFragment) getSupportFragmentManager().findFragmentByTag(TourInformationFragment.TAG);
        return fragment != null && fragment.onPushNotificationChatMessageReceived(message);
    }

    private void addPushNotification(Message message) {
        refreshBadgeCount();
        if (mapEntourageFragment != null) {
            mapEntourageFragment.onPushNotificationReceived(message);
        }
        MyEntouragesFragment myEntouragesFragment = (MyEntouragesFragment) getSupportFragmentManager().findFragmentByTag(MyEntouragesFragment.TAG);
        if (myEntouragesFragment != null) {
            myEntouragesFragment.onPushNotificationReceived(message);
        }
    }

    // ----------------------------------
    // Helper functions
    // ----------------------------------

    private void refreshBadgeCount() {
        EntourageApplication application = EntourageApplication.get(getApplicationContext());
        discussionBadgeView.setBadgeCount(application.badgeCount);
    }

    // ----------------------------------
    // Helper functions
    // ----------------------------------

    public boolean isGuideShown() {
        return !(mainFragment instanceof MapEntourageFragment);
    }

}
