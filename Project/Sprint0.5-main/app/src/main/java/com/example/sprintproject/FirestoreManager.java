package com.example.sprintproject;

import com.example.sprintproject.model.Budget;
import com.example.sprintproject.model.Expense;
import com.example.sprintproject.model.SavingsCircle;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class FirestoreManager {
    private static volatile FirestoreManager instance;
    private final FirebaseFirestore db;

    private FirestoreManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirestoreManager getInstance() {
        if (instance == null) {
            synchronized (FirestoreManager.class) {
                if (instance == null) {
                    instance = new FirestoreManager();
                }
            }
        }
        return instance;
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    public CollectionReference budgetsReference(String uid) {
        return db.collection("users").document(uid).collection("budgets");
    }

    public CollectionReference expensesReference(String uid) {
        return db.collection("users").document(uid).collection("expenses");
    }

    public CollectionReference categoriesReference(String uid) {
        return db.collection("users").document(uid).collection("categories");
    }

    public CollectionReference savingsCircleReference(String uid) {
        return db.collection("users").document(uid).collection("savingsCircle");
    }

    public void addUser(String uid, Map<String, Object> userData) {
        db.collection("users").document(uid).set(userData);
    }

    public void addBudget(String uid, Budget budget) {
        budgetsReference(uid).add(budget);
    }

    public void addExpense(String uid, Expense expense) {
        expensesReference(uid).add(expense);
    }

    public void addGroup(String uid, SavingsCircle savingsCircle) {
        savingsCircleReference(uid).add(savingsCircle);
    }
}
