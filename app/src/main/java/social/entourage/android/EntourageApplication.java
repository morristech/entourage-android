package social.entourage.android;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.flurry.android.FlurryAgent;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import net.danlew.android.joda.JodaTimeAndroid;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Scope;
import javax.inject.Singleton;

import dagger.Component;
import dagger.Module;
import dagger.Provides;
import social.entourage.android.api.ApiModule;
import social.entourage.android.api.tape.EncounterTapeService;
import social.entourage.android.authentication.AuthenticationController;
import social.entourage.android.authentication.ComplexPreferences;
import social.entourage.android.authentication.login.LoginActivity;
import social.entourage.android.authentication.login.LoginInformationFragment;
import social.entourage.android.authentication.login.LoginInformationPresenter;
import social.entourage.android.guide.GuideMapEntourageFragment;
import social.entourage.android.guide.GuideMapPresenter;
import social.entourage.android.guide.poi.ReadPoiActivity;
import social.entourage.android.map.MapEntourageFragment;
import social.entourage.android.map.MapPresenter;
import social.entourage.android.map.choice.ChoiceFragment;
import social.entourage.android.map.choice.ChoicePresenter;
import social.entourage.android.map.confirmation.ConfirmationActivity;
import social.entourage.android.map.encounter.CreateEncounterActivity;
import social.entourage.android.map.encounter.ReadEncounterActivity;
import social.entourage.android.map.tour.TourInformationFragment;
import social.entourage.android.map.tour.TourInformationPresenter;
import social.entourage.android.map.tour.TourService;
import social.entourage.android.message.MessageActivity;
import social.entourage.android.user.UserFragment;
import social.entourage.android.user.UserPresenter;

/**
 * Application setup for Flurry, JodaTime and Dagger
 */
public class EntourageApplication extends Application {

    // ----------------------------------
    // ATTRIBUTES
    // ----------------------------------

    private static EntourageApplication application;
    private static Context context;
    private EntourageComponent component;
    private EntourageActivityComponent activityComponent;

    // ----------------------------------
    // LIFECYCLE
    // ----------------------------------

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
        context = getApplicationContext();
        //TODO: build components

        setupFlurry();
        JodaTimeAndroid.init(this);
    }

    // ----------------------------------
    // PUBLIC METHODS
    // ----------------------------------

    public static EntourageApplication application() {
        return application;
    }

    public EntourageComponent getComponent() {
        return component;
    }

    public EntourageActivityComponent getActivityComponent() {
        return activityComponent;
    }

    // ----------------------------------
    // PRIVATE METHODS
    // ----------------------------------

    private void setupFlurry() {
        FlurryAgent.setLogEnabled(true);
        FlurryAgent.setLogLevel(Log.VERBOSE);
        FlurryAgent.setLogEvents(true);
        FlurryAgent.init(this, BuildConfig.FLURRY_API_KEY);
    }

    // ----------------------------------
    // COMPONENTS
    // ----------------------------------

    @ActivityScope
    @Component(dependencies = EntourageComponent.class, modules = {EntourageModule.class, ApiModule.class})
    public interface EntourageActivityComponent {
        void inject(LoginActivity activity);
        void inject(DrawerActivity activity);
        void inject(CreateEncounterActivity activity);
        void inject(ReadEncounterActivity activity);
        void inject(ConfirmationActivity activity);
        void inject(ReadPoiActivity activity);
        void inject(MessageActivity activity);
        void inject(LoginInformationFragment fragment);
        void inject(MapEntourageFragment fragment);
        void inject(GuideMapEntourageFragment fragment);
        void inject(ChoiceFragment fragment);
        void inject(TourInformationFragment fragment);
        void inject(UserFragment fragment);

        LoginInformationPresenter getLoginInformationPresenter();
        MapPresenter getMapPresenter();
        GuideMapPresenter getGuideMapPresenter();
        ChoicePresenter getChoicePresenter();
        TourInformationPresenter getTourInformationPresenter();
        UserPresenter getUserPresenter();
    }

    @Singleton
    @Component(modules = {EntourageModule.class, ApiModule.class})
    public interface EntourageComponent {
        void inject(TourService service);
        void inject(EncounterTapeService service);
    }

    // ----------------------------------
    // MODULES
    // ----------------------------------

    @Module
    public static class EntourageModule {

        private final EntourageApplication application;

        public EntourageModule(EntourageApplication application) {
            this.application = application;
        }

        @Provides
        EntourageApplication providesApplication() { return application; }

        @Provides
        public Context providesContext() { return context; }

        @Provides @Singleton
        public Bus providesBus() { return new Bus(ThreadEnforcer.ANY); }

        @Provides @Singleton
        public AuthenticationController providesAuthenticationController(ComplexPreferences userSharedPref) {
            return new AuthenticationController(userSharedPref).init();
        }

        @Provides @Singleton
        public ComplexPreferences providesUserSharedPreferences() {
            return ComplexPreferences.getComplexPreferences(application, "userPref", Context.MODE_PRIVATE);
        }
    }

    // ----------------------------------
    // SCOPES
    // ----------------------------------

    @Scope
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ActivityScope {}

}
