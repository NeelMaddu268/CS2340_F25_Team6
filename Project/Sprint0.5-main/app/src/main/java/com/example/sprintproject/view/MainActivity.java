// app/src/main/java/com/example/sprintproject/view/MainActivity.java
package com.example.sprintproject.view;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.sprintproject.R;
import com.example.sprintproject.viewmodel.DashboardViewModel;
import com.example.sprintproject.viewmodel.DateViewModel;
import com.example.sprintproject.viewmodel.NotificationQueueManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private DateViewModel dateVM;
    private DashboardViewModel dashVM;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- ViewModels ---
        dateVM = new ViewModelProvider(this).get(DateViewModel.class);
        dashVM = new ViewModelProvider(this).get(DashboardViewModel.class);

        // Start budgets listener for totals & warnings
        dashVM.loadData();

        // Hook notifications to date + budgets
        NotificationQueueManager nq = NotificationQueueManager.getInstance();
        nq.registerDateObserver(dateVM);

        dashVM.getBudgetsList().observe(this, budgets -> {
            if (budgets != null) {
                nq.checkForBudgetWarning(budgets);
            }
        });

        // --- Simple auth gate: if not logged in, go to LoginActivity ---
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            // prevent user from returning here without logging in
            finish();
        }
    }
}
