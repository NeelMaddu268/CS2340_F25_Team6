package com.example.sprintproject.viewmodel;

import androidx.lifecycle.ViewModel;

import com.example.sprintproject.FirestoreManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class BudgetDetailsViewModel extends ViewModel {

    private final FirestoreManager firestore = FirestoreManager.getInstance();

    /** Trying to put the calculator data into subcollection under the budget */
    public void saveCalculation(String budgetId, double total, double spent, double remaining,
                                Runnable onSuccess, Runnable onFailure) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            if (onFailure != null) onFailure.run();
            return;
        }
        String uid = auth.getCurrentUser().getUid();

        Map<String, Object> calc = new HashMap<>();
        calc.put("total", total);
        calc.put("spent", spent);
        calc.put("remaining", remaining);
        calc.put("lastUpdated", System.currentTimeMillis());

        firestore.budgetsReference(uid)
                .document(budgetId)
                .collection("calculations")
                .document("userCalculations")
                .set(calc)
                .addOnSuccessListener(a -> {
                    if (onSuccess != null) onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    if (onFailure != null) onFailure.run();
                });
    }

    /** Load the calculation data, if it has any from /calculations */
    public void loadCalculation(String budgetId, FirestoreCalcCallback callback) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();

        firestore.budgetsReference(uid)
                .document(budgetId)
                .collection("calculations")
                .document("userCalculations")
                .get()
                .addOnSuccessListener((DocumentSnapshot doc) -> {
                    if (doc.exists() && callback != null) {
                        Double total = doc.getDouble("total");
                        Double spent = doc.getDouble("spent");
                        Double remaining = doc.getDouble("remaining");
                        callback.onCalculationLoaded(total, spent, remaining);
                    }
                });
    }

    public interface FirestoreCalcCallback {
        void onCalculationLoaded(Double total, Double spent, Double remaining);
    }
}
