// This adapter showcases a list of budgets on the dashboard, showing each budgets details such as name, spent amount, etc.
// The adapter automatically updates the internally managed list whenever there is new data.

package com.example.sprintproject.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sprintproject.R;
import com.example.sprintproject.model.Budget;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardBudgetAdapter
        extends RecyclerView.Adapter<DashboardBudgetAdapter.ViewHolder> {

    private final List<Budget> budgets = new ArrayList<>();

    public void updateData(List<Budget> newBudgets) {
        budgets.clear();
        if (newBudgets != null) {
            budgets.addAll(newBudgets);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_remaining_budget, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Budget b = budgets.get(position);
        holder.name.setText(b.getName());
        holder.spent.setText(String.format(Locale.US, "Spent: $%.2f", b.getSpentToDate()));
        holder.remaining.setText(String.format(Locale.US,
                "Remaining: $%.2f / Total: $%.2f", b.getMoneyRemaining(), b.getAmount()));
    }

    @Override
    public int getItemCount() {
        return budgets.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView name;
        private TextView spent;
        private TextView remaining;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textBudgetName);
            spent = itemView.findViewById(R.id.textBudgetSpent);
            remaining = itemView.findViewById(R.id.textBudgetRemaining);
        }

        public TextView getName() {
            return name;
        }

        public TextView getSpent() {
            return spent;
        }

        public TextView getRemaining() {
            return remaining;
        }

    }
}