package com.example.sprintproject.model;

public class Expense {
    private String name;
    private double amount;
    private String category;
    private String date;

    public Expense() {

    }

    public Expense(String name, double amount, String category, String date) {
        this.name = name;
        this.amount = amount;
        this.category = category;
        this.date = date;
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
}
