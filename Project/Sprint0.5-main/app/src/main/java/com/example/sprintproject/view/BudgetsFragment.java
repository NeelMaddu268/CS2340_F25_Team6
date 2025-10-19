package com.example.sprintproject.view;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
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

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sprintproject.R;
import com.example.sprintproject.model.AppDate;
import com.example.sprintproject.viewmodel.BudgetCreationViewModel;
import com.example.sprintproject.viewmodel.BudgetsFragmentViewModel;
import com.example.sprintproject.viewmodel.DateViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class BudgetsFragment extends Fragment {

    private Button addBudget;
    private BudgetsFragmentViewModel budgetsFragmentViewModel;
    private DateViewModel dateVM;
    private RecyclerView recyclerView;
    private BudgetAdapter adapter;

    public BudgetsFragment() {
        super(R.layout.fragment_budgets);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view == null) return null;

        addBudget = view.findViewById(R.id.addBudget);

        EdgeToEdge.enable(requireActivity());
        ViewCompat.setOnApplyWindowInsetsListener(
                view.findViewById(R.id.budgets_layout), (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                });

        recyclerView = view.findViewById(R.id.budgetsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new BudgetAdapter(budget -> {
            Intent intent = new Intent(requireContext(), BudgetDetailsActivity.class);
            intent.putExtra("budgetId", budget.getId());
            intent.putExtra("budgetName", budget.getName());
            intent.putExtra("budgetAmount", budget.getAmount());
            intent.putExtra("budgetCategory", budget.getCategory());
            intent.putExtra("budgetFrequency", budget.getFrequency());
            intent.putExtra("budgetStartDate", budget.getStartDate());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        // Activity-scoped so Dashboard + Budgets share the same date
        budgetsFragmentViewModel = new ViewModelProvider(requireActivity())
                .get(BudgetsFragmentViewModel.class);
        dateVM = new ViewModelProvider(requireActivity())
                .get(DateViewModel.class);

        // Render list (force new instance so DiffUtil always updates)
        budgetsFragmentViewModel.getBudgets()
                .observe(getViewLifecycleOwner(),
                        list -> adapter.submitList(list == null ? null : new java.util.ArrayList<>(list)));

        // Seed based on current date: if it's today -> show all; else filter immediately
        AppDate seed = dateVM.getCurrentDate().getValue();
        if (seed == null || isToday(seed)) {
            budgetsFragmentViewModel.loadBudgets();
        } else {
            budgetsFragmentViewModel.loadBudgetsFor(seed);
        }


        dateVM.getCurrentDate().observe(getViewLifecycleOwner(), d -> {
            if (d == null || isToday(d)) {
                budgetsFragmentViewModel.loadBudgets();
            } else {
                budgetsFragmentViewModel.loadBudgetsFor(d);
            }
        });


        // ---------- Add Budget dialog (unchanged) ----------
        addBudget.setOnClickListener(v -> {
            View popupView = getLayoutInflater().inflate(R.layout.popup_budget_creation, null);
            AlertDialog dialog = new AlertDialog.Builder(requireActivity()).setView(popupView).create();

            EditText budgetNameEntry = popupView.findViewById(R.id.BudgetNameEntry);
            EditText budgetAmountEntry = popupView.findViewById(R.id.BudgetAmountEntry);
            Spinner  budgetFrequencyEntry = popupView.findViewById(R.id.BudgetFrequencyEntry);
            EditText budgetDateEntry = popupView.findViewById(R.id.BudgetDateEntry);
            EditText budgetCategoryEntry = popupView.findViewById(R.id.BudgetCategoryEntry);
            Button   createBudgetButton = popupView.findViewById(R.id.createBudgetButton);
            Button   cancelButton = popupView.findViewById(R.id.cancelButton);

            BudgetCreationViewModel budgetCreationViewModel = new BudgetCreationViewModel();

            String[] frequencies = {"Select a Frequency", "Weekly", "Monthly"};
            ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(
                    popupView.getContext(), android.R.layout.simple_spinner_item, frequencies);
            freqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            budgetFrequencyEntry.setAdapter(freqAdapter);

            budgetFrequencyEntry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> parent, View view1, int pos, long id) {
                    boolean enable = pos != 0;
                    budgetDateEntry.setEnabled(enable);
                    budgetDateEntry.setFocusable(false);
                    budgetDateEntry.setClickable(enable);
                    if (!enable) budgetDateEntry.setText("");
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {
                    budgetDateEntry.setEnabled(false);
                    budgetDateEntry.setFocusable(false);
                    budgetDateEntry.setClickable(false);
                }
            });

            budgetDateEntry.setOnClickListener(m -> {
                String selectedFrequency = budgetFrequencyEntry.getSelectedItem().toString();
                final Calendar today = Calendar.getInstance();
                int year = today.get(Calendar.YEAR);
                int month = today.get(Calendar.MONTH);
                int day = today.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        requireContext(),
                        (view1, y, mZero, dd) -> {
                            Calendar selectedDate = Calendar.getInstance();
                            selectedDate.set(y, mZero, dd);
                            if ("Monthly".equals(selectedFrequency)) {
                                selectedDate.set(Calendar.DAY_OF_MONTH, 1);
                                if (selectedDate.before(today)) {
                                    selectedDate.add(Calendar.MONTH, 1);
                                }
                            }
                            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
                            budgetDateEntry.setText(sdf.format(selectedDate.getTime()));
                        },
                        year, month, day
                );
                datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
                datePickerDialog.show();
            });

            cancelButton.setOnClickListener(x -> dialog.dismiss());
            createBudgetButton.setOnClickListener(x -> {
                String name = budgetNameEntry.getText().toString();
                String date = budgetDateEntry.getText().toString();
                String amount = budgetAmountEntry.getText().toString();
                String category = budgetCategoryEntry.getText().toString();
                String frequency = budgetFrequencyEntry.getSelectedItem().toString();

                boolean isValid = true;
                try {
                    int intAmount = Integer.parseInt(amount);
                    if (intAmount <= 0) { budgetAmountEntry.setError("Amount must be greater than 0"); isValid = false; }
                    if (budgetFrequencyEntry.getSelectedItemPosition() == 0) {
                        TextView errorText = (TextView) budgetFrequencyEntry.getSelectedView();
                        if (errorText != null) errorText.setError("");
                        isValid = false;
                    }
                    if (name.isEmpty()) { budgetNameEntry.setError("Please enter a name"); isValid = false; }
                    if (category.isEmpty()) { budgetCategoryEntry.setError("Please enter a category"); isValid = false; }
                    if (date.isEmpty()) { budgetDateEntry.setError("Please select a date"); isValid = false; }
                } catch (NumberFormatException e) {
                    budgetAmountEntry.setError("Amount must be a number");
                    isValid = false;
                }

                if (isValid) {
                    long timestamp = System.currentTimeMillis();
                    budgetCreationViewModel.createBudget(name, date, amount, category, frequency, timestamp, () -> {
                        AppDate appDate = dateVM.getCurrentDate().getValue();
                        if (appDate != null) {
                            budgetsFragmentViewModel.loadBudgetsFor(appDate);
                        }
                    });
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

    @Override
    public void onResume() {
        super.onResume();
        if (dateVM != null) {
            com.example.sprintproject.model.AppDate d = dateVM.getCurrentDate().getValue();
            if (d == null || isToday(d)) {
                budgetsFragmentViewModel.loadBudgets();
            } else {
                budgetsFragmentViewModel.loadBudgetsFor(d);
            }
        }
    }


    private boolean isToday(com.example.sprintproject.model.AppDate d) {
        if (d == null) return false;
        java.util.Calendar c = java.util.Calendar.getInstance();
        int y = c.get(java.util.Calendar.YEAR);
        int m = c.get(java.util.Calendar.MONTH) + 1; // 1..12
        int day = c.get(java.util.Calendar.DAY_OF_MONTH);
        return d.getYear() == y && d.getMonth() == m && d.getDay() == day;
    }
}