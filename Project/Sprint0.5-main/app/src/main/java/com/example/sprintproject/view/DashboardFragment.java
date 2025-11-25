package com.example.sprintproject.view;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sprintproject.R;
import com.example.sprintproject.model.AppDate;
import com.example.sprintproject.model.ThemeManager;
import com.example.sprintproject.viewmodel.AuthenticationViewModel;
import com.example.sprintproject.viewmodel.DateViewModel;
import com.example.sprintproject.viewmodel.DashboardViewModel;

import com.example.sprintproject.charts.ChartFactory;
import com.example.sprintproject.charts.Charts;
import com.example.sprintproject.strategies.ExpenseWindowStrategy;
import com.example.sprintproject.strategies.AllTimeWindowStrategy;

import com.example.sprintproject.viewmodel.FirestoreManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private DateViewModel dateVM;
    private DashboardViewModel dashboardVM;
    AuthenticationViewModel authenticationViewModel;


    private Charts charts;

    private ExpenseWindowStrategy currentStrategy = new AllTimeWindowStrategy();

    public DashboardFragment() {
        super(R.layout.fragment_dashboard);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view == null) {
            return null;
        }

        authenticationViewModel = new AuthenticationViewModel();
        dateVM = new ViewModelProvider(requireActivity()).get(DateViewModel.class);
        dashboardVM = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);

        ImageButton btnCalendar;
        ImageButton btnProfile;
        SwitchCompat themeSwitch;
        TextView headerText;
        TextView totalSpentText;
        TextView totalRemainingText;
        Button logoutButton;
        RecyclerView budgetRecycler;
        DashboardBudgetAdapter budgetAdapter;

        btnCalendar = view.findViewById(R.id.btnCalendar);
        btnProfile = view.findViewById(R.id.btnProfile);
        themeSwitch = view.findViewById(R.id.themeSwitch);
        syncThemeSwitchWithFirestore(themeSwitch);
        headerText = view.findViewById(R.id.dashboardTitle);
        logoutButton = view.findViewById(R.id.logout);
        totalSpentText = view.findViewById(R.id.textTotalSpent);
        totalRemainingText = view.findViewById(R.id.textTotalRemaining);
        budgetRecycler = view.findViewById(R.id.recyclerRemainingBudgets);

        headerText.setText("Dashboard");

        EdgeToEdge.enable(requireActivity());
        ViewCompat.setOnApplyWindowInsetsListener(
                view.findViewById(R.id.dashboard_layout),
                (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });

        budgetAdapter = new DashboardBudgetAdapter();
        budgetRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        budgetRecycler.setAdapter(budgetAdapter);

        charts = ChartFactory.attach(view);

        dashboardVM.getBudgetsList().observe(getViewLifecycleOwner(), budgetAdapter::updateData);

        dashboardVM.getTotalSpentAllTime().observe(getViewLifecycleOwner(), total ->
                totalSpentText.setText(String.format(Locale.US,
                        "Total Spent (All Time): $%.2f", total)));

        dashboardVM.getTotalRemaining().observe(getViewLifecycleOwner(), total ->
                totalRemainingText.setText(String.format(Locale.US,
                        "Remaining This Cycle: $%.2f", total)));

        dateVM.getCurrentDate().observe(getViewLifecycleOwner(), date -> {
            if (date != null) {
                dashboardVM.loadDataFor(date);

                loadChartsWithStrategy();
            }
        });

        if (btnCalendar != null) {
            btnCalendar.setOnClickListener(v -> openDatePicker());
        }

        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), ProfileActivity.class);
                startActivity(intent);
            });
        }

        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            authenticationViewModel.toggleTheme(isChecked, requireContext());
        });

        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                authenticationViewModel.logout(requireContext());
                startActivity(new Intent(getActivity(), MainActivity.class));
                requireActivity().finish();
            });
        }

        dashboardVM.loadData();
        loadChartsWithStrategy();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        AppDate currentDate = dateVM.getCurrentDate().getValue();
        if (currentDate != null) {
            dashboardVM.loadDataFor(currentDate);
        }
        loadChartsWithStrategy();
    }

    private void syncThemeSwitchWithFirestore(SwitchCompat themeSwitch) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance().collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.contains("darkMode") && themeSwitch != null && isAdded()) {
                        boolean darkMode = doc.getBoolean("darkMode");

                        themeSwitch.setOnCheckedChangeListener(null);

                        themeSwitch.setChecked(darkMode);

                        ThemeManager.applyTheme(darkMode, requireContext());

                        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            authenticationViewModel.toggleTheme(isChecked, requireContext());
                        });
                    }
                })
                .addOnFailureListener(e -> Log.w("ThemeSync", "Failed to fetch theme", e));
    }


    private void loadChartsWithStrategy() {
        if (charts == null) {
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            charts.getPie().render(new java.util.HashMap<>());
            charts.getBar().render(0.0, 0.0);
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        FirestoreManager fm = FirestoreManager.getInstance();

        currentStrategy.loadPie(fm, uid, charts.getPie());
        currentStrategy.loadBar(fm, uid, charts.getBar());
    }

    private void openDatePicker() {
        AppDate stored = dateVM.getCurrentDate().getValue();
        final Calendar seed = Calendar.getInstance();
        if (stored != null) {
            seed.set(Calendar.YEAR, stored.getYear());
            seed.set(Calendar.MONTH, stored.getMonth() - 1);
            seed.set(Calendar.DAY_OF_MONTH, stored.getDay());
        }

        int year = seed.get(Calendar.YEAR);
        int month0 = seed.get(Calendar.MONTH);
        int day = seed.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dlg = new DatePickerDialog(
                requireContext(),
                (picker, y, mZero, dd) -> dateVM.setDate(new AppDate(y, mZero + 1, dd), dd),
                year, month0, day
        );
        dlg.show();
    }
}