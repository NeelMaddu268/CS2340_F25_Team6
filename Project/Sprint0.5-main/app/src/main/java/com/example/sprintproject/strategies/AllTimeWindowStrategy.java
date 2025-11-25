// This class takes care of the all-time expense filtering strategy by
// returning every expense for a given user
// without date limits. Works with the firestore manager to build a query
// used by the ExpenseWindowStrategy.

package com.example.sprintproject.strategies;

import com.example.sprintproject.viewmodel.FirestoreManager;
import com.google.firebase.firestore.Query;

public class AllTimeWindowStrategy extends ExpenseWindowStrategy {
    @Override
    protected Query windowQuery(FirestoreManager fm, String uid) {
        return fm.expensesReference(uid);
    }
}
