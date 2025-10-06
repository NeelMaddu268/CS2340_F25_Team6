package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ExpenseLogFragmentViewModel extends ViewModel {

    private final MutableLiveData<String> text = new MutableLiveData<>();

    public ExpenseLogFragmentViewModel() {
        // Just sets a sample value (not used for logic)
        text.setValue("Hello from ViewModel (placeholder)");
    }

    public LiveData<String> getText() {
        return text;
    }

    public void doNothing() {
        // This method does nothing
    }
}
