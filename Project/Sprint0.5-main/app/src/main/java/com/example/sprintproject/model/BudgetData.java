// This class is a data collection for any budgets
// that are created and stored before its saved onto firebase.
// This class also stores category id and timestamps
// (date) so that the budget can be referenced later on.

package com.example.sprintproject.model;

public class BudgetData {
    private String name;
    private double amount;
    private String category;
    private String frequency;
    private String startDate;
    private String categoryId;

    private long startDateTimeStamp;

    public BudgetData(String name, double amount, String category, String frequency,
                      String startDate, String categoryId, long startDateTimeStamp) {
        this.name = name;
        this.amount = amount;
        this.category = category;
        this.frequency = frequency;
        this.startDate = startDate;
        this.categoryId = categoryId;
        this.startDateTimeStamp = startDateTimeStamp;
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

    public long getStartDateTimestamp() {
        return startDateTimeStamp;
    }
}
