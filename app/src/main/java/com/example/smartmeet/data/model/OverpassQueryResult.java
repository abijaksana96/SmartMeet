package com.example.smartmeet.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class OverpassQueryResult {
    @SerializedName("elements")
    private List<OverpassElement> elements;

    public List<OverpassElement> getElements() { return elements; }
}