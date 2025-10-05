package com.example.sprintproject.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.example.sprintproject.R;

public class SavingsCircleFragment extends Fragment {

    public SavingsCircleFragment() {
        super(R.layout.fragment_savingscircle);
    }

    @Override

    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {

        View view = super.onCreateView(inflater, container, savedInstanceState);
        // Initialize UI elements here
        return view;
    }
}
