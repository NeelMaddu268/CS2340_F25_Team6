// this class contains both the pie and bar charts in one place
// Helps make it easier for the app to update the pie and bar charts.

package com.example.sprintproject.charts;

public class Charts {
    private final PieChartController pie;
    private final BarChartController bar;

    public Charts(PieChartController pie, BarChartController bar) {
        this.pie = pie;
        this.bar = bar;
    }

    public PieChartController getPie() {
        return pie;
    }

    public BarChartController getBar() {
        return bar;
    }
}
