package com.example.smartmeet.data.network;

import com.example.smartmeet.data.model.OverpassQueryResult;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface OverpassApiService {
    @POST("/api/interpreter")
    Call<OverpassQueryResult> queryOverpass(@Body RequestBody body);
}
