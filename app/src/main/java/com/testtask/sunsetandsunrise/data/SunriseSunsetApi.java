package com.testtask.sunsetandsunrise.data;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SunriseSunsetApi {

    String BASE_URL = "https://api.sunrise-sunset.org/";

    @GET("json?date=today&formatted=1")
    Call<Result> getResult(@Query("lat") float latitude, @Query("lng") float longitude);
}
