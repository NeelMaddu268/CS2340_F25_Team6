package com.example.sprintproject.view;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.sprintproject.R;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.example.sprintproject.viewmodel.BudgetCreationViewModel;

public class BudgetsFragment extends Fragment {

    private Button addBudget;

    public BudgetsFragment() {
        super(R.layout.fragment_budgets);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {

        View view = super.onCreateView(inflater, container, savedInstanceState);

        addBudget = view.findViewById(R.id.addBudget);

        EdgeToEdge.enable(requireActivity());
        ViewCompat.setOnApplyWindowInsetsListener(
                view.findViewById(R.id.budgets_layout),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );
                    return insets;
                }
        );

        Button addBudget = view.findViewById(R.id.addBudget);

        addBudget.setOnClickListener(v -> {
            View popupView = getLayoutInflater().inflate(R.layout.popup_budget_creation, null);

            AlertDialog dialog = new AlertDialog.Builder(requireActivity())
                    .setView(popupView)
                    .create();

            EditText budgetNameEntry = popupView.findViewById(R.id.BudgetNameEntry);
            EditText budgetAmountEntry = popupView.findViewById(R.id.BudgetAmountEntry);
            Spinner budgetFrequencyEntry = popupView.findViewById(R.id.BudgetFrequencyEntry);
            EditText budgetDateEntry = popupView.findViewById(R.id.BudgetDateEntry);
            EditText budgetCategoryEntry = popupView.findViewById(R.id.BudgetCategoryEntry);
            Button createBudgetButton = popupView.findViewById(R.id.createBudgetButton);
            Button cancelButton = popupView.findViewById(R.id.cancelButton);
            BudgetCreationViewModel budgetCreationViewModel = new BudgetCreationViewModel();

            String[] frequencies = {"Select a Frequency", "Weekly", "Monthly"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(popupView.getContext(), android.R.layout.simple_spinner_item, frequencies);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            budgetFrequencyEntry.setAdapter(adapter);

            cancelButton.setOnClickListener(view1 -> dialog.dismiss());

            createBudgetButton.setOnClickListener(view1 -> {
                String name = budgetNameEntry.getText().toString();
                String date = budgetDateEntry.getText().toString();
                String amount = budgetAmountEntry.getText().toString();
                String category = budgetCategoryEntry.getText().toString();
                String frequency = budgetFrequencyEntry.toString();

                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date currentDate = new Date();
                Date startDate;
                try {
                    int intAmount = Integer.parseInt(amount);
                    if (intAmount <= 0) {
                        budgetAmountEntry.setError("Amount must be greater than 0");
                    } else {
                        budgetCreationViewModel.createBudget(name, date, amount, category, frequency);
                        dialog.dismiss();
                    }
                } catch (NumberFormatException e) {
                    budgetAmountEntry.setError("Amount must be a number");
                }

                try {
                    startDate = format.parse(date);
                    if (startDate.compareTo(currentDate) < 0) {
                        budgetDateEntry.setError("Start date must be in the future");
                    } else {
                        budgetCreationViewModel.createBudget(name, date, amount, category, frequency);
                        dialog.dismiss();
                    }
                } catch (ParseException e) {
                    budgetDateEntry.setError("Start date must be in correct format.");
                }
                budgetNameEntry.setText("");
                budgetDateEntry.setText("");
                budgetAmountEntry.setText("");
                budgetCategoryEntry.setText("");
                budgetFrequencyEntry.setSelection(0);
            });

            dialog.show();
        });

        return view;
    }
}
