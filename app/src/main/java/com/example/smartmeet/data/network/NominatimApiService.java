package com.example.smartmeet.data.network;


import com.example.smartmeet.data.model.GeoResult;
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

}
