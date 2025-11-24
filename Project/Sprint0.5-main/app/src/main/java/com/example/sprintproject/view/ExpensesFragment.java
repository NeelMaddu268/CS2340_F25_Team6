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
import com.example.sprintproject.model.AppDate;
import com.example.sprintproject.model.ExpenseData;
import com.example.sprintproject.viewmodel.BudgetsFragmentViewModel;
import com.example.sprintproject.viewmodel.DateViewModel;
import com.example.sprintproject.viewmodel.ExpenseCreationViewModel;
import com.example.sprintproject.viewmodel.ExpensesFragmentViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ExpensesFragment extends Fragment {

    private ExpensesFragmentViewModel expensesFragmentViewModel;
    private BudgetsFragmentViewModel budgetsFragmentViewModel;
    private ExpenseCreationViewModel expenseCreationViewModel;
    private DateViewModel dateVM;
    // Sonar fix: removed field adapter

    public ExpensesFragment() {
        super(R.layout.fragment_expenses);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view == null) {
            return null;
        }

        setupInsets(view);
        initViewModels();

        // adapter is LOCAL now (Sonar fix)
        ExpenseAdapter adapter = setupRecycler(view);

        observeExpenses(adapter);
        observeDateAndLoadInitial(); // ✅ Sonar fix: removed unused adapter param
        setupAddExpenseButton(view);

        return view;
    }

    private void setupInsets(View view) {
        EdgeToEdge.enable(requireActivity());
        ViewCompat.setOnApplyWindowInsetsListener(
                view.findViewById(R.id.expenselog_layout),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left,
                            systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                });
    }

    private void initViewModels() {
        expensesFragmentViewModel = new ViewModelProvider(requireActivity())
                .get(ExpensesFragmentViewModel.class);
        budgetsFragmentViewModel = new ViewModelProvider(requireActivity())
                .get(BudgetsFragmentViewModel.class);
        expenseCreationViewModel = new ViewModelProvider(requireActivity())
                .get(ExpenseCreationViewModel.class);
        dateVM = new ViewModelProvider(requireActivity())
                .get(DateViewModel.class);
    }

    // CHANGED: return adapter instead of storing as field
    private ExpenseAdapter setupRecycler(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.expensesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        ExpenseAdapter adapter = new ExpenseAdapter(expense -> {
            Intent intent = new Intent(requireContext(), ExpenseDetailsActivity.class);
            intent.putExtra("expenseName", expense.getName());
            intent.putExtra("expenseAmount", expense.getAmount());
            intent.putExtra("expenseCategory", expense.getCategory());
            intent.putExtra("expenseDate", expense.getDate());
            intent.putExtra("expenseNotes", expense.getNotes());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
        return adapter;
    }

    // CHANGED: take adapter as param
    private void observeExpenses(ExpenseAdapter adapter) {
        expensesFragmentViewModel.getExpenses().observe(
                getViewLifecycleOwner(),
                list -> adapter.submitList(list == null ? null : new ArrayList<>(list))
        );
    }

    // ✅ Sonar fix: removed unused parameter
    private void observeDateAndLoadInitial() {
        dateVM.getCurrentDate().observe(getViewLifecycleOwner(), selected -> {
            if (selected != null) {
                expensesFragmentViewModel.loadExpensesFor(selected);
            }
        });

        AppDate current = dateVM.getCurrentDate().getValue();
        if (current != null) {
            expensesFragmentViewModel.loadExpensesFor(current);
        } else {
            expensesFragmentViewModel.loadExpenses();
        }
    }

    private void setupAddExpenseButton(View view) {
        Button addExpense = view.findViewById(R.id.addExpense);
        addExpense.setOnClickListener(v -> showAddExpenseDialog());
    }

    private void showAddExpenseDialog() {
        View popupView = getLayoutInflater().inflate(R.layout.popup_expense_creation, null);
        AlertDialog dialog = new AlertDialog.Builder(requireActivity())
                .setView(popupView)
                .create();

        DialogViews dv = bindDialogViews(popupView);
        setupGroupSavingsSpinner(dv);
        observeCategoriesIntoSpinner(dv.categorySpinner);
        observeCircleNamesIntoSpinner(dv.chooseCircleSpinner);

        expenseCreationViewModel.loadCategories();

        dv.closeButton.setOnClickListener(v -> dialog.dismiss());
        dv.createBtn.setOnClickListener(v -> onCreateExpenseClicked(dv, dialog));
        setupExpenseDatePicker(dv.expenseDate);

        dialog.show();
    }

    private DialogViews bindDialogViews(View popupView) {
        DialogViews dv = new DialogViews();
        dv.expenseName = popupView.findViewById(R.id.ExpenseName);
        dv.expenseAmount = popupView.findViewById(R.id.ExpenseAmount);
        dv.expenseDate = popupView.findViewById(R.id.ExpenseDate);
        dv.expenseNotes = popupView.findViewById(R.id.ExpenseNotes);
        dv.createBtn = popupView.findViewById(R.id.createExpenseButton);
        dv.closeButton = popupView.findViewById(R.id.closeButton);

        dv.categorySpinner = popupView.findViewById(R.id.expenseCategorySpinner);
        dv.groupSavingsContributionSpinner = popupView.findViewById(R.id.groupSavingsSpinner);
        dv.chooseCircle = popupView.findViewById(R.id.chooseCircle);
        dv.chooseCircleSpinner = popupView.findViewById(R.id.chooseCircleSpinner);
        return dv;
    }

    private void setupGroupSavingsSpinner(DialogViews dv) {
        ArrayAdapter<String> groupSavingsAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"No", "Yes"}
        );
        groupSavingsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dv.groupSavingsContributionSpinner.setAdapter(groupSavingsAdapter);

        dv.groupSavingsContributionSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                                               View view, int position, long id) {
                        boolean isYes = "Yes".equalsIgnoreCase(
                                String.valueOf(parent.getItemAtPosition(position)));

                        dv.chooseCircle.setVisibility(isYes ? View.VISIBLE : View.GONE);
                        dv.chooseCircleSpinner.setVisibility(isYes ? View.VISIBLE : View.GONE);

                        if (isYes) {
                            expenseCreationViewModel.loadUserCircles();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        dv.chooseCircle.setVisibility(View.GONE);
                        dv.chooseCircleSpinner.setVisibility(View.GONE);
                    }
                });
    }

    private void observeCategoriesIntoSpinner(Spinner categorySpinner) {
        expenseCreationViewModel.getCategories().observe(
                getViewLifecycleOwner(),
                categories -> {
                    List<String> allCategories = new ArrayList<>();
                    allCategories.add("Choose a category");
                    if (categories != null) {
                        allCategories.addAll(categories);
                    }
                    ArrayAdapter<String> miniAdapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_spinner_dropdown_item,
                            allCategories
                    );
                    categorySpinner.setAdapter(miniAdapter);
                });
    }

    private void observeCircleNamesIntoSpinner(Spinner chooseCircleSpinner) {
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
                        return;
                    }

                    List<String> allCircles = new ArrayList<>();
                    allCircles.add("Select a Circle");
                    allCircles.addAll(circles);

                    ArrayAdapter<String> miniAdapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_spinner_dropdown_item,
                            allCircles
                    );
                    chooseCircleSpinner.setAdapter(miniAdapter);
                });
    }

    private void onCreateExpenseClicked(DialogViews dv, AlertDialog dialog) {
        String name = dv.expenseName.getText().toString();
        String date = dv.expenseDate.getText().toString();
        String amount = dv.expenseAmount.getText().toString();
        String category = String.valueOf(dv.categorySpinner.getSelectedItem());
        String notes = dv.expenseNotes.getText().toString();

        if (!validateExpenseInputs(dv, name, date, amount, category)) {
            return;
        }

        boolean contributesToGroupSavings =
                "Yes".equals(String.valueOf(dv.groupSavingsContributionSpinner.getSelectedItem()));

        String circleId = null;
        if (contributesToGroupSavings) {
            circleId = resolveCircleIdOrShowError(dv.chooseCircleSpinner);
            if (circleId == null) {
                return;
            }
        }

        ExpenseData data = new ExpenseData(
                name, date, amount, category, notes, contributesToGroupSavings, circleId);

        expenseCreationViewModel.createExpense(data, budgetsFragmentViewModel::loadBudgets);

        dialog.dismiss();
        clearDialogInputs(dv);
    }

    private boolean validateExpenseInputs(DialogViews dv,
                                          String name, String date,
                                          String amount, String category) {
        boolean isValid = true;

        try {
            int intAmount = Integer.parseInt(amount);
            if (intAmount <= 0) {
                dv.expenseAmount.setError("Amount must be greater than 0");
                isValid = false;
            }
        } catch (NumberFormatException e) {
            dv.expenseAmount.setError("Amount must be a number");
            isValid = false;
        }

        if (name.isEmpty()) {
            dv.expenseName.setError("Please enter a name");
            isValid = false;
        }

        if ("Choose a category".equals(category)) {
            Toast.makeText(requireContext(), "Please select a valid category",
                    Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (date.isEmpty()) {
            dv.expenseDate.setError("Please select a date");
            isValid = false;
        }

        return isValid;
    }

    private String resolveCircleIdOrShowError(Spinner chooseCircleSpinner) {
        String selectedCircleName = (String) chooseCircleSpinner.getSelectedItem();
        if (selectedCircleName != null
                && !"No circles found".equals(selectedCircleName)
                && !"Select a Circle".equals(selectedCircleName)) {
            return expenseCreationViewModel.getCircleIdForName(selectedCircleName);
        }

        Toast.makeText(requireContext(), "Please choose a circle",
                Toast.LENGTH_SHORT).show();
        return null;
    }

    private void clearDialogInputs(DialogViews dv) {
        dv.expenseName.setText("");
        dv.expenseDate.setText("");
        dv.expenseAmount.setText("");
        dv.categorySpinner.setSelection(0);
        dv.expenseNotes.setText("");
        dv.groupSavingsContributionSpinner.setSelection(0);
        dv.chooseCircleSpinner.setSelection(0);
    }

    private void setupExpenseDatePicker(EditText expenseDate) {
        expenseDate.setOnClickListener(v1 ->
                dateVM.getCurrentDate().observe(getViewLifecycleOwner(), appDate -> {
                    if (appDate == null) {
                        return;
                    }

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
                                SimpleDateFormat sdf =
                                        new SimpleDateFormat("MMM dd, yyyy", Locale.US);
                                expenseDate.setText(sdf.format(sel.getTime()));
                            },
                            year, month, day
                    );

                    picker.getDatePicker().setMaxDate(maxCalendar.getTimeInMillis());
                    picker.show();
                })
        );
    }

    private static class DialogViews {
        EditText expenseName;
        EditText expenseAmount;
        EditText expenseDate;
        EditText expenseNotes;
        Button createBtn;
        Button closeButton;
        Spinner categorySpinner;
        Spinner groupSavingsContributionSpinner;
        TextView chooseCircle;
        Spinner chooseCircleSpinner;
    }
}
