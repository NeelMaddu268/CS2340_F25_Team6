package com.example.sprintproject.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.sprintproject.R;
import com.example.sprintproject.model.Budget;

import java.util.List;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {
    private final Context context;
    private List<Budget> budgets;
    private final OnBudgetClickListener onBudgetClickListener;

    public BudgetAdapter(
            Context context, List<Budget> budgets, OnBudgetClickListener onBudgetClickListener) {
        this.context = context;
        this.budgets = budgets;
        this.onBudgetClickListener = onBudgetClickListener;
    }

    // Creating row layouts
    @Override
    public BudgetViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_budget, parent, false);
        return new BudgetViewHolder(view);
    }

    // Binding data to each row
    @Override
    public void onBindViewHolder(BudgetViewHolder holder, int position) {
        Budget budget = budgets.get(position);
        holder.nameText.setText(budget.getName());
        holder.amountText.setText(String.valueOf(budget.getAmount()));
        holder.categoryText.setText(budget.getCategory());
        holder.frequencyText.setText(budget.getFrequency());
        holder.startDateText.setText(budget.getStartDate());

        holder.itemView.setOnClickListener(v -> onBudgetClickListener.onBudgetClick(budget));
    }

    public int getItemCount() {
        return budgets.size();
    }

    // Update the list, use the adapter
    public void updateData(List<Budget> newBudgetsList) {
        this.budgets = newBudgetsList;
        notifyDataSetChanged();
    }

    static class BudgetViewHolder extends RecyclerView.ViewHolder {
        private TextView nameText;
        private TextView amountText;
        private TextView categoryText;
        private TextView frequencyText;
        private TextView startDateText;

        public BudgetViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.textBudgetName);
            amountText = itemView.findViewById(R.id.textBudgetAmount);
            categoryText = itemView.findViewById(R.id.textBudgetCategory);
            frequencyText = itemView.findViewById(R.id.textBudgetFrequency);
            startDateText = itemView.findViewById(R.id.textBudgetDate);
        }
    }

    public interface OnBudgetClickListener {
        void onBudgetClick(Budget budget);
    }
}
