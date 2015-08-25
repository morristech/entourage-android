package social.entourage.android.api;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import retrofit.Endpoint;
import retrofit.Endpoints;
import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;
import social.entourage.android.BuildConfig;
import social.entourage.android.authentication.AuthenticationInterceptor;
/**
 * Module related to Application
 * Providing API related dependencies
 */
@Module
public class ApiModule {

    @Provides
    @Singleton
    public Endpoint providesEndPoint() {
        return Endpoints.newFixedEndpoint(BuildConfig.ENTOURAGE_URL);
    }

    @Provides
    @Singleton
    public RestAdapter providesRestAdapter(final Endpoint endpoint, final AuthenticationInterceptor interceptor) {
        Gson gson = new GsonBuilder()
                .addSerializationExclusionStrategy(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                        final Expose expose = fieldAttributes.getAnnotation(Expose.class);
                        return expose != null && !expose.serialize();
                    }
                    @Override
                    public boolean shouldSkipClass(Class<?> aClass) {
                        return false;
                    }
                }).addDeserializationExclusionStrategy(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                        final Expose expose = fieldAttributes.getAnnotation(Expose.class);
                        return expose != null && !expose.deserialize();
                    }
                    @Override
                    public boolean shouldSkipClass(Class<?> aClass) {
                        return false;
                    }
                })
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
                .create();

        return new RestAdapter.Builder()
                .setEndpoint(endpoint)
                .setRequestInterceptor(interceptor)
                .setLogLevel(BuildConfig.DEBUG ? RestAdapter.LogLevel.FULL : RestAdapter.LogLevel.BASIC)
                .setConverter(new GsonConverter(gson))
                .build();
    }

    @Provides
    @Singleton
    public LoginRequest providesLoginService(final RestAdapter restAdapter) {
        return restAdapter.create(LoginRequest.class);
    }

    @Provides
    @Singleton
    public MapRequest providesMapService(final RestAdapter restAdapter) {
        return restAdapter.create(MapRequest.class);
    }

    @Provides
    @Singleton
    public EncounterRequest providesEncounterService(final RestAdapter restAdapter) {
        return restAdapter.create(EncounterRequest.class);
    }

    @Provides
    @Singleton
    public TourRequest providesTourRequest(final RestAdapter restAdapter) {
        return restAdapter.create(TourRequest.class);
    }

    @Provides
    @Singleton
    public UserRequest providesUserRequest(final RestAdapter restAdapter) {
        return restAdapter.create(UserRequest.class);
    }
}
