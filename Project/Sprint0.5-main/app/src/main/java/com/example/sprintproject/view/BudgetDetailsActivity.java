package com.example.sprintproject.view;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.sprintproject.R;
import com.example.sprintproject.model.Budget;
import com.example.sprintproject.viewmodel.BudgetsFragmentViewModel;

import android.text.Editable;
import android.text.TextWatcher;

public class BudgetDetailsActivity extends AppCompatActivity {

    private TextView budgetSurplusText;

    private ProgressBar budgetProgressBar;

    private EditText budgetInputTotal;
    private EditText budgetInputSpent;
    private EditText budgetInputRemaining;

    private Button budgetComputeButton;
    private Button budgetSaveButton;

    private BudgetsFragmentViewModel viewModel;
    private Budget currentBudget;

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

        budgetProgressBar = findViewById(R.id.budgetProgressBar);
        budgetSurplusText = findViewById(R.id.budgetPositiveText);
        budgetInputTotal = findViewById(R.id.budgetInputTotal);
        budgetInputSpent = findViewById(R.id.budgetInputSpent);
        budgetInputRemaining = findViewById(R.id.budgetInputRemaining);
        budgetComputeButton = findViewById(R.id.budgetComputeButton);
        budgetSaveButton = findViewById(R.id.budgetSaveButton);

        TextWatcher liveUpdates = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int before, int count) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                String totalString = budgetInputTotal.getText().toString();
                String spentString = budgetInputSpent.getText().toString();
                String remainingString = budgetInputRemaining.getText().toString();

                int filledInputs = 0;
                if (!totalString.isEmpty()) {
                    filledInputs++;
                }
                if (!spentString.isEmpty()) {
                    filledInputs++;
                }
                if (!remainingString.isEmpty()) {
                    filledInputs++;
                }
                if (filledInputs >= 2) {
                    double total = totalString.isEmpty() ? 0.0 : Double.parseDouble(totalString);
                    double spent = spentString.isEmpty() ? 0.0 : Double.parseDouble(spentString);
                    double remaining = remainingString.isEmpty() ? 0.0 : Double.parseDouble(remainingString);

                    if (totalString.isEmpty()) {
                        total = spent + remaining;
                        budgetInputTotal.setHint("Total: $" + String.valueOf(total));
                    } else if (spentString.isEmpty()) {
                        spent = total - remaining;
                        budgetInputSpent.setHint("Spent to Date: $" + String.valueOf(spent));
                    } else if (remainingString.isEmpty()) {
                        remaining = total - spent;
                        budgetInputRemaining.setHint("Remaining: $" + String.valueOf(remaining));
                    }

                    updateProgressBar(total, spent);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        };

        budgetInputTotal.addTextChangedListener(liveUpdates);
        budgetInputRemaining.addTextChangedListener(liveUpdates);
        budgetInputSpent.addTextChangedListener(liveUpdates);

        viewModel = new ViewModelProvider(this).get(BudgetsFragmentViewModel.class);

        String budgetID = getIntent().getStringExtra("budgetId");
        if (budgetID != null) {
            viewModel.getBudgetById(budgetID).observe(this, budget -> {
                if (budget != null) {
                    currentBudget = budget;
                    updateBudgetUI(budget);
                }
            });

        }

        // the 2 input fill the 3 input automatically
        budgetComputeButton.setOnClickListener(v -> {
            String totalString = budgetInputTotal.getText().toString();
            String spentString = budgetInputSpent.getText().toString();
            String remainingString = budgetInputRemaining.getText().toString();

            int filledInputs = 0;
            if (!TextUtils.isEmpty(totalString)) {
                filledInputs++;
            }
            if (!TextUtils.isEmpty(spentString)) {
                filledInputs++;
            }
            if (!TextUtils.isEmpty(remainingString)) {
                filledInputs++;
            }
            // what happens when less than 2 are filled
            if (filledInputs < 2) {
                Toast.makeText(this, "Please fill in 2 inputs", Toast.LENGTH_SHORT).show();
                return;
            }
            double total;
            double spent;
            double remaining;
            if (TextUtils.isEmpty(totalString)) {
                total = 0.0;
            } else {
                total = Double.parseDouble(totalString);
            }
            if (TextUtils.isEmpty(spentString)) {
                spent = 0.0;
            } else {
                spent = Double.parseDouble(spentString);
            }
            if (TextUtils.isEmpty(remainingString)) {
                remaining = 0.0;
            } else {
                remaining = Double.parseDouble(remainingString);
            }

            if (TextUtils.isEmpty(totalString)) {
                total = spent + remaining;
                budgetInputTotal.setText(String.valueOf(total));
            } else if (TextUtils.isEmpty(spentString)) {
                spent = total - remaining;
                budgetInputSpent.setText(String.valueOf(spent));
            } else if (TextUtils.isEmpty(remainingString)) {
                remaining = total - spent;
                budgetInputRemaining.setText(String.valueOf(remaining));
            }

            updateProgressBar(total, spent);
        });

        // save the calculations
        budgetSaveButton.setOnClickListener(v -> {
            if (currentBudget == null) {
                Toast.makeText(this, "Budget does not exist.", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double total = Double.parseDouble(budgetInputTotal.getText().toString());
                double spent = Double.parseDouble(budgetInputSpent.getText().toString());
                double remaining = Double.parseDouble(budgetInputRemaining.getText().toString());

                currentBudget.setAmount(total);
                currentBudget.setSpentToDate(spent);
                currentBudget.setMoneyRemaining(remaining);

                viewModel.updateBudget(currentBudget);
                Toast.makeText(this, "Budget saved!", Toast.LENGTH_SHORT).show();

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid numbers were entered.", Toast.LENGTH_SHORT).show();
            }
        });


    }

    // this is how to update the UI
    private void updateBudgetUI(Budget budget) {
        budgetInputTotal.setText(String.valueOf(budget.getAmount()));
        budgetInputSpent.setText(String.valueOf(budget.getSpentToDate()));
        budgetInputRemaining.setText(String.valueOf(budget.getMoneyRemaining()));
        updateProgressBar(budget.getAmount(), budget.getSpentToDate());
    }

    // updating the progress bar UI
    private void updateProgressBar(double total, double spent) {
        if (total > 0) {
            int budgetPercent = (int) ((spent/total) * 100);
            budgetProgressBar.setProgress(budgetPercent);

            double budgetSurplus = total - spent;
            if (budgetSurplus >= 0) {
                budgetSurplusText.setText("Surplus: $" + String.format("%.2f", budgetSurplus));
            } else {
                budgetSurplusText.setText("Over budget by: $" + String.format("%.2f", Math.abs(budgetSurplus)));
            }
        }
    }
}

