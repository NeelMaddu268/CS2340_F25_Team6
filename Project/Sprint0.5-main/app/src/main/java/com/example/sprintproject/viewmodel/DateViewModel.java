package com.example.sprintproject.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.sprintproject.model.AppDate;

import java.util.Calendar;

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
        // Was: loadSavedDate();
        resetToToday();   // always start on today
    }

    public LiveData<AppDate> getCurrentDate() {
        return currentDate;
    }

    public LiveData<Integer> getCurrentDay() {
        return currentDay;
    }

    public void setDate(AppDate date, int day) {
        currentDate.setValue(date);
        currentDay.setValue(day);
        saveDate(date.getYear(), date.getMonth(), day);
    }

    public void resetToToday() {
        Calendar c = Calendar.getInstance();
        AppDate today = new AppDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
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


