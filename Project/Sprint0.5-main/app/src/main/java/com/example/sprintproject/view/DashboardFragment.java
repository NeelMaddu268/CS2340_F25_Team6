package com.example.sprintproject.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.example.sprintproject.R;

public class DashboardFragment extends Fragment {

    public DashboardFragment() {
        super(R.layout.fragment_dashboard); // Use your existing XML layout (rename if needed)
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout
        View view = super.onCreateView(inflater, container, savedInstanceState);

        // TODO: Initialize UI elements if needed, e.g. buttons
        // You won't use startActivity here. Navigation happens via MainActivity's nav bar.

        return view;
    }
}
