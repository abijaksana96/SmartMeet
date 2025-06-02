package com.example.smartmeet.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartmeet.R;
import com.example.smartmeet.theme.ThemePreferences;

public class MainActivity extends AppCompatActivity {

    private ImageView nightModeToggle;
    private ThemePreferences themePreferences; // Tambahkan ini

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // PENTING: Panggil setTheme atau set default night mode SEBELUM super.onCreate()
        themePreferences = new ThemePreferences(this);
        // Terapkan tema yang disimpan saat aplikasi dimulai
        if (themePreferences.isDarkModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        nightModeToggle = findViewById(R.id.night_mode_toogle); // ImageView Anda

        // Atur status awal ikon berdasarkan preferensi
        if (themePreferences.isDarkModeEnabled()) {
            nightModeToggle.setImageState(new int[]{android.R.attr.state_checked}, true);
        } else {
            nightModeToggle.setImageState(new int[]{-android.R.attr.state_checked}, true);
        }


        nightModeToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle mode malam
                boolean newNightModeState = !themePreferences.isDarkModeEnabled();
                themePreferences.setDarkModeEnabled(newNightModeState); // Simpan preferensi

                if (newNightModeState) {
                    // Aktifkan mode malam
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    nightModeToggle.setImageState(new int[]{android.R.attr.state_checked}, true);
                } else {
                    // Nonaktifkan mode malam
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    nightModeToggle.setImageState(new int[]{-android.R.attr.state_checked}, true);
                }


                // Me-recreate Activity agar tema baru diterapkan
                recreate();
            }
        });
        // ... kode onCreate lainnya
    }

    // ... metode lain ...
}