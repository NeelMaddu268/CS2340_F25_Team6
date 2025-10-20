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

    public void createExpense(String name, String date,
                              String amountString, String category, String notes) {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            text.setValue("User not logged in");
            return;
        }

        String uid = auth.getCurrentUser().getUid();

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

        FirestoreManager.getInstance().expensesReference(uid)
                .add(expense)
                .addOnSuccessListener(documentReference -> {
                    String expenseId = documentReference.getId();

                    FirestoreManager.getInstance().categoriesReference(uid)
                            .whereEqualTo("name", normalizedCategory)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                if (!querySnapshot.isEmpty()) {
                                    DocumentSnapshot categoryDoc =
                                            querySnapshot.getDocuments().get(0);
                                    categoryDoc.getReference().update("expenses",
                                            FieldValue.arrayUnion(expenseId));
                                } else {
                                    Map<String, Object> newCategory = new HashMap<>();
                                    newCategory.put("name", normalizedCategory);
                                    newCategory.put("budgets", Collections.emptyList());
                                    newCategory.put("expenses",
                                            Collections.singletonList(expenseId));
                                    FirestoreManager.getInstance()
                                            .categoriesReference(uid).add(newCategory);
                                }
                            });

                    FirestoreManager.getInstance().budgetsReference(uid)
                            .whereEqualTo("category", normalizedCategory)
                            .get()
                            .addOnSuccessListener(budgetQuery -> {
                                if (!budgetQuery.isEmpty()) {
                                    DocumentSnapshot budgetDoc = budgetQuery.getDocuments().get(0);
                                    Budget budget = budgetDoc.toObject(Budget.class);
                                    if (budget != null) {
                                        long budgetStart = budget.getStartDateTimestamp();
                                        long budgetEnd = budgetStart;

                                        if (budget.getFrequency().equalsIgnoreCase("Weekly")) {
                                            budgetEnd += 7L * 24 * 60 * 60 * 1000;
                                        } else if (budget.getFrequency()
                                                .equalsIgnoreCase("Monthly")) {
                                            budgetEnd += 30L * 24 * 60 * 60 * 1000;
                                        }

                                        long finalBudgetEnd = budgetEnd;
                                        FirestoreManager.getInstance().expensesReference(uid)
                                                .whereEqualTo("category", normalizedCategory)
                                                .get()
                                                .addOnSuccessListener(expenseQuery -> {
                                                    double spentToDate = 0;
                                                    for (DocumentSnapshot expDoc
                                                            : expenseQuery.getDocuments()) {
                                                        Expense e = expDoc.toObject(Expense.class);
                                                        if (e != null) {
                                                            long t = e.getTimestamp();
                                                            if (t >= budgetStart
                                                                    && t <= finalBudgetEnd) {
                                                                spentToDate += e.getAmount();
                                                            }
                                                        }
                                                    }

                                                    double remaining =
                                                            budget.getAmount() - spentToDate;
                                                    FirestoreManager.getInstance()
                                                            .budgetsReference(uid)
                                                            .document(budgetDoc.getId())
                                                            .update(
                                                                    "spentToDate", spentToDate,
                                                                    "moneyRemaining", remaining
                                                        );
                                                });
                                    }
                                }
                            });
                })
                .addOnFailureListener(e -> System.err.println("Failed to add expense: "
                        + e.getMessage()));
    }

    public void createSampleExpenses() {

        //        {"Eating Budget", "Oct 17, 2025", "100.00", "Eating", "Weekly"},
        //        {"Travel Budget", "Oct 19, 2025", "1000.00", "Travel", "Monthly"},
        //        {"Gaming Budget", "Oct 21, 2025", "1500.00", "Gaming", "Weekly"}

        createExpense("Tin Drum", "Oct 15, 2025", "20.00", "Eating", null);
        createExpense("Panda Express", "Oct 20, 2025", "30.00", "Eating", "Was Hungry");

        createExpense("Hawaii", "Oct 18, 2025", "100.00", "Travel", null);
        createExpense("Spain", "Oct 19, 2025", "500.00", "Travel", "Spring Break");

        createExpense("Xbox", "Oct 21, 2025", "200.00", "Gaming", null);
        createExpense("PS5", "Oct 22, 2025", "800.00", "Gaming", "Xbox Broke");

        createExpense("Loan", "Oct 07, 2025", "1000.00", "Other", null);

    }

    public void createSampleDate(Runnable onComplete) {
        createExpense("Tin Drum", "Oct 19, 2025", "20.00", "Eating", null);
        createExpense("Panda Express", "Oct 20, 2025", "30.00", "Eating", "Was Hungry");

        createExpense("Hawaii", "Oct 20, 2025", "100.00", "Travel", null);
        createExpense("Spain", "Oct 21, 2025", "500.00", "Travel", "Spring Break");

        createExpense("Xbox", "Oct 21, 2025", "200.00", "Gaming", null);
        createExpense("PS5", "Oct 22, 2025", "800.00", "Gaming", "Xbox Broke");

        createExpense("Loan", "Oct 07, 2025", "1000.00", "Other", null);

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
