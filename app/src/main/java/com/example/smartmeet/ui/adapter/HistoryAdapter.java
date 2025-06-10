package com.example.smartmeet.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.chip.Chip;
import com.example.smartmeet.R;
import com.example.smartmeet.data.local.SearchHistory;
import com.example.smartmeet.data.model.Venue;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<SearchHistory> historyList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(SearchHistory item);
        void onDeleteClick(SearchHistory item);
    }

    public HistoryAdapter(List<SearchHistory> historyList, OnItemClickListener listener) {
        this.historyList = historyList;
        this.listener = listener;
    }

    public void updateData(List<SearchHistory> newList) {
        this.historyList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        SearchHistory history = historyList.get(position);
        holder.bind(history, listener);
    }

    @Override
    public int getItemCount() {
        return historyList == null ? 0 : historyList.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvInput, tvAmenity;
        Chip chipVenues;
        ImageButton deleteButton;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.item_history_date);
            tvInput = itemView.findViewById(R.id.item_history_input);
            tvAmenity = itemView.findViewById(R.id.item_history_amenity);
            chipVenues = itemView.findViewById(R.id.item_history_venues);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }

        void bind(SearchHistory history, OnItemClickListener listener) {
            // Tanggal & waktu
            String dateStr = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    .format(new Date(history.getTimestamp()));
            tvDate.setText(dateStr);

            // Alamat
            List<String> addresses = history.getInputAddresses();
            String inputAddr = addresses != null && !addresses.isEmpty()
                    ? String.join(", ", addresses)
                    : "Alamat tidak tersedia";
            tvInput.setText(inputAddr);

            // Amenity
            String amenity = history.getAmenity() != null ? history.getAmenity() : "-";
            tvAmenity.setText(amenity);

            // Jumlah venue
            List<Venue> venues = history.getResultVenues();
            int venueCount = venues != null ? venues.size() : 0;
            String chipText = venueCount + " venue ditemukan";
            chipVenues.setText(chipText);

            // Item click (seluruh card)
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(history);
            });

            deleteButton.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(history);
            });
        }
    }
}