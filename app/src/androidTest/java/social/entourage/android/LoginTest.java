package social.entourage.android;

import android.content.Context;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.espresso.IdlingResource;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.jakewharton.espresso.OkHttp3IdlingResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import okhttp3.OkHttpClient;
import social.entourage.android.authentication.login.LoginActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class LoginTest {
    @Rule public ActivityTestRule<LoginActivity> activityRule = new ActivityTestRule<>(LoginActivity.class);
    private IdlingResource resource;

    @Before
    public void setUp() {
        checkNoUserIsLoggedIn();
        Context context = activityRule.getActivity();
        OkHttpClient client = EntourageApplication.get(context).getEntourageComponent().getOkHttpClient();
        resource = OkHttp3IdlingResource.create("OkHttp", client);
        IdlingRegistry.getInstance().register(resource);
    }

    private void checkNoUserIsLoggedIn() {
        if(EntourageApplication.me() != null) {
            Context context = activityRule.getActivity();
            EntourageApplication.get(context).getEntourageComponent().getAuthenticationController().logOutUser();
        }
    }

    @After
    public void tearDown() throws Exception {
        IdlingRegistry.getInstance().unregister(resource);
        checkNoUserIsLoggedIn();
    }

    @Test
    public void loginOK() {
        //first we try with wrong input
        loginFailureWrongPassword();
        loginFailureWrongPhoneNumberFormat();

        checkNoUserIsLoggedIn();
        onView(withId(R.id.login_button_login)).perform(click());

        onView(withId(R.id.login_edit_phone)).perform(typeText(BuildConfig.TEST_ACCOUNT_LOGIN));
        onView(withId(R.id.login_edit_code)).perform(typeText(BuildConfig.TEST_ACCOUNT_PWD));
        onView(withId(R.id.login_button_signup)).perform(click());

        onView(withText(R.string.login_error_title)).check(doesNotExist());
    }

    /*@Test
    @TODO fix this because of unexpected errors in OkHttp3IdlingResource
    public void loginOKwithCountryCode() {
        checkNoUserIsLoggedIn();
        onView(withId(R.id.login_button_login)).perform(click());

        onView(withId(R.id.login_edit_phone)).perform(typeText(BuildConfig.TEST_ACCOUNT_LOGIN.replaceFirst("0", "+33")));
        onView(withId(R.id.login_edit_code)).perform(typeText(BuildConfig.TEST_ACCOUNT_PWD));
        onView(withId(R.id.login_button_signup)).perform(click());

        onView(withText(R.string.login_error_title)).check(doesNotExist());
    }*/

    //Test
    public void loginFailureWrongPassword() {
        checkNoUserIsLoggedIn();
        onView(withId(R.id.login_button_login)).perform(click());

        onView(withId(R.id.login_edit_phone)).perform(typeText(BuildConfig.TEST_ACCOUNT_LOGIN));
        onView(withId(R.id.login_edit_code)).perform(typeText("999999"));
        onView(withId(R.id.login_button_signup)).perform(click());

        onView(withText(R.string.login_error_title)).check(matches(isDisplayed()));
        onView(withText(R.string.login_retry_label)).perform(click());
        onView(withId(R.id.login_back_button)).perform(click());
    }

    //Test
    public void loginFailureWrongPhoneNumberFormat() {
        checkNoUserIsLoggedIn();
        onView(withId(R.id.login_button_login)).perform(click());

        onView(withId(R.id.login_edit_phone)).perform(typeText("012345678"));
        onView(withId(R.id.login_edit_code)).perform(typeText("000000"));
        onView(withId(R.id.login_button_signup)).perform(click());

        onView(withText(R.string.login_error_title)).check(matches(isDisplayed()));
        onView(withText(R.string.login_retry_label)).perform(click());
        onView(withId(R.id.login_back_button)).perform(click());
    }
}
