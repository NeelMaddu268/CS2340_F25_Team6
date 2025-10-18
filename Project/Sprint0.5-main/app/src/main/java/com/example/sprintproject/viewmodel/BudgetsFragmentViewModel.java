package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.model.Budget;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class BudgetsFragmentViewModel extends ViewModel {

    private final MutableLiveData<List<Budget>> budgetsLiveData =
            new MutableLiveData<>(new ArrayList<>());
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<List<Budget>> getBudgets() {
        return budgetsLiveData;
    }

    public void loadBudgets() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String uid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("budgets")
                .orderBy("startDate", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error == null && value != null)  {
                        List<Budget> budgets = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Budget budget = doc.toObject(Budget.class);
                            if (budget != null) {
                                budget.setId(doc.getId()); // set the documented id here
                                if (budget.getMoneyRemaining() == 0 && budget.getSpentToDate() > 0) {
                                    budget.setMoneyRemaining(budget.getAmount() - budget.getSpentToDate());
                                }
                                budgets.add(budget);
                            }
                        }
                        budgetsLiveData.setValue(budgets);
                    }
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
}
