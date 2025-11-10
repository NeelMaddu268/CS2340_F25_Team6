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
import com.example.sprintproject.viewmodel.AuthenticationViewModel;
import com.example.sprintproject.viewmodel.DateViewModel;
import com.example.sprintproject.viewmodel.DashboardViewModel;

// Charts
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
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

    // Charts
    private PieChart pieChart;
    private BarChart barChart;

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

        btnCalendar = view.findViewById(R.id.btnCalendar);
        headerText = view.findViewById(R.id.dashboardTitle);
        logoutButton = view.findViewById(R.id.logout);
        totalSpentText = view.findViewById(R.id.textTotalSpent);
        totalRemainingText = view.findViewById(R.id.textTotalRemaining);
        budgetRecycler = view.findViewById(R.id.recyclerRemainingBudgets);

        pieChart = view.findViewById(R.id.pieChart);
        barChart = view.findViewById(R.id.barChartTotals);
        setupPieChart();
        setupBarChart();

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
                loadPieAllTime();
                loadBarAllTime();
            }
        });

        if (btnCalendar != null) {
            btnCalendar.setOnClickListener(v -> openDatePicker());
        }

        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                authenticationViewModel.logout();
                startActivity(new Intent(getActivity(), MainActivity.class));
                requireActivity().finish();
            });
        }

        dashboardVM.loadData();
        loadPieAllTime();
        loadBarAllTime();

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
        }
        loadPieAllTime();
        loadBarAllTime();
    }

    // =============== PIE (ALL-TIME) ===============
    private void setupPieChart() {
        if (pieChart == null) {
            return;
        }
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(45f);
        pieChart.setTransparentCircleRadius(50f);
        pieChart.setCenterText("Spending by\nCategory");
        pieChart.setCenterTextSize(14f);
        pieChart.getLegend().setEnabled(true);
        pieChart.setEntryLabelTextSize(12f);
    }

    private void loadPieAllTime() {
        if (pieChart == null) {
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            renderPie(new HashMap<String, Double>());
            return;
        }
        String uid = auth.getCurrentUser().getUid();

        FirestoreManager.getInstance()
                .expensesReference(uid)
                .get()
                .addOnSuccessListener(qs -> {
                    Map<String, Double> totals = new HashMap<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        Double amt = readDouble(d, "amount");
                        String cat = d.getString("category");
                        if (amt == null || cat == null) {
                            continue;
                        }
                        String key = cat.trim().toLowerCase(Locale.US);
                        Double prev = totals.get(key);
                        totals.put(key, (prev == null ? 0.0 : prev) + amt);
                    }
                    renderPie(totals);
                })
                .addOnFailureListener(e -> renderPie(new HashMap<String, Double>()));
    }

    private void renderPie(Map<String, Double> totals) {
        if (pieChart == null) {
            return;
        }

        double grand = 0.0;
        for (double v : totals.values()) {
            grand += v;
        }

        if (grand <= 0.0001) {
            pieChart.clear();
            pieChart.setCenterText("No spending");
            pieChart.invalidate();
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> e : totals.entrySet()) {
            float pct = (float) (e.getValue() / grand * 100.0);
            if (pct <= 0f) {
                continue;
            }
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

    // =============== BAR (ALL-TIME) ===============
    private void setupBarChart() {
        if (barChart == null) {
            return;
        }
        barChart.getDescription().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getLegend().setEnabled(false);
        barChart.setScaleEnabled(false);
        barChart.setDoubleTapToZoomEnabled(false);

        barChart.getXAxis().setGranularity(1f);
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getXAxis()
                .setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);

        final String[] labels = new String[]{"Budget (All Time*)", "Spent (All Time)"};
        barChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                int i = Math.round(value);
                if (i < 0 || i >= labels.length) {
                    return "";
                }
                return labels[i];
            }
        });
    }

    private void loadBarAllTime() {
        if (barChart == null) {
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            renderBar(0.0, 0.0);
            return;
        }
        String uid = auth.getCurrentUser().getUid();

        // Sum expenses (all time)
        FirestoreManager.getInstance()
                .expensesReference(uid)
                .get()
                .addOnSuccessListener(expenseSnap -> {
                    double spent = 0.0;
                    for (DocumentSnapshot d : expenseSnap.getDocuments()) {
                        Double amt = readDouble(d, "amount");
                        if (amt != null) {
                            spent += amt;
                        }
                    }
                    final double spentFinal = spent;

                    // Sum budgets (all time) across docs: field "total"
                    FirestoreManager.getInstance()
                            .budgetsReference(uid)
                            .get()
                            .addOnSuccessListener(budgetSnap -> {
                                double budgetSum = 0.0;
                                for (DocumentSnapshot b : budgetSnap.getDocuments()) {
                                    Double t = coalesce(
                                            readDouble(b, "total"),
                                            readDouble(b, "amount"),
                                            readDouble(b, "limit"),
                                            readDouble(b, "value"),
                                            readDouble(b, "budget")
                                    );
                                    if (t != null) {
                                        budgetSum += t;
                                    }

                                }
                                renderBar(budgetSum, spentFinal);
                            })
                            .addOnFailureListener(e -> renderBar(0.0, spentFinal));
                })
                .addOnFailureListener(e -> renderBar(0.0, 0.0));
    }

    @SafeVarargs
    private static <T> T coalesce(T... vals) {
        for (T v : vals) {
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private void renderBar(double budgetAllTime, double spentAllTime) {
        if (barChart == null) {
            return;
        }

        List<BarEntry> bars = new ArrayList<>();
        bars.add(new BarEntry(0f, (float) budgetAllTime));
        bars.add(new BarEntry(1f, (float) spentAllTime));

        BarDataSet set = new BarDataSet(bars, "");
        set.setColors(ColorTemplate.COLORFUL_COLORS);
        set.setValueTextSize(12f);

        BarData data = new BarData(set);
        data.setBarWidth(0.6f);

        barChart.setData(data);
        barChart.setFitBars(true);
        barChart.invalidate();
    }

    // =============== Helpers ===============
    private static Double readDouble(DocumentSnapshot d, String field) {
        Object o = d.get(field);
        if (o == null) {
            return null;
        }
        if (o instanceof Double) {
            return (Double) o;
        }
        if (o instanceof Long) {
            return ((Long) o).doubleValue();
        }
        if (o instanceof Integer) {
            return ((Integer) o).doubleValue();
        }
        if (o instanceof Float) {
            return ((Float) o).doubleValue();
        }
        if (o instanceof String) {
            try {
                return Double.parseDouble((String) o);
            } catch (NumberFormatException ignore) {
                return null;
            }
        }
        return null;
    }

    private String pretty(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim();
        return t.isEmpty() ? "" : t.substring(0, 1).toUpperCase() + t.substring(1);
    }
}
