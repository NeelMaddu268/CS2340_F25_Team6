package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.FirestoreManager;
import com.example.sprintproject.model.Budget;
import com.example.sprintproject.model.Expense;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExpenseCreationViewModel extends ViewModel {

    private final MutableLiveData<String> text = new MutableLiveData<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final MutableLiveData<List<String>> categoriesLiveData =
            new MutableLiveData<>(new ArrayList<>());

    public ExpenseCreationViewModel() {
        // Just sets a sample value (not used for logic)
        text.setValue("Hello from ViewModel (placeholder)");
    }

    public LiveData<String> getText() {
        return text;
    }

    public LiveData<List<String>> getCategories() {
        return categoriesLiveData;
    }

    public void loadCategories() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirestoreManager.getInstance().categoriesReference(uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> categoryNames = new ArrayList<>();
                    for (DocumentSnapshot doc: querySnapshot) {
                        if (doc.getString("name") != null) {
                            categoryNames.add(doc.getString("name"));
                        }
                    }
                    categoriesLiveData.setValue(categoryNames);
                })
                .addOnFailureListener(e -> {
                    categoriesLiveData.setValue(new ArrayList<>());
                });
    }

    public void createExpense(String name, String date, String amountString,
                              String category, String notes, Runnable onBudgetUpdated) {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        System.out.println("[createExpense] Starting for category=" + category
                + ", amount=" + amountString + ", date=" + date);


        if (auth.getCurrentUser() == null) {
            text.setValue("User not logged in");
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        // Normalize category
        String normalizedCategory = category == null ? "" : category.trim().toLowerCase(Locale.US);

        double amount;
        try {
            amount = Double.parseDouble(amountString);
            if (amount <= 0) {
                text.setValue("Amount must be greater than 0");
                return;
            }
        } catch (NumberFormatException e) {
            text.setValue("Invalid amount");
            return;
        }

        long timestamp = parseDateToMillis(date);
        Expense expense = new Expense(name, amount, normalizedCategory, date, notes);
        expense.setTimestamp(timestamp);

        //Step 1: Add the expense to Firestore
        FirestoreManager.getInstance().expensesReference(uid)
                .add(expense)
                .addOnSuccessListener(documentReference -> {

                    System.out.println("[createExpense] Expense added successfully! ID="
                            + documentReference.getId());

                    System.out.println("Expense added successfully: " + name);

                    String expenseId = documentReference.getId();

                    //Step 1: Find or create the category document and update its expenses array
                    FirestoreManager.getInstance().categoriesReference(uid)
                            .whereEqualTo("name", normalizedCategory)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(categoryQuery -> {
                                System.out.println("[createExpense] Category query success. Found "
                                        + categoryQuery.size() + " docs.");
                                if (!categoryQuery.isEmpty()) {
                                    // Category exists — just add this expense ID
                                    System.out.println("[createExpense] Added "
                                            + "expense ID to existing category: "
                                            + normalizedCategory);
                                    DocumentSnapshot categoryDoc =
                                            categoryQuery.getDocuments().get(0);
                                    categoryDoc.getReference().update("expenses",
                                            FieldValue.arrayUnion(expenseId));
                                } else {
                                    // Category doesn't exist — create a new one
                                    System.out.println("[createExpense] Created new category for: "
                                            + normalizedCategory);
                                    Map<String, Object> newCategory = new HashMap<>();
                                    newCategory.put("name", normalizedCategory);
                                    newCategory.put("budgets",
                                            Collections.emptyList());
                                    newCategory.put("expenses",
                                            Collections.singletonList(expenseId));

                                    FirestoreManager.getInstance()
                                            .categoriesReference(uid)
                                            .add(newCategory)
                                            .addOnSuccessListener(ref ->
                                                    System.out.println("Created new category for "
                                                            + normalizedCategory))
                                            .addOnFailureListener(e ->
                                                    System.err.println("Failed to create category: "
                                                            + e.getMessage()));
                                }
                            })
                            .addOnFailureListener(e -> {
                                System.err.println("[createExpense] Category query failed: "
                                        + e.getMessage());
                            });



                    // Step 2: Update matching budget totals

                    FirestoreManager.getInstance().budgetsReference(uid)
                            .whereEqualTo("category", normalizedCategory)
                            .orderBy("startDateTimestamp", Query.Direction.DESCENDING)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(budgetQuery -> {
                                System.out.println("[createExpense] Budget query success. Found "
                                        + budgetQuery.size() + " docs.");
                                if (budgetQuery.isEmpty()) {
                                    System.out.println(
                                            "[createExpense] No budget found for category: "
                                            + normalizedCategory);
                                    return;
                                }
                                DocumentSnapshot budgetDoc = budgetQuery.getDocuments().get(0);
                                Budget budget = budgetDoc.toObject(Budget.class);
                                if (budget == null) {
                                    return;
                                }

                                long budgetStart = budget.getStartDateTimestamp();
                                long budgetEnd = budgetStart;
                                if ("Weekly".equalsIgnoreCase(budget.getFrequency())) {
                                    budgetEnd += 7L * 24 * 60 * 60 * 1000;
                                } else if ("Monthly".equalsIgnoreCase(budget.getFrequency())) {
                                    budgetEnd += 30L * 24 * 60 * 60 * 1000;
                                }
                                long finalBudgetEnd = budgetEnd;

                                //Nested correctly inside budgetQuery success
                                FirestoreManager.getInstance().expensesReference(uid)
                                        .whereEqualTo("category", normalizedCategory)
                                        .get()
                                        .addOnSuccessListener(expenseQuery -> {
                                            System.out.println("[createExpense] Fetched "
                                                    + expenseQuery.size() +
                                                    " expenses for category " + normalizedCategory);
                                            double spentToDate = 0;
                                            long now = System.currentTimeMillis();
                                            long effectiveEnd = Math.min(finalBudgetEnd, now);

                                            for (DocumentSnapshot expDoc
                                                    : expenseQuery.getDocuments()) {
                                                Expense e = expDoc.toObject(Expense.class);
                                                if (e != null) {
                                                    System.out.println("Expense: " + e.getName()
                                                            + " | $" + e.getAmount() + " | " + e.getDate());
                                                    long t = e.getTimestamp();
                                                    if (t >= budgetStart - 24L * 60 * 60 * 1000 && t <= effectiveEnd) {
                                                        spentToDate += e.getAmount();
                                                        System.out.println(" Counted toward budget: "
                                                                + e.getName());
                                                    } else {
                                                        System.out.println("Skipped (out of range): "
                                                                + e.getName());
                                                    }
                                                }
                                            }
                                            System.out.println("[createExpense] SpentToDate calculated: "
                                                    + spentToDate);


                                            double remaining = budget.getAmount() - spentToDate;
                                            boolean overBudget = remaining < 0;

                                            double finalSpentToDate = spentToDate;
                                            double finalSpentToDate1 = spentToDate;
                                            FirestoreManager.getInstance()
                                                    .budgetsReference(uid)
                                                    .document(budgetDoc.getId())
                                                    .update(
                                                            "spentToDate", spentToDate,
                                                            "moneyRemaining", remaining,
                                                            "overBudget", overBudget
                                                    )
                                                    .addOnSuccessListener(a -> {
                                                        System.out.println("[createExpense] Budget '"
                                                                + budget.getName() + "' updated: spent=" + finalSpentToDate1 + ", remaining=" + remaining);
                                                        if (onBudgetUpdated != null) {
                                                            onBudgetUpdated.run();
                                                        }
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        System.err.println(
                                                                "[createExpense] Failed to update budget: "
                                                                + e.getMessage());
                                                    });
                                        })
                                        .addOnFailureListener(e ->
                                                System.err.println("Expense fetch failed: "
                                                        + e.getMessage()));
                            })
                            .addOnFailureListener(e ->
                                    System.err.println("Budget fetch failed: " + e.getMessage()));

                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                });

    }


    public void createSampleExpenses() {

        //        {"Eating Budget", "Oct 17, 2025", "100.00", "Eating", "Weekly"},
        //        {"Travel Budget", "Oct 19, 2025", "1000.00", "Travel", "Monthly"},
        //        {"Gaming Budget", "Oct 21, 2025", "1500.00", "Gaming", "Weekly"}

        createExpense("Tin Drum", "Oct 15, 2025", "20.00", "eating", null, null);
        createExpense("Panda Express", "Oct 20, 2025", "30.00", "eating", "Was Hungry", null);

        createExpense("Hawaii", "Oct 18, 2025", "100.00", "travel", null, null);
        createExpense("Spain", "Oct 19, 2025", "500.00", "travel", "Spring Break", null);

        createExpense("Xbox", "Oct 21, 2025", "200.00", "gaming", null, null);
        createExpense("PS5", "Oct 22, 2025", "800.00", "gaming", "Xbox Broke", null);

        createExpense("Loan", "Oct 07, 2025", "1000.00", "other", null, null);

    }

    public void createSampleDate(Runnable onComplete) {
        createExpense("Tin Drum", "Oct 19, 2025", "20.00", "eating", null, null);
        createExpense("Panda Express", "Oct 20, 2025", "30.00", "eating", "Was Hungry", null);

        createExpense("Hawaii", "Oct 20, 2025", "100.00", "travel", null, null);
        createExpense("Spain", "Oct 21, 2025", "500.00", "travel", "Spring Break", null);

        createExpense("Xbox", "Oct 21, 2025", "200.00", "gaming", null, null);
        createExpense("PS5", "Oct 22, 2025", "800.00", "gaming", "Xbox Broke", null);

        createExpense("Loan", "Oct 07, 2025", "1000.00", "other", null, null);

        BudgetCreationViewModel budgetCreationViewModel = new BudgetCreationViewModel();

        budgetCreationViewModel.createSampleBudgets(() -> {
            System.out.println("sample budgets made after expenses");
            if (onComplete != null) {
                onComplete.run();
            }
        });

    }

    private long parseDateToMillis(String dateString) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        try {
            Date date = sdf.parse(dateString);
            if (date != null) {
                return date.getTime();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return System.currentTimeMillis();
    }
}
