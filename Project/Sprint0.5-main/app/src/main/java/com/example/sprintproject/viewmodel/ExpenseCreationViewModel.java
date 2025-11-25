// The ViewModel manages the creation of expenses and updating the related
// budgets and categories.
// Also loads the categories and circles and creates
// sample expense data for testing.

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

    private static final String TAG = "ExpenseCreationVM";

    // Sample constants
    private static final String EATING = "eating";
    private static final String TRAVEL = "travel";
    private static final String GAMING = "gaming";
    private static final String DATE20 = "Oct 20, 2025";
    private static final String DATE21 = "Oct 21, 2025";

    // Time constants (avoid magic numbers)
    private static final long MILLIS_PER_DAY = 24L * 60 * 60 * 1000;
    private static final int WEEK_DAYS = 7;
    private static final int MONTH_DAYS = 30;

    private final MutableLiveData<String> text = new MutableLiveData<>();
    private final MutableLiveData<List<String>> categoriesLiveData =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<String>> circleNamesLive =
            new MutableLiveData<>(new ArrayList<>());
    private final Map<String, String> circleNameToId = new HashMap<>();

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
        String uid = getUidOrFinish(() ->
                categoriesLiveData.setValue(new ArrayList<>())
        );
        if (uid == null) {
            return;
        }

        FirestoreManager.getInstance()
                .categoriesReference(uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> categoryNames = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        String n = doc.getString("name");
                        if (n != null && !n.trim().isEmpty()) {
                            categoryNames.add(n);
                        }
                    }
                    categoriesLiveData.setValue(categoryNames);
                })
                .addOnFailureListener(e -> categoriesLiveData.setValue(new ArrayList<>()));
    }

    public void loadUserCircles() {
        String uid = getUidOrClearCircles();
        if (uid == null) {
            return;
        }

        FirestoreManager.getInstance()
                .userSavingsCirclePointers(uid)
                .get()
                .addOnSuccessListener(qs -> handlePointerSnapshot(qs))
                .addOnFailureListener(e -> clearCirclesAndPublish());
    }

    private String getUidOrClearCircles() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            clearCirclesAndPublish();
            return null;
        }
        return auth.getCurrentUser().getUid();
    }

    private void handlePointerSnapshot(QuerySnapshot qs) {
        List<String> names = new ArrayList<>();
        circleNameToId.clear();

        if (qs == null || qs.isEmpty()) {
            circleNamesLive.setValue(names);
            return;
        }

        PendingCounter pending = new PendingCounter(() ->
                circleNamesLive.setValue(new ArrayList<>(names))
        );

        for (DocumentSnapshot doc : qs.getDocuments()) {
            processPointerDoc(doc, names, pending);
        }

        pending.maybePublishNow();
    }

    private void processPointerDoc(
            DocumentSnapshot doc,
            List<String> names,
            PendingCounter pending
    ) {
        String circleId = safeTrim(doc.getString("circleId"));
        if (circleId.isEmpty()) {
            return;
        }

        String pointerName = safeTrim(doc.getString("name"));
        if (!pointerName.isEmpty()) {
            addCircleName(pointerName, circleId, names);
            return;
        }

        fetchAndAddCircleName(circleId, names, pending);
    }

    private void fetchAndAddCircleName(
            String circleId,
            List<String> names,
            PendingCounter pending
    ) {
        pending.increment();
        FirestoreManager.getInstance()
                .savingsCircleDoc(circleId)
                .get()
                .addOnSuccessListener(globalDoc -> {
                    String fetched = safeTrim(globalDoc.getString("name"));
                    if (!fetched.isEmpty()) {
                        addCircleName(fetched, circleId, names);
                    }
                })
                .addOnCompleteListener(task -> pending.decrement());
    }

    private void addCircleName(String name, String circleId, List<String> names) {
        circleNameToId.put(name, circleId);
        names.add(name);
    }

    private void clearCirclesAndPublish() {
        circleNameToId.clear();
        circleNamesLive.setValue(new ArrayList<>());
    }

    public void createExpense(
            String name, String date, String amountString,
            String category, String notes, Runnable onBudgetUpdated
    ) {
        ExpenseData data = new ExpenseData(
                name, date, amountString, category, notes, false, null
        );
        createExpense(data, onBudgetUpdated);
    }

    public void createExpense(ExpenseData data, Runnable onBudgetUpdated) {
        String uid = getUidOrFinish(null);
        if (uid == null) {
            text.setValue("User not logged in");
            return;
        }

        Log.d(TAG, "Writing expense for uid=" + uid);

        String normalizedCategory = normalizeCategory(data.getCategory());
        Double amount = parseAmount(data.getAmountString());
        if (amount == null) {
            return;
        }

        long timestamp = parseDateToMillis(data.getDate());

        Expense expense = new Expense(
                data.getName(), amount, normalizedCategory,
                data.getDate(), data.getNotes()
        );
        expense.setTimestamp(timestamp);
        expense.setContributesToGroupSavings(data.getContributesToGroupSavings());

        FirestoreManager.getInstance()
            .expensesReference(uid)
            .add(expense)
            .addOnSuccessListener(docRef -> {
                FirestoreManager.getInstance().incrementField(uid, "totalExpenses");
                handleCategoryUpdate(uid, normalizedCategory, docRef.getId());
                handleBudgetUpdate(uid, normalizedCategory, onBudgetUpdated);

                maybeUpdateGroupSavings(data, uid, amount);
            })
            .addOnFailureListener(e ->
                text.setValue("Failed to create expense")
            );
    }

    private void maybeUpdateGroupSavings(ExpenseData data, String uid, double amount) {
        if (data.getContributesToGroupSavings()
                && data.getCircleId() != null
                && !data.getCircleId().trim().isEmpty()) {
            updateGroupSavingsByCircleId(data.getCircleId(), uid, amount);
        }
    }

    private void updateGroupSavingsByCircleId(String circleId, String uid, double amount) {
        FirestoreManager.getInstance()
                .savingsCircleDoc(circleId)
                .update("spent", FieldValue.increment(amount));

        FirestoreManager.getInstance()
                .savingsCircleDoc(circleId)
                .update("contributions." + uid, FieldValue.increment(amount));
    }


    private void handleCategoryUpdate(String uid, String category, String expenseId) {
        FirestoreManager.getInstance()
            .categoriesReference(uid)
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
            })
            .addOnFailureListener(e ->
                Log.w(TAG, "Failed to update category", e)
            );
    }

    private void createNewCategory(String uid, String category, String expenseId) {
        Map<String, Object> newCategory = new HashMap<>();
        newCategory.put("name", category);
        newCategory.put("budgets", Collections.emptyList());
        newCategory.put("expenses", Collections.singletonList(expenseId));

        FirestoreManager.getInstance()
            .categoriesReference(uid)
            .add(newCategory)
            .addOnFailureListener(e ->
                Log.w(TAG, "Failed to create new category", e)
            );
    }

    private void handleBudgetUpdate(String uid, String category, Runnable onBudgetUpdated) {
        FirestoreManager.getInstance()
            .budgetsReference(uid)
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

                FirestoreManager.getInstance()
                    .expensesReference(uid)
                    .whereEqualTo("category", category)
                    .get()
                    .addOnSuccessListener(expenseQuery -> {
                        double spent = calculateSpentToDate(
                            expenseQuery, budgetStart, budgetEnd
                        );
                        updateBudgetDoc(uid, budgetDoc, budget, spent, onBudgetUpdated);
                    })
                    .addOnFailureListener(e ->
                        Log.w(TAG, "Failed to recalc budget spend", e)
                    );
            })
            .addOnFailureListener(e ->
                Log.w(TAG, "Failed to fetch budget", e)
            );
    }

    private long calcBudgetEnd(long start, String freq) {
        if ("Weekly".equalsIgnoreCase(freq)) {
            return start + WEEK_DAYS * MILLIS_PER_DAY;
        }
        if ("Monthly".equalsIgnoreCase(freq)) {
            return start + MONTH_DAYS * MILLIS_PER_DAY;
        }
        return start;
    }

    private double calculateSpentToDate(QuerySnapshot expenseQuery, long start, long end) {
        double spent = 0;
        long now = System.currentTimeMillis();
        long effectiveEnd = Math.min(end, now);
        long startWindow = start - MILLIS_PER_DAY; // inclusive buffer

        for (DocumentSnapshot doc : expenseQuery.getDocuments()) {
            Expense e = doc.toObject(Expense.class);
            if (e == null) {
                continue;
            }

            long t = e.getTimestamp();
            if (t >= startWindow && t <= effectiveEnd) {
                spent += e.getAmount();
            }
        }
        return spent;
    }

    private void updateBudgetDoc(
            String uid,
            DocumentSnapshot doc,
            Budget budget,
            double spent,
            Runnable onBudgetUpdated
    ) {
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
            .addOnSuccessListener(a -> runOnComplete(onBudgetUpdated))
            .addOnFailureListener(e ->
                Log.w(TAG, "Failed to update budget doc", e)
            );
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

    private String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private String getUidOrFinish(Runnable onComplete) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            runOnComplete(onComplete);
            return null;
        }
        return auth.getCurrentUser().getUid();
    }

    private void runOnComplete(Runnable onComplete) {
        if (onComplete != null) {
            onComplete.run();
        }
    }

    private long parseDateToMillis(String dateString) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        try {
            Date date = sdf.parse(dateString);
            if (date != null) {
                return date.getTime();
            }
        } catch (Exception e) {
            Log.w(TAG, "Bad date: " + dateString, e);
        }
        return System.currentTimeMillis();
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

        // NOTE: ViewModels should normally be obtained via ViewModelProvider.
        // This is only used to seed sample data in a non-production path.
        BudgetCreationViewModel budgetCreationViewModel = new BudgetCreationViewModel();
        budgetCreationViewModel.createSampleBudgets(() -> runOnComplete(onComplete));
    }

    private static class PendingCounter {
        private int count = 0;
        private final Runnable onZero;

        PendingCounter(Runnable onZero) {
            this.onZero = onZero;
        }

        void increment() {
            count++;
        }

        void decrement() {
            count--;
            if (count == 0) {
                onZero.run();
            }
        }

        void maybePublishNow() {
            if (count == 0) {
                onZero.run();
            }
        }
    }
}


