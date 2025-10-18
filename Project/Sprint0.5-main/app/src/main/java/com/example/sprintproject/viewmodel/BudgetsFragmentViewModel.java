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
                            budgets.add(budget);
                        }
                        budgetsLiveData.setValue(budgets);
                    }
                });
    }
}
