// This class is a helper class that connects the chart views to their controller class
// This class creates and returns a chart object that can be updated with new data by the app.

package com.example.sprintproject.charts;

import android.view.View;

import com.example.sprintproject.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;

public class ChartFactory {

    private ChartFactory() {
        throw new IllegalStateException("Chart Factory");
    }

    public static Charts attach(View root) {
        PieChart pie = root.findViewById(R.id.pieChart);
        BarChart bar = root.findViewById(R.id.barChartTotals);
        PieChartController pieCtl = new PieChartController(pie);
        BarChartController barCtl = new BarChartController(bar);
        return new Charts(pieCtl, barCtl);
    }
}
