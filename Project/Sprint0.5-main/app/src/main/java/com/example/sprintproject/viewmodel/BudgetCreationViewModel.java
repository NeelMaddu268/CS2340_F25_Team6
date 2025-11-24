package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.model.Budget;
import com.example.sprintproject.model.BudgetData;
import com.example.sprintproject.model.Expense;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class BudgetCreationViewModel extends ViewModel {

    private final MutableLiveData<String> text = new MutableLiveData<>();
    private static final String BUDGET_STRING = "budgets";

    public LiveData<String> getText() {
        return text;
    }

    public void createBudget(
            String name,
            String date,
            String amountString,
            String category,
            String frequency,
            long timestamp,
            Runnable onComplete
    ) {
        String uid = getUidOrFinish(onComplete);
        if (uid == null) return;

        String normalizedCategory = normalizeCategory(category);

        Double amount = parseAmountOrFinish(amountString, onComplete);
        if (amount == null) return;

        FirestoreManager.getInstance()
                .categoriesReference(uid)
                .whereEqualTo("name", normalizedCategory)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    BudgetData budgetData = buildBudgetData(
                            name,
                            amount,
                            normalizedCategory,
                            frequency,
                            date,
                            timestamp,
                            querySnapshot
                    );
                    addBudgetToFirestore(uid, budgetData, onComplete);
                })
                .addOnFailureListener(e -> runOnComplete(onComplete));
    }

    /** ---------------- Refactored to reduce cognitive complexity ---------------- */

    private void addBudgetToFirestore(String uid, BudgetData budgetData, Runnable onComplete) {
        String category = normalizeCategory(budgetData.getCategory());

        FirestoreManager.getInstance()
                .expensesReference(uid)
                .whereEqualTo("category", category)
                .get()
                .addOnSuccessListener(expenseQuery -> {
                    double spentToDate = calculateSpentToDate(
                            expenseQuery,
                            budgetData.getStartDateTimestamp()
                    );

                    Budget budget = buildBudgetFromData(budgetData, category, spentToDate);

                    saveBudget(uid, budget, budgetData.getCategoryId(), category, onComplete);
                })
                .addOnFailureListener(e -> runOnComplete(onComplete));
    }

    /** ---------------- Helpers (keep each tiny & low-branching) ---------------- */

    private String getUidOrFinish(Runnable onComplete) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            runOnComplete(onComplete);
            return null;
        }
        return auth.getCurrentUser().getUid();
    }

    private String normalizeCategory(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.US);
    }

    private Double parseAmountOrFinish(String amountString, Runnable onComplete) {
        try {
            return Double.parseDouble(amountString);
        } catch (NumberFormatException e) {
            text.setValue("Invalid amount");
            runOnComplete(onComplete);
            return null;
        }
    }

    private BudgetData buildBudgetData(
            String name,
            double amount,
            String normalizedCategory,
            String frequency,
            String date,
            long timestamp,
            QuerySnapshot categoryQuery
    ) {
        String categoryId = null;
        if (categoryQuery != null && !categoryQuery.isEmpty()) {
            categoryId = categoryQuery.getDocuments().get(0).getId();
        }
        return new BudgetData(
                name,
                amount,
                normalizedCategory,
                frequency,
                date,
                categoryId,
                timestamp
        );
    }

    private long normalizeToStartOfDay(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private double calculateSpentToDate(QuerySnapshot expenseQuery, long startTimestamp) {
        double spent = 0.0;
        long startOfDay = normalizeToStartOfDay(startTimestamp);

        for (DocumentSnapshot doc : expenseQuery.getDocuments()) {
            Expense e = doc.toObject(Expense.class);
            if (e != null && e.getTimestamp() >= startOfDay) {
                spent += e.getAmount();
            }
        }
        return spent;
    }

    private Budget buildBudgetFromData(BudgetData data, String category, double spentToDate) {
        long startOfDay = normalizeToStartOfDay(data.getStartDateTimestamp());

        Budget budget = new Budget(
                data.getName(),
                data.getAmount(),
                category,
                data.getFrequency(),
                data.getStartDate()
        );

        budget.setStartDateTimestamp(startOfDay);
        budget.setSpentToDate(spentToDate);
        budget.setMoneyRemaining(data.getAmount() - spentToDate);
        budget.setCompleted(false);
        budget.setHasPreviousCycle(false);
        budget.setPreviousCycleOverBudget(false);
        budget.setPreviousCycleEndTimestamp(0L);

        return budget;
    }

    private void saveBudget(
            String uid,
            Budget budget,
            String categoryId,
            String categoryName,
            Runnable onComplete
    ) {
        FirestoreManager.getInstance()
                .budgetsReference(uid)
                .add(budget)
                .addOnSuccessListener(budgetRef -> {
                    String budgetId = budgetRef.getId();
                    FirestoreManager.getInstance().incrementField(uid, "totalBudgets");
                    linkBudgetToCategory(uid, budgetId, categoryId, categoryName, onComplete);
                })
                .addOnFailureListener(e -> runOnComplete(onComplete));
    }

    private void linkBudgetToCategory(
            String uid,
            String budgetId,
            String categoryId,
            String categoryName,
            Runnable onComplete
    ) {
        if (categoryId != null) {
            addBudgetIdToCategoryDoc(uid, categoryId, budgetId, onComplete);
        } else {
            findOrCreateCategoryByName(uid, categoryName, budgetId, onComplete);
        }
    }

    private void addBudgetIdToCategoryDoc(
            String uid,
            String categoryId,
            String budgetId,
            Runnable onComplete
    ) {
        FirestoreManager.getInstance()
                .categoriesReference(uid)
                .document(categoryId)
                .update(BUDGET_STRING, FieldValue.arrayUnion(budgetId))
                .addOnCompleteListener(task -> runOnComplete(onComplete));
    }

    private void findOrCreateCategoryByName(
            String uid,
            String categoryName,
            String budgetId,
            Runnable onComplete
    ) {
        FirestoreManager.getInstance()
                .categoriesReference(uid)
                .whereEqualTo("name", categoryName)
                .limit(1)
                .get()
                .addOnSuccessListener(catSnap -> {
                    if (!catSnap.isEmpty()) {
                        DocumentSnapshot existing = catSnap.getDocuments().get(0);
                        existing.getReference().update(
                                BUDGET_STRING,
                                FieldValue.arrayUnion(budgetId)
                        );
                    } else {
                        createCategory(uid, categoryName, budgetId);
                    }
                    runOnComplete(onComplete);
                })
                .addOnFailureListener(e -> runOnComplete(onComplete));
    }

    private void createCategory(String uid, String categoryName, String budgetId) {
        Map<String, Object> cat = new HashMap<>();
        cat.put("name", categoryName);
        cat.put(BUDGET_STRING, Collections.singletonList(budgetId));
        cat.put("expenses", Collections.emptyList());

        FirestoreManager.getInstance()
                .categoriesReference(uid)
                .add(cat);
    }

    private void runOnComplete(Runnable onComplete) {
        if (onComplete != null) onComplete.run();
    }

    /** ---------------- Sample budgets ---------------- */

    public void createSampleBudgets(Runnable onComplete) {
        String[][] sampleBudgets = {
                {"Eating Budget", "Oct 17, 2025", "100.00", "eating", "Weekly"},
                {"Travel Budget", "Oct 19, 2025", "1000.00", "travel", "Monthly"},
                {"Gaming Budget", "Oct 21, 2025", "1500.00", "gaming", "Weekly"}
        };

        final int[] completedCount = {0};
        final int totalBudgets = sampleBudgets.length;

        for (String[] budgetData : sampleBudgets) {
            long timestamp = parseDateToMillis(budgetData[1]);
            createBudget(
                    budgetData[0],
                    budgetData[1],
                    budgetData[2],
                    budgetData[3],
                    budgetData[4],
                    timestamp,
                    () -> {
                        completedCount[0]++;
                        if (completedCount[0] == totalBudgets) {
                            runOnComplete(onComplete);
                        }
                    }
            );
        }
    }

    private long parseDateToMillis(String dateString) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        sdf.setTimeZone(TimeZone.getDefault());

        try {
            Date date = sdf.parse(dateString);
            if (date != null) return date.getTime();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return System.currentTimeMillis();
    }
}
