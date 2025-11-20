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
import com.example.sprintproject.viewmodel.SavingsCircleFragmentViewModel;


import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class BudgetsFragment extends Fragment {

    private Button addBudget;
    private BudgetsFragmentViewModel budgetsFragmentViewModel;
    private SavingsCircleFragmentViewModel savingsCircleFragmentViewModel;
    private DateViewModel dateVM;
    private BudgetAdapter budgetAdapter;
    private SavingsCircleAdapter savingsCircleAdapter;

    public BudgetsFragment() {
        super(R.layout.fragment_budgets);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view == null) {
            return null;
        }

        addBudget = view.findViewById(R.id.addBudget);

        EdgeToEdge.enable(requireActivity());
        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.budgets_layout),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top,
                            systemBars.right, systemBars.bottom);
                    return insets;
                });

        setupBudgetRecyclerView(view);

        budgetsFragmentViewModel = new ViewModelProvider(requireActivity())
                .get(BudgetsFragmentViewModel.class);
        dateVM = new ViewModelProvider(requireActivity())
                .get(DateViewModel.class);

        budgetsFragmentViewModel.getBudgets().observe(
                getViewLifecycleOwner(),
                list -> budgetAdapter.submitList(list == null ? null : new ArrayList<>(list))
        );

        setupSavingsCircleRecyclerView(view);
        savingsCircleFragmentViewModel = new ViewModelProvider(requireActivity())
                .get(SavingsCircleFragmentViewModel.class);
        savingsCircleFragmentViewModel.loadSavingsCircle();
        savingsCircleFragmentViewModel.getSavingsCircle().observe(
                getViewLifecycleOwner(),
                list -> savingsCircleAdapter.submitList(list == null ? null : new ArrayList<>(list))
        );

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

        setupAddBudgetDialog();
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
        if (d == null) {
            return false;
        }
        java.util.Calendar c = java.util.Calendar.getInstance();
        int y = c.get(java.util.Calendar.YEAR);
        int m = c.get(java.util.Calendar.MONTH) + 1; // 1..12
        int day = c.get(java.util.Calendar.DAY_OF_MONTH);
        return d.getYear() == y && d.getMonth() == m && d.getDay() == day;
    }

    private void setupBudgetRecyclerView(View view) {
        RecyclerView budgetRecyclerView;

        budgetRecyclerView = view.findViewById(R.id.budgetsRecyclerView);
        budgetRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        budgetAdapter = new BudgetAdapter(budget -> {
            Intent intent = new Intent(requireContext(), BudgetDetailsActivity.class);
            intent.putExtra("budgetId", budget.getId());
            intent.putExtra("budgetName", budget.getName());
            intent.putExtra("budgetAmount", budget.getAmount());
            intent.putExtra("budgetCategory", budget.getCategory());
            intent.putExtra("budgetFrequency", budget.getFrequency());
            intent.putExtra("budgetStartDate", budget.getStartDate());
            startActivity(intent);
        });
        budgetRecyclerView.setAdapter(budgetAdapter);
    }

    private void setupSavingsCircleRecyclerView(View view) {
        RecyclerView savingsCircleRecyclerView;

        savingsCircleRecyclerView = view.findViewById(R.id.savingsCircleRecyclerView);
        savingsCircleRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        savingsCircleFragmentViewModel = new ViewModelProvider(requireActivity())
                .get(SavingsCircleFragmentViewModel.class);
        savingsCircleAdapter = new SavingsCircleAdapter(savings -> {
            Intent intent = new Intent(requireContext(), SavingsCircleDetailsActivity.class);
            intent.putExtra("circleId", savings.getId());
            intent.putExtra("groupName", savings.getName());
            intent.putStringArrayListExtra("groupEmails",
                    new ArrayList<>(savings.getMemberEmails()));
            intent.putExtra("groupInvite", savings.getInvite());
            intent.putExtra("groupChallengeTitle", savings.getTitle());
            intent.putExtra("groupChallengeGoal", savings.getGoal());
            intent.putExtra("groupFrequency", savings.getFrequency());
            intent.putExtra("groupNotes", savings.getNotes());
            intent.putExtra("creationDate", savings.getCreatorDateJoined().toIso());
            intent.putExtra("datesJoined", (Serializable) savings.getDatesJoined());
            intent.putExtra("contributions", (Serializable) savings.getContributions());
            intent.putExtra("creatorId", savings.getCreatorId());
            startActivity(intent);
        });
        savingsCircleRecyclerView.setAdapter(savingsCircleAdapter);
    }

    private void setupAddBudgetDialog() {
        addBudget.setOnClickListener(v -> {
            View popupView = getLayoutInflater().inflate(R.layout.popup_budget_creation, null);
            AlertDialog dialog =
                    new AlertDialog.Builder(requireActivity()).setView(popupView).create();
            setupDialogContents(popupView, dialog);
            dialog.show();
        });
    }

    private void setupDialogContents(View popupView, AlertDialog dialog) {
        EditText budgetNameEntry = popupView.findViewById(R.id.BudgetNameEntry);
        EditText budgetAmountEntry = popupView.findViewById(R.id.BudgetAmountEntry);
        Spinner budgetFrequencyEntry = popupView.findViewById(R.id.BudgetFrequencyEntry);
        EditText budgetDateEntry = popupView.findViewById(R.id.BudgetDateEntry);
        EditText budgetCategoryEntry = popupView.findViewById(R.id.BudgetCategoryEntry);
        Button createBudgetButton = popupView.findViewById(R.id.createBudgetButton);
        Button cancelButton = popupView.findViewById(R.id.cancelButton);

        BudgetCreationViewModel budgetCreationViewModel =
                new ViewModelProvider(requireActivity()).get(BudgetCreationViewModel.class);
        setupFrequencySpinner(popupView, budgetFrequencyEntry, budgetDateEntry);

        budgetDateEntry.setOnClickListener(v -> {
            DateViewModel dateViewModel = new ViewModelProvider(requireActivity())
                    .get(DateViewModel.class);

            dateViewModel.getCurrentDate().observe(getViewLifecycleOwner(), appDate -> {
                if (appDate == null) {
                    return;
                }

                Calendar minCalendar = Calendar.getInstance();
                minCalendar.set(appDate.getYear(), appDate.getMonth() - 1, appDate.getDay(), 0, 0, 0);
                minCalendar.set(Calendar.MILLISECOND, 0);

                final Calendar today = Calendar.getInstance();
                int year = today.get(Calendar.YEAR);
                int month = today.get(Calendar.MONTH);
                int day = today.get(Calendar.DAY_OF_MONTH);

                String selectedFrequency = budgetFrequencyEntry.getSelectedItem().toString();

                DatePickerDialog picker = new DatePickerDialog(
                        requireContext(),
                        (view, y, mZero, dd) -> {

                            int displayMonth = mZero + 1;
                            int displayDay = dd;
                            int displayYear = y;

                            if ("Monthly".equals(selectedFrequency)) {
                                displayDay = 1;
                                Calendar sel = Calendar.getInstance();
                                sel.set(y, mZero, 1, 0, 0, 0);
                                sel.set(Calendar.MILLISECOND, 0);
                                if (sel.before(today)) {
                                    sel.add(Calendar.MONTH, 1);
                                    displayMonth = sel.get(Calendar.MONTH) + 1;
                                    displayYear = sel.get(Calendar.YEAR);
                                }
                            }

                            String dateString = String.format(Locale.US, "%02d/%02d/%04d",
                                    displayMonth, displayDay, displayYear);
                            budgetDateEntry.setText(dateString);
                        },
                        year, month, day
                );

                picker.getDatePicker().setMinDate(minCalendar.getTimeInMillis());
                picker.show();
            });
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
                if (intAmount <= 0) {
                    budgetAmountEntry.setError("Amount must be greater than 0");
                    isValid = false;
                }
                if (budgetFrequencyEntry.getSelectedItemPosition() == 0) {
                    TextView errorText = (TextView) budgetFrequencyEntry.getSelectedView();
                    if (errorText != null) {
                        errorText.setError("");
                    }
                    isValid = false;
                }
                if (name.isEmpty()) {
                    budgetNameEntry.setError("Please enter a name");
                    isValid = false;
                }
                if (category.isEmpty()) {
                    budgetCategoryEntry.setError("Please enter a category");
                    isValid = false;
                }
                if (date.isEmpty()) {
                    budgetDateEntry.setError("Please select a date");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                budgetAmountEntry.setError("Amount must be a number");
                isValid = false;
            }

            if (isValid) {
                long timestamp;
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    timestamp = sdf.parse(date).getTime();
                } catch (Exception e) {
                    timestamp = System.currentTimeMillis(); // fallback
                }

                budgetCreationViewModel.createBudget(
                        name, date, amount, category, frequency, timestamp, () -> {
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
    }

    private void setupFrequencySpinner(
            View popupView,
            Spinner budgetFrequencyEntry,
            EditText budgetDateEntry
    ) {
        String[] frequencies = {"Select a Frequency", "Weekly", "Monthly"};
        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(
                popupView.getContext(),
                android.R.layout.simple_spinner_item,
                frequencies
        );
        freqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        budgetFrequencyEntry.setAdapter(freqAdapter);

        budgetFrequencyEntry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                boolean enable = pos != 0;
                budgetDateEntry.setEnabled(enable);
                budgetDateEntry.setFocusable(false);
                budgetDateEntry.setClickable(enable);
                if (!enable) {
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
    }
}