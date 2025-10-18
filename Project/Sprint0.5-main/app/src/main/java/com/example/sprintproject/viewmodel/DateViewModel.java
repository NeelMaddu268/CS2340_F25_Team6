package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.model.AppDate;

import java.util.Calendar;

public class DateViewModel extends ViewModel {
    private final MutableLiveData<AppDate> currentDate = new MutableLiveData<>(today());

    public LiveData<AppDate> getCurrentDate() { return currentDate; }

    public void setDate(AppDate date) { currentDate.setValue(date); }

    public void resetToToday() { currentDate.setValue(today()); }

    private AppDate today() {
        Calendar c = Calendar.getInstance();
        return new AppDate(
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH)
        );
    }
}

