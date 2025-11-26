package com.example.sprintproject.model;

public interface FinanceElement {
    void accept(FinanceVisitor visitor);
}

