package com.example.sprintproject.view;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.sprintproject.R;
import com.example.sprintproject.model.AppDate;
import com.example.sprintproject.viewmodel.AuthenticationViewModel;
import com.example.sprintproject.viewmodel.DateViewModel;

import java.util.Calendar;

public class DashboardFragment extends Fragment {

    private AuthenticationViewModel authenticationViewModel;
    private DateViewModel dateVM;

    private ImageButton btnCalendar;
    private TextView headerText;
    private Button logoutButton;

    public DashboardFragment() {
        // Inflates res/layout/fragment_dashboard.xml
        super(R.layout.fragment_dashboard);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view == null) return null;

        authenticationViewModel = new AuthenticationViewModel();

        // ---- find views
        btnCalendar  = view.findViewById(R.id.btnCalendar);
        headerText   = view.findViewById(R.id.dashboardTitle);
        logoutButton = view.findViewById(R.id.logout);

        if (headerText != null) {
            headerText.setText("Dashboard");
        }

        EdgeToEdge.enable(requireActivity());
        ViewCompat.setOnApplyWindowInsetsListener(
                view.findViewById(R.id.dashboard_layout),
                (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });

        // Shared across the Activity so all fragments see the same date
        dateVM = new ViewModelProvider(requireActivity()).get(DateViewModel.class);

        // Open the picker seeded from the LAST CHOSEN date (fallback = today)
        if (btnCalendar != null) {
            btnCalendar.setOnClickListener(v -> openDatePicker());
        }

        // Logout
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                authenticationViewModel.logout();
                startActivity(new Intent(getActivity(), MainActivity.class));
                requireActivity().finish();
            });
        }

        return view;
    }

    /**
     * Opens the DatePickerDialog seeded from DateViewModel's current date.
     * If none is set yet, falls back to today's date.
     */
    private void openDatePicker() {
        // Seed from the last saved date (fallback = today)
        AppDate stored = dateVM.getCurrentDate().getValue();

        final Calendar seed = Calendar.getInstance();
        if (stored != null) {
            seed.set(Calendar.YEAR, stored.getYear());
            seed.set(Calendar.MONTH, stored.getMonth() - 1);      // DatePicker uses 0..11
            // If AppDate tracks day, use it; otherwise seed keeps today's day
            try {
                seed.set(Calendar.DAY_OF_MONTH, stored.getDay());
            } catch (Exception ignored) { /* AppDate may not have day; safe to ignore */ }
        }

        int year   = seed.get(Calendar.YEAR);
        int month0 = seed.get(Calendar.MONTH);
        int day    = seed.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dlg = new DatePickerDialog(
                requireContext(),
                (picker, y, mZero, dd) -> {
                    // Pass both the AppDate and the day
                    dateVM.setDate(new AppDate(y, mZero + 1, dd), dd);
                },
                year, month0, day
        );
        dlg.show();
    }
}



