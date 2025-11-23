package com.example.sprintproject.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Expense {
    private String name;
    private double amount;
    private String category;
    private String date;
    private String notes;

    private long timestamp;
    private boolean contributesToGroupSavings;

    public Expense() {

    }

    public Expense(String name, double amount, String category, String date, String notes) {
        this.name = name;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.notes = notes;
        this.timestamp = parseDateToMillis(date);
    }

    public Expense(String name, double amount, String category, String date) {
        this.name = name;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.notes = null;
        this.timestamp = parseDateToMillis(date);
    }

    private long parseDateToMillis(String dateString) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        try {
            Date parseDate = sdf.parse(dateString);
            if (parseDate != null) {
                return parseDate.getTime();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return System.currentTimeMillis();
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

    public String getDate() {
        return date;
    }

    public String getNotes() {
        return notes;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setDate(String startDate) {
        this.date = startDate;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean contributesToGroupSavings() {
        return contributesToGroupSavings;
    }

    public void setContributesToGroupSavings(boolean contributesToGroupSavings) {
        this.contributesToGroupSavings = contributesToGroupSavings;
    }
}
