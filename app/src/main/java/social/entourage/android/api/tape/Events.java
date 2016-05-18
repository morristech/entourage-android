package social.entourage.android.api.tape;

import com.google.android.gms.maps.model.LatLng;

import social.entourage.android.api.model.Message;
import social.entourage.android.api.model.TimestampedObject;
import social.entourage.android.api.model.map.BaseEntourage;
import social.entourage.android.api.model.map.Encounter;
import social.entourage.android.api.model.map.Entourage;
import social.entourage.android.api.model.map.Tour;

public class Events {

    /**
     * Event triggering the checking of the intent action
     */
    public static class OnCheckIntentActionEvent {

        public OnCheckIntentActionEvent() {}

    }

    /**
     * Event bearing connection state for the offline encounters queue
     */
    public static class OnConnectionChangedEvent {

        private boolean connected;

        public OnConnectionChangedEvent(boolean connected) {
            this.connected = connected;
        }

        public boolean isConnected() {
            return connected;
        }
    }

    /**
     * Event bearing the registration id when obtained from Google Cloud Messaging (for push notifications)
     */
    public static class OnGCMTokenObtainedEvent {

        private String registrationId;

        public OnGCMTokenObtainedEvent(String registrationId) {
            this.registrationId = registrationId;
        }

        public String getRegistrationId() {
            return registrationId;
        }
    }

    /**
     * Event bearing the new location on which to center the map
     */
    public static class OnBetterLocationEvent {

        private LatLng location;

        public OnBetterLocationEvent(LatLng location) {
            this.location = location;
        }

        public LatLng getLocation() {
            return location;
        }
    }

    /**
     * Event bearing the user's choice regarding the displaying of his past tours
     */
    public static class OnUserChoiceEvent {

        private boolean userHistory;

        public OnUserChoiceEvent(boolean userHistory) {
            this.userHistory = userHistory;
        }

        public boolean isUserHistory() {
            return userHistory;
        }
    }

    /**
     * Event signaling that user info is updated
     */
    public static class OnUserInfoUpdatedEvent {

        public OnUserInfoUpdatedEvent() {};
    }

    /**
     * Event signaling that user wants to act on a tour
     */
    public static class OnUserActEvent {

        public static String ACT_JOIN = "join";
        public static String ACT_QUIT = "quit";

        private String act;
        private BaseEntourage baseEntourage;

        public OnUserActEvent(String act, BaseEntourage baseEntourage) {
            this.act = act;
            this.baseEntourage = baseEntourage;
        }

        public String getAct() {
            return act;
        }

        public BaseEntourage getBaseEntourage() {
            return baseEntourage;
        }
    }

    /**
     * Event signaling that user view is requested
     */
    public static class OnUserViewRequestedEvent {

        private int userId;

        public OnUserViewRequestedEvent(int userId) {
            this.userId = userId;
        };

        public int getUserId() {
            return userId;
        }
    }

    /**
     * Event signaling that tour info view is requested
     */
    public static class OnEntourageInfoViewRequestedEvent {

        private BaseEntourage baseEntourage;

        public OnEntourageInfoViewRequestedEvent(BaseEntourage baseEntourage) {
            this.baseEntourage = baseEntourage;
        };

        public BaseEntourage getBaseEntourage() {
            return baseEntourage;
        }
    }

    /**
     * Event signaling that tour info view is requested
     */
    public static class OnTourEncounterViewRequestedEvent {

        private Encounter encounter;

        public OnTourEncounterViewRequestedEvent(Encounter encounter) {
            this.encounter = encounter;
        };

        public Encounter getEncounter() {
            return encounter;
        }
    }

    /**
     * Event signaling that the tour needs to be closed/freezed
     */
    public static class OnEntourageCloseRequestEvent {

        private BaseEntourage baseEntourage;

        public OnEntourageCloseRequestEvent(BaseEntourage baseEntourage) {
            this.baseEntourage = baseEntourage;
        };

        public BaseEntourage getBaseEntourage() {
            return baseEntourage;
        }
    }

    /**
     * Event triggering the tours service location listener when the permission has been granted
     */
    public static class OnLocationPermissionGranted {

        private boolean isPermissionGranted;

        public OnLocationPermissionGranted(boolean isPermissionGranted) {
            this.isPermissionGranted = isPermissionGranted;
        }

        public boolean isPermissionGranted() {
            return isPermissionGranted;
        }
    }

    /**
     * Event signaling a push notification has been received
     */
    public static class OnPushNotificationReceived {

        private Message message;

        public OnPushNotificationReceived(Message message) {
            this.message = message;
        }

        public Message getMessage() {
            return message;
        }
    }

    /**
     * Event signaling that an encounter was created
     */
    public static class OnEncounterCreated {

        private Encounter encounter;

        public OnEncounterCreated(Encounter encounter) {
            this.encounter = encounter;
        }

        public Encounter getEncounter() {
            return encounter;
        }

    }

    /**
     * Event signaling that an entourage was created
     */
    public static class OnEntourageCreated {

        private Entourage entourage;

        public OnEntourageCreated(Entourage entourage) {
            this.entourage = entourage;
        }

        public Entourage getEntourage() {
            return entourage;
        }

    }

    /**
     * Event signaling that the map filter was changed
     */
    public static class OnMapFilterChanged {

        public OnMapFilterChanged() {
        }

    }
}
