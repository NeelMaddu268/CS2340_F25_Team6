package com.example.sprintproject.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.sprintproject.R;
import com.example.sprintproject.model.Budget;
import com.example.sprintproject.model.Expense;

import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {
    private final Context context;
    private List<Expense> expenses;
    private final OnExpenseClickListener onExpenseClickListener;

    public ExpenseAdapter(
            Context context, List<Expense> expenses, OnExpenseClickListener onExpenseClickListener) {
        this.context = context;
        this.expenses = expenses;
        this.onExpenseClickListener = onExpenseClickListener;
    }

    // Creating row layouts
    @Override
    public ExpenseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    // Binding data to each row
    @Override
    public void onBindViewHolder(ExpenseViewHolder holder, int position) {
        Expense expense = expenses.get(position);
        holder.nameText.setText(expense.getName());
        holder.amountText.setText(String.valueOf(expense.getAmount()));
        holder.categoryText.setText(expense.getCategory());
        holder.startDateText.setText(expense.getDate());

        holder.itemView.setOnClickListener(v -> onExpenseClickListener.onExpenseClick(expense));
    }

    public int getItemCount() {
        return expenses.size();
    }

    // Update the list, use the adapter
    public void updateData(List<Expense> newExpensesList) {
        this.expenses = newExpensesList;
        notifyDataSetChanged();
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        private TextView nameText;
        private TextView amountText;
        private TextView categoryText;
        private TextView frequencyText;
        private TextView startDateText;

        public ExpenseViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.textExpenseName);
            amountText = itemView.findViewById(R.id.textExpenseAmount);
            categoryText = itemView.findViewById(R.id.textExpenseCategory);
            frequencyText = itemView.findViewById(R.id.textExpenseFrequency);
            startDateText = itemView.findViewById(R.id.textExpenseDate);
        }
    }

    public interface OnExpenseClickListener {
        void onExpenseClick(Expense budget);
    }
}
