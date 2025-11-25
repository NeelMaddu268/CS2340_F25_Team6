// This class shows a user account and stores the user's basic information.
// This class also tracks the users activity such as creating budgets and expenses.

package com.example.sprintproject.model;

public class User {
    private final String email;
    private final String name;
    private final String password;
    private int totalExpenses;
    private int totalBudgets;

    public User(String email, String name, String password, int totalExpenses, int totalBudgets) {
        this.email = email;
        this.name = name;
        this.password = password;
        this.totalExpenses = totalExpenses;
        this.totalBudgets = totalBudgets;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public int getTotalExpenses() {
        return totalExpenses;
    }

    public int getTotalBudgets() {
        return totalBudgets;
    }
}
