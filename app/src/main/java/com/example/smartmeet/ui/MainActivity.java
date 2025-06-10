package com.example.smartmeet.ui;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.smartmeet.R;
import com.example.smartmeet.theme.ThemePreferences;

public class MainActivity extends AppCompatActivity {

    private ThemePreferences themePreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Terapkan tema lebih dulu
        themePreferences = new ThemePreferences(this);
        if (themePreferences.isDarkModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Ganti icon overflow
        Drawable overflowIcon = ContextCompat.getDrawable(this, R.drawable.ic_more_vert);
        toolbar.setOverflowIcon(overflowIcon);

        EdgeToEdge.enable(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu); // Pastikan nama file menu benar
        // Update icon night mode sesuai status
        MenuItem nightModeItem = menu.findItem(R.id.night_mode_toggle);
        if (nightModeItem != null) {
            if (themePreferences.isDarkModeEnabled()) {
                nightModeItem.setIcon(R.drawable.night_mode_on); // ganti sesuai drawable aktif
            } else {
                nightModeItem.setIcon(R.drawable.night_mode_off); // ganti sesuai drawable off
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.night_mode_toggle) {
            boolean newNightModeState = !themePreferences.isDarkModeEnabled();
            themePreferences.setDarkModeEnabled(newNightModeState);

            if (newNightModeState) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            recreate();
            return true;
        } else if (id == R.id.action_history) {
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
            navController.navigate(R.id.historyFragment);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}