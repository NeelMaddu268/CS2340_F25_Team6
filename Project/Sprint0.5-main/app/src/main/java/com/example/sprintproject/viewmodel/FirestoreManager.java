package com.example.sprintproject.viewmodel;

import com.example.sprintproject.model.Budget;
import com.example.sprintproject.model.Expense;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

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

    public CollectionReference savingsCirclesGlobalReference() {
        return db.collection("savingsCircles");
    }

    public CollectionReference userSavingsCirclePointers(String uid) {
        return db.collection("users").document(uid).collection("savingsCirclePointers");
    }

    public CollectionReference invitationsReference() {
        return db.collection("invitations");
    }

    public void addUser(String uid, Map<String, Object> userData) {
        db.collection("users").document(uid).set(userData);
    }

    public Query invitationsForUser(String uid) {
        return invitationsReference().whereEqualTo("toUid", uid);
    }


    public void addBudget(String uid, Budget budget) {
        budgetsReference(uid).add(budget);
    }

    public void addExpense(String uid, Expense expense) {
        expensesReference(uid).add(expense);
    }

}
