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
import android.widget.Toast;
import android.text.TextWatcher; // Import ini

import com.example.smartmeet.R;
import com.example.smartmeet.data.model.GeoResult;
import com.example.smartmeet.data.network.ApiClient;
import com.example.smartmeet.data.network.NominatimApiService;
import com.example.smartmeet.databinding.FragmentInputBinding;
import com.example.smartmeet.util.LocationUtil;
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
    private FragmentInputBinding binding;
    private NominatimApiService nominatimService;
    private List<String> addresses = new ArrayList<>(); // List untuk alamat yang diinput
    private List<GeoResult> geocodedResults = new ArrayList<>(); // List hasil geocoding
    private int inputVisibleCount = 2;
    private final int maxInput = 5;
    private Handler handler = new Handler(Looper.getMainLooper()); // main thread
    private Call<List<GeoResult>> currentAutocompleteCall;
    private Map<AutoCompleteTextView, Boolean> isSelectingSuggestion = new HashMap<>();
    private LocationUtil locationUtil = new LocationUtil();

    // Gunakan Map untuk menyimpan Runnable debounce per AutoCompleteTextView
    private Map<AutoCompleteTextView, Runnable> debounceRunnables = new HashMap<>();
    private Map<AutoCompleteTextView, TextWatcher> textWatchers = new HashMap<>(); // Untuk membersihkan listener

    private static final long DEBOUNCE_DELAY = 500;

    private boolean isGeocodingError = false;
    private int lastGeocodingVisibleCount = 0;
    private List<String> lastGeocodingAddresses = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentInputBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // List inputLayout untuk menambahkan inputan ketika button add diklik
        TextInputLayout[] inputLayouts = new TextInputLayout[]{
                binding.tf3,
                binding.tf4,
                binding.tf5
        };

        // Fitur autocomplete dengan debounce
        setupAutocompleteWithDebounce(binding.address1);
        setupAutocompleteWithDebounce(binding.address2);
        setupAutocompleteWithDebounce(binding.address3);
        setupAutocompleteWithDebounce(binding.address4);
        setupAutocompleteWithDebounce(binding.address5);

        // Atur adapter untuk spinner dengan mengambil array dari amenities_array
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(view.getContext(),
                R.array.amenities_array, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // atur dropdown spinner
        binding.amenitySpinner.setAdapter(spinnerAdapter); // set adapter dari spinner

        nominatimService = ApiClient.getNominatimApiService(); // inisialisasi nominatim service

        // Click listener untuk button tambah
        binding.addButton.setOnClickListener(v -> {
            if (inputVisibleCount < maxInput) {
                inputLayouts[inputVisibleCount - 2].setVisibility(View.VISIBLE);
                inputVisibleCount++;
                if (inputVisibleCount == maxInput) {
                    binding.addButton.setVisibility(View.GONE);
                }
            }
            binding.delButton.setVisibility(View.VISIBLE);
        });

        // Click listener untuk button delete
        binding.delButton.setOnClickListener(v -> {
            if (inputVisibleCount > 2) {
                inputVisibleCount--;
                TextInputLayout currentLayoutToHide = inputLayouts[inputVisibleCount - 2];
                AutoCompleteTextView currentInputToClear = (AutoCompleteTextView) currentLayoutToHide.getEditText();
                if (currentInputToClear != null) {
                    currentInputToClear.setText("");
                }
                currentLayoutToHide.setVisibility(View.GONE);

                if (inputVisibleCount == 2) {
                    binding.delButton.setVisibility(View.GONE);
                }
            } else {
                Toast.makeText(getContext(), "Minimal 2 alamat!", Toast.LENGTH_SHORT).show();
            }
            binding.addButton.setVisibility(View.VISIBLE);
        });

        // Click listener untuk button search
        binding.searchButton.setOnClickListener(v -> {
            addresses.clear(); // membersihkan list terlebih dahulu
            geocodedResults.clear(); // membersihkan hasil geocoding

            addresses = getNonEmptyAddresses(inputVisibleCount); // mendapatkan address yang tidak empty

            if (addresses.size() < 2) {
                Toast.makeText(getContext(), "Masukkan minimal 2 alamat", Toast.LENGTH_SHORT).show();
                return;
            }

            performGeocoding();
        });

        updateButtonVisibility(inputVisibleCount, maxInput);
    }

    @Override
    public void onResume() {
        super.onResume();
        inputVisibleCount = 2;
        resetInputViews();
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
        isSelectingSuggestion.clear();
        binding = null;
    }

    private void setupAutocompleteWithDebounce(AutoCompleteTextView inputField) {
        // Adapter untuk suggestion autocomplete
        ArrayAdapter<String> suggestionAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        inputField.setAdapter(suggestionAdapter);

        inputField.setThreshold(1); // threshold untuk minimal
        inputField.setOnDismissListener(() -> Log.d("AutoComplete", "Dismissed"));

        // Inisialisasi flag untuk inputField ini
        isSelectingSuggestion.put(inputField, false);

        // Set listener untuk item click pada dropdown
        inputField.setOnItemClickListener((parent, view, position, id) -> {
            // Setelah item diklik, set flag menjadi true
            isSelectingSuggestion.put(inputField, true);
            // Kosongkan adapter agar suggestion tidak muncul lagi
            suggestionAdapter.clear();
            suggestionAdapter.notifyDataSetChanged();
            inputField.dismissDropDown();
        });

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
                if (isSelectingSuggestion.get(inputField) != null && isSelectingSuggestion.get(inputField)) {
                    isSelectingSuggestion.put(inputField, false);
                }
                // Hapus callback sebelumnya untuk inputField ini setiap kali teks berubah
                handler.removeCallbacks(debounceRunnables.get(inputField));
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                // Periksa apakah perubahan teks ini berasal dari pemilihan suggestion
                if (isSelectingSuggestion.get(inputField) != null && isSelectingSuggestion.get(inputField)) {
                    Log.d("AutoComplete", "Ignoring afterTextChanged due to suggestion selection for: " + s.toString());
                    // Reset flag setelah pemrosesan, sehingga ketikan berikutnya akan memicu pencarian
                    isSelectingSuggestion.put(inputField, false);
                    // Pastikan dropdown ditutup dan adapter dikosongkan
                    suggestionAdapter.clear();
                    suggestionAdapter.notifyDataSetChanged();
                    inputField.dismissDropDown();
                    return; // Hentikan pemrosesan lebih lanjut
                }

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
                        // Tambahkan pengecekan flag isSelectingSuggestion
                        if (isSelectingSuggestion.get(inputField) != null && isSelectingSuggestion.get(inputField)) {
                            // Jangan tampilkan dropdown jika user baru saja memilih suggestion
                            inputField.dismissDropDown();
                            return;
                        }
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

    // Fungsi geocoding untuk mendapatkan koordinat lokasi
    private void performGeocoding() {
        geocodedResults.clear();
        final int totalAddresses = addresses.size();
        final int[] geocodingCompletedCount = {0};
        isGeocodingError = false;
        lastGeocodingVisibleCount = inputVisibleCount;
        lastGeocodingAddresses = new ArrayList<>(addresses);

        showLoadingOverlay(false);

        if (totalAddresses == 0) {
            Toast.makeText(getContext(), "Tidak ada alamat yang valid untuk di-geocode.", Toast.LENGTH_SHORT).show();
            hideLoadingOverlay();
            return;
        }

        for (String address : addresses) {
            nominatimService.search(address, "json", 1, 1).enqueue(new Callback<List<GeoResult>>() {
                @Override
                public void onResponse(Call<List<GeoResult>> call, Response<List<GeoResult>> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        GeoResult result = response.body().get(0);
                        synchronized (geocodedResults) {
                            geocodedResults.add(result);
                        }
                    } else {
                        isGeocodingError = true;
                    }
                    geocodingCompletedCount[0]++;
                    if (geocodingCompletedCount[0] == totalAddresses) {
                        onAllGeocodingCompleted();
                    }
                }

                @Override
                public void onFailure(Call<List<GeoResult>> call, Throwable t) {
                    isGeocodingError = true;
                    geocodingCompletedCount[0]++;
                    if (geocodingCompletedCount[0] == totalAddresses) {
                        onAllGeocodingCompleted();
                    }
                }
            });
        }
    }

    // Tambahkan logika midpoint dan pengiriman bundle ke ResultsFragment
    private void onAllGeocodingCompleted() {
        if (geocodedResults.isEmpty()) {
            showLoadingOverlay(true);
            Toast.makeText(getContext(), "Semua alamat gagal di-geocode. Coba lagi!", Toast.LENGTH_LONG).show();
            return;
        }

        // Hitung midpoint
        LocationUtil.Midpoint midpoint = locationUtil.calculateMidpoint(geocodedResults);
        if (midpoint == null) {
            Toast.makeText(getContext(), "Tidak bisa menghitung titik tengah.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ambil amenity yang dipilih (jika ada spinner)
        String selectedAmenity = binding.amenitySpinner.getSelectedItem().toString();

        // Siapkan bundle
        Bundle bundle = new Bundle();
        bundle.putDouble("midpoint_lat", midpoint.latitude);
        bundle.putDouble("midpoint_lon", midpoint.longitude);

        ArrayList<String> latitudes = new ArrayList<>();
        ArrayList<String> longitudes = new ArrayList<>();
        for (GeoResult res : geocodedResults) {
            latitudes.add(res.getLat());
            longitudes.add(res.getLon());
        }
        bundle.putStringArrayList("participant_lats", latitudes);
        bundle.putStringArrayList("participant_lons", longitudes);
        bundle.putString("amenity", selectedAmenity);

        // Navigasi ke ResultsFragment
        hideLoadingOverlay();
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
        navController.navigate(R.id.inputFragment_to_resultsFragment, bundle);
    }


    private void resetInputViews() {
        AutoCompleteTextView[] addressFields = getAutoCompleteTextView(); // daftar inputan

        // looping untuk mengosongkan semua inputan
        for (AutoCompleteTextView input : addressFields) {
            if (input != null) {
                input.setText("");
                isSelectingSuggestion.put(input, false); // Reset flag
            }
        }

        // Daftar ID TextInputLayout yang akan disembunyikan
        int[] textInputLayoutIds = {
                R.id.tf_3,
                R.id.tf_4,
                R.id.tf_5
        };

        View view = getView();
        if (view != null) {
            for (int id : textInputLayoutIds) {
                TextInputLayout textInputLayout = view.findViewById(id);
                if (textInputLayout != null) {
                    textInputLayout.setVisibility(View.GONE);
                }
            }
        }

        if (binding.addButton != null) binding.addButton.setVisibility(View.VISIBLE);
        if (binding.delButton != null) binding.delButton.setVisibility(View.GONE);
    }

    // Fungsi untuk mendapatkan alamat yang tidak empty
    private List<String> getNonEmptyAddresses(int inputVisibleCount) {
        List<String> addresses = new ArrayList<>();
        AutoCompleteTextView[] addressFields = getAutoCompleteTextView(); // dapatkan list inputan

        for (int i = 0; i < inputVisibleCount && i < addressFields.length; i++) {
            String address = addressFields[i].getText().toString().trim();
            if (!address.isEmpty()) {
                addresses.add(address);
            }
        }

        return addresses;
    }

    // Fungsi untuk update visibilitas button
    private void updateButtonVisibility(int inputVisibleCount, int maxInput) {
        binding.delButton.setVisibility(inputVisibleCount <= 2 ? View.GONE : View.VISIBLE);
        binding.addButton.setVisibility(inputVisibleCount >= maxInput ? View.GONE : View.VISIBLE);
    }

    // Fungsi untuk mendapatkan daftar inputan
    private AutoCompleteTextView[] getAutoCompleteTextView() {
        AutoCompleteTextView[] addressFields = {
                binding.address1,
                binding.address2,
                binding.address3,
                binding.address4,
                binding.address5
        };
        return addressFields;
    }

    private void showLoadingOverlay(boolean showRetry) {
        binding.loadingOverlay.setVisibility(View.VISIBLE);
        binding.retryButton.setVisibility(showRetry ? View.VISIBLE : View.GONE);
        binding.geocodeProgress.setVisibility(showRetry ? View.GONE : View.VISIBLE);
        binding.loadingText.setText(showRetry ? "Gagal geocoding. Coba lagi?" : "Sedang mencari koordinat alamat...");
        setInputEnabled(false);
    }

    private void hideLoadingOverlay() {
        binding.loadingOverlay.setVisibility(View.GONE);
        setInputEnabled(true);
    }

    private void setInputEnabled(boolean enabled) {
        for (AutoCompleteTextView input : getAutoCompleteTextView()) {
            input.setEnabled(enabled);
        }
        binding.addButton.setEnabled(enabled);
        binding.delButton.setEnabled(enabled);
        binding.searchButton.setEnabled(enabled);
        binding.amenitySpinner.setEnabled(enabled);
    }

}