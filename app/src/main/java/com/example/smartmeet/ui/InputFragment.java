package com.example.smartmeet.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import android.text.TextWatcher; // Import ini

import com.example.smartmeet.R;
import com.example.smartmeet.data.model.GeoResult;
import com.example.smartmeet.data.network.ApiClient;
import com.example.smartmeet.data.network.NominatimApiService;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap; // Tambahkan ini
import java.util.List;
import java.util.Map; // Tambahkan ini
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InputFragment extends Fragment {

    private Button buttonSearch, btnAdd, btnDel;
    private AutoCompleteTextView addressInput1, addressInput2, addressInput3, addressInput4, addressInput5;
    private Spinner amenitySpinner;
    private NominatimApiService nominatimService;

    private List<String> addresses = new ArrayList<>();
    private List<GeoResult> geocodedResults = new ArrayList<>();
    private int inputVisibleCount = 2;
    private final int maxInput = 5;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Call<List<GeoResult>> currentAutocompleteCall;

    // Gunakan Map untuk menyimpan Runnable debounce per AutoCompleteTextView
    private Map<AutoCompleteTextView, Runnable> debounceRunnables = new HashMap<>();
    private Map<AutoCompleteTextView, TextWatcher> textWatchers = new HashMap<>(); // Untuk membersihkan listener

    private static final long DEBOUNCE_DELAY = 500;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_input, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        addressInput1 = view.findViewById(R.id.address1);
        addressInput2 = view.findViewById(R.id.address2);
        addressInput3 = view.findViewById(R.id.tf_3).findViewById(R.id.address3);
        addressInput4 = view.findViewById(R.id.tf_4).findViewById(R.id.address4);
        addressInput5 = view.findViewById(R.id.tf_5).findViewById(R.id.address5);

        TextInputLayout[] inputLayouts = new TextInputLayout[]{
                view.findViewById(R.id.tf_3),
                view.findViewById(R.id.tf_4),
                view.findViewById(R.id.tf_5)
        };

        setupAutocompleteWithDebounce(addressInput1);
        setupAutocompleteWithDebounce(addressInput2);
        setupAutocompleteWithDebounce(addressInput3);
        setupAutocompleteWithDebounce(addressInput4);
        setupAutocompleteWithDebounce(addressInput5);

        amenitySpinner = view.findViewById(R.id.amenity_spinner);

        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(view.getContext(),
                R.array.amenities_array, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        amenitySpinner.setAdapter(spinnerAdapter);

        buttonSearch = view.findViewById(R.id.search_button);
        btnAdd = view.findViewById(R.id.add_button);
        btnDel = view.findViewById(R.id.del_button);

        nominatimService = ApiClient.getNominatimApiService();

        btnAdd.setOnClickListener(v -> {
            if (inputVisibleCount < maxInput) {
                inputLayouts[inputVisibleCount - 2].setVisibility(View.VISIBLE);
                inputVisibleCount++;
                if (inputVisibleCount == maxInput) {
                    btnAdd.setVisibility(View.GONE);
                }
            }
            btnDel.setVisibility(View.VISIBLE);
        });

        btnDel.setOnClickListener(v -> {
            if (inputVisibleCount > 2) {
                inputVisibleCount--;
                TextInputLayout currentLayoutToHide = inputLayouts[inputVisibleCount - 2];
                AutoCompleteTextView currentInputToClear = (AutoCompleteTextView) currentLayoutToHide.getEditText();
                if (currentInputToClear != null) {
                    currentInputToClear.setText("");
                }
                currentLayoutToHide.setVisibility(View.GONE);

                if (inputVisibleCount == 2) {
                    btnDel.setVisibility(View.GONE);
                }
            } else {
                Toast.makeText(getContext(), "Minimal 2 alamat!", Toast.LENGTH_SHORT).show();
            }
            btnAdd.setVisibility(View.VISIBLE);
        });

        buttonSearch.setOnClickListener(v -> {
            addresses.clear();
            geocodedResults.clear();

            String addr1 = addressInput1.getText().toString().trim();
            String addr2 = addressInput2.getText().toString().trim();
            String addr3 = addressInput3.getText().toString().trim();
            String addr4 = addressInput4.getText().toString().trim();
            String addr5 = addressInput5.getText().toString().trim();

            if (!addr1.isEmpty()) addresses.add(addr1);
            if (!addr2.isEmpty()) addresses.add(addr2);
            if (inputVisibleCount > 2 && !addr3.isEmpty()) addresses.add(addr3);
            if (inputVisibleCount > 3 && !addr4.isEmpty()) addresses.add(addr4);
            if (inputVisibleCount > 4 && !addr5.isEmpty()) addresses.add(addr5);

            if (addresses.size() < 2) {
                Toast.makeText(getContext(), "Masukkan minimal 2 alamat", Toast.LENGTH_SHORT).show();
                return;
            }
            performGeocoding();

            Log.d("listAddress", String.valueOf(addresses));
            Log.d("inputVisible", String.valueOf(inputVisibleCount));
        });

        if (inputVisibleCount <= 2) {
            btnDel.setVisibility(View.GONE);
        } else {
            btnDel.setVisibility(View.VISIBLE);
        }
        if (inputVisibleCount >= maxInput) {
            btnAdd.setVisibility(View.GONE);
        } else {
            btnAdd.setVisibility(View.VISIBLE);
        }
    }

    private void performGeocoding() {
        geocodedResults.clear();
        final int totalAddresses = addresses.size();
        final int[] geocodingCompletedCount = {0};

        if (totalAddresses == 0) {
            Toast.makeText(getContext(), "Tidak ada alamat yang valid untuk di-geocode.", Toast.LENGTH_SHORT).show();
            return;
        }

        for (String address : addresses) {
            nominatimService.search(address, "json", 1, 1).enqueue(new Callback<List<GeoResult>>() {
                @Override
                public void onResponse(Call<List<GeoResult>> call, Response<List<GeoResult>> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        GeoResult result = response.body().get(0);
                        Log.d("InputFragment", "Geocoded " + address + ": " + result.getLat() + "," + result.getLon());

                        synchronized (geocodedResults) {
                            geocodedResults.add(result);
                        }
                    } else {
                        Log.e("InputFragment", "Geocoding failed for " + address + ": " + response.code() + " - " + response.message());
                        try {
                            if (response.errorBody() != null) {
                                Log.e("InputFragment", "Error Body: " + response.errorBody().string());
                            }
                        } catch (IOException e) {
                            Log.e("InputFragment", "Error reading error body", e);
                        }
                        Toast.makeText(getContext(), "Gagal mendapatkan koordinat untuk: " + address, Toast.LENGTH_SHORT).show();
                    }
                    geocodingCompletedCount[0]++;
                    if (geocodingCompletedCount[0] == totalAddresses) {
                        onAllGeocodingCompleted();
                    }
                }

                @Override
                public void onFailure(Call<List<GeoResult>> call, Throwable t) {
                    Log.e("InputFragment", "Geocoding error for " + address, t);
                    Toast.makeText(getContext(), "Error jaringan saat geocoding: " + address, Toast.LENGTH_SHORT).show();
                    geocodingCompletedCount[0]++;
                    if (geocodingCompletedCount[0] == totalAddresses) {
                        onAllGeocodingCompleted();
                    }
                }
            });
        }
    }

    private void onAllGeocodingCompleted() {
        if (geocodedResults.size() < addresses.size()) {
            Toast.makeText(getContext(), "Beberapa alamat gagal di-geocode. Silakan periksa log.", Toast.LENGTH_LONG).show();
            if (geocodedResults.size() < 2) {
                Toast.makeText(getContext(), "Tidak cukup alamat yang berhasil di-geocode untuk melanjutkan.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        Log.d("InputFragment", "Semua alamat berhasil di-geocode: " + geocodedResults.size());

        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.inputFragment_to_resultsFragment);
    }

    private void setupAutocompleteWithDebounce(AutoCompleteTextView inputField) {
        ArrayAdapter<String> suggestionAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        inputField.setAdapter(suggestionAdapter);

        inputField.setThreshold(3);
        inputField.setOnDismissListener(() -> Log.d("AutoComplete", "Dismissed"));

        // Hapus TextWatcher dan Runnable yang lama jika ada untuk inputField ini
        if (textWatchers.containsKey(inputField)) {
            inputField.removeTextChangedListener(textWatchers.get(inputField));
            handler.removeCallbacks(debounceRunnables.get(inputField));
        }

        TextWatcher newTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Hapus callback sebelumnya untuk inputField ini setiap kali teks berubah
                handler.removeCallbacks(debounceRunnables.get(inputField));
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (s.length() >= 2) {
                    // Buat Runnable baru dengan query saat ini
                    Runnable searchRunnable = () -> {
                        performAutocompleteSearch(s.toString(), suggestionAdapter, inputField);
                    };
                    // Simpan Runnable baru di map
                    debounceRunnables.put(inputField, searchRunnable);
                    // Jadwalkan Runnable dengan delay
                    handler.postDelayed(searchRunnable, DEBOUNCE_DELAY);
                } else {
                    // Jika teks kurang dari threshold, hapus saran dan Runnable
                    suggestionAdapter.clear();
                    suggestionAdapter.notifyDataSetChanged();
                    handler.removeCallbacks(debounceRunnables.get(inputField));
                    inputField.dismissDropDown(); // Tambahkan ini agar dropdown tertutup saat teks dihapus
                }
            }
        };

        // Tambahkan TextWatcher baru ke inputField dan simpan di map
        inputField.addTextChangedListener(newTextWatcher);
        textWatchers.put(inputField, newTextWatcher);
    }

    private void performAutocompleteSearch(String query, ArrayAdapter<String> adapter, AutoCompleteTextView inputField) {
        // Batalkan panggilan sebelumnya jika ada dan sedang berjalan
        if (currentAutocompleteCall != null && !currentAutocompleteCall.isCanceled()) {
            currentAutocompleteCall.cancel();
            Log.d("AutoComplete", "Previous call cancelled for query: " + query);
        }

        // Buat panggilan baru dan simpan referensinya
        currentAutocompleteCall = nominatimService.autocomplete(query, "json", 5);
        currentAutocompleteCall.enqueue(new Callback<List<GeoResult>>() {
            @Override
            public void onResponse(@NonNull Call<List<GeoResult>> call, @NonNull Response<List<GeoResult>> response) {
                // Jika panggilan ini sudah dibatalkan, jangan proses responsnya
                if (call.isCanceled()) {
                    Log.d("AutoComplete", "Call was cancelled, ignoring response for query: " + query);
                    return;
                }

                // Penting: Periksa apakah query saat ini di input field masih sama dengan query untuk respons ini
                // Ini mencegah pembaruan UI dengan data basi jika pengguna mengetik lagi dengan sangat cepat.
                if (!inputField.getText().toString().equals(query)) {
                    Log.d("AutoComplete", "Query changed (" + inputField.getText().toString() + "), ignoring stale response for: " + query);
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    List<GeoResult> geoResults = response.body();
                    List<String> suggestions = new ArrayList<>();
                    for (GeoResult result : geoResults) {
                        if (result.getDisplayName() != null && !result.getDisplayName().isEmpty()) {
                            suggestions.add(result.getDisplayName());
                        }
                    }
                    Log.d("AutoComplete", String.valueOf(suggestions));
                    Log.d("AutoComplete", "Suggestions updated, count: " + suggestions.size() + " for query: " + query);
                    adapter.clear();
                    adapter.addAll(suggestions);
                    adapter.getFilter().filter(inputField.getText().toString());
                    adapter.notifyDataSetChanged(); // Notify dulu sebelum post

                    // Runnable di-post ke message queue UI thread
                    inputField.postDelayed(() -> {
                        // Periksa lagi kondisi SEBELUM showDropDown
                        // Fokus utama adalah apakah teks masih sama dan adapter punya item
                        if (inputField.isFocused() && inputField.getText().toString().equals(query)) {
                            if (adapter.getCount() > 0) { // Gunakan adapter yang sama yang baru diisi
                                adapter.notifyDataSetChanged();
                                inputField.showDropDown();
                                Log.d("AutoComplete", "showDropDown() called. Query: " + query + ", Adapter count: " + adapter.getCount());
                            } else {
                                Log.d("AutoComplete", "Dropdown not shown: Adapter empty. Query: " + query + ", Focused: " + inputField.isFocused() + ", Text: " + inputField.getText().toString());
                                inputField.dismissDropDown(); // Pastikan ditutup jika adapter kosong
                            }
                        } else {
                            Log.d("AutoComplete", "Dropdown not shown: Conditions not met. Query: " + query + ", Focused: " + inputField.isFocused() + ", Text: " + inputField.getText().toString() + ", Adapter count: " + adapter.getCount());
                            inputField.dismissDropDown(); // Pastikan ditutup
                        }
                    }, 100);

                } else {
                    Log.e("AutoComplete", "API Error: " + response.code() + " - " + response.message() + " for query: " + query);
                    adapter.clear(); // Kosongkan adapter jika ada error
                    adapter.notifyDataSetChanged();
                    inputField.dismissDropDown();
                }
            }

            @Override
            public void onFailure(Call<List<GeoResult>> call, Throwable t) {
                if (call.isCanceled()) {
                    Log.d("AutoComplete", "Call was cancelled, ignoring failure for query: " + query);
                    return;
                }
                Log.e("Autocomplete", "Error fetching suggestions for query: " + query, t);
                if (inputField.getText().toString().equals(query)) { // Hanya bersihkan jika query masih relevan
                    adapter.clear();
                    adapter.notifyDataSetChanged();
                    inputField.dismissDropDown();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        inputVisibleCount = 2;
        resetInputViews();
    }

    private void resetInputViews() {
        if (addressInput1 != null) addressInput1.setText("");
        if (addressInput2 != null) addressInput2.setText("");
        if (addressInput3 != null) addressInput3.setText("");
        if (addressInput4 != null) addressInput4.setText("");
        if (addressInput5 != null) addressInput5.setText("");

        View view = getView();
        if (view != null) {
            TextInputLayout tf3 = view.findViewById(R.id.tf_3);
            TextInputLayout tf4 = view.findViewById(R.id.tf_4);
            TextInputLayout tf5 = view.findViewById(R.id.tf_5);

            if (tf3 != null && tf3.getEditText() != null) tf3.getEditText().setText("");
            if (tf4 != null && tf4.getEditText() != null) tf4.getEditText().setText("");
            if (tf5 != null && tf5.getEditText() != null) tf5.getEditText().setText("");

            tf3.setVisibility(View.GONE);
            tf4.setVisibility(View.GONE);
            tf5.setVisibility(View.GONE);
        }

        if (btnAdd != null) btnAdd.setVisibility(View.VISIBLE);
        if (btnDel != null) btnDel.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (currentAutocompleteCall != null && !currentAutocompleteCall.isCanceled()) {
            currentAutocompleteCall.cancel();
        }
        // Hapus semua callback Runnable dan TextWatcher untuk mencegah memory leaks
        handler.removeCallbacksAndMessages(null);
        for (Map.Entry<AutoCompleteTextView, TextWatcher> entry : textWatchers.entrySet()) {
            entry.getKey().removeTextChangedListener(entry.getValue());
        }
        textWatchers.clear();
        debounceRunnables.clear();
    }
}