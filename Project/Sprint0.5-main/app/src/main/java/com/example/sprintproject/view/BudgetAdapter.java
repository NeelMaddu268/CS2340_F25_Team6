// The adapter manages the display of all the budgets in a scrollable manner in
// recyclerview by binding each budgets data into rows.
// This adapter also manages the colors indicating the progress of the budgets,
// status messages and on click behaviors
// to let the users see their budget status and more details.

package com.example.sprintproject.view;

import android.graphics.Color;
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
                    return oldItem.getId() != null && oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Budget oldItem,
                                                  @NonNull Budget newItem) {
                    return oldItem.getAmount() == newItem.getAmount()
                            && oldItem.getSpentToDate() == newItem.getSpentToDate()
                            && oldItem.getMoneyRemaining() == newItem.getMoneyRemaining()
                            && safeEq(oldItem.getName(), newItem.getName())
                            && safeEq(oldItem.getCategory(), newItem.getCategory())
                            && safeEq(oldItem.getFrequency(), newItem.getFrequency())
                            && safeEq(oldItem.getStartDate(), newItem.getStartDate());
                }

                private boolean safeEq(String a, String b) {
                    return (a == null && b == null) || (a != null && a.equals(b));
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
        String pinkColor = "#FFCDD2";
        Budget budget = getItem(position);
        holder.nameText.setText(budget.getName());
        holder.amountText.setText(String.valueOf(budget.getAmount()));
        String displayCategory = budget.getCategory();
        if (displayCategory != null && !displayCategory.isEmpty()) {
            displayCategory = displayCategory.substring(0, 1)
                    .toUpperCase() + displayCategory.substring(1);
        }
        holder.categoryText.setText(displayCategory);
        holder.frequencyText.setText(budget.getFrequency());
        holder.startDateText.setText(budget.getStartDate());

        if (budget.isHasPreviousCycle()) {
            holder.lastWeekBudgetTitle.setText("Last cycle you were ");
            if (budget.isPreviousCycleOverBudget()) {
                holder.lastWeekBudgetText.setText("over budget.");
                holder.lastWeekBudgetTitle.setVisibility(View.VISIBLE);
                holder.lastWeekBudgetText.setVisibility(View.VISIBLE);
                holder.lastWeekBudgetTitle.setBackgroundColor(Color.parseColor(pinkColor));
                holder.lastWeekBudgetText.setBackgroundColor(Color.parseColor(pinkColor));
            } else {
                holder.lastWeekBudgetText.setText("on track.");
                holder.lastWeekBudgetTitle.setVisibility(View.VISIBLE);
                holder.lastWeekBudgetText.setVisibility(View.VISIBLE);
                holder.lastWeekBudgetTitle.setBackgroundColor(Color.parseColor("#cdffda"));
                holder.lastWeekBudgetText.setBackgroundColor(Color.parseColor("#cdffda"));
            }
        } else {
            holder.lastWeekBudgetTitle.setVisibility(View.GONE);
            holder.lastWeekBudgetText.setVisibility(View.GONE);
        }
        if (budget.isOverBudget()) {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFCDD2"));
        } else {
            holder.itemView.setBackgroundColor(Color.parseColor("#e3dd98"));
        }

        String remainingBudgetBalance =
                String.format(Locale.US, "$%.2f", budget.getMoneyRemaining());

        holder.amountText.setText("Remaining: "
                + remainingBudgetBalance);

        holder.itemView.setOnClickListener(v -> onBudgetClickListener.onBudgetClick(budget));
    }

    static class BudgetViewHolder extends RecyclerView.ViewHolder {
        private TextView nameText;
        private TextView amountText;
        private TextView categoryText;
        private TextView frequencyText;
        private TextView startDateText;
        private TextView lastWeekBudgetTitle;
        private TextView lastWeekBudgetText;

        public BudgetViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.textBudgetName);
            amountText = itemView.findViewById(R.id.textBudgetAmount);
            categoryText = itemView.findViewById(R.id.textBudgetCategory);
            frequencyText = itemView.findViewById(R.id.textBudgetFrequency);
            startDateText = itemView.findViewById(R.id.textBudgetDate);
            lastWeekBudgetTitle = itemView.findViewById(R.id.lastWeekBudgetTitle);
            lastWeekBudgetText = itemView.findViewById(R.id.lastWeekBudgetText);
        }
    }

    public interface OnBudgetClickListener {
        void onBudgetClick(Budget budget);
    }
}
