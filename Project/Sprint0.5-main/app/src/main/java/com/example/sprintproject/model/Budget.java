package com.example.sprintproject.model;

import java.util.Date;

public class Budget {
    private final String name;
    private final double amount;
    private final String category;
    private final String frequency;
    private final Date startDate;

    public Budget(String name, double amount, String category, String frequency, Date startDate) {
        this.name = name;
        this.amount = amount;
        this.category = category;
        this.frequency = frequency;
        this.startDate = startDate;
    }

    public String getName() {
        return name;
    }

    public double getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public String getFrequency() {
        return frequency;
    }

    public Date getStartDate() {
        return startDate;
    }
}
