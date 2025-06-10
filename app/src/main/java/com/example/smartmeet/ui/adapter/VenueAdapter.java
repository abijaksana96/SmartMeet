package com.example.smartmeet.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smartmeet.R;
import com.example.smartmeet.data.model.Venue;
import com.google.android.material.chip.Chip;

import java.util.List;
import java.util.Locale;

public class VenueAdapter extends RecyclerView.Adapter<VenueAdapter.VenueViewHolder> {
    private List<Venue> venues;
    private final OnVenueClickListener listener;

    public interface OnVenueClickListener {
        void onVenueClick(Venue venue);
    }

    public VenueAdapter(List<Venue> venues, OnVenueClickListener listener) {
        this.venues = venues;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VenueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_venue, parent, false);
        return new VenueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VenueViewHolder holder, int position) {
        holder.bind(venues.get(position));
    }

    @Override
    public int getItemCount() {
        return venues != null ? venues.size() : 0;
    }

    public void updateVenues(List<Venue> newVenues) {
        this.venues = newVenues;
        notifyDataSetChanged();
    }

    class VenueViewHolder extends RecyclerView.ViewHolder {
        private final ImageView typeIcon;
        private final TextView venueName;
        private final TextView venueType;
        private final Chip distanceChip;

        VenueViewHolder(@NonNull View itemView) {
            super(itemView);
            typeIcon = itemView.findViewById(R.id.venue_type_icon);
            venueName = itemView.findViewById(R.id.venue_name);
            venueType = itemView.findViewById(R.id.venue_type);
            distanceChip = itemView.findViewById(R.id.distance_chip);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onVenueClick(venues.get(position));
                }
            });
        }

        void bind(Venue venue) {
            venueName.setText(venue.getName());
            venueType.setText(venue.getType());

            // Set icon berdasarkan tipe venue
            typeIcon.setImageResource(getVenueTypeIcon(venue.getType()));

            // Format jarak
            String distance = formatDistance(venue.getDistanceToMidpoint());
            distanceChip.setText(distance);
        }

        private int getVenueTypeIcon(String type) {
            switch (type.toLowerCase()) {
                case "cafe":
                    return R.drawable.ic_cafe;
                case "restoran":
                    return R.drawable.ic_restaurant;
                case "halte":
                    return R.drawable.ic_bus_stop;
                case "taman":
                    return R.drawable.ic_park;
                default:
                    return R.drawable.ic_venue; // fallback icon
            }
        }
        private String formatDistance(double distance) {
            if (distance < 1000) {
                return String.format(Locale.getDefault(), "%.0fm", distance);
            } else {
                return String.format(Locale.getDefault(), "%.1fkm", distance/1000);
            }
        }
    }
}