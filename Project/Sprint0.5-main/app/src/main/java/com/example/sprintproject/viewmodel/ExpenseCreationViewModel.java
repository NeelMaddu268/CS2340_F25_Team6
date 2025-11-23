package com.example.sprintproject.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.model.Budget;
import com.example.sprintproject.model.Expense;
import com.example.sprintproject.model.ExpenseData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
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
    private static final String EATING = "eating";
    private static final String DATE20 = "Oct 20, 2025";
    private static final String TRAVEL = "travel";
    private static final String GAMING = "gaming";
    private static final String DATE21 = "Oct 21, 2025";
    private final MutableLiveData<String> text = new MutableLiveData<>();
    private final MutableLiveData<List<String>> categoriesLiveData =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<String>> circleNamesLive =
            new MutableLiveData<>(new ArrayList<>());
    private final Map<String, String> circleNameToId = new HashMap<>();

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

    public LiveData<List<String>> getCircleNames() {
        return circleNamesLive;
    }

    public String getCircleIdForName(String name) {
        return circleNameToId.get(name);
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
                .addOnFailureListener(e ->
                    categoriesLiveData.setValue(new ArrayList<>())
        );
    }

    public void loadUserCircles() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            circleNamesLive.setValue(new ArrayList<>());
            circleNameToId.clear();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        FirestoreManager.getInstance()
                .userSavingsCirclePointers(uid) // read pointers under the user
                .get()
                .addOnSuccessListener(qs -> {
                    List<String> names = new ArrayList<>();
                    circleNameToId.clear();

                    if (qs == null || qs.isEmpty()) {
                        circleNamesLive.setValue(names);
                        return;
                    }

                    final int[] pending = {0};

                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        String circleId = doc.getString("circleId");
                        // Most pointer docs store "name" (not "circleName")
                        String pointerName = doc.getString("name");

                        if (circleId == null || circleId.trim().isEmpty()) {
                            continue;
                        }

                        if (pointerName != null && !pointerName.trim().isEmpty()) {
                            // Fast path: pointer already has a name
                            circleNameToId.put(pointerName, circleId);
                            names.add(pointerName);
                        } else {
                            // Fallback: fetch the name from the global circle doc
                            pending[0]++;
                            FirestoreManager.getInstance()
                                    .savingsCircleDoc(circleId)
                                    .get()
                                    .addOnSuccessListener(globalDoc -> {
                                        String fetched = globalDoc.getString("name");
                                        if (fetched != null && !fetched.trim().isEmpty()) {
                                            circleNameToId.put(fetched, circleId);
                                            names.add(fetched);
                                        }
                                    })
                                    .addOnCompleteListener(task -> {
                                        // decrement regardless of success/failure
                                        pending[0]--;
                                        if (pending[0] == 0) {
                                            circleNamesLive.setValue(new ArrayList<>(names));
                                        }
                                    });
                        }
                    }

                    // If there were no fallbacks, publish immediately
                    if (pending[0] == 0) {
                        circleNamesLive.setValue(names);
                    }
                })
                .addOnFailureListener(e -> {
                    circleNameToId.clear();
                    circleNamesLive.setValue(new ArrayList<>());
                });
    }

    public void createExpense(
            String name, String date, String amountString,
            String category, String notes, Runnable onBudgetUpdated) {

        // default: not contributing, no circle
        ExpenseData data = new ExpenseData(name, date, amountString, category, notes, false, null);
        createExpense(data, onBudgetUpdated);
    }


    public void createExpense(
            ExpenseData data,
            Runnable onBudgetUpdated) {

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            text.setValue("User not logged in");
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        Log.d("EXP", "Writing expense for uid=" + uid);
        String normalizedCategory = normalizeCategory(data.getCategory());
        Double amount = parseAmount(data.getAmountString());
        if (amount == null) {
            return;
        }

        long timestamp = parseDateToMillis(data.getDate());
        Expense expense = new Expense(data.getName(), amount, normalizedCategory,
                data.getDate(), data.getNotes());
        expense.setTimestamp(timestamp);
        expense.setContributesToGroupSavings(data.getContributesToGroupSavings());

        FirestoreManager.getInstance().expensesReference(uid)
                .add(expense)
                .addOnSuccessListener(docRef -> {
                    FirestoreManager.getInstance().incrementField(uid, "totalExpenses");
                    handleCategoryUpdate(uid, normalizedCategory, docRef.getId());
                    handleBudgetUpdate(uid, normalizedCategory, onBudgetUpdated);

                    if (data.getContributesToGroupSavings() && data.getCircleId() != null
                            && !data.getCircleId().isEmpty()) {
                        updateGroupSavingsByCircleId(data.getCircleId(), uid, amount);
                    }
                });
    }

    private void updateGroupSavingsByCircleId(String circleId, String uid, double amount) {
        FirestoreManager.getInstance()
                .savingsCircleDoc(circleId)
                .update("spent", FieldValue.increment(amount));
        FirestoreManager.getInstance()
                .savingsCircleDoc(circleId)
                .update("contributions." + uid, FieldValue.increment(amount));
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
                    if (!query.isEmpty()) {
                        query.getDocuments().get(0).getReference()
                                .update("expenses", FieldValue.arrayUnion(expenseId));
                    } else {
                        createNewCategory(uid, category, expenseId);
                    }
                });
    }

    private void createNewCategory(String uid, String category, String expenseId) {
        Map<String, Object> newCategory = new HashMap<>();
        newCategory.put("name", category);
        newCategory.put("budgets", Collections.emptyList());
        newCategory.put("expenses", Collections.singletonList(expenseId));

        FirestoreManager.getInstance().categoriesReference(uid)
                .add(newCategory);
    }

    private void handleBudgetUpdate(String uid, String category, Runnable onBudgetUpdated) {
        FirestoreManager.getInstance().budgetsReference(uid)
                .whereEqualTo("category", category)
                .orderBy("startDateTimestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
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
                            });
                });
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
            }
        }
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
                    if (onBudgetUpdated != null) {
                        onBudgetUpdated.run();
                    }
                });
    }


    public void createSampleExpenses() {
        createExpense("Tin Drum", "Oct 15, 2025", "20.00", EATING, null, null);
        createExpense("Panda Express", DATE20, "30.00", EATING, "Was Hungry", null);

        createExpense("Hawaii", "Oct 18, 2025", "100.00", TRAVEL, null, null);
        createExpense("Spain", "Oct 19, 2025", "500.00", TRAVEL, "Spring Break", null);

        createExpense("Xbox", DATE21, "200.00", GAMING, null, null);
        createExpense("PS5", "Oct 22, 2025", "800.00", GAMING, "Xbox Broke", null);

        createExpense("Loan", "Oct 07, 2025", "1000.00", "other", null, null);

    }

    public void createSampleDate(Runnable onComplete) {
        createExpense("Tin Drum", "Oct 19, 2025", "20.00", EATING, null, null);
        createExpense("Panda Express", DATE20, "30.00", EATING, "Was Hungry", null);

        createExpense("Hawaii", DATE20, "100.00", TRAVEL, null, null);
        createExpense("Spain", DATE21, "500.00", TRAVEL, "Spring Break", null);

        createExpense("Xbox", DATE21, "200.00", GAMING, null, null);
        createExpense("PS5", "Oct 22, 2025", "800.00", GAMING, "Xbox Broke", null);

        createExpense("Loan", "Oct 07, 2025", "1000.00", "other", null, null);

        BudgetCreationViewModel budgetCreationViewModel = new BudgetCreationViewModel();

        budgetCreationViewModel.createSampleBudgets(() -> {
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
