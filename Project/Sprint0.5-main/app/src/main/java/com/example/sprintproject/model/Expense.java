package com.example.sprintproject.model;

import java.util.Date;

public class Expense {
    private final String name;
    private final double amount;
    private final String category;
    private final String startDate;

    public Expense(String name, double amount, String category, String startDate) {
        this.name = name;
        this.amount = amount;
        this.category = category;
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

    public String getStartDate() {
        return startDate;
    }
}
