package com.example.smartmeet.data.model;

import java.util.List;

public class RouteRequest {
    private List<List<Double>> coordinates;

    public RouteRequest(List<List<Double>> coordinates) {
        this.coordinates = coordinates;
    }

    public List<List<Double>> getCoordinates() { return coordinates; }
}