package com.example.smartmeet.util;

import android.util.Log;

import com.example.smartmeet.data.model.GeoResult;

import java.util.List;

public class LocationUtil {
    public static class Midpoint {
        public double latitude;
        public double longitude;
        public Midpoint(double lat, double lon) {
            this.latitude = lat;
            this.longitude = lon;
        }
    }

    public Midpoint calculateMidpoint(List<GeoResult> results) {
        if (results == null || results.isEmpty()) {
            return null;
        }
        double sumLat = 0;
        double sumLon = 0;
        int count = 0;
        for (GeoResult result : results) {
            try {
                sumLat += Double.parseDouble(result.getLat());
                sumLon += Double.parseDouble(result.getLon());
                count++;
            } catch (NumberFormatException e) {
                Log.e("MidpointCalc", "Invalid lat/lon format: " + result.getLat() + "," + result.getLon());
            }
        }
        if (count == 0) return null;
        return new Midpoint(sumLat / count, sumLon / count);
    }

    public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Radius bumi dalam meter
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // Jarak dalam meter
    }
}
