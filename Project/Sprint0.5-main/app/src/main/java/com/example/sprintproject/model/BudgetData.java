package com.example.sprintproject.model;

public class BudgetData {
    private String name;
    private double amount;
    private String category;
    private String frequency;
    private String startDate;
    private String categoryId;

    public BudgetData(String name, double amount, String category,
                      String frequency, String startDate, String categoryId) {
        this.name = name;
        this.amount = amount;
        this.category = category;
        this.frequency = frequency;
        this.startDate = startDate;
        this.categoryId = categoryId;
    }

    // Getters for the fields
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

    public String getCategoryId() {
        return categoryId;
    }
}
