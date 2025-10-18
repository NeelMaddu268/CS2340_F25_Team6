package com.example.sprintproject.view;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

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

import java.util.ArrayList;

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
            Bundle savedInstanceState
    ) {
        
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
                }
        );

        recyclerView = view.findViewById(R.id.expensesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new ExpenseAdapter(requireContext(), new ArrayList<>(), expense -> {
            Intent intent =  new Intent(requireContext(), ExpenseDetailsActivity.class);
            intent.putExtra("expenseName", expense.getName());
            intent.putExtra("expenseAmount", expense.getAmount());
            intent.putExtra("expenseCategory", expense.getCategory());
            intent.putExtra("expenseStartDate", expense.getDate());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);


        expensesFragmentViewModel = new ViewModelProvider(this).get(ExpensesFragmentViewModel.class);
        expensesFragmentViewModel.getExpenses().observe(getViewLifecycleOwner(), expenses -> {
            adapter.updateData(expenses);
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
            EditText expenseCategory = popupView.findViewById(R.id.ExpenseCategory);
            EditText expenseDate = popupView.findViewById(R.id.ExpenseDate);
            Button createBtn = popupView.findViewById(R.id.createExpenseButton);
            Button closeButton = popupView.findViewById(R.id.closeButton);
            ExpenseCreationViewModel expenseCreationViewModel = new ExpenseCreationViewModel();

            closeButton.setOnClickListener(view1 -> dialog.dismiss());

            createBtn.setOnClickListener(view1 -> {
                String name = expenseName.getText().toString();
                String date = expenseDate.getText().toString();
                String amount = expenseAmount.getText().toString();
                String category = expenseCategory.getText().toString();

                expenseName.setText("");
                expenseDate.setText("");
                expenseAmount.setText("");
                expenseCategory.setText("");

                expenseCreationViewModel.createExpense(name, date, amount, category);

                dialog.dismiss();
            });

            dialog.show();
        });



        return view;
    }
}
