// This ViewModel handles the UI text for the chatbot screen and keeps it stored as LiveData.


package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ChatbotFragmentViewModel extends ViewModel {

    private final MutableLiveData<String> text = new MutableLiveData<>();

    public ChatbotFragmentViewModel() {
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
