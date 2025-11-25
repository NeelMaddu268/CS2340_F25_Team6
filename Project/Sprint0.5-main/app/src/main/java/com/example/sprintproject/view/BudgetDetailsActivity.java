// This activity displays the information about the chosen budget and lets
// the user calculate different metrics such as totals, spent amount, etc.
// The activity observes the budget data through the view models and updates
// the UI accordingly, and lets the user save previous calculations.

package com.example.sprintproject.view;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.sprintproject.R;
import com.example.sprintproject.model.Budget;
import com.example.sprintproject.viewmodel.BudgetDetailsViewModel;
import com.example.sprintproject.viewmodel.BudgetsFragmentViewModel;

import java.util.Locale;

public class BudgetDetailsActivity extends AppCompatActivity {

    private TextView budgetSurplusText;
    private ProgressBar budgetProgressBar;

    private EditText budgetInputTotal;
    private EditText budgetInputSpent;
    private EditText budgetInputRemaining;

    private Button budgetComputeButton;
    private Button budgetSaveButton;

    private Budget currentBudget;

    private static final String OVER_BUDGET_STRING = "Over budget by: $";
    private static final String SURPLUS_STRING = "Surplus: $";

    private BudgetDetailsViewModel budgetDetailsViewModel;
    private BudgetsFragmentViewModel budgetsViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_details);

        initBudgetViews();
        initViewModels();
        bindIntentBudgetDetails();
        setupBackButton();

        observeBudgetAndCalculation();

        setupComputeButton();
        setupSaveButton();
    }

    private void initViewModels() {
        budgetDetailsViewModel = new ViewModelProvider(this).get(BudgetDetailsViewModel.class);
        budgetsViewModel = new ViewModelProvider(this).get(BudgetsFragmentViewModel.class);
    }

    private void bindIntentBudgetDetails() {
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
        budgetAmountTextView.setText(String.format(Locale.US, "$%.2f", budgetAmount));
        budgetCategoryTextView.setText(budgetCategory);
        budgetFrequencyTextView.setText(budgetFrequency);
        budgetStartDateTextView.setText(budgetStartDate);
    }

    private void setupBackButton() {
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(view -> finish());
    }

    private void observeBudgetAndCalculation() {
        String budgetID = getIntent().getStringExtra("budgetId");
        if (budgetID == null) {
            return;
        }

        budgetsViewModel.getBudgetById(budgetID).observe(this, budget -> {
            if (budget == null) {
                return;
            }

            currentBudget = budget;
            updateBudgetUI(budget);
            loadAndDisplayCalculation(budgetID);
        });
    }

    private void loadAndDisplayCalculation(String budgetID) {
        budgetDetailsViewModel.loadCalculation(budgetID, (total, spent, remaining) -> {
            setIfNotNull(budgetInputTotal, total);
            setIfNotNull(budgetInputSpent, spent);
            setIfNotNull(budgetInputRemaining, remaining);
            updateSurplusText(total, spent);
        });
    }

    private void setIfNotNull(EditText view, Double value) {
        if (value != null) {
            view.setText(String.valueOf(value));
        }
    }

    private void updateSurplusText(Double total, Double spent) {
        if (total == null || spent == null) {
            return;
        }

        double surplus = total - spent;
        if (surplus >= 0) {
            budgetSurplusText.setText(SURPLUS_STRING + String.format("%.2f", surplus));
        } else {
            budgetSurplusText.setText(
                    OVER_BUDGET_STRING + String.format("%.2f", Math.abs(surplus))
            );
        }
    }

    private void setupSaveButton() {
        budgetSaveButton.setOnClickListener(v -> {
            if (currentBudget == null) {
                Toast.makeText(this, "Budget does not exist.", Toast.LENGTH_SHORT).show();
                return;
            }

            Double total = parseDoubleOrNull(budgetInputTotal.getText().toString());
            Double spent = parseDoubleOrNull(budgetInputSpent.getText().toString());
            Double remaining = parseDoubleOrNull(budgetInputRemaining.getText().toString());

            if (total == null || spent == null || remaining == null) {
                Toast.makeText(this, "Invalid numbers were entered.", Toast.LENGTH_SHORT).show();
                return;
            }

            budgetDetailsViewModel.saveCalculation(
                    currentBudget.getId(),
                    total,
                    spent,
                    remaining,
                    () -> Toast.makeText(this, "Budget saved!", Toast.LENGTH_SHORT).show(),
                    () -> Toast.makeText(this, "Failed to save!", Toast.LENGTH_SHORT).show()
            );
        });
    }

    private Double parseDoubleOrNull(String val) {
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // this is how to update the UI
    private void updateBudgetUI(Budget budget) {
        budgetSurplusText.setText("");
        updateProgressBar(budget.getAmount(), budget.getSpentToDate());
    }

    // updating the progress bar UI
    private void updateProgressBar(double total, double spent) {
        if (total > 0) {
            int budgetPercent = (int) ((spent / total) * 100);
            budgetProgressBar.setProgress(budgetPercent);

            double budgetSurplus = total - spent;
            if (budgetSurplus >= 0) {
                budgetSurplusText.setText(
                        SURPLUS_STRING + String.format("%.2f", budgetSurplus));
            } else {
                budgetSurplusText.setText(
                        OVER_BUDGET_STRING + String.format("%.2f", Math.abs(budgetSurplus)));
            }
        }
    }

    private double parseCarefully(String val) {
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private void initBudgetViews() {
        budgetProgressBar = findViewById(R.id.budgetProgressBar);
        budgetSurplusText = findViewById(R.id.budgetPositiveText);
        budgetInputTotal = findViewById(R.id.budgetInputTotal);
        budgetInputSpent = findViewById(R.id.budgetInputSpent);
        budgetInputRemaining = findViewById(R.id.budgetInputRemaining);
        budgetComputeButton = findViewById(R.id.budgetComputeButton);
        budgetSaveButton = findViewById(R.id.budgetSaveButton);
    }

    private void setupComputeButton() {
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

            if (filledInputs < 2) {
                Toast.makeText(this, "Please fill in 2 inputs", Toast.LENGTH_SHORT).show();
                return;
            }

            double total = parseCarefully(totalString);
            double spent = parseCarefully(spentString);
            double remaining = parseCarefully(remainingString);

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

            double surplus = total - spent;
            if (surplus >= 0) {
                budgetSurplusText.setText(SURPLUS_STRING + String.format("%.2f", surplus));
            } else {
                budgetSurplusText.setText(OVER_BUDGET_STRING
                        + String.format("%.2f", Math.abs(surplus)));
            }
        });
    }
}


