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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BudgetCreationViewModel extends ViewModel {

    private final MutableLiveData<String> text = new MutableLiveData<>();

    private static final String BUDGET_STRING = "budgets";


    public LiveData<String> getText() {
        return text;
    }

    public void createBudget(
            String name, String date, String amountString, String category,
            String frequency, long timestamp, Runnable onComplete) {

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        final String normalizedCategory = category.trim().toLowerCase(Locale.US);

        double amount;
        try {
            amount = Double.parseDouble(amountString);
        } catch (NumberFormatException e) {
            text.setValue("Invalid amount");
            amount = 0.0;
        }

        double finalAmount = amount;

        FirestoreManager.getInstance().categoriesReference(uid)
                .whereEqualTo("name", normalizedCategory)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    BudgetData budgetData;
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot categoryDoc = querySnapshot.getDocuments().get(0);
                        budgetData = new BudgetData(
                                name, finalAmount, normalizedCategory,
                                frequency, date, categoryDoc.getId(), timestamp
                        );
                    } else {
                        budgetData = new BudgetData(
                                name, finalAmount, normalizedCategory,
                                frequency, date, null, timestamp
                        );
                    }

                    addBudgetToFirestore(uid, budgetData, onComplete);
                })
                .addOnFailureListener(e -> {
                    if (onComplete != null) {
                        onComplete.run();
                    }
                    new BudgetsFragmentViewModel().loadBudgets();
                });
    }


    private void addBudgetToFirestore(String uid, BudgetData budgetData, Runnable onComplete) {
        String category = budgetData.getCategory().trim().toLowerCase(Locale.US);

        FirestoreManager.getInstance().expensesReference(uid)
                .whereEqualTo("category", category)
                .get()
                .addOnSuccessListener(expenseQuery -> {
                    double spentToDate = 0.0;
                    long start = budgetData.getStartDateTimestamp();

                    Calendar normalized = Calendar.getInstance();
                    normalized.setTimeInMillis(start);
                    normalized.set(Calendar.HOUR_OF_DAY, 0);
                    normalized.set(Calendar.MINUTE, 0);
                    normalized.set(Calendar.SECOND, 0);
                    normalized.set(Calendar.MILLISECOND, 0);
                    start = normalized.getTimeInMillis();

                    for (DocumentSnapshot doc : expenseQuery.getDocuments()) {
                        Expense e = doc.toObject(Expense.class);
                        if (e != null && e.getTimestamp() >= start) {
                            spentToDate += e.getAmount();
                        }
                    }

                    Budget budget = new Budget(
                            budgetData.getName(),
                            budgetData.getAmount(),
                            category,
                            budgetData.getFrequency(),
                            budgetData.getStartDate()
                    );
                    budget.setStartDateTimestamp(start);
                    budget.setSpentToDate(spentToDate);
                    budget.setMoneyRemaining(budgetData.getAmount() - spentToDate);
                    budget.setCompleted(false);
                    budget.setHasPreviousCycle(false);
                    budget.setPreviousCycleOverBudget(false);
                    budget.setPreviousCycleEndTimestamp(0L);

                    FirestoreManager.getInstance().budgetsReference(uid)
                            .add(budget)
                            .addOnSuccessListener(budgetRef -> {
                                String budgetId = budgetRef.getId();

                                if (budgetData.getCategoryId() != null) {
                                    FirestoreManager.getInstance()
                                            .categoriesReference(uid)
                                            .document(budgetData.getCategoryId())
                                            .update(BUDGET_STRING, FieldValue.arrayUnion(budgetId))
                                            .addOnCompleteListener(task -> {
                                                if (onComplete != null) {
                                                    onComplete.run();
                                                }
                                            });
                                } else {
                                    FirestoreManager.getInstance()
                                            .categoriesReference(uid)
                                            .whereEqualTo("name", category)
                                            .limit(1)
                                            .get()
                                            .addOnSuccessListener(catSnap -> {
                                                if (!catSnap.isEmpty()) {
                                                    // Found existing, add budget
                                                    DocumentSnapshot existing =
                                                            catSnap.getDocuments().get(0);
                                                    existing.getReference().update(
                                                            BUDGET_STRING,
                                                            FieldValue.arrayUnion(budgetId)
                                                    );
                                                } else {
                                                    // Create if not found
                                                    Map<String, Object> cat = new HashMap<>();
                                                    cat.put("name",
                                                            category.trim()
                                                                    .toLowerCase(
                                                                            Locale.US));
                                                    cat.put(BUDGET_STRING,
                                                            Collections.singletonList(budgetId));
                                                    cat.put("expenses", Collections.emptyList());
                                                    FirestoreManager.getInstance()
                                                            .categoriesReference(uid)
                                                            .add(cat);

                                                }
                                                if (onComplete != null) {
                                                    onComplete.run();
                                                }
                                            });

                                }
                            })
                            .addOnFailureListener(e -> {
                                if (onComplete != null) {
                                    onComplete.run();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
    }


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
                    budgetData[0], // name
                    budgetData[1], // date
                    budgetData[2], // amount
                    budgetData[3], // category
                    budgetData[4], // frequency
                    timestamp,
                    () -> {
                        completedCount[0]++;
                        if (completedCount[0] == totalBudgets && onComplete != null) {
                            onComplete.run();
                        }
                    }
            );
        }
    }

    private long parseDateToMillis(String dateString) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        sdf.setTimeZone(java.util.TimeZone.getDefault());

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