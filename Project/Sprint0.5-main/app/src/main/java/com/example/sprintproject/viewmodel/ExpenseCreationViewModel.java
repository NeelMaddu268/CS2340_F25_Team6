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
import java.util.Arrays;
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

        String uid = auth.getCurrentUser().getUid();

        double amount;
        try {
            amount = Double.parseDouble(amountString);
        } catch (NumberFormatException e) {
            text.setValue("Invalid amount");
            amount = 0.0; //temporarily set the amount to 0 if amount is invalid
        }

        long timestamp = parseDateToMillis(date);
        Expense expense = new Expense(name, amount, category, date, notes);
        expense.setTimestamp(timestamp);

        double finalAmount = amount;

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

                    FirestoreManager.getInstance().budgetsReference(uid)
                            .whereEqualTo("category", category)
                            .get()
                            .addOnSuccessListener(budgetQuery -> {
                                if (!budgetQuery.isEmpty()) {
                                    DocumentSnapshot budgetDocument =
                                            budgetQuery.getDocuments().get(0);
                                    Budget budget = budgetDocument.toObject(Budget.class);
                                    if (budget != null) {
                                        long budgetStart = budget.getStartDateTimestamp();

                                        FirestoreManager.getInstance().expensesReference(uid)
                                                .whereEqualTo("category", category)
                                                .get()
                                                .addOnSuccessListener(expenseQuery -> {
                                                    double spentToDate = 0;
                                                    for (DocumentSnapshot expenseDoc: expenseQuery.getDocuments()) {
                                                        Expense expense2 = expenseDoc.toObject(Expense.class);
                                                        if (expense2 != null && expense2.getTimestamp() >= budgetStart) {
                                                            spentToDate += expense2.getAmount();
                                                        }
                                                    }

                                                    double moneyRemaining = budget.getAmount() - spentToDate;

                                                    FirestoreManager.getInstance().budgetsReference(uid)
                                                            .document(budgetDocument.getId())
                                                            .update(
                                                                    "spentToDate", spentToDate,
                                                                    "moneyRemaining", moneyRemaining
                                                            )
                                                            .addOnSuccessListener(aVoid -> System.out.println(
                                                                    "Budget updated with new expense"))

                                                            .addOnFailureListener(e -> System.out.println(
                                                                    "Failed to update budget totals"));
                                                });
                                    }
                                } else {
                                    System.out.println(
                                            "No matching budget found for this category");
                                }
                            });
                    System.out.println("Expense added");
                })
                .addOnFailureListener(e -> {
                    System.out.println("Expense failed to add");
                });
    }

    public void createSampleExpenses() {

//        {"Eating Budget", "Oct 17, 2025", "100.00", "Eating", "Weekly"},
//        {"Travel Budget", "Oct 19, 2025", "1000.00", "Travel", "Monthly"},
//        {"Gaming Budget", "Oct 21, 2025", "1500.00", "Gaming", "Weekly"}
//
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
