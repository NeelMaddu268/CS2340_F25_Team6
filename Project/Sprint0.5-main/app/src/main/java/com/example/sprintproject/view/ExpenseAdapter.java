// This adapter lists the expenses in a RecyclerView and displays the data for each expense in a row format.
// The adapter updates the expense list in real time and allows the to user to view and edit the expenses through on click actions.

package com.example.sprintproject.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sprintproject.R;
import com.example.sprintproject.model.Expense;

import java.util.Locale;

public class ExpenseAdapter extends ListAdapter<Expense, ExpenseAdapter.ExpenseViewHolder> {

    private final OnExpenseClickListener onExpenseClickListener;

    private static final DiffUtil.ItemCallback<Expense> EXPENSE_DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Expense>() {
                @Override
                public boolean areItemsTheSame(@NonNull Expense oldItem, @NonNull Expense newItem) {
                    return oldItem.getName().equals(newItem.getName())
                            && oldItem.getDate().equals(newItem.getDate())
                            && oldItem.getAmount() == newItem.getAmount();
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull Expense oldItem, @NonNull Expense newItem) {
                    return oldItem.getAmount() == newItem.getAmount()
                            && oldItem.getName().equals(newItem.getName())
                            && oldItem.getCategory().equals(newItem.getCategory())
                            && oldItem.getDate().equals(newItem.getDate());
                }
            };

    public ExpenseAdapter(OnExpenseClickListener onExpenseClickListener) {
        super(EXPENSE_DIFF_CALLBACK);
        this.onExpenseClickListener = onExpenseClickListener;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = getItem(position);
        holder.nameText.setText(expense.getName());
        holder.categoryText.setText(expense.getCategory());
        holder.startDateText.setText(expense.getDate());

        holder.amountText.setText(String.format(Locale.US, "$%.2f", expense.getAmount()));

        holder.itemView.setOnClickListener(v -> onExpenseClickListener.onExpenseClick(expense));
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        private TextView nameText;
        private TextView amountText;
        private TextView categoryText;
        private TextView startDateText;

        public ExpenseViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.textExpenseName);
            amountText = itemView.findViewById(R.id.textExpenseAmount);
            categoryText = itemView.findViewById(R.id.textExpenseCategory);
            startDateText = itemView.findViewById(R.id.textExpenseDate);
        }
    }

    public interface OnExpenseClickListener {
        void onExpenseClick(Expense expense);
    }
}