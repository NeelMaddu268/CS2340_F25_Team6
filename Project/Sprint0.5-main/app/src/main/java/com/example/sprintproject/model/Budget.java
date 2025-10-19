package com.example.sprintproject.model;

public class Budget {
    private String name;
    private double amount;
    private String category;
    private String frequency;
    private String startDate;
    private long startDateTimestamp;
    private boolean isCompleted;
    private boolean wasOverBudget;

    private String id; // stores the firebase document id
    private double spentToDate; // money spent up to today
    private double moneyRemaining; // left over money in budget

    public Budget() {

    }

    public Budget(String name, double amount, String category, String frequency, String startDate) {
        this.name = name;
        this.amount = amount;
        this.category = category;
        this.frequency = frequency;
        this.startDate = startDate;
        this.isCompleted = false;
        this.wasOverBudget = false;
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getSpentToDate() {
        return spentToDate;
    }
    public void setSpentToDate(double spentToDate) {
        this.spentToDate = spentToDate;
    }

    public double getMoneyRemaining() {
        return moneyRemaining;
    }
    public void setMoneyRemaining(double moneyRemaining) {
        this.moneyRemaining = moneyRemaining;
    }

    public boolean overBudget() {
        return moneyRemaining < 0;
    }

    public double getProgressPercent() {
        if (amount <= 0) {
            return 0;
        } else {
            return Math.min(100.0, (spentToDate / amount) * 100.0);
        }
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public boolean wasOverBudget() {
        return wasOverBudget;
    }

    public void setWasOverBudget(boolean wasOverBudget) {
        this.wasOverBudget = wasOverBudget;
    }

    public long getStartDateTimestamp() {
        return startDateTimestamp;
    }

    public void setStartDateTimestamp(long startDateTimestamp) {
        this.startDateTimestamp = startDateTimestamp;
    }
}
