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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sprintproject.R;
import com.example.sprintproject.model.AppDate;
import com.example.sprintproject.model.Budget;
import com.example.sprintproject.viewmodel.AuthenticationViewModel;
import com.example.sprintproject.viewmodel.DateViewModel;
import com.example.sprintproject.viewmodel.DashboardViewModel;

// --- PIE CHART ---
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

// --- BAR CHART (Totals: Spent vs Budget) ---
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.example.sprintproject.viewmodel.FirestoreManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private AuthenticationViewModel authenticationViewModel;
    private DateViewModel dateVM;
    private DashboardViewModel dashboardVM;

    private ImageButton btnCalendar;
    private TextView headerText;
    private TextView totalSpentText;
    private TextView totalRemainingText;
    private Button logoutButton;
    private RecyclerView budgetRecycler;
    private DashboardBudgetAdapter budgetAdapter;

    // --- PIE CHART ---
    private PieChart pieChart;

    // --- BAR CHART: 2 bars (Spent vs Budget) ---
    private BarChart barChartTotals;

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

        // Initialize ViewModels
        authenticationViewModel = new AuthenticationViewModel();
        dateVM = new ViewModelProvider(requireActivity()).get(DateViewModel.class);
        dashboardVM = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);

        // UI References
        btnCalendar = view.findViewById(R.id.btnCalendar);
        headerText = view.findViewById(R.id.dashboardTitle);
        logoutButton = view.findViewById(R.id.logout);
        totalSpentText = view.findViewById(R.id.textTotalSpent);
        totalRemainingText = view.findViewById(R.id.textTotalRemaining);
        budgetRecycler = view.findViewById(R.id.recyclerRemainingBudgets);

        // --- PIE CHART ---
        pieChart = view.findViewById(R.id.pieChart);
        setupPieChart();

        // --- BAR CHART (Totals) ---
        barChartTotals = view.findViewById(R.id.barChartTotals);
        setupTotalsBarChart();

        headerText.setText("Dashboard");

        // Edge-to-edge insets
        EdgeToEdge.enable(requireActivity());
        ViewCompat.setOnApplyWindowInsetsListener(
                view.findViewById(R.id.dashboard_layout),
                (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });

        // Recycler setup
        budgetAdapter = new DashboardBudgetAdapter();
        budgetRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        budgetRecycler.setAdapter(budgetAdapter);

        // Observers
        dashboardVM.getBudgetsList().observe(getViewLifecycleOwner(), budgets -> {
            // keep original recycler behavior
            budgetAdapter.updateData(budgets);
            // update the two-bar totals using the same current-cycle numbers as the cards
            renderTotalsBarChart(budgets);
        });

        dashboardVM.getTotalSpentAllTime().observe(getViewLifecycleOwner(), total ->
                totalSpentText.setText(String.format(Locale.US,
                        "Total Spent (All Time): $%.2f", total)));

        dashboardVM.getTotalRemaining().observe(getViewLifecycleOwner(), total ->
                totalRemainingText.setText(String.format(Locale.US,
                        "Remaining This Cycle: $%.2f", total)));

        dateVM.getCurrentDate().observe(getViewLifecycleOwner(), date -> {
            if (date != null) {
                dashboardVM.loadDataFor(date);
                // --- PIE CHART ---
                loadPieFor(date);
            }
        });

        // Calendar picker
        if (btnCalendar != null) {
            btnCalendar.setOnClickListener(v -> openDatePicker());
        }

        // Logout button
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                authenticationViewModel.logout();
                startActivity(new Intent(getActivity(), MainActivity.class));
                requireActivity().finish();
            });
        }

        // Initial load
        dashboardVM.loadData();
        // --- PIE CHART ---
        AppDate now = dateVM.getCurrentDate().getValue();
        if (now != null) {
            loadPieFor(now);
        }

        return view;
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

    @Override
    public void onResume() {
        super.onResume();
        AppDate currentDate = dateVM.getCurrentDate().getValue();
        if (currentDate != null) {
            dashboardVM.loadDataFor(currentDate);
            // --- PIE CHART ---
            loadPieFor(currentDate);
        }
    }

    // =========================
    // --- PIE CHART: helpers ---
    // =========================
    private void setupPieChart() {
        if (pieChart == null) return;
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(45f);
        pieChart.setTransparentCircleRadius(50f);
        pieChart.setCenterText("Spending by Category");
        pieChart.setCenterTextSize(14f);
        pieChart.getLegend().setEnabled(true);
        pieChart.setEntryLabelTextSize(12f);
    }

    /**
     * Loads expenses for the **month containing the given AppDate**, aggregates by category,
     * and renders % slices.
     */
    private void loadPieFor(@NonNull AppDate date) {
        if (pieChart == null) return;

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            renderPie(new HashMap<>());
            return;
        }
        String uid = auth.getCurrentUser().getUid();

        // Build [startOfMonth, startOfNextMonth) from the selected date
        Calendar start = Calendar.getInstance();
        start.set(date.getYear(), date.getMonth() - 1, 1, 0, 0, 0);
        start.set(Calendar.MILLISECOND, 0);
        long startMs = start.getTimeInMillis();

        Calendar end = (Calendar) start.clone();
        end.add(Calendar.MONTH, 1);
        long endMs = end.getTimeInMillis();

        FirestoreManager.getInstance()
                .expensesReference(uid)
                .whereGreaterThanOrEqualTo("timestamp", startMs)
                .whereLessThan("timestamp", endMs)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(qs -> {
                    Map<String, Double> totals = new HashMap<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        Double amt = d.getDouble("amount");
                        String cat = d.getString("category");
                        if (amt == null || cat == null) continue;
                        String key = cat.trim().toLowerCase(Locale.US);
                        totals.put(key, totals.getOrDefault(key, 0.0) + amt);
                    }
                    renderPie(totals);
                })
                .addOnFailureListener(e -> renderPie(new HashMap<>()));
    }

    private void renderPie(Map<String, Double> totals) {
        if (pieChart == null) return;

        double grand = 0.0;
        for (double v : totals.values()) grand += v;

        if (grand <= 0.0001) {
            pieChart.clear();
            pieChart.setCenterText("No spending");
            pieChart.invalidate();
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> e : totals.entrySet()) {
            float pct = (float) (e.getValue() / grand * 100.0);
            if (pct <= 0f) continue;
            entries.add(new PieEntry(pct, pretty(e.getKey())));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setSliceSpace(2f);
        dataSet.setValueTextSize(12f);
        dataSet.setValueFormatter(new PercentFormatter(pieChart));

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.highlightValues(null);
        pieChart.invalidate();
    }

    private String pretty(String raw) {
        if (raw == null) return "";
        String t = raw.trim();
        return t.isEmpty() ? "" : t.substring(0, 1).toUpperCase() + t.substring(1);
    }

    // ==========================================
    // --- TOTALS BAR CHART (Spent vs Budget) ---
    // ==========================================
    private void setupTotalsBarChart() {
        if (barChartTotals == null) return;
        barChartTotals.getDescription().setEnabled(false);
        barChartTotals.getAxisRight().setEnabled(false);
        barChartTotals.getLegend().setEnabled(false);
        barChartTotals.setNoDataText("No chart data available.");

        // Y axis starts at 0
        barChartTotals.getAxisLeft().setAxisMinimum(0f);

        // Bottom X labels: “Spent”, “Budget”
        XAxis x = barChartTotals.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawGridLines(false);
        x.setGranularity(1f);
        x.setValueFormatter(new IndexAxisValueFormatter(new String[]{"Spent", "Budget"}));
        x.setAxisMinimum(-0.5f);
        x.setAxisMaximum(1.5f);
    }

    private void renderTotalsBarChart(@Nullable List<Budget> budgets) {
        if (barChartTotals == null) return;

        if (budgets == null || budgets.isEmpty()) {
            barChartTotals.clear();
            barChartTotals.invalidate();
            return;
        }

        double sumSpent = 0.0;
        double sumBudget = 0.0;

        // DashboardVM already computes each Budget's spentToDate for the CURRENT cycle (weekly/monthly).
        for (Budget b : budgets) {
            if (b == null) continue;
            sumSpent  += Math.max(0, b.getSpentToDate());
            sumBudget += Math.max(0, b.getAmount());
        }

        List<BarEntry> entries = new ArrayList<>(2);
        entries.add(new BarEntry(0f, (float) sumSpent));
        entries.add(new BarEntry(1f, (float) sumBudget));

        BarDataSet set = new BarDataSet(entries, "");
        // leave default colors (distinct by MPAndroidChart)
        set.setValueTextSize(12f);

        BarData data = new BarData(set);
        data.setBarWidth(0.5f); // nice, readable bars

        barChartTotals.setData(data);
        barChartTotals.animateY(450);
        barChartTotals.invalidate();
    }
}
