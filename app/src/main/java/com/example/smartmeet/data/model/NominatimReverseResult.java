package com.example.smartmeet.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class NominatimReverseResult {
    @SerializedName("boundingbox")
    private List<String> boundingbox; // ["minLat", "maxLat", "minLon", "maxLon"]

    public List<String> getBoundingbox() { return boundingbox; }
}
