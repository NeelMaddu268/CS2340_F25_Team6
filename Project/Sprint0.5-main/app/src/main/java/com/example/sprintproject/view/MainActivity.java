package com.example.sprintproject.view;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.sprintproject.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize BottomNavigationView
        BottomNavigationView navView = findViewById(R.id.bottom_nav_menu);

        // Set default fragment when app starts
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new DashboardFragment())
                .commit();

        // Handle navigation item clicks
        navView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            switch (item.getItemId()) {
                case R.id.Dashboard:
                    selectedFragment = new DashboardFragment();
                    break;
                case R.id.Budgets:
                    selectedFragment = new BudgetsFragment();
                    break;
                case R.id.ExpenseLogs:
                    selectedFragment = new ExpenseLogFragment();
                    break;
                case R.id.SavingsCircles:
                    selectedFragment = new SavingsCircleFragment();
                    break;
            }

            // Swap the fragment
            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }

            return true;
        });
    }
}
