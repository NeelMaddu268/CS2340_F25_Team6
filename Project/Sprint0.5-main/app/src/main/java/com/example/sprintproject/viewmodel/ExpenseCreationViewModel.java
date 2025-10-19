package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.FirestoreManager;
import com.example.sprintproject.model.Expense;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
        FirestoreManager.getInstance().expensesReference(uid).add(expense)
                .addOnSuccessListener(documentReference -> {
                    String expenseId = documentReference.getId();

                    FirestoreManager.getInstance().categoriesReference(uid)
                            .whereEqualTo("name", category)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                if (!querySnapshot.isEmpty()) {
                                    //category exists
                                    DocumentSnapshot categoryDocument =
                                            querySnapshot.getDocuments().get(0);
                                    FirestoreManager.getInstance().categoriesReference(uid)
                                            .document(categoryDocument.getId())
                                            .update("expenses", FieldValue.arrayUnion(expenseId));
                                } else {
                                    //category doesn't exist yet, make it
                                    Map<String, Object> newCategory = new HashMap<>();
                                    newCategory.put("name", category);
                                    newCategory.put("budgets", Arrays.asList());
                                    newCategory.put("expenses", Arrays.asList(expenseId));
                                    FirestoreManager.getInstance()
                                            .categoriesReference(uid).add(newCategory);
                                }
                            });
                    System.out.println("Expense added");
                })
                .addOnFailureListener(e -> {
                    System.err.println("Expense failed to add");
                });
    }
    public void createSampleExpenses() {
        createExpense("Tin Drum", "2023-05-01", "10.00", "Eating");
        createExpense("Panda Express", "2023-05-02", "30.00", "Eating");

        createExpense("Hawaii", "2023-05-03", "500.00", "Travel");
        createExpense("Spain", "2023-05-04", "300.00", "Travel");

        createExpense("Xbox", "2023-05-05", "500.00", "Gaming");
        createExpense("PS5", "2023-05-06", "800.00", "Gaming");

    }
}
