package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.FirestoreManager;
import com.example.sprintproject.model.Budget;
import com.example.sprintproject.model.BudgetData;
import com.example.sprintproject.model.Expense;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BudgetCreationViewModel extends ViewModel {

    private final MutableLiveData<String> text = new MutableLiveData<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<String> getText() {
        return text;
    }

    public void createBudget(
            String name, String date, String amountString, String category,
            String frequency, long timestamp, Runnable onComplete) {
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

        FirestoreManager.getInstance().categoriesReference(uid)
                .whereEqualTo("name", category)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    BudgetData budgetData;
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
                        budgetData = new BudgetData(name, finalAmount,
                                category, frequency, date, categoryDocument.getId(), timestamp);
                    } else {
                        //category doesn't exist yet, make it
                        budgetData = new BudgetData(name, finalAmount,
                                category, frequency, date, null, timestamp);
                    }
                    addBudgetToFirestore(uid, budgetData, onComplete);
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


        FirestoreManager.getInstance().expensesReference(uid)
                .whereEqualTo("category", budgetData.getCategory())
                .get()
                .addOnSuccessListener(expenseQuery -> {
                    long budgetStartTimestamp = budgetData.getStartDateTimestamp();
                    double spentToDate = 0.0;
                    for (DocumentSnapshot doc : expenseQuery.getDocuments()) {
                        Expense expense = doc.toObject(Expense.class);
                        if (expense != null) {
                            long expenseTimestamp = expense.getTimestamp();

                            long budgetEndTimestamp = budgetStartTimestamp;
                            if (budgetData.getFrequency().equalsIgnoreCase("Weekly")) {
                                budgetEndTimestamp += 7 * 24 * 60 * 60 * 1000L;
                            } else if (budgetData.getFrequency().equalsIgnoreCase("Monthly")) {
                                Calendar c = Calendar.getInstance();
                                c.setTimeInMillis(budgetStartTimestamp);
                                c.add(Calendar.MONTH, 1);
                                budgetEndTimestamp = c.getTimeInMillis();
                            }

                            if (expenseTimestamp >= budgetStartTimestamp
                                    && expenseTimestamp < budgetEndTimestamp) {
                                spentToDate += expense.getAmount();
                            }

                        }
                    }

                    Budget budget = new Budget(
                            budgetData.getName(),
                            budgetData.getAmount(),
                            budgetData.getCategory(),
                            budgetData.getFrequency(),
                            budgetData.getStartDate());

                    budget.setStartDateTimestamp(budgetData.getStartDateTimestamp());

                    budget.setSpentToDate(spentToDate);
                    budget.setMoneyRemaining(budgetData.getAmount() - spentToDate);


                    FirestoreManager.getInstance().budgetsReference(uid).add(budget)
                            .addOnSuccessListener(documentReference -> {
                                String budgetId = documentReference.getId();

                                if (budgetData.getCategoryId() != null) {
                                    //Category already existed, edge case
                                    FirestoreManager.getInstance().categoriesReference(uid)
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
                                    FirestoreManager.getInstance().categoriesReference(uid)
                                            .add(newCategory)
                                            .addOnCompleteListener(task -> {
                                                if (onComplete != null) {
                                                    onComplete.run();
                                                }
                                            });
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    System.err.println("Budget failed to add");
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
    }


    public void createSampleBudgets(Runnable onComplete) {
        Calendar calendar = Calendar.getInstance();
        String[][] sampleBudgets = {
                {"Eating Budget", "Oct 20, 2025", "100.00", "Eating", "Weekly"},
                {"Travel Budget", "Oct 21, 2025", "1000.00", "Travel", "Monthly"},
                {"Gaming Budget", "Oct 22, 2025", "1500.00", "Gaming", "Weekly"}
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