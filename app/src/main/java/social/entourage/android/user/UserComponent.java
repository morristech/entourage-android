package social.entourage.android.user;

import dagger.Component;
import social.entourage.android.ActivityScope;
import social.entourage.android.EntourageComponent;

/**
 * Component linked to UserFragment lifecycle
 * Provides a UserPresenter
 * @see UserFragment
 * @see UserPresenter
 */
@ActivityScope
@Component(
        dependencies = EntourageComponent.class,
        modules = UserModule.class
)
@SuppressWarnings("unused")
public interface UserComponent {
    void inject(UserFragment fragment);

    UserPresenter getUserPresenter();
}
