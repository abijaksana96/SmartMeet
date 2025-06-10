package com.example.smartmeet.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.smartmeet.R;
import com.example.smartmeet.data.model.OverpassElement;
import com.example.smartmeet.data.model.OverpassQueryResult;
import com.example.smartmeet.data.model.Venue;
import com.example.smartmeet.data.network.ApiClient;
import com.example.smartmeet.data.network.OverpassApiService;
import com.example.smartmeet.ui.adapter.VenueAdapter;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResultsFragment extends Fragment implements VenueAdapter.OnVenueClickListener {
    private MapView map = null;
    private double midpointLat, midpointLon;
    private ArrayList<String> participantLats, participantLons;
    private String selectedAmenity;
    private OverpassApiService overpassService;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private List<OverpassElement> venueResults = new ArrayList<>();
    private RecyclerView recyclerViewVenues;
    private VenueAdapter venueAdapter;
    private List<Venue> processedVenues = new ArrayList<>();
    private ArrayList<GeoPoint> participantGeoPoints = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            midpointLat = getArguments().getDouble("midpoint_lat");
            midpointLon = getArguments().getDouble("midpoint_lon");
            participantLats = getArguments().getStringArrayList("participant_lats");
            participantLons = getArguments().getStringArrayList("participant_lons");
            selectedAmenity = getArguments().getString("amenity");
            Log.d("ResultsFragment", "Midpoint diterima: " + midpointLat + "," + midpointLon);
            overpassService = ApiClient.getOverpassApiService();
        }
        Context ctx = getActivity().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(getActivity().getPackageName());

        if (participantLats != null && participantLons != null) {
            for (int i = 0; i < participantLats.size(); i++) {
                try {
                    double lat = Double.parseDouble(participantLats.get(i));
                    double lon = Double.parseDouble(participantLons.get(i));
                    participantGeoPoints.add(new GeoPoint(lat, lon));
                } catch (NumberFormatException e) {
                    Log.e("ResultsFragment", "Error parsing participant coordinates", e);
                }
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_results, container, false);
        map = view.findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);

        GeoPoint startPoint = new GeoPoint(midpointLat, midpointLon);
        map.getController().setCenter(startPoint);

        // Tambahkan marker untuk midpoint
        Marker midpointMarker = new Marker(map);
        midpointMarker.setPosition(startPoint);
        midpointMarker.setTitle("Titik Tengah");
        midpointMarker.setIcon(getBitmapDrawableFromVector(R.drawable.ic_midpoint));
        midpointMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(midpointMarker);

        // Tambahkan marker untuk setiap peserta
        if (participantLats != null && participantLons != null) {
            for (int i = 0; i < participantLats.size(); i++) {
                try {
                    double lat = Double.parseDouble(participantLats.get(i));
                    double lon = Double.parseDouble(participantLons.get(i));
                    GeoPoint participantPoint = new GeoPoint(lat, lon);
                    Marker participantMarker = new Marker(map);
                    participantMarker.setPosition(participantPoint);
                    participantMarker.setAnchor(0.5f, 0.9f);
                    participantMarker.setTitle("Peserta " + (i + 1));
                    participantMarker.setIcon(getBitmapDrawableFromVector(R.drawable.ic_participant));
                    participantMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    map.getOverlays().add(participantMarker);
                } catch (NumberFormatException e) {
                    Log.e("ResultsFragment", "Invalid participant lat/lon", e);
                }
            }
        }
        map.invalidate();

        recyclerViewVenues = view.findViewById(R.id.recycler_view_venues);
        recyclerViewVenues.setLayoutManager(new LinearLayoutManager(getContext()));
        venueAdapter = new VenueAdapter(processedVenues, (VenueAdapter.OnVenueClickListener) this);
        recyclerViewVenues.setAdapter(venueAdapter);

        if (midpointLat != 0 && midpointLon != 0 && selectedAmenity != null) {
            fetchVenues(midpointLat, midpointLon, selectedAmenity);
        }

        LinearLayout bottomSheet = view.findViewById(R.id.bottom_sheet);
        BottomSheetBehavior<LinearLayout> bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.setPeekHeight(100);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }

    private void fetchVenues(double lat, double lon, String amenity) {
        String amenityQueryTag = "node[amenity=" + amenity.toLowerCase() + "]";
        if (amenity.equalsIgnoreCase("Halte")) {
            amenityQueryTag = "node[highway=bus_stop]";
        } else if (amenity.equalsIgnoreCase("Restoran")) {
            amenityQueryTag = "node[amenity=restaurant]";
        }
        String query = "[out:json][timeout:25];" +
                "(" + amenityQueryTag + "(around:2000," + lat + "," + lon + ");" +
                ");out body center;";
        Log.d("ResultsFragment", "Overpass Query: " + query);
        RequestBody body = RequestBody.create(MediaType.parse("text/plain"), query);

        executorService.execute(() -> {
            overpassService.queryOverpass(body).enqueue(new Callback<OverpassQueryResult>() {
                @Override
                public void onResponse(Call<OverpassQueryResult> call, Response<OverpassQueryResult> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        venueResults = response.body().getElements();
                        Log.d("ResultsFragment", "Venues found: " + venueResults.size());
                        getActivity().runOnUiThread(() -> processAndDisplayVenues(venueResults));
                        addVenueMarkersToMap(venueResults);
                    } else {
                        Log.e("ResultsFragment", "Overpass API call failed: " + response.code() + " " + response.message());
                        try {
                            Log.e("ResultsFragment", "Error body: " + response.errorBody().string());
                        } catch (Exception e) { e.printStackTrace(); }
                        Toast.makeText(getContext(), "Gagal mencari tempat: " + response.message(), Toast.LENGTH_LONG).show();
                    }
                }
                @Override
                public void onFailure(Call<OverpassQueryResult> call, Throwable t) {
                    Log.e("ResultsFragment", "Overpass API call error", t);
                    Toast.makeText(getContext(), "Error Overpass: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void processAndDisplayVenues(List<OverpassElement> elements) {
        processedVenues.clear();
        GeoPoint midpointGeo = new GeoPoint(midpointLat, midpointLon);
        for (OverpassElement element : elements) {
            if (element.getLat() != 0 && element.getLon() != 0 && element.getName() != null && !element.getName().isEmpty()) {
                Venue venue = new Venue(
                        element.getId(),
                        element.getName(),
                        element.getLat(),
                        element.getLon(),
                        element.getAmenityType() != null ? element.getAmenityType() : selectedAmenity
                );
                double distToMid = midpointGeo.distanceToAsDouble(new GeoPoint(venue.getLatitude(), venue.getLongitude()));
                venue.setDistanceToMidpoint(distToMid);
                processedVenues.add(venue);
            }
        }
        Collections.sort(processedVenues, Comparator.comparingDouble(Venue::getDistanceToMidpoint));
        List<Venue> topVenues = processedVenues.size() > 5 ? new ArrayList<>(processedVenues.subList(0, 5)) : new ArrayList<>(processedVenues);
        venueAdapter.updateVenues(topVenues);
        addVenueMarkersToMapFromProcessed(topVenues);
    }

    private void addVenueMarkersToMap(List<OverpassElement> venues) {
        if (map == null || venues == null) return;
        for (OverpassElement venue : venues) {
            if (venue.getLat() != 0 && venue.getLon() != 0) {
                GeoPoint venuePoint = new GeoPoint(venue.getLat(), venue.getLon());
                Marker venueMarker = new Marker(map);
                venueMarker.setPosition(venuePoint);
                venueMarker.setTitle(venue.getName() != null ? venue.getName() : venue.getAmenityType());
                venueMarker.setIcon(getBitmapDrawableFromVector(R.drawable.ic_venue));
                venueMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                map.getOverlays().add(venueMarker);
            }
        }
        map.invalidate();
    }

    private void addVenueMarkersToMapFromProcessed(List<Venue> venues) {
        if (map == null || venues == null) return;
        for (Venue venue : venues) {
            GeoPoint venuePoint = new GeoPoint(venue.getLatitude(), venue.getLongitude());
            Marker venueMarker = new Marker(map);
            venueMarker.setPosition(venuePoint);
            venueMarker.setTitle(venue.getName());
            venueMarker.setSubDescription(String.format(Locale.getDefault(), "%.0f m dari titik tengah", venue.getDistanceToMidpoint()));
            venueMarker.setIcon(getBitmapDrawableFromVector(R.drawable.ic_venue));
            venueMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            map.getOverlays().add(venueMarker);
        }
        map.invalidate();
    }

    // Fungsi reusable untuk menerapkan custom icon vector ke Marker
    private Drawable getBitmapDrawableFromVector(int drawableResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(requireContext(), drawableResId);
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
    public void onVenueClick(Venue venue) {
        Log.d("ResultsFragment", "Kirim ke DetailActivity: venue=" + venue +
                ", midpointLat=" + midpointLat + ", midpointLon=" + midpointLon);
        Intent intent = new Intent(getActivity(), com.example.smartmeet.ui.DetailActivity.class);
        intent.putExtra("VENUE_DATA", venue);
        intent.putExtra("MIDPOINT_LAT", midpointLat);
        intent.putExtra("MIDPOINT_LON", midpointLon);
        startActivity(intent);
    }
}