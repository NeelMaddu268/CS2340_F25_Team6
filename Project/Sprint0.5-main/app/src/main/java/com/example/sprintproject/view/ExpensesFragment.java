package com.example.sprintproject.view;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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
import com.example.sprintproject.viewmodel.ExpenseCreationViewModel;
import com.example.sprintproject.viewmodel.ExpensesFragmentViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ExpensesFragment extends Fragment {

    private ExpensesFragmentViewModel expensesFragmentViewModel;
    private RecyclerView recyclerView;
    private ExpenseAdapter adapter;

    public ExpensesFragment() {
        super(R.layout.fragment_expenses);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        EdgeToEdge.enable(requireActivity());
        ViewCompat.setOnApplyWindowInsetsListener(
                view.findViewById(R.id.expenselog_layout),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );
                    return insets;
                });

        recyclerView = view.findViewById(R.id.expensesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

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


        expensesFragmentViewModel =
                new ViewModelProvider(this).get(ExpensesFragmentViewModel.class);
        expensesFragmentViewModel.getExpenses()
                .observe(getViewLifecycleOwner(), expenses -> {
                    adapter.submitList(expenses);
                });

        expensesFragmentViewModel.loadExpenses();

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
            Button closeButton = popupView.findViewById(R.id.closeButton);
            Spinner categorySpinner = popupView.findViewById(R.id.expenseCategorySpinner);
            ExpenseCreationViewModel expenseCreationViewModel = new ExpenseCreationViewModel();

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
                    expenseCreationViewModel.createExpense(name, date, amount, category, notes);
                    dialog.dismiss();
                    expenseName.setText("");
                    expenseDate.setText("");
                    expenseAmount.setText("");
                    categorySpinner.setSelection(0);
                    expenseNotes.setText("");
                }
            });

            expenseDate.setOnClickListener(v1 -> {
                final Calendar today = Calendar.getInstance();
                int year = today.get(Calendar.YEAR);
                int month = today.get(Calendar.MONTH);
                int day = today.get(Calendar.DAY_OF_MONTH);
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        requireContext(),
                        (view1, selectedYear, selectedMonth, selectedDay) -> {
                            Calendar selectedDate = Calendar.getInstance();
                            selectedDate.set(selectedYear, selectedMonth, selectedDay);
                            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
                            String formattedDate = sdf.format(selectedDate.getTime());
                            expenseDate.setText(formattedDate);
                        }, year, month, day);
                datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
                datePickerDialog.show();
            });

            dialog.show();
        });



        return view;
    }
}
