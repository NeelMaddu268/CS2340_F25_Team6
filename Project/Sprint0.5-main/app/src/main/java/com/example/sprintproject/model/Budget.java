package com.example.sprintproject.model;

public class Budget {
    private String name;
    private double amount;
    private String category;
    private String frequency;
    private String startDate;
    private long startDateTimestamp;
    private boolean isCompleted;

    private boolean isOverBudget;
    private boolean hasPreviousCycle;
    private boolean previousCycleOverBudget;
    private long previousCycleEndTimestamp;

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
        this.isOverBudget = false;
        this.hasPreviousCycle = false;
        this.previousCycleOverBudget = false;
        this.previousCycleEndTimestamp = 0L;
    }

    public boolean isHasPreviousCycle() {
        return hasPreviousCycle;
    }

    public void setHasPreviousCycle(boolean hasPreviousCycle) {
        this.hasPreviousCycle = hasPreviousCycle;
    }

    public boolean isPreviousCycleOverBudget() {
        return previousCycleOverBudget;
    }

    public void setPreviousCycleOverBudget(boolean previousCycleOverBudget) {
        this.previousCycleOverBudget = previousCycleOverBudget;
    }

    public long getPreviousCycleEndTimestamp() {
        return previousCycleEndTimestamp;
    }

    public void setPreviousCycleEndTimestamp(long previousCycleEndTimestamp) {
        this.previousCycleEndTimestamp = previousCycleEndTimestamp;
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

    public boolean isOverBudget() {
        return isOverBudget;
    }

    public void setIsOverBudget(boolean isOverBudget) {
        this.isOverBudget = isOverBudget;
    }

    public long getStartDateTimestamp() {
        return startDateTimestamp;
    }

    public void setStartDateTimestamp(long startDateTimestamp) {
        this.startDateTimestamp = startDateTimestamp;
    }
}
