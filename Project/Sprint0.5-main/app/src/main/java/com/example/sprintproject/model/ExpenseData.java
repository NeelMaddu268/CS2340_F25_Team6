package com.example.sprintproject.model;

public class ExpenseData {
    private String name;
    private String date;
    private String amountString;
    private String category;
    private String notes;
    private boolean contributesToGroupSavings;
    private String circleId;

    public ExpenseData(String name, String date, String amountString,
                        String category, String notes, boolean contributesToGroupSavings,
                        String circleId) {
        this.name = name;
        this.date = date;
        this.amountString = amountString;
        this.category = category;
        this.notes = notes;
        this.contributesToGroupSavings = contributesToGroupSavings;
        this.circleId = circleId;
    }

    public String getName() {
        return name;
    }
    public String getDate() {
        return date;
    }
    public String getAmountString() {
        return amountString;
    }
    public String getCategory() {
        return category;
    }
    public String getNotes() {
        return notes;
    }
    public boolean getContributesToGroupSavings() {
        return contributesToGroupSavings;
    }
    public String getCircleId() {
        return circleId;
    }
}
