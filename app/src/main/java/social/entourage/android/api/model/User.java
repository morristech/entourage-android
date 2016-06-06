package social.entourage.android.api.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class User implements Serializable {

    // ----------------------------------
    // CONSTANTS
    // ----------------------------------

    public static final String KEY_USER_ID = "social.entourage.android.KEY_USER_ID";
    public static final String KEY_USER = "social.entourage.android.KEY_USER";

    public static final String TYPE_PUBLIC = "public";
    public static final String TYPE_PRO = "pro";

    // ----------------------------------
    // ATTRIBUTES
    // ----------------------------------

    private final int id;

    private String email;

    @Expose(serialize = false, deserialize = false)
    private String phone;

    @SerializedName("first_name")
    private String firstName;

    @SerializedName("last_name")
    private String lastName;

    @SerializedName("display_name")
    private final String displayName;

    private final String token;

    @Expose(serialize = false, deserialize = true)
    private final Stats stats;

    @Expose(serialize = false, deserialize = true)
    private final Organization organization;

    @SerializedName("avatar_url")
    private String avatarURL;

    private String smsCode;

    @SerializedName("user_type")
    private String type = TYPE_PRO;

    private boolean entourageDisclaimerShown = false;

    // ----------------------------------
    // CONSTRUCTOR
    // ----------------------------------

    private User(final int id, final String email, final String displayName, final Stats stats, final Organization organization, final String token, final String avatarURL) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.stats = stats;
        this.organization = organization;
        this.token = token;
        this.avatarURL = avatarURL;
    }

    public User clone() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (User) ois.readObject();
        } catch (IOException e) {
            return null;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    // ----------------------------------
    // GETTERS & SETTERS
    // ----------------------------------

    public int getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getDisplayName() { return displayName;}

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getToken() {
        return token;
    }

    public Stats getStats() {
        return stats;
    }

    public Organization getOrganization() {
        return organization;
    }

    public String getAvatarURL() {
        return avatarURL;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setAvatarURL(String avatarURL) {
        this.avatarURL = avatarURL;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public String getSmsCode() {
        return smsCode;
    }

    public void setSmsCode(final String smsCode) {
        this.smsCode = smsCode;
    }

    public boolean isEntourageDisclaimerShown() {
        return entourageDisclaimerShown;
    }

    public void setEntourageDisclaimerShown(final boolean entourageDisclaimerShown) {
        this.entourageDisclaimerShown = entourageDisclaimerShown;
    }

    public void incrementTours() {
        stats.setTourCount(stats.getTourCount() + 1);
    }

    public void incrementEncouters() {
        stats.setEncounterCount(stats.getEncounterCount() + 1);
    }

    public static String decodeURL(String encodedURL) {
        if (encodedURL == null) {
            return encodedURL;
        }
        return encodedURL.replace('\u0026', '&');
    }

    public boolean isPro() {
        return TYPE_PRO.equals(type);
    }

    // ----------------------------------
    // BUILDER
    // ----------------------------------

    @SuppressWarnings("UnusedReturnValue")
    public static class Builder {
        private int id;
        private String email, displayName, token, avatarURL;
        private Stats stats;
        private Organization organization;

        public Builder() {
        }

        public Builder withId(final int id) {
            this.id = id;
            return this;
        }

        public Builder withEmail(final String email) {
            this.email = email;
            return this;
        }

        public Builder withDisplayName(final String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder withStats(final Stats stats) {
            this.stats = stats;
            return this;
        }

        public Builder withOrganization(final Organization organization) {
            this.organization = organization;
            return this;
        }

        public Builder withToken(final String token) {
            this.token = token;
            return this;
        }

        public Builder withAvatarURL(final String avatarURL) {
            this.avatarURL = avatarURL;
            return this;
        }

        public User build() {
            if (id == -1) {
                return null;
            }
            if (email == null) {
                return null;
            }
            if (displayName == null) {
                return null;
            }
            if (stats == null) {
                return null;
            }
            if (organization == null) {
                return null;
            }
            if (token == null) {
                return null;
            }
            return new User(id, email, displayName, stats, organization, token, avatarURL);
        }
    }

    // ----------------------------------
    // WRAPPER
    // ----------------------------------

    public static class UserWrapper {

        private User user;

        public UserWrapper(User user) {
            this.user = user;
        }

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }
    }
}
