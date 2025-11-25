// This class manages the app's color theme - light and dark mode by
// saving the preference and loading the
// selected mode using SharedPreferences. This class provides helper
// methods that allows the user's theme preference to be used in any
// part of the app.

package com.example.sprintproject.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {
    private ThemeManager() {
        throw new UnsupportedOperationException("Utility class");
    }
    private static final String PREF_NAME = "theme_prefs";
    private static final String PREF_KEY = "is_dark";

    public static void applyTheme(boolean darkMode, Context context) {
        // Save locally
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_KEY, darkMode).apply();

        // Apply immediate theme
        AppCompatDelegate.setDefaultNightMode(
                darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    public static boolean isDarkModeEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_KEY, false);
    }

    // Reset on logout
    public static void clearTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }
}

