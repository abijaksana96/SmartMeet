package com.example.smartmeet.data.model;
import com.google.gson.annotations.SerializedName;

public class GeoResult {
    @SerializedName("lat")
    private String lat;

    @SerializedName("lon")
    private String lon;

    @SerializedName("display_name")
    private String displayName;

    // Getter methods
    public String getLat() { return lat; }
    public String getLon() { return lon; }
    public String getDisplayName() { return displayName; }
}