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
import com.example.sprintproject.viewmodel.BudgetsFragmentViewModel;
import com.example.sprintproject.viewmodel.ExpenseCreationViewModel;
import com.example.sprintproject.viewmodel.ExpensesFragmentViewModel;
import com.example.sprintproject.viewmodel.DateViewModel;              // <-- ADD

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ExpensesFragment extends Fragment {

    private ExpensesFragmentViewModel expensesFragmentViewModel;
    private DateViewModel dateVM;                                    // <-- ADD
    private RecyclerView recyclerView;
    private ExpenseAdapter adapter;
    private BudgetsFragmentViewModel budgetsFragmentViewModel;


    public ExpensesFragment() {
        super(R.layout.fragment_expenses);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        EdgeToEdge.enable(requireActivity());
        ViewCompat.setOnApplyWindowInsetsListener(
                view.findViewById(R.id.expenselog_layout),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left,
                            systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                });

        recyclerView = view.findViewById(R.id.expensesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        budgetsFragmentViewModel = new ViewModelProvider(requireActivity())
                .get(BudgetsFragmentViewModel.class);
        adapter = new ExpenseAdapter(expense -> {
            Intent intent = new Intent(requireContext(), ExpenseDetailsActivity.class);
            intent.putExtra("expenseName", expense.getName());
            intent.putExtra("expenseAmount", expense.getAmount());
            intent.putExtra("expenseCategory", expense.getCategory());
            intent.putExtra("expenseDate", expense.getDate());
            intent.putExtra("expenseNotes", expense.getNotes());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        expensesFragmentViewModel = new ViewModelProvider(requireActivity())
                .get(ExpensesFragmentViewModel.class);
        dateVM = new ViewModelProvider(requireActivity())
                .get(DateViewModel.class);

        expensesFragmentViewModel.getExpenses().observe(
                getViewLifecycleOwner(),
                list -> adapter.submitList(list == null ? null : new ArrayList<>(list))
        );

        dateVM.getCurrentDate().observe(getViewLifecycleOwner(), selected -> {
            if (selected != null) {
                expensesFragmentViewModel.loadExpensesFor(selected);
            }
        });

        if (dateVM.getCurrentDate().getValue() != null) {
            expensesFragmentViewModel.loadExpensesFor(dateVM.getCurrentDate().getValue());
        } else {
            expensesFragmentViewModel.loadExpenses();
        }

        Button addExpense = view.findViewById(R.id.addExpense);
        addExpense.setOnClickListener(v -> {
            View popupView = getLayoutInflater().inflate(R.layout.popup_expense_creation, null);
            AlertDialog dialog = new AlertDialog.Builder(requireActivity())
                    .setView(popupView)
                    .create();
            EditText expenseName = popupView.findViewById(R.id.ExpenseName);
            EditText expenseAmount = popupView.findViewById(R.id.ExpenseAmount);
            EditText expenseDate = popupView.findViewById(R.id.ExpenseDate);
            EditText expenseNotes = popupView.findViewById(R.id.ExpenseNotes);
            Button createBtn = popupView.findViewById(R.id.createExpenseButton);
            ExpenseCreationViewModel expenseCreationViewModel =
                    new ViewModelProvider(requireActivity()).get(ExpenseCreationViewModel.class);
            Button closeButton = popupView.findViewById(R.id.closeButton);
            Spinner categorySpinner = popupView.findViewById(R.id.expenseCategorySpinner);

            Spinner groupSavingsContributionSpinner =
                    popupView.findViewById(R.id.groupSavingsSpinner);
            TextView chooseCircle = popupView.findViewById(R.id.chooseCircle);
            Spinner chooseCircleSpinner = popupView.findViewById(R.id.chooseCircleSpinner);

            ArrayAdapter<String> groupSavingsAdapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    new String[]{"No", "Yes"}
            );
            groupSavingsAdapter
                    .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            groupSavingsContributionSpinner.setAdapter(groupSavingsAdapter);

            groupSavingsContributionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String choice = parent.getItemAtPosition(position).toString();
                    boolean isYes = choice.equalsIgnoreCase("Yes");
                    chooseCircle.setVisibility(isYes ? View.VISIBLE : View.GONE);
                    chooseCircleSpinner.setVisibility(isYes ? View.VISIBLE : View.GONE);

                    if (isYes) {
                        expenseCreationViewModel.loadUserCircles();
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            closeButton.setOnClickListener(view1 -> dialog.dismiss());

            expenseCreationViewModel.getCategories().observe(
                    getViewLifecycleOwner(),
                    categories -> {
                        List<String> allCategories = new ArrayList<>();
                        allCategories.add("Choose a category");
                        allCategories.addAll(categories);
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                requireContext(),
                                android.R.layout.simple_spinner_dropdown_item,
                                allCategories
                        );
                        categorySpinner.setAdapter(adapter);
                    });

            expenseCreationViewModel.getCircleNames().observe(
                    getViewLifecycleOwner(),
                    circles -> {
                        if (circles == null || circles.isEmpty()) {
                            chooseCircleSpinner.setAdapter(
                                    new ArrayAdapter<>(
                                            requireContext(),
                                            android.R.layout.simple_spinner_dropdown_item,
                                            new String[]{"No circles found"}
                                    )
                            );
                        } else {
                            List<String> allCircles = new ArrayList<>();
                            allCircles.add("Select a Circle");
                            allCircles.addAll(circles);
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                    requireContext(),
                                    android.R.layout.simple_spinner_dropdown_item,
                                    allCircles
                            );
                            chooseCircleSpinner.setAdapter(adapter);
                        }
                    }
            );

            expenseCreationViewModel.loadCategories();

            createBtn.setOnClickListener(view1 -> {
                String name = expenseName.getText().toString();
                String date = expenseDate.getText().toString();
                String amount = expenseAmount.getText().toString();
                String category = categorySpinner.getSelectedItem().toString();
                String notes = expenseNotes.getText().toString();
                boolean isValid = true;
                try {
                    int intAmount = Integer.parseInt(amount);
                    if (intAmount <= 0) {
                        expenseAmount.setError("Amount must be greater than 0");
                        isValid = false;
                    }
                    if (name.equals("")) {
                        expenseName.setError("Please enter a name");
                        isValid = false;
                    }
                    if (category.equals("Choose a category")) {
                        Toast.makeText(requireContext(), "Please select a valid category",
                                Toast.LENGTH_SHORT).show();
                        isValid = false;
                    }
                    if (date.equals("")) {
                        expenseDate.setError("Please select a date");
                        isValid = false;
                    }
                } catch (NumberFormatException e) {
                    expenseAmount.setError("Amount must be a number");
                    isValid = false;
                }

                if (isValid) {
                    boolean contributesToGroupSavings =

                            groupSavingsContributionSpinner.getSelectedItem().toString().equals("Yes");

                    String circleId = null;
                    if (contributesToGroupSavings) {
                        String selectedCircleName = (String) chooseCircleSpinner.getSelectedItem();
                        if (selectedCircleName != null
                                && !"No circles found".equals(selectedCircleName)
                                && !"Select a Circle".equals(selectedCircleName)) {
                            circleId = expenseCreationViewModel.getCircleIdForName(selectedCircleName);
                        } else {
                            Toast.makeText(requireContext(), "Please choose a circle", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    expenseCreationViewModel.createExpense(
                            name,
                            date,
                            amount,
                            category,
                            notes,
                            contributesToGroupSavings,
                            circleId,
                            () -> budgetsFragmentViewModel.loadBudgets()
                    );

                    dialog.dismiss();
                    expenseName.setText("");
                    expenseDate.setText("");
                    expenseAmount.setText("");
                    categorySpinner.setSelection(0);
                    expenseNotes.setText("");
                }
            });


//            expenseDate.setOnClickListener(v1 -> {
//                final Calendar today = Calendar.getInstance();
//                int year = today.get(Calendar.YEAR);
//                int month = today.get(Calendar.MONTH);
//                int day = today.get(Calendar.DAY_OF_MONTH);
//                DatePickerDialog picker = new DatePickerDialog(
//                        requireContext(),
//                        (view1, y, mZero, dd) -> {
//                            Calendar sel = Calendar.getInstance();
//                            sel.set(y, mZero, dd);
//                            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
//                            expenseDate.setText(sdf.format(sel.getTime()));
//                        },
//                        year, month, day
//                );
//                picker.getDatePicker().setMaxDate(System.currentTimeMillis());
//                picker.show();
//            });

            expenseDate.setOnClickListener(v1 -> {
                dateVM.getCurrentDate().observe(getViewLifecycleOwner(), appDate -> {
                    if (appDate == null) return;
                    Calendar maxCalendar = Calendar.getInstance();
                    maxCalendar.set(appDate.getYear(), appDate.getMonth() - 1, appDate.getDay());
                    final Calendar today = Calendar.getInstance();
                    int year = today.get(Calendar.YEAR);
                    int month = today.get(Calendar.MONTH);
                    int day = today.get(Calendar.DAY_OF_MONTH);
                    DatePickerDialog picker = new DatePickerDialog(
                            requireContext(),
                            (view1, y, mZero, dd) -> {
                                Calendar sel = Calendar.getInstance();
                                sel.set(y, mZero, dd);
                                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
                                expenseDate.setText(sdf.format(sel.getTime()));
                            },
                            year, month, day
                    );
                    picker.getDatePicker().setMaxDate(maxCalendar.getTimeInMillis());
                    picker.show();
                });
            });

            dialog.show();
        });

        return view;
    }
}
