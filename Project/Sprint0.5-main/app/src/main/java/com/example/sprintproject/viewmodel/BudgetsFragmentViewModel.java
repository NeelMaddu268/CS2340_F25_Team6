package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.FirestoreManager;
import com.example.sprintproject.model.Budget;
import com.example.sprintproject.model.AppDate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BudgetsFragmentViewModel extends ViewModel {

    private final MutableLiveData<List<Budget>> budgetsLiveData =
            new MutableLiveData<>(new ArrayList<>());
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<List<Budget>> getBudgets() {
        return budgetsLiveData;
    }

    public void loadBudgets(AppDate appDate) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirestoreManager.getInstance().budgetsReference(uid)
                .orderBy("startDateTimestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null || querySnapshot == null) {
                        budgetsLiveData.setValue(new ArrayList<>());
                        return;
                    }

                    List<Budget> budgets = new ArrayList<>();

                    Calendar calendar = Calendar.getInstance();
                    calendar.set(appDate.getYear(), appDate.getMonth() - 1,
                            appDate.getDay(), 0, 0, 0);

                    Date currentDate = calendar.getTime();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Budget budget = doc.toObject(Budget.class);
                        if (budget != null) {
                            budget.setId(doc.getId()); // set the documented id here

                            boolean expired = isBudgetExpired(budget, currentDate);
                            if (expired) {

                                boolean over = budget.overBudget();

                                if (!budget.isHasPreviousCycle()) {
                                    budget.setHasPreviousCycle(true);
                                } else {
                                    budget.setPreviousCycleOverBudget(budget.isOverBudget());
                                }

                                budget.setIsOverBudget(over);
                                budget.setCompleted(true);

                                budget.setPreviousCycleEndTimestamp(System.currentTimeMillis());

                                updateBudget(budget);
                            } else {
                                budgets.add(budget);
                            }
                        }
                    }
                    budgetsLiveData.setValue(budgets);
                });
    }
             
    public LiveData<Budget> getBudgetById(String budgetId) {
        MutableLiveData<Budget> live = new MutableLiveData<>();
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users")
                .document(userID)
                .collection("budgets")
                .document(budgetId)
                .addSnapshotListener((snap, err) -> {
                    if (err != null) {
                        err.printStackTrace();
                        return;
                    }
                    if (snap != null && snap.exists()) {
                        Budget b = snap.toObject(Budget.class);
                        if (b != null) {
                            b.setId(snap.getId());
                            live.setValue(b);
                        }
                    }
                });
        return live;
    }
    public void updateBudget(Budget budget) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            System.err.println("UpdateBudget: no verified user found!");
            return;
        }

        String userID = auth.getCurrentUser().getUid();

        if (budget.getId() == null || budget.getId().isEmpty()) {
            System.err.println("UpdateBudget: no id found!");
            return;
        }

        db.collection("users")
                .document(userID)
                .collection("budgets")
                .document(budget.getId())
                .set(budget)
                .addOnSuccessListener(aVoid -> {
                    System.out.println("Budget updated successfully");
                })
                .addOnFailureListener(e -> {
                    System.err.println("Budget failed to update");
                });
    }

    private boolean isBudgetExpired(Budget budget, Date currentDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        try {
            Date startDate = sdf.parse(budget.getStartDate());
            if (startDate == null) {
                return false;
            }

            Calendar start = Calendar.getInstance();
            start.setTime(startDate);
            Calendar end = Calendar.getInstance();

            if (budget.getFrequency().equalsIgnoreCase("Weekly")) {
                end.setTime(start.getTime());
                end.add(Calendar.DAY_OF_YEAR, 7);
            } else if (budget.getFrequency().equalsIgnoreCase("Monthly")) {
                end.setTime(start.getTime());
                end.add(Calendar.MONTH, 1);
            }
            return currentDate.after(end.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }
}
