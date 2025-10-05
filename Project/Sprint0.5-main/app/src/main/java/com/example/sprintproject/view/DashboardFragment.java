package com.example.sprintproject.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.sprintproject.R;
import com.example.sprintproject.viewmodel.AuthenticationViewModel;

public class DashboardFragment extends Fragment {

    private AuthenticationViewModel authenticationViewModel;

    private Button logoutButton;

    public DashboardFragment() {
        super(R.layout.fragment_dashboard); // Use your existing XML layout (rename if needed)
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater,
                container, savedInstanceState);

        authenticationViewModel = new AuthenticationViewModel();

        logoutButton = view.findViewById(R.id.logout);

        EdgeToEdge.enable(requireActivity());
        ViewCompat.setOnApplyWindowInsetsListener(
                view.findViewById(R.id.dashboard_layout), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

        logoutButton.setOnClickListener(v -> {
            authenticationViewModel.logout();
            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);

            requireActivity().finish();
        });

        return view;
    }
}
