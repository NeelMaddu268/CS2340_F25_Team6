package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.model.Budget;
import com.example.sprintproject.model.Expense;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

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

    public void createExpense(
            String name, String date, String amountString,
            String category, String notes, Runnable onBudgetUpdated) {

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            text.setValue("User not logged in");
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        String normalizedCategory = normalizeCategory(category);
        Double amount = parseAmount(amountString);
        if (amount == null) {
            return;
        }

        long timestamp = parseDateToMillis(date);
        Expense expense = new Expense(name, amount, normalizedCategory, date, notes);
        expense.setTimestamp(timestamp);

        System.out.println("[createExpense] Starting for category=" + category
                + ", amount=" + amount + ", date=" + date);

        FirestoreManager.getInstance().expensesReference(uid)
                .add(expense)
                .addOnSuccessListener(docRef -> {
                    System.out.println("[createExpense] Expense added successfully! ID="
                            + docRef.getId());
                    handleCategoryUpdate(uid, normalizedCategory, docRef.getId());
                    handleBudgetUpdate(uid, normalizedCategory, onBudgetUpdated);
                })
                .addOnFailureListener(e -> {
                    System.err.println("[createExpense] Failed to add expense: "
                            + e.getMessage());
                    e.printStackTrace();
                });
    }

    private String normalizeCategory(String category) {
        return category == null ? "" : category.trim().toLowerCase(Locale.US);
    }

    private Double parseAmount(String amountString) {
        try {
            double amount = Double.parseDouble(amountString);
            if (amount <= 0) {
                text.setValue("Amount must be greater than 0");
                return null;
            }
            return amount;
        } catch (NumberFormatException e) {
            text.setValue("Invalid amount");
            return null;
        }
    }

    private void handleCategoryUpdate(String uid, String category, String expenseId) {
        FirestoreManager.getInstance().categoriesReference(uid)
                .whereEqualTo("name", category)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    System.out.println("[handleCategoryUpdate] Found " + query.size() + " docs.");
                    if (!query.isEmpty()) {
                        query.getDocuments().get(0).getReference()
                                .update("expenses", FieldValue.arrayUnion(expenseId));
                        System.out.println("Added expense to existing category: " + category);
                    } else {
                        createNewCategory(uid, category, expenseId);
                    }
                })
                .addOnFailureListener(e ->
                        System.err.println("[handleCategoryUpdate] Failed: " + e.getMessage()));
    }

    private void createNewCategory(String uid, String category, String expenseId) {
        Map<String, Object> newCategory = new HashMap<>();
        newCategory.put("name", category);
        newCategory.put("budgets", Collections.emptyList());
        newCategory.put("expenses", Collections.singletonList(expenseId));

        FirestoreManager.getInstance().categoriesReference(uid)
                .add(newCategory)
                .addOnSuccessListener(ref ->
                        System.out.println("Created new category: " + category))
                .addOnFailureListener(e ->
                        System.err.println("Failed to create category: " + e.getMessage()));
    }

    private void handleBudgetUpdate(String uid, String category, Runnable onBudgetUpdated) {
        FirestoreManager.getInstance().budgetsReference(uid)
                .whereEqualTo("category", category)
                .orderBy("startDateTimestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    System.out.println("[handleBudgetUpdate] Found " + query.size()
                            + " budget docs.");
                    if (query.isEmpty()) {
                        return;
                    }

                    DocumentSnapshot budgetDoc = query.getDocuments().get(0);
                    Budget budget = budgetDoc.toObject(Budget.class);
                    if (budget == null) {
                        return;
                    }

                    long budgetStart = budget.getStartDateTimestamp();
                    long budgetEnd = calcBudgetEnd(budgetStart, budget.getFrequency());

                    FirestoreManager.getInstance().expensesReference(uid)
                            .whereEqualTo("category", category)
                            .get()
                            .addOnSuccessListener(expenseQuery -> {
                                double spent = calculateSpentToDate(
                                        expenseQuery, budgetStart, budgetEnd);
                                updateBudgetDoc(uid, budgetDoc, budget, spent, onBudgetUpdated);
                            })
                            .addOnFailureListener(e ->
                                    System.err.println("[handleBudgetUpdate] Expense fetch failed: "
                                            + e.getMessage()));
                })
                .addOnFailureListener(e ->
                        System.err.println("[handleBudgetUpdate] Budget fetch failed: "
                                + e.getMessage()));
    }

    private long calcBudgetEnd(long start, String freq) {
        if ("Weekly".equalsIgnoreCase(freq)) {
            return start + 7L * 24 * 60 * 60 * 1000;
        }
        if ("Monthly".equalsIgnoreCase(freq)) {
            return start + 30L * 24 * 60 * 60 * 1000;
        }
        return start;
    }

    private double calculateSpentToDate(QuerySnapshot expenseQuery, long start, long end) {
        double spent = 0;
        long now = System.currentTimeMillis();
        long effectiveEnd = Math.min(end, now);

        for (DocumentSnapshot doc : expenseQuery.getDocuments()) {
            Expense e = doc.toObject(Expense.class);
            if (e == null) {
                continue;
            }
            long t = e.getTimestamp();
            if (t >= start - 24L * 60 * 60 * 1000 && t <= effectiveEnd) {
                spent += e.getAmount();
                System.out.println("Counted: " + e.getName());
            } else {
                System.out.println("Skipped (out of range): " + e.getName());
            }
        }
        System.out.println("[calculateSpentToDate] Total spent: " + spent);
        return spent;
    }

    private void updateBudgetDoc(String uid, DocumentSnapshot doc, Budget budget,
                                 double spent, Runnable onBudgetUpdated) {
        double remaining = budget.getAmount() - spent;
        boolean overBudget = remaining < 0;

        FirestoreManager.getInstance()
                .budgetsReference(uid)
                .document(doc.getId())
                .update(
                        "spentToDate", spent,
                        "moneyRemaining", remaining,
                        "overBudget", overBudget
                )
                .addOnSuccessListener(a -> {
                    System.out.println("[updateBudgetDoc] Budget '" + budget.getName()
                            + "' updated: spent=" + spent + ", remaining=" + remaining);
                    if (onBudgetUpdated != null) {
                        onBudgetUpdated.run();
                    }
                })
                .addOnFailureListener(e ->
                        System.err.println("[updateBudgetDoc] Failed: " + e.getMessage()));
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
