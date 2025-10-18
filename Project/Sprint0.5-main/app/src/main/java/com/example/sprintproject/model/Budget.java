package com.example.sprintproject.model;

public class Budget {
    private String name;
    private double amount;
    private String category;
    private String frequency;
    private String startDate;

    public Budget() {

    }

    public Budget(String name, double amount, String category, String frequency, String startDate) {
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

    public String getStartDate() {
        return startDate;
    }
}
