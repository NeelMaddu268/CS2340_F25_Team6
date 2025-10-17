package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.model.Expense;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ExpenseCreationViewModel extends ViewModel {

    private final MutableLiveData<String> text = new MutableLiveData<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public ExpenseCreationViewModel() {
        // Just sets a sample value (not used for logic)
        text.setValue("Hello from ViewModel (placeholder)");
    }

    public LiveData<String> getText() {
        return text;
    }

    public void createExpense(String name, String date, String amountString, String category) {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        String uid = auth.getCurrentUser().getUid();

        double amount;
        try {
            amount = Double.parseDouble(amountString);
        } catch (NumberFormatException e) {
            text.setValue("Invalid amount");
            amount = 0.0; //temporarily set the amount to 0 if amount is invalid
        }

        Expense expense = new Expense(name, amount, category, date);
        db.collection("users")
                .document(uid)
                .collection("expenses")
                .add(expense)
                .addOnSuccessListener(documentReference -> {
                    System.out.println("Expense added");
                })
                .addOnFailureListener(e -> {
                    System.err.println("Expense failed to add");
                });
    }
    public void createSampleExpenses() {
        createExpense("Expense 1", "2023-05-01", "100.00", "Category 1");
        createExpense("Expense 2", "2023-05-02", "101.00", "Category 1");
        createExpense("Expense 3", "2023-05-03", "102.00", "Category 2");
        createExpense("Expense 4", "2023-05-04", "103.00", "Category 2");

    }
}
