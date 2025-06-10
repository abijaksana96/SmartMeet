package com.example.smartmeet.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartmeet.R;
import com.example.smartmeet.data.local.SearchHistory;
import com.example.smartmeet.data.local.SearchHistoryDbHandler;
import com.example.smartmeet.ui.adapter.HistoryAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class HistoryFragment extends Fragment implements HistoryAdapter.OnItemClickListener {
    private SearchHistoryDbHandler dbHandler;
    private List<SearchHistory> historyItems = new ArrayList<>();
    private HistoryAdapter adapter;
    private RecyclerView recyclerView;
    private View emptyStateLayout;
    private TextInputEditText searchEditText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.history_recycler_view);
        emptyStateLayout = view.findViewById(R.id.empty_state_layout);
        searchEditText = view.findViewById(R.id.search_edit_text);

        ImageButton backButton = view.findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        ImageButton clearAllButton = view.findViewById(R.id.clear_all_button);
        clearAllButton.setOnClickListener(v -> showDeleteAllConfirmationDialog());

        adapter = new HistoryAdapter(historyItems, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        dbHandler = new SearchHistoryDbHandler(requireContext());
        dbHandler.open();

        loadHistory();

        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterHistory(s.toString());
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void loadHistory() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<SearchHistory> data = dbHandler.getAllHistory();
            requireActivity().runOnUiThread(() -> {
                historyItems.clear();
                if (data != null) historyItems.addAll(data);
                adapter.updateData(new ArrayList<>(historyItems));
                handleEmptyState();
            });
        });
    }

    private void filterHistory(String query) {
        if (historyItems == null) return;
        List<SearchHistory> filtered = new ArrayList<>();
        for (SearchHistory item : historyItems) {
            String addresses = item.getInputAddresses() != null ? String.join(", ", item.getInputAddresses()) : "";
            String amenity = item.getAmenity() != null ? item.getAmenity() : "";
            if (addresses.toLowerCase().contains(query.toLowerCase())
                    || amenity.toLowerCase().contains(query.toLowerCase())) {
                filtered.add(item);
            }
        }
        adapter.updateData(filtered);
        emptyStateLayout.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void handleEmptyState() {
        boolean isEmpty = historyItems.isEmpty();
        emptyStateLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        if (dbHandler != null) {
            dbHandler.close();
            dbHandler = null;
        }
        super.onDestroyView();
    }

    @Override
    public void onItemClick(SearchHistory item) {
        // Implement item click action here
    }

    @Override
    public void onDeleteClick(SearchHistory item) {
        showDeleteConfirmationDialog(item);
    }

    private void showDeleteConfirmationDialog(SearchHistory item) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Hapus Riwayat")
                .setMessage("Apakah Anda yakin ingin menghapus riwayat pencarian ini?")
                .setPositiveButton("Hapus", (dialog, which) -> deleteHistoryItem(item))
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showDeleteAllConfirmationDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Hapus Semua Riwayat")
                .setMessage("Apakah Anda yakin ingin menghapus semua riwayat pencarian?")
                .setPositiveButton("Hapus Semua", (dialog, which) -> deleteAllHistory())
                .setNegativeButton("Batal", null)
                .show();
    }

    private void deleteHistoryItem(SearchHistory item) {
        Executors.newSingleThreadExecutor().execute(() -> {
            dbHandler.deleteHistory(item.getTimestamp());
            requireActivity().runOnUiThread(() -> {
                historyItems.remove(item);
                adapter.updateData(new ArrayList<>(historyItems));
                handleEmptyState();
                showSnackbar("Riwayat berhasil dihapus");
            });
        });
    }

    private void deleteAllHistory() {
        Executors.newSingleThreadExecutor().execute(() -> {
            dbHandler.deleteAllHistory();
            requireActivity().runOnUiThread(() -> {
                historyItems.clear();
                adapter.updateData(new ArrayList<>());
                handleEmptyState();
                showSnackbar("Semua riwayat berhasil dihapus");
            });
        });
    }

    private void showSnackbar(String message) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show();
    }
}