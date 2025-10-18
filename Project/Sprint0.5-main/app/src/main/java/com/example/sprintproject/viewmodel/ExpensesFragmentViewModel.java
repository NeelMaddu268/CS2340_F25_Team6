package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.model.Expense;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ExpensesFragmentViewModel extends ViewModel {


    private final MutableLiveData<List<Expense>> expensesLiveData =
            new MutableLiveData<>(new ArrayList<>());
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<List<Expense>> getExpenses() {
        return expensesLiveData;
    }

    public void loadExpenses() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String uid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("expenses")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error == null && value != null)  {
                        List<Expense> expenses = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Expense expense = doc.toObject(Expense.class);
                            expenses.add(expense);
                        }
                        expensesLiveData.setValue(expenses);
                    }
                });
    }
}
