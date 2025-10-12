package com.example.sprintproject.model;

import java.util.Date;

public class Expense {
    private final String name;
    private final double amount;
    private final String category;
    private final Date startDate;

    public Expense(String name, double amount, String category, Date startDate) {
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

    public Date getStartDate() {
        return startDate;
    }
}
