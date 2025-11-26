package com.example.sprintproject.model;

public interface FinanceVisitor {
    void visit(Budget budget);
    void visit(Expense expense);
}

