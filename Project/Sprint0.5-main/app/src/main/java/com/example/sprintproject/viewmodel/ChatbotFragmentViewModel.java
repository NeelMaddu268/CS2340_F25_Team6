// The viewmodel holds a placeholder test that is exposed from the
// LiveData for basic UI binding.

package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ChatbotFragmentViewModel extends ViewModel {

    private final MutableLiveData<String> text = new MutableLiveData<>();

    public ChatbotFragmentViewModel() {
        text.setValue("Hello from ViewModel (placeholder)");
    }

    public LiveData<String> getText() {
        return text;
    }

    public void doNothing() {
        // This method does nothing
    }
}
