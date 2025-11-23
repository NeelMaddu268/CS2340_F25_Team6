package com.example.sprintproject.charts;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PieChartController {
    private final PieChart pie;

    public PieChartController(PieChart pie) {
        this.pie = pie;
        if (pie != null) {
            pie.setUsePercentValues(true);
            pie.getDescription().setEnabled(false);
            pie.setDrawHoleEnabled(true);
            pie.setHoleRadius(45f);
            pie.setTransparentCircleRadius(50f);
            pie.setCenterText("Spending by\nCategory");
            pie.setCenterTextSize(14f);
            pie.getLegend().setEnabled(true);
            pie.setEntryLabelTextSize(12f);
        }
    }

    public void render(Map<String, Double> totals) {
        if (pie == null) {
            return;
        }
        double sum = 0.0;
        for (Double d : totals.values()) {
            sum += (d == null ? 0.0 : d);
        }

        if (sum <= 0.0001) {
            pie.clear();
            pie.setCenterText("No spending");
            pie.invalidate();
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> e : totals.entrySet()) {
            double v = (e.getValue() == null) ? 0.0 : e.getValue();
            float pct = 0;
            if (sum != 0) {
                pct = (float) ((v / sum) * 100.0f);
            }
            if (pct > 0) {
                entries.add(new PieEntry(pct, cap(e.getKey())));
            }
        }

        PieDataSet ds = new PieDataSet(entries, "");
        ds.setColors(ColorTemplate.MATERIAL_COLORS);
        ds.setSliceSpace(2f);
        ds.setValueTextSize(12f);
        ds.setValueFormatter(new PercentFormatter(pie));

        pie.setData(new PieData(ds));
        pie.highlightValues(null);
        pie.invalidate();
    }

    private static String cap(String s) {
        if (s == null) {
            return "";
        }
        s = s.trim();
        return s.isEmpty() ? ""
                : s.substring(0, 1).toUpperCase(Locale.US) + s.substring(1);
    }
}
