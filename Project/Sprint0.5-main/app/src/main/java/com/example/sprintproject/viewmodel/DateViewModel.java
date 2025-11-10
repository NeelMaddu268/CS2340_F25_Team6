package com.example.sprintproject.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.sprintproject.model.AppDate;

import java.util.Calendar;

/**
 * Central app date controller (shared across fragments/activities).
 * Keeps an AppDate persisted, allows advancing by days,
 * and notifies observers whenever it changes.
 */
public class DateViewModel extends AndroidViewModel {
    private static final String PREFS_NAME = "app_date_prefs";
    private static final String KEY_YEAR = "year";
    private static final String KEY_MONTH = "month";
    private static final String KEY_DAY = "day";

    private final MutableLiveData<AppDate> currentDate = new MutableLiveData<>();
    private final MutableLiveData<Integer> currentDay = new MutableLiveData<>();
    private final SharedPreferences prefs;

    public DateViewModel(@NonNull Application application) {
        super(application);
        prefs = application.getSharedPreferences(PREFS_NAME, Application.MODE_PRIVATE);
        // always start on today for deterministic testing
        resetToToday();
    }

    public LiveData<AppDate> getCurrentDate() {
        return currentDate;
    }

    public LiveData<Integer> getCurrentDay() {
        return currentDay;
    }

    public void setDate(AppDate date, int day) {
        if (date == null) return;
        currentDate.setValue(date);
        currentDay.setValue(day);
        saveDate(date.getYear(), date.getMonth(), day);
    }

    /** Re-emit same date (forces UI recomputation). */
    public void reemit() {
        AppDate d = currentDate.getValue();
        if (d != null) {
            currentDate.setValue(new AppDate(d.getYear(), d.getMonth(), d.getDay()));
        }
    }

    /** Step the app date by N days (±). */
    public void nudgeDays(int delta) {
        AppDate d = currentDate.getValue();
        if (d == null) {
            resetToToday();
            return;
        }
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, d.getYear());
        cal.set(Calendar.MONTH, d.getMonth() - 1);
        cal.set(Calendar.DAY_OF_MONTH, d.getDay());
        cal.add(Calendar.DAY_OF_YEAR, delta);

        AppDate next = new AppDate(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
        );
        setDate(next, next.getDay());
    }

    public void nextDay() { nudgeDays(1); }
    public void prevDay() { nudgeDays(-1); }

    /** Reset to real-world today. */
    public void resetToToday() {
        Calendar c = Calendar.getInstance();
        AppDate today = new AppDate(
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH)
        );
        setDate(today, today.getDay());
    }

    private void loadSavedDate() {
        int y = prefs.getInt(KEY_YEAR, -1);
        int m = prefs.getInt(KEY_MONTH, -1);
        int d = prefs.getInt(KEY_DAY, -1);

        if (y == -1 || m == -1 || d == -1) {
            resetToToday();
        } else {
            currentDate.setValue(new AppDate(y, m, d));
            currentDay.setValue(d);
        }
    }

    private void saveDate(int year, int month, int day) {
        prefs.edit()
                .putInt(KEY_YEAR, year)
                .putInt(KEY_MONTH, month)
                .putInt(KEY_DAY, day)
                .apply();
    }
}



