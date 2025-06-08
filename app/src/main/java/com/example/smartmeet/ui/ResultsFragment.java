package com.example.smartmeet.ui;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.smartmeet.R;
import com.example.smartmeet.data.model.OverpassElement;
import com.example.smartmeet.data.model.OverpassQueryResult;
import com.example.smartmeet.data.network.ApiClient;
import com.example.smartmeet.data.network.OverpassApiService;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResultsFragment extends Fragment {
    private MapView map = null;
    private double midpointLat, midpointLon;
    private ArrayList<String> participantLats, participantLons;
    private String selectedAmenity;
    private OverpassApiService overpassService;
    private ExecutorService executorService = Executors.newSingleThreadExecutor(); // Atau gunakan yang sudah ada dari InputFragment jika di-pass
    private List<OverpassElement> venueResults = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ambil data dari Bundle
        if (getArguments() != null) {
            midpointLat = getArguments().getDouble("midpoint_lat");
            midpointLon = getArguments().getDouble("midpoint_lon");
            participantLats = getArguments().getStringArrayList("participant_lats");
            participantLons = getArguments().getStringArrayList("participant_lons");
            selectedAmenity = getArguments().getString("amenity");
            Log.d("ResultsFragment", "Midpoint diterima: " + midpointLat + "," + midpointLon);

            overpassService = ApiClient.getOverpassApiService(); // Initialize Overpass service
        }

        // Konfigurasi OSMDroid (penting, lakukan sekali)
        Context ctx = getActivity().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(getActivity().getPackageName());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_results, container, false);
        map = view.findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK); // Atau tile source lain
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0); // Set zoom awal

        // Pusatkan peta ke midpoint
        GeoPoint startPoint = new GeoPoint(midpointLat, midpointLon);
        map.getController().setCenter(startPoint);

        // Tambahkan marker untuk midpoint
        Marker midpointMarker = new Marker(map);
        midpointMarker.setPosition(startPoint);
        midpointMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        midpointMarker.setTitle("Titik Tengah");
        // midpointMarker.setIcon(getResources().getDrawable(R.drawable.ic_midpoint_marker)); // Custom icon
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
                    participantMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    participantMarker.setTitle("Peserta " + (i + 1));
                    // participantMarker.setIcon(getResources().getDrawable(R.drawable.ic_participant_marker)); // Custom icon
                    map.getOverlays().add(participantMarker);
                } catch (NumberFormatException e) {
                    Log.e("ResultsFragment", "Invalid participant lat/lon", e);
                }
            }
        }
        map.invalidate(); // Refresh peta

        // Setelah peta siap, panggil API untuk cari tempat
        if (midpointLat != 0 && midpointLon != 0 && selectedAmenity != null) {
            fetchVenues(midpointLat, midpointLon, selectedAmenity);
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume(); // Diperlukan untuk OSMDroid
    }

    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();  // Diperlukan untuk OSMDroid
    }

    private void fetchVenues(double lat, double lon, String amenity) {
        // Tampilkan loading indicator
        String amenityQueryTag = "node[amenity=" + amenity.toLowerCase() + "]"; // Sesuaikan dengan tag Overpass
        if (amenity.equalsIgnoreCase("Halte")) { // Contoh mapping
            amenityQueryTag = "node[highway=bus_stop]";
        } else if (amenity.equalsIgnoreCase("Restoran")) {
            amenityQueryTag = "node[amenity=restaurant]";
        }


        String query = "[out:json][timeout:25];" +
                "(" + amenityQueryTag + "(around:2000," + lat + "," + lon + ");" +
                ");out body center;"; // 'center' untuk mendapatkan lat/lon dari way/relation juga

        Log.d("ResultsFragment", "Overpass Query: " + query);
        RequestBody body = RequestBody.create(MediaType.parse("text/plain"), query);

        executorService.execute(() -> {
            overpassService.queryOverpass(body).enqueue(new Callback<OverpassQueryResult>() {
                @Override
                public void onResponse(Call<OverpassQueryResult> call, Response<OverpassQueryResult> response) {
                    // Sembunyikan loading indicator
                    if (response.isSuccessful() && response.body() != null) {
                        venueResults = response.body().getElements();
                        Log.d("ResultsFragment", "Venues found: " + venueResults.size());
                        // Lanjutkan ke parsing hasil dan tampilkan di RecyclerView (Hari ke-5)
                        // Juga tambahkan marker untuk venue di peta
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
                    // Sembunyikan loading indicator
                    Log.e("ResultsFragment", "Overpass API call error", t);
                    Toast.makeText(getContext(), "Error Overpass: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void addVenueMarkersToMap(List<OverpassElement> venues) {
        if (map == null || venues == null) return;
        for (OverpassElement venue : venues) {
            if (venue.getLat() != 0 && venue.getLon() != 0) { // Pastikan ada koordinat
                GeoPoint venuePoint = new GeoPoint(venue.getLat(), venue.getLon());
                Marker venueMarker = new Marker(map);
                venueMarker.setPosition(venuePoint);
                venueMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                venueMarker.setTitle(venue.getName() != null ? venue.getName() : venue.getAmenityType());
                // venueMarker.setIcon(getResources().getDrawable(R.drawable.ic_venue_marker)); // Custom icon
                map.getOverlays().add(venueMarker);
            }
        }
        map.invalidate(); // Refresh peta
    }
}