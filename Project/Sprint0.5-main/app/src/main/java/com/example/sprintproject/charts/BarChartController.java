// This class helps create a chart that compares the users total spending and their total budget.
// This class helps configure the appearance of the chart and updates the chart with the new data whenever render() is called.

package com.example.sprintproject.charts;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;

public class BarChartController {
    private final BarChart bar;

    public BarChartController(BarChart bar) {
        this.bar = bar;
        if (bar != null) {
            bar.getDescription().setEnabled(false);
            bar.getLegend().setEnabled(false);
            bar.setScaleEnabled(false);
            bar.setPinchZoom(false);

            XAxis x = bar.getXAxis();
            x.setPosition(XAxis.XAxisPosition.BOTTOM);
            x.setDrawGridLines(false);
            x.setGranularity(1f);
            x.setAxisMinimum(-0.5f);
            x.setAxisMaximum(1.5f);

            YAxis left = bar.getAxisLeft();
            left.setDrawGridLines(true);
            left.setAxisMinimum(0f);
            bar.getAxisRight().setEnabled(false);
        }
    }

    public void render(double budgetAllTime, double spentAllTime) {
        if (bar == null) {
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, (float) budgetAllTime));
        entries.add(new BarEntry(1f, (float) spentAllTime));

        float max = Math.max((float) budgetAllTime, (float) spentAllTime);
        if (max <= 0f) {
            bar.clear();
            bar.invalidate();
            return;
        }

        BarDataSet set = new BarDataSet(entries, "");
        set.setColors(ColorTemplate.MATERIAL_COLORS[3], ColorTemplate.MATERIAL_COLORS[0]);
        set.setValueTextSize(12f);

        BarData data = new BarData(set);
        data.setBarWidth(0.5f);

        bar.setData(data);
        bar.getAxisLeft().setAxisMaximum(max * 1.15f);
        bar.invalidate();
    }
}
