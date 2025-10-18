package com.example.sprintproject.view;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sprintproject.R;

public class BudgetDetailsActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_details);

        // Get the budget details from the intent
        String budgetName = getIntent().getStringExtra("budgetName");
        double budgetAmount = getIntent().getDoubleExtra("budgetAmount", 0.0);
        String budgetCategory = getIntent().getStringExtra("budgetCategory");
        String budgetFrequency = getIntent().getStringExtra("budgetFrequency");
        String budgetStartDate = getIntent().getStringExtra("budgetStartDate");

        // Update the UI with the budget details
        TextView budgetNameTextView = findViewById(R.id.budgetNameTextView);
        TextView budgetAmountTextView = findViewById(R.id.budgetAmountTextView);
        TextView budgetCategoryTextView = findViewById(R.id.budgetCategoryTextView);
        TextView budgetFrequencyTextView = findViewById(R.id.budgetFrequencyTextView);
        TextView budgetStartDateTextView = findViewById(R.id.budgetStartDateTextView);

        budgetNameTextView.setText(budgetName);
        budgetAmountTextView.setText(String.valueOf(budgetAmount));
        budgetCategoryTextView.setText(budgetCategory);
        budgetFrequencyTextView.setText(budgetFrequency);
        budgetStartDateTextView.setText(budgetStartDate);

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(view -> finish());
    }
}
