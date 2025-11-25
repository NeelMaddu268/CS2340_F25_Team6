// This class manages the app's color theme - light and dark mode by saving the preference and loading the
// selected mode using SharedPreferences. This class provides helper methods that allows the user's theme preference to be used in any part of the app.

package com.example.sprintproject.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

public class ThemeManager {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME = "selected_theme";

    private ThemeManager() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void saveTheme(Context context, boolean isDarkMode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_THEME, isDarkMode).apply();
    }

    public static boolean loadTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_THEME, false); // default to light mode
    }
}

