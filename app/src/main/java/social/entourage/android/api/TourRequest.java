package social.entourage.android.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import social.entourage.android.api.model.map.Tour;
import social.entourage.android.api.model.map.TourPoint;
import social.entourage.android.api.model.map.TourUser;

public interface TourRequest {

    @POST("tours.json")
    Call<Tour.TourWrapper> tour(@Body Tour.TourWrapper tourWrapper);

    @POST("tours/{tour_id}/tour_points.json")
    Call<Tour.TourWrapper> tourPoints(@Path("tour_id") long tourId, @Body TourPoint.TourPointWrapper points);

    @PUT("tours/{id}.json")
    Call<Tour.TourWrapper> closeTour(@Path("id") long tourId, @Body Tour.TourWrapper tourWrapper);

    @GET("tours.json")
    Call<Tour.ToursWrapper> retrieveToursNearby(@Query("limit") int limit,
                             @Query("type") String type,
                             @Query("vehicle_type") String vehicleType,
                             @Query("latitude") double latitude,
                             @Query("longitude") double longitude,
                             @Query("distance") double distance);

    @GET("users/{user_id}/tours.json")
    Call<Tour.ToursWrapper> retrieveToursByUserId(@Path("user_id") int userId,
                               @Query("page") int page,
                               @Query("per") int per);

    @GET("users/{user_id}/tours.json")
    Call<Tour.ToursWrapper> retrieveToursByUserIdAndPoint(@Path("user_id") int userId,
                                       @Query("page") int page,
                                       @Query("per") int per,
                                       @Query("latitude") double latitude,
                                       @Query("longitude") double longitude,
                                       @Query("distance") double distance);

    @GET("tours/{tour_id}/users.json")
    Call<TourUser.TourUsersWrapper> retrieveTourUsers(
            @Path("tour_id") long tourId
    );

    @GET("tours/{tour_id}/users.json")
    Call<TourUser.TourUsersWrapper> retrieveTourUsers(
            @Path("tour_id") long tourId,
            @Query("page") int page,
            @Query("per") int per
    );
}