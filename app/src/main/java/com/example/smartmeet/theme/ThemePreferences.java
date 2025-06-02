package com.example.smartmeet.theme;

import android.content.Context;
import android.content.SharedPreferences;

public class ThemePreferences {
    private static final String PREF_NAME = "app_theme_prefs";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";

    private final SharedPreferences preferences;

    public ThemePreferences(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setDarkModeEnabled(boolean isEnabled) {
        preferences.edit().putBoolean(KEY_DARK_MODE, isEnabled).apply();
    }

    public boolean isDarkModeEnabled() {
        return preferences.getBoolean(KEY_DARK_MODE, false); // Default: mode terang
    }
}