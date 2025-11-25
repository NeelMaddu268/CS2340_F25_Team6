// The ViewModel handles the saving and retreiving of the budget calculation data for each budget.
// Manages proper authentication and delivers the stored values of
// the budgets back to the UI for proper display.

package com.example.sprintproject.viewmodel;

import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class BudgetDetailsViewModel extends ViewModel {

    private final FirestoreManager firestore = FirestoreManager.getInstance();

    /**
     * Saves the calculation data for a specific budget under the user's Firestore collection.
     *
     * @param budgetId  The ID of the budget document to save the calculation under.
     * @param total     The total budget amount.
     * @param spent     The total amount spent so far.
     * @param remaining The remaining budget balance.
     * @param onSuccess Callback to execute if the data is successfully saved.
     * @param onFailure Callback to execute if the save operation fails.
     */
    public void saveCalculation(String budgetId,
                                       double total, double spent, double remaining,
                                Runnable onSuccess, Runnable onFailure) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            if (onFailure != null) {
                onFailure.run();
            }
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
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                })
                .addOnFailureListener(e -> {
                    if (onFailure != null) {
                        onFailure.run();
                    }
                });
    }

    /**
     * Loads the calculation data for a specific budget from Firestore, if available.
     *
     * @param budgetId The ID of the budget to load calculations for.
     * @param callback The callback to handle loaded calculation data.
     */
    public void loadCalculation(String budgetId, FirestoreCalcCallback callback) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            return;
        }

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
