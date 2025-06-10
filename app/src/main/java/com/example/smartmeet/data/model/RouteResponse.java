package com.example.smartmeet.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RouteResponse {
    @SerializedName("routes")
    private List<Route> routes;

    public List<Route> getRoutes() { return routes; }

    public static class Route {
        @SerializedName("geometry")
        private String geometry; // Encoded polyline

        @SerializedName("summary")
        private Summary summary;

        public String getGeometry() { return geometry; }
        public Summary getSummary() { return summary; }
    }

    public static class Summary {
        @SerializedName("distance")
        private double distance;
        @SerializedName("duration")
        private double duration;
        public double getDistance() { return distance; }
        public double getDuration() { return duration; }
    }
}