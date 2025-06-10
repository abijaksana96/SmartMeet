package com.example.smartmeet.data.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Venue implements Parcelable {
    private long id;
    private String name;
    private double latitude;
    private double longitude;
    private String type; // e.g., "cafe", "restaurant"
    private double distanceToMidpoint; // Jarak dari midpoint ke venue
    // Bisa juga tambahkan List<Double> distancesToParticipants;

    public Venue(long id, String name, double latitude, double longitude, String type) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
    }

    // Getter dan Setter
    public long getId() { return id; }
    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getType() { return type; }
    public double getDistanceToMidpoint() { return distanceToMidpoint; }
    public void setDistanceToMidpoint(double distanceToMidpoint) { this.distanceToMidpoint = distanceToMidpoint; }


    // Implementasi Parcelable agar bisa dikirim via Intent
    protected Venue(Parcel in) {
        id = in.readLong();
        name = in.readString();
        latitude = in.readDouble();
        longitude = in.readDouble();
        type = in.readString();
        distanceToMidpoint = in.readDouble();
    }

    public static final Creator<Venue> CREATOR = new Creator<Venue>() {
        @Override
        public Venue createFromParcel(Parcel in) {
            return new Venue(in);
        }

        @Override
        public Venue[] newArray(int size) {
            return new Venue[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeString(type);
        dest.writeDouble(distanceToMidpoint);
    }
}