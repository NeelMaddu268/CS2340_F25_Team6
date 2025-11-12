package com.example.sprintproject.strategies;

import com.example.sprintproject.viewmodel.FirestoreManager;
import com.google.firebase.firestore.Query;

public class AllTimeWindowStrategy extends ExpenseWindowStrategy {
    @Override
    protected Query windowQuery(FirestoreManager fm, String uid) {
        return fm.expensesReference(uid);
    }
}
