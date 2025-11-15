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
