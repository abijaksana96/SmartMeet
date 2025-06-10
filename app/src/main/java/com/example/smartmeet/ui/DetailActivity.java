package com.example.smartmeet.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.example.smartmeet.R;
import com.example.smartmeet.data.model.Venue;
import com.example.smartmeet.data.model.RouteRequest;
import com.example.smartmeet.data.model.RouteResponse;
import com.example.smartmeet.data.network.ApiClient;
import com.example.smartmeet.data.network.OpenRouteServiceApi;
import com.example.smartmeet.util.PolylineDecoder;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailActivity extends AppCompatActivity {

    private MapView detailMap;
    private TextView venueNameText, venueTypeText;
    private Button openRouteExternalButton;
    private Venue venue;
    private double midpointLat, midpointLon;
    private OpenRouteServiceApi orsService;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // OSMDroid Configuration
        Log.d("DetailActivity", "OSMDroid config start");
        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setUserAgentValue(getPackageName());
        Log.d("DetailActivity", "OSMDroid config done");

        setContentView(R.layout.activity_detail);

        detailMap = findViewById(R.id.detail_map);
        venueNameText = findViewById(R.id.detail_venue_name);
        venueTypeText = findViewById(R.id.detail_venue_type);
        openRouteExternalButton = findViewById(R.id.button_open_route_external);

        orsService = ApiClient.getOpenRouteServiceApi();

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("VENUE_DATA")) {
            venue = intent.getParcelableExtra("VENUE_DATA");
            midpointLat = intent.getDoubleExtra("MIDPOINT_LAT", 0);
            midpointLon = intent.getDoubleExtra("MIDPOINT_LON", 0);
            Log.d("DetailActivity", "Terima venue=" + venue +
                    ", midpointLat=" + midpointLat + ", midpointLon=" + midpointLon);

            if (venue != null) {
                Log.d("DetailActivity", "Venue detail: id=" + venue.getId() + ", name=" + venue.getName()
                        + ", lat=" + venue.getLatitude() + ", lon=" + venue.getLongitude() + ", type=" + venue.getType());
            }
        } else {
            Log.e("DetailActivity", "Intent venue data missing!");
        }

        if (venue != null) {
            venueNameText.setText(venue.getName());
            venueTypeText.setText(venue.getType());
            setupMap();
            if (midpointLat != 0 && midpointLon != 0) {
                fetchRoute();
            } else {
                Log.e("DetailActivity", "Midpoint data is zero or missing: lat=" + midpointLat + ", lon=" + midpointLon);
            }
        } else {
            Toast.makeText(this, "Data venue tidak ditemukan", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Lanjutan dari kode kamu (tepat setelah else { Toast.makeText ... finish(); ... }

        openRouteExternalButton.setOnClickListener(v -> {
            if (venue != null) {
                String uri = String.format(Locale.ENGLISH, "geo:%f,%f?q=%f,%f(%s)",
                        venue.getLatitude(), venue.getLongitude(), venue.getLatitude(), venue.getLongitude(), venue.getName());
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    // Fallback ke browser jika Google Maps tidak ada
                    Intent webIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/maps/search/?api=1&query=" + venue.getLatitude() + "," + venue.getLongitude()));
                    startActivity(webIntent);
                }
            }
        });
    }

    private void setupMap() {
        detailMap.setTileSource(TileSourceFactory.MAPNIK);
        detailMap.setMultiTouchControls(true);
        detailMap.getController().setZoom(16.0);

        GeoPoint venuePoint = new GeoPoint(venue.getLatitude(), venue.getLongitude());

        Marker venueMarker = new Marker(detailMap);
        venueMarker.setPosition(venuePoint);
        venueMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        venueMarker.setTitle(venue.getName());
        venueMarker.setIcon(getBitmapDrawableFromVector(R.drawable.ic_venue));
        detailMap.getOverlays().add(venueMarker);

        if (midpointLat != 0 && midpointLon != 0) {
            GeoPoint midpointPoint = new GeoPoint(midpointLat, midpointLon);
            Marker midpointMarker = new Marker(detailMap);
            midpointMarker.setPosition(midpointPoint);
            midpointMarker.setTitle("Titik Tengah Awal");
            midpointMarker.setIcon(getBitmapDrawableFromVector(R.drawable.ic_midpoint));
            detailMap.getOverlays().add(midpointMarker);

            double north = Math.max(venue.getLatitude(), midpointLat);
            double south = Math.min(venue.getLatitude(), midpointLat);
            double east = Math.max(venue.getLongitude(), midpointLon);
            double west = Math.min(venue.getLongitude(), midpointLon);

            Log.d("DetailActivity", String.format("BBox coords: N=%.8f, E=%.8f, S=%.8f, W=%.8f", north, east, south, west));

            if (isLatValid(north) && isLatValid(south) && isLonValid(east) && isLonValid(west)
                    && north > south && east > west) {
                try {
                    BoundingBox boundingBox = new BoundingBox(north, east, south, west);
                    detailMap.zoomToBoundingBox(boundingBox, true, 100);
                } catch (IllegalArgumentException e) {
                    Log.e("DetailActivity", "BoundingBox manual out of range: " + e.getMessage());
                    detailMap.getController().setCenter(venuePoint);
                    detailMap.getController().setZoom(16.0);
                }
            } else {
                Log.e("DetailActivity", "Invalid bounding box: N=" + north + " S=" + south + " E=" + east + " W=" + west);
                detailMap.getController().setCenter(venuePoint);
                detailMap.getController().setZoom(16.0);
            }
        } else {
            detailMap.getController().setCenter(venuePoint);
            detailMap.getController().setZoom(16.0);
        }
        detailMap.invalidate();
    }

    private void fetchRoute() {
        List<List<Double>> coordinates = Arrays.asList(
                Arrays.asList(midpointLon, midpointLat),
                Arrays.asList(venue.getLongitude(), venue.getLatitude())
        );
        RouteRequest request = new RouteRequest(coordinates);

        // Log JSON RouteRequest
        Gson gson = new Gson();
        String jsonRequest = gson.toJson(request);
        Log.d("DetailActivity", "RouteRequest JSON: " + jsonRequest);

        executorService.execute(() -> {
            orsService.getRoute(ApiClient.ORS_API_KEY, "driving-car","encodedpolyline", request)
                    .enqueue(new Callback<RouteResponse>() {
                        @Override
                        public void onResponse(Call<RouteResponse> call, Response<RouteResponse> response) {
                            if (response.isSuccessful() && response.body() != null &&
                                    response.body().getRoutes() != null && !response.body().getRoutes().isEmpty()) {
                                String encodedPolyline = response.body().getRoutes().get(0).getGeometry();
                                if (encodedPolyline != null && !encodedPolyline.isEmpty()) {
                                    List<GeoPoint> routePoints = PolylineDecoder.decode(encodedPolyline, false); // false: polyline5
                                    if (!routePoints.isEmpty()) {
                                        runOnUiThread(() -> drawRoute(routePoints));
                                    }
                                } else {
                                    Log.e("DetailActivity", "Encoded polyline kosong dari ORS");
                                }
                            } else {
                                Log.e("DetailActivity", "ORS Route failed: " + response.message() + " code=" + response.code());
                                try {
                                    if (response.errorBody() != null) {
                                        String errorBody = response.errorBody().string();
                                        Log.e("DetailActivity", "ORS Error Body: " + errorBody);
                                    }
                                } catch (IOException e) {
                                    Log.e("DetailActivity", "Error reading errorBody", e);
                                }
                                runOnUiThread(() -> Toast.makeText(DetailActivity.this, "Gagal mendapatkan rute: " + response.message(), Toast.LENGTH_LONG).show());
                            }
                        }

                        @Override
                        public void onFailure(Call<RouteResponse> call, Throwable t) {
                            Log.e("DetailActivity", "ORS Route error", t);
                            runOnUiThread(() -> Toast.makeText(DetailActivity.this, "Error rute: " + t.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    });
        });
    }

    private void drawRoute(List<GeoPoint> routePoints) {
        Polyline routePolyline = new Polyline();
        routePolyline.setPoints(routePoints);
        routePolyline.getOutlinePaint().setColor(Color.BLUE);
        routePolyline.getOutlinePaint().setStrokeWidth(8f);
        detailMap.getOverlays().add(0, routePolyline);

        // Zoom ke rute
        if (routePoints.size() >= 2) {
            double north = Double.NEGATIVE_INFINITY, south = Double.POSITIVE_INFINITY;
            double east = Double.NEGATIVE_INFINITY, west = Double.POSITIVE_INFINITY;
            for (GeoPoint p : routePoints) {
                north = Math.max(north, p.getLatitude());
                south = Math.min(south, p.getLatitude());
                east = Math.max(east, p.getLongitude());
                west = Math.min(west, p.getLongitude());
            }
            if (isLatValid(north) && isLatValid(south) && isLonValid(east) && isLonValid(west) && north > south && east > west) {
                try {
                    BoundingBox routeBoundingBox = new BoundingBox(north, east, south, west);
                    detailMap.zoomToBoundingBox(routeBoundingBox, true, 150);
                } catch (IllegalArgumentException e) {
                    Log.e("DetailActivity", "Route BoundingBox out of range: " + e.getMessage());
                    fallbackToCenter(routePoints);
                }
            } else {
                Log.e("DetailActivity", String.format("Invalid route bounding box: N=%.8f, S=%.8f, E=%.8f, W=%.8f",
                        north, south, east, west));
                fallbackToCenter(routePoints);
            }
        } else if (!routePoints.isEmpty()) {
            detailMap.getController().setCenter(routePoints.get(0));
        }

        detailMap.invalidate();
    }

    private void fallbackToCenter(List<GeoPoint> routePoints) {
        if (!routePoints.isEmpty()) {
            detailMap.getController().setCenter(routePoints.get(0));
            detailMap.getController().setZoom(16.0);
        }
    }

    private boolean isLatValid(double lat) {
        return !Double.isNaN(lat) && !Double.isInfinite(lat)
                && lat >= -85.05112877980658 && lat <= 85.05112877980658;
    }

    private boolean isLonValid(double lon) {
        return !Double.isNaN(lon) && !Double.isInfinite(lon)
                && lon >= -180 && lon <= 180;
    }

    private Drawable getBitmapDrawableFromVector(int drawableResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(this, drawableResId);
        if (vectorDrawable == null) return null;

        // Ubah vector ke bitmap 100x100 px
        int sizePx = 100;
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, sizePx, sizePx);
        vectorDrawable.draw(canvas);
        return new BitmapDrawable(getResources(), bitmap);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (detailMap != null) detailMap.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (detailMap != null) detailMap.onPause();
    }
}