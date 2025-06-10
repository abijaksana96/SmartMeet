package com.example.smartmeet.data.local;

import com.example.smartmeet.data.model.Venue;
import java.util.List;

public class SearchHistory {
    private long id;
    private long timestamp;
    private List<String> inputAddresses;
    private String amenity;
    private List<Venue> resultVenues;

    public SearchHistory(long id, long timestamp, List<String> inputAddresses, String amenity, List<Venue> resultVenues) {
        this.id = id;
        this.timestamp = timestamp;
        this.inputAddresses = inputAddresses;
        this.amenity = amenity;
        this.resultVenues = resultVenues;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public List<String> getInputAddresses() { return inputAddresses; }
    public void setInputAddresses(List<String> inputAddresses) { this.inputAddresses = inputAddresses; }
    public String getAmenity() { return amenity; }
    public void setAmenity(String amenity) { this.amenity = amenity; }
    public List<Venue> getResultVenues() { return resultVenues; }
    public void setResultVenues(List<Venue> resultVenues) { this.resultVenues = resultVenues; }
}