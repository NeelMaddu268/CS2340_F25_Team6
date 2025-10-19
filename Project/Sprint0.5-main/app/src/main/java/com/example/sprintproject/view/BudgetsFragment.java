package com.example.sprintproject.view;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sprintproject.R;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import com.example.sprintproject.viewmodel.BudgetCreationViewModel;
import com.example.sprintproject.viewmodel.BudgetsFragmentViewModel;

import java.util.ArrayList;


public class BudgetsFragment extends Fragment {

    private Button addBudget;
    private BudgetsFragmentViewModel budgetsFragmentViewModel;
    private RecyclerView recyclerView;
    private BudgetAdapter adapter;

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

        recyclerView = view.findViewById(R.id.budgetsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new BudgetAdapter(requireContext(), new ArrayList<>(), budget -> {
            Intent intent =  new Intent(requireContext(), BudgetDetailsActivity.class);
            intent.putExtra("budgetId", budget.getId()); // shares details of an existing budget
            intent.putExtra("budgetName", budget.getName());
            intent.putExtra("budgetAmount", budget.getAmount());
            intent.putExtra("budgetCategory", budget.getCategory());
            intent.putExtra("budgetFrequency", budget.getFrequency());
            intent.putExtra("budgetStartDate", budget.getStartDate());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);


        budgetsFragmentViewModel = new ViewModelProvider(this).get(BudgetsFragmentViewModel.class);
        budgetsFragmentViewModel.getBudgets().observe(getViewLifecycleOwner(), budgets -> {
            adapter.updateData(budgets);
        });

        budgetsFragmentViewModel.loadBudgets();

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
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    popupView.getContext(), android.R.layout.simple_spinner_item, frequencies);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            budgetFrequencyEntry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position != 0) {
                        budgetDateEntry.setEnabled(true);
                        budgetDateEntry.setFocusable(false);
                        budgetDateEntry.setClickable(true);
                    } else {
                        budgetDateEntry.setEnabled(false);
                        budgetDateEntry.setFocusable(false);
                        budgetDateEntry.setClickable(false);
                        budgetDateEntry.setText("");
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    budgetDateEntry.setEnabled(false);
                    budgetDateEntry.setFocusable(false);
                    budgetDateEntry.setClickable(false);
                }
            });

            budgetFrequencyEntry.setAdapter(adapter);
            budgetDateEntry.setOnClickListener(m -> {
                String selectedFrequency = budgetFrequencyEntry.getSelectedItem().toString();

                final Calendar today = Calendar.getInstance();
                int year = today.get(Calendar.YEAR);
                int month = today.get(Calendar.MONTH);
                int day = today.get(Calendar.DAY_OF_MONTH);
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        requireContext(),
                        (view1, selectedYear, selectedMonth, selectedDay) -> {
                            Calendar selectedDate = Calendar.getInstance();
                            selectedDate.set(selectedYear, selectedMonth, selectedDay);
                            if (selectedFrequency.equals("Monthly")) {
                                selectedDate.set(Calendar.DAY_OF_MONTH, 1);
                                if (selectedDate.before(today)) {
                                    selectedDate.add(Calendar.MONTH, 1);
                                }
                            }

                            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
                            String formattedDate = sdf.format(selectedDate.getTime());
                            budgetDateEntry.setText(formattedDate);
                        }, year, month, day
                );

                datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
                datePickerDialog.show();
            });

            cancelButton.setOnClickListener(view1 -> dialog.dismiss());

            createBudgetButton.setOnClickListener(view1 -> {
                String name = budgetNameEntry.getText().toString();
                String date = budgetDateEntry.getText().toString();
                String amount = budgetAmountEntry.getText().toString();
                String category = budgetCategoryEntry.getText().toString();
                String frequency = budgetFrequencyEntry.getSelectedItem().toString();
                boolean isValid = true;
                try {
                    int intAmount = Integer.parseInt(amount);
                    if (intAmount <= 0) {
                        budgetAmountEntry.setError("Amount must be greater than 0");
                        isValid = false;
                    }
                    if (budgetFrequencyEntry.getSelectedItemPosition() == 0) {
                        TextView errorText = (TextView) budgetFrequencyEntry.getSelectedView();
                        errorText.setError("");
                    }
                    if (name.equals("")) {
                        budgetNameEntry.setError("Please enter a name");
                        isValid = false;
                    }
                    if (category.equals("")) {
                        budgetCategoryEntry.setError("Please enter a category");
                        isValid = false;
                    }
                    if (date.equals("")) {
                        budgetDateEntry.setError("Please select a date");
                        isValid = false;
                    }
                } catch (NumberFormatException e) {
                    budgetAmountEntry.setError("Amount must be a number");
                    isValid = false;
                }
                if (isValid) {
                    budgetCreationViewModel.createBudget(name, date, amount, category, frequency,null);
                    dialog.dismiss();
                    budgetNameEntry.setText("");
                    budgetDateEntry.setText("");
                    budgetAmountEntry.setText("");
                    budgetCategoryEntry.setText("");
                    budgetFrequencyEntry.setSelection(0);
                }
            });

            dialog.show();
        });

        return view;
    }
}
