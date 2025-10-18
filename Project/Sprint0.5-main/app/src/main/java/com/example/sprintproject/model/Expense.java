package com.example.sprintproject.model;

public class Expense {
    private String name;
    private double amount;
    private String category;
    private String startDate;

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

    public void setName(String name) {
        this.name = name;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }
}
