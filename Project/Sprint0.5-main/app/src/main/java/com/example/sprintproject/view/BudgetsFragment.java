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
import com.example.sprintproject.viewmodel.BudgetCreationViewModel;
import com.example.sprintproject.viewmodel.BudgetsFragmentViewModel;

import java.util.ArrayList;


public class BudgetsFragment extends Fragment {

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


        Button addBudget = view.findViewById(R.id.addBudget);

        addBudget.setOnClickListener(v -> {
            View popupView = getLayoutInflater().inflate(R.layout.popup_budget_creation, null);

            AlertDialog dialog = new AlertDialog.Builder(requireActivity())
                    .setView(popupView)
                    .create();

            EditText budgetNameEntry = popupView.findViewById(R.id.BudgetNameEntry);
            EditText budgetAmountEntry = popupView.findViewById(R.id.BudgetAmountEntry);
            EditText budgetFrequencyEntry = popupView.findViewById(R.id.BudgetFrequencyEntry);
            EditText budgetDateEntry = popupView.findViewById(R.id.BudgetDateEntry);
            EditText budgetCategoryEntry = popupView.findViewById(R.id.BudgetCategoryEntry);
            Button createBudgetButton = popupView.findViewById(R.id.createBudgetButton);
            Button closeButton = popupView.findViewById(R.id.closeButton);
            BudgetCreationViewModel budgetCreationViewModel = new BudgetCreationViewModel();

            closeButton.setOnClickListener(view1 -> dialog.dismiss());

            createBudgetButton.setOnClickListener(view1 -> {
                String name = budgetNameEntry.getText().toString();
                String date = budgetDateEntry.getText().toString();
                String amount = budgetAmountEntry.getText().toString();
                String category = budgetCategoryEntry.getText().toString();
                String frequency = budgetFrequencyEntry.getText().toString();

                budgetNameEntry.setText("");
                budgetDateEntry.setText("");
                budgetAmountEntry.setText("");
                budgetCategoryEntry.setText("");
                budgetFrequencyEntry.setText("");

                budgetCreationViewModel.createBudget(name, date, amount, category, frequency, null);

                dialog.dismiss();
            });

            dialog.show();
        });

        return view;
    }
}
