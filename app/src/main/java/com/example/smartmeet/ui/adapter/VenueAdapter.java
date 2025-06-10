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
import java.util.List;
import java.util.Locale;

public class VenueAdapter extends RecyclerView.Adapter<VenueAdapter.VenueViewHolder> {

    private List<Venue> venueList;
    private OnVenueClickListener listener;

    public interface OnVenueClickListener {
        void onVenueClick(Venue venue);
    }

    public VenueAdapter(List<Venue> venueList, OnVenueClickListener listener) {
        this.venueList = venueList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VenueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_venue, parent, false);
        return new VenueViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull VenueViewHolder holder, int position) {
        Venue currentVenue = venueList.get(position);
        holder.venueName.setText(currentVenue.getName());
        holder.venueType.setText(currentVenue.getType());
        holder.venueDistance.setText(String.format(Locale.getDefault(), "%.0f m", currentVenue.getDistanceToMidpoint()));
        // Set ikon berdasarkan tipe venue (opsional)
        // holder.venueIcon.setImageResource(getIconForType(currentVenue.getType()));

        holder.itemView.setOnClickListener(v -> listener.onVenueClick(currentVenue));
    }

    @Override
    public int getItemCount() {
        return venueList.size();
    }

    public void updateVenues(List<Venue> newVenues) {
        this.venueList.clear();
        this.venueList.addAll(newVenues);
        notifyDataSetChanged();
    }

    static class VenueViewHolder extends RecyclerView.ViewHolder {
        ImageView venueIcon;
        TextView venueName, venueType, venueDistance;

        VenueViewHolder(View view) {
            super(view);
            venueIcon = view.findViewById(R.id.venue_icon);
            venueName = view.findViewById(R.id.venue_name);
            venueType = view.findViewById(R.id.venue_type);
            venueDistance = view.findViewById(R.id.venue_distance);
        }
    }
}