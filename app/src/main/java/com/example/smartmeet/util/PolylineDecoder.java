package com.example.smartmeet.util;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class PolylineDecoder {
    public static List<GeoPoint> decode(final String encodedPath, boolean isPolyline6) {
        int len = encodedPath.length();
        final List<GeoPoint> path = new ArrayList<>(len / 2);
        int index = 0;
        int lat = 0;
        int lng = 0;
        int precision = isPolyline6 ? 6 : 5; // For ORS, typically 5 (geojson geometry)

        while (index < len) {
            int result = 1;
            int shift = 0;
            int b;
            do {
                b = encodedPath.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lat += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            result = 1;
            shift = 0;
            do {
                b = encodedPath.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lng += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            if (isPolyline6) {
                path.add(new GeoPoint(lat * 1e-6, lng * 1e-6));
            } else {
                path.add(new GeoPoint(lat * 1e-5, lng * 1e-5));
            }
        }
        return path;
    }
}