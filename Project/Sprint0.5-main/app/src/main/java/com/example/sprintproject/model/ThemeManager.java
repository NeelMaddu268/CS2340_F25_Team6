package com.example.sprintproject.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {

    //    private static final String PREFS_NAME = "theme_prefs";
    //    private static final String KEY_THEME = "selected_theme";
    //
    //    private ThemeManager() {
    //        throw new UnsupportedOperationException("Utility class");
    //    }
    //
    //    public static void saveTheme(Context context, boolean isDarkMode) {
    //        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    //        prefs.edit().putBoolean(KEY_THEME, isDarkMode).apply();
    //    }
    //
    //    public static boolean loadTheme(Context context) {
    //        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    //        return prefs.getBoolean(KEY_THEME, false); // default = light
    //    }
    //
    //    public static void applyTheme(boolean isDarkMode) {
    //        AppCompatDelegate.setDefaultNightMode(
    //                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
    //        );
    //    }
    //
    //    public static void clearTheme(Context context) {
    //        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    //        prefs.edit().clear().apply();
    //    }
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

