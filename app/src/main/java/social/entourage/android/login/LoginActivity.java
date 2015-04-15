package social.entourage.android.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import social.entourage.android.map.MapActivity;
import social.entourage.android.EntourageActivity;
import social.entourage.android.R;

public class LoginActivity extends EntourageActivity {

    @Inject
    LoginPresenter loginPresenter;

    @InjectView(R.id.edittext_email)
    EditText emailEditText;
    @InjectView(R.id.button_login)
    Button loginButton;

    @Override
    protected List<Object> getScopedModules() {
        return Arrays.<Object>asList(new LoginModule(this));
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ButterKnife.inject(this);
    }

    @OnClick(R.id.button_login)
    public void onLoginClick(Button loginButton) {
        startLoader();
        loginPresenter.login(emailEditText.getText().toString());
    }

    public void startMapActivity() {
        startActivity(new Intent(this, MapActivity.class));
        resetLoginButton();
        finish();
    }

    public void loginFail() {
        resetLoginButton();
        Toast.makeText(this, getString(R.string.login_fail), Toast.LENGTH_SHORT).show();
    }

    private void startLoader() {
        loginButton.setText(R.string.loading_button_text);
        loginButton.setEnabled(false);
    }

    private void resetLoginButton() {
        loginButton.setText(R.string.login_button_text);
        loginButton.setEnabled(true);
    }
}
