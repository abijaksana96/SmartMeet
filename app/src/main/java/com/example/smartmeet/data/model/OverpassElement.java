package com.example.smartmeet.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class OverpassElement {
    @SerializedName("type")
    private String type;
    @SerializedName("id")
    private long id;
    @SerializedName("lat")
    private double lat;
    @SerializedName("lon")
    private double lon;
    @SerializedName("tags")
    private Map<String, String> tags; // Akan berisi "name", "amenity", dll.

    public long getId() { return id; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }
    public Map<String, String> getTags() { return tags; }
    public String getName() { return tags != null ? tags.get("name") : null; }
    public String getAmenityType() { return tags != null ? tags.get("amenity") : null; }

}
