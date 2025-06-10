package com.example.smartmeet.data.network;

import com.example.smartmeet.data.model.RouteRequest;
import com.example.smartmeet.data.model.RouteResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;


public interface OpenRouteServiceApi {
    // Ganti YOUR_ORS_API_KEY dengan API key Anda
    // Profil bisa: driving-car, cycling-regular, foot-walking, dll.
    @POST("/v2/directions/{profile}/json")
    Call<RouteResponse> getRoute(
            @Header("Authorization") String apiKey,
            @Path("profile") String profile,
            @Query("geometry_format") String geometryFormat,
            @Body RouteRequest body
    );
}