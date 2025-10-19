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
import com.example.sprintproject.model.Budget;

import java.util.Locale;

public class BudgetAdapter extends ListAdapter<Budget, BudgetAdapter.BudgetViewHolder> {

    private final OnBudgetClickListener onBudgetClickListener;

    private static final DiffUtil.ItemCallback<Budget> BUDGET_DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Budget>() {
                @Override
                public boolean areItemsTheSame(@NonNull Budget oldItem, @NonNull Budget newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull Budget oldItem, @NonNull Budget newItem) {
                    return oldItem.getAmount() == newItem.getAmount()
                            && oldItem.getSpentToDate() == newItem.getSpentToDate()
                            && oldItem.getMoneyRemaining() == newItem.getMoneyRemaining()
                            && oldItem.getName().equals(newItem.getName());
                }
            };

    public BudgetAdapter(OnBudgetClickListener onBudgetClickListener) {
        super(BUDGET_DIFF_CALLBACK);
        this.onBudgetClickListener = onBudgetClickListener;
    }

    // Creating row layouts
    @Override
    public BudgetViewHolder onCreateViewHolder(
            ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_budget, parent, false);
        return new BudgetViewHolder(view);
    }

    // Binding data to each row
    @Override
    public void onBindViewHolder(BudgetViewHolder holder, int position) {
        Budget budget = getItem(position);
        holder.nameText.setText(budget.getName());
        holder.amountText.setText(String.valueOf(budget.getAmount()));
        holder.categoryText.setText(budget.getCategory());
        holder.frequencyText.setText(budget.getFrequency());
        holder.startDateText.setText(budget.getStartDate());

        String totalAmount = String.format(Locale.US, "$%.2f", budget.getAmount());

        holder.amountText.setText("Total: " + totalAmount);

        holder.itemView.setOnClickListener(v -> onBudgetClickListener.onBudgetClick(budget));
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
