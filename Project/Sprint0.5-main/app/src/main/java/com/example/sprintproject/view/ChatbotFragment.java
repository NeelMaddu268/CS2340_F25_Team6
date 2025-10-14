package com.example.sprintproject.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.example.sprintproject.R;

public class ChatbotFragment extends Fragment {

    public ChatbotFragment() {
        super(R.layout.fragment_chatbot); // Use your existing XML layout (rename if needed)
    }

    @Override

    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {

        View view = super.onCreateView(inflater, container, savedInstanceState);

        return view;
    }
}
