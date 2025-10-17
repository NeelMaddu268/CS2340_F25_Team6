package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.model.Budget;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class BudgetCreationViewModel extends ViewModel {

    private final MutableLiveData<String> text = new MutableLiveData<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<String> getText() {
        return text;
    }

    public void createBudget(
            String name, String date, String amountString, String category, String frequency) {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        String uid = auth.getCurrentUser().getUid();

        double amount;
        try {
            amount = Double.parseDouble(amountString);
        } catch (NumberFormatException e) {
            text.setValue("Invalid amount");
            amount = 0.0; //temporarily set the amount to 0 if amount is invalid
        }

        Budget budget = new Budget(name, amount, category, frequency, date);
        db.collection("users")
                .document(uid)
                .collection("budgets")
                .add(budget)
                .addOnSuccessListener(documentReference -> {
                    System.out.println("Budget added");
                })
                .addOnFailureListener(e -> {
                    System.err.println("Budget failed to add");
                });
    }
    public void createSampleBudgets() {
        createBudget("Budget 1", "2023-05-01", "100.00", "Category 1", "Weekly");
        createBudget("Budget 2", "2023-05-02", "101.00", "Category 1", "Monthly");
        createBudget("Budget 3", "2023-05-03", "102.00", "Category 2", "Weekly");
        createBudget("Budget 4", "2023-05-04", "103.00", "Category 2", "Monthly");

    }
}
