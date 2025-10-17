package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.time.LocalDate;

public class DateViewModel extends ViewModel {

    private final MutableLiveData<LocalDate> currentDate =
            new MutableLiveData<>(LocalDate.now());

    public LiveData<LocalDate> getCurrentDate() {
        return currentDate;
    }

    public void setDate(LocalDate date) {
        currentDate.setValue(date);
    }

    public void resetToToday() {
        currentDate.setValue(LocalDate.now());
    }
}

