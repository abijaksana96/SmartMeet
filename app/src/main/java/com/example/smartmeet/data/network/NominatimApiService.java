package com.example.smartmeet.data.network;


import com.example.smartmeet.data.model.GeoResult;
import com.example.smartmeet.data.model.NominatimReverseResult;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NominatimApiService {
    @GET("/search")
    Call<List<GeoResult>> search(
            @Query("q") String address,
            @Query("format") String format,
            @Query("limit") int limit,
            @Query("addressdetails") int addressDetails
    );

    @GET("/search")
    Call<List<GeoResult>> autocomplete(
            @Query("q") String query,
            @Query("format") String format,
            @Query("limit") int limit
    );

    @GET("/reverse?")
    Call<NominatimReverseResult> reverseSearch(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("format") String format
    );

}
