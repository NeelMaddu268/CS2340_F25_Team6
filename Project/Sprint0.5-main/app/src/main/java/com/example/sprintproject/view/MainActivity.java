package com.example.sprintproject.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.sprintproject.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // -------------------------
        // Bottom Navigation Setup
        // -------------------------
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        // Load the DashboardFragment by default
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new DashboardFragment())
                    .commit();
        }

        // Handle navigation item clicks
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.Budgets) {
                selectedFragment = new BudgetsFragment();
            } else if (itemId == R.id.Dashboard) {
                selectedFragment = new DashboardFragment();
            } else if (itemId == R.id.ExpenseLogs) {
                selectedFragment = new ExpenseLogFragment();
            } else if (itemId == R.id.SavingsCircles) {
                selectedFragment = new SavingsCircleFragment();
            } // else if (itemId == R.id.Chatbot) {
            // selectedFragment = new ChatbotFragment();
            //}

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }

            return true;
        });

        // -------------------------
        // Start / Quit Buttons Setup
        // -------------------------
        Button btnStart = findViewById(R.id.btnStart);
        Button btnQuit = findViewById(R.id.btnQuit);

        btnStart.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        btnQuit.setOnClickListener(v -> {
            finishAffinity();
            System.exit(0);
        });
    }
}
