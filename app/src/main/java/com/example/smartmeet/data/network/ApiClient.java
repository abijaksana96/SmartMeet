package com.example.smartmeet.data.network;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org";
    private static Retrofit nominatimRetrofit = null;
    private static final String OVERPASS_BASE_URL = "https://overpass-api.de";
    private static Retrofit overpassRetrofit = null;

    public static NominatimApiService getNominatimApiService() {
        if (nominatimRetrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY); // Level BODY akan mencetak request/response body

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(chain -> { // Ini interceptor untuk User-Agent
                        Request request = chain.request()
                                .newBuilder()
                                .header("User-Agent", "SmartMeet/1.0 (email@domain.com)")
                                .build();
                        return chain.proceed(request);
                    })
                    .addInterceptor(logging)
                    .build();

            nominatimRetrofit = new Retrofit.Builder()
                    .baseUrl(NOMINATIM_BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return nominatimRetrofit.create(NominatimApiService.class);
    }

    public static OverpassApiService getOverpassApiService() {
        if (overpassRetrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();

            overpassRetrofit = new Retrofit.Builder()
                    .baseUrl(OVERPASS_BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return overpassRetrofit.create(OverpassApiService.class);
    }
}