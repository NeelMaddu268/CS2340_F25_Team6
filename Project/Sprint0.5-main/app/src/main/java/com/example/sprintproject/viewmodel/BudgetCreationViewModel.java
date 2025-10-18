package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.model.Budget;
import com.example.sprintproject.model.BudgetData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BudgetCreationViewModel extends ViewModel {

    private final MutableLiveData<String> text = new MutableLiveData<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<String> getText() {
        return text;
    }

    public void createBudget(
            String name, String date, String amountString, String category,
            String frequency, Runnable onComplete) {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        String uid = auth.getCurrentUser().getUid();

        double amount;
        try {
            amount = Double.parseDouble(amountString);
        } catch (NumberFormatException e) {
            text.setValue("Invalid amount");
            amount = 0.0; //temporarily set the amount to 0 if amount is invalid
        }
        double finalAmount = amount;

        //Looking for the category, if it exists
        db.collection("users")
                .document(uid)
                .collection("categories")
                .whereEqualTo("name", category)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        //category exists
                        DocumentSnapshot categoryDocument = querySnapshot.getDocuments().get(0);
                        Object budgetsObject = categoryDocument.get("budgets");

                        if (budgetsObject instanceof java.util.List
                                && !((java.util.List<?>) budgetsObject).isEmpty()) {
                            //This category already has a budget
                            text.setValue("This category already has a budget");
                            if (onComplete != null) {
                                onComplete.run();
                            }
                            return;
                        }

                        //Category exists but has no budgets in it, edge case
                        BudgetData budgetData = new BudgetData(name, finalAmount,
                                category, frequency, date, categoryDocument.getId());
                        addBudgetToFirestore(uid, budgetData, onComplete);
                    } else {
                        //category doesn't exist yet, make it
                        BudgetData budgetData = new BudgetData(name, finalAmount,
                                category, frequency, date, null);
                        addBudgetToFirestore(uid, budgetData, onComplete);
                    }
                })
                .addOnFailureListener(e -> {
                    System.err.println("Budget failed to add");
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
    }

    private void addBudgetToFirestore(
            String uid, BudgetData budgetData, Runnable onComplete) {
        Budget budget = new Budget(
                budgetData.getName(),
                budgetData.getAmount(),
                budgetData.getCategory(),
                budgetData.getFrequency(),
                budgetData.getStartDate());

        db.collection("users").document(uid)
                .collection("budgets")
                .add(budget)
                .addOnSuccessListener(documentReference -> {
                    String budgetId = documentReference.getId();

                    if (budgetData.getCategoryId() != null) {
                        //Category already existed, edge case
                        db.collection("users").document(uid)
                                .collection("categories")
                                .document(budgetData.getCategoryId())
                                .update("budgets", Collections.singletonList(budgetId))
                                .addOnCompleteListener(task -> {
                                    if (onComplete != null) {
                                        onComplete.run();
                                    }
                                });
                    } else {
                        //Category doesn't exist
                        Map<String, Object> newCategory = new HashMap<>();
                        newCategory.put("name", budgetData.getCategory());
                        newCategory.put("budgets", Arrays.asList(budgetId));
                        newCategory.put("expenses", Arrays.asList());
                        db.collection("users").document(uid)
                                .collection("categories")
                                .add(newCategory)
                                .addOnCompleteListener(task -> {
                                    if (onComplete != null) {
                                        onComplete.run();
                                    }
                                });
                    }
                });
    }


    public void createSampleBudgets(Runnable onComplete) {
        String[][] sampleBudgets = {
                {"Eating Budget", "2023-05-01", "100.00", "Eating", "Weekly"},
                {"Travel Budget", "2023-05-02", "1000.00", "Travel", "Monthly"},
                {"Gaming Budget", "2023-05-03", "1500.00", "Gaming", "Weekly"}
        };

        final int[] completedCount = {0};
        final int totalBudgets = sampleBudgets.length;

        for (String[] budgetData : sampleBudgets) {
            createBudget(
                    budgetData[0], // name
                    budgetData[1], // date
                    budgetData[2], // amount
                    budgetData[3], // category
                    budgetData[4], // frequency
                    () -> {
                        completedCount[0]++;
                        if (completedCount[0] == totalBudgets && onComplete != null) {
                            onComplete.run();
                        }
                    }
            );
        }
    }
}