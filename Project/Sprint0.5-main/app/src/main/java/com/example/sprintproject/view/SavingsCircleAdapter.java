// This adapter showcases a list of savings circles and its respctive details.
// It also displays the progress of each saving circle based on colors and
// lets the user view more details on click actions.

package com.example.sprintproject.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ListAdapter;

import com.example.sprintproject.R;
import com.example.sprintproject.model.SavingsCircle;

public class SavingsCircleAdapter extends ListAdapter<SavingsCircle,
        SavingsCircleAdapter.SavingsCircleViewHolder> {

    private final OnSavingsCircleClickListener onSavingsCircleClickListener;

    private static final DiffUtil.ItemCallback<SavingsCircle> SAVINGS_CIRCLE_DIFF_CALLBACK =
            new DiffUtil.ItemCallback<SavingsCircle>() {
                @Override
                public boolean areItemsTheSame(@NonNull SavingsCircle oldItem,
                                               @NonNull SavingsCircle newItem) {
                    return oldItem.getName().equals(newItem.getName());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull SavingsCircle oldItem, @NonNull SavingsCircle newItem) {
                    return oldItem.getName().equals(newItem.getName())
                            && oldItem.getTitle().equals(newItem.getTitle())
                            && oldItem.getGoal() == newItem.getGoal()
                            && oldItem.getFrequency().equals(newItem.getFrequency())
                            && ((oldItem.getNotes() == null && newItem.getNotes() == null)
                            || (oldItem.getNotes() != null
                            && oldItem.getNotes().equals(newItem.getNotes())));
                }
            };

    public SavingsCircleAdapter(OnSavingsCircleClickListener onSavingsCircleClickListener) {
        super(SAVINGS_CIRCLE_DIFF_CALLBACK);
        this.onSavingsCircleClickListener = onSavingsCircleClickListener;
    }

    @NonNull
    @Override
    public SavingsCircleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_savingscircle, parent, false);
        return new SavingsCircleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SavingsCircleViewHolder holder, int position) {
        SavingsCircle circle = getItem(position);

        // Text fields
        holder.groupName.setText(circle.getName());
        holder.groupTitle.setText(circle.getTitle());
        holder.groupGoal.setText("$" + circle.getGoal());
        holder.groupFrequency.setText(circle.getFrequency());

        // Background color based on group goal status
        int color = getGroupStatusColor(circle, holder.itemView.getContext());
        holder.itemView.setBackgroundColor(color);

        // Click handler
        holder.itemView.setOnClickListener(v ->
                onSavingsCircleClickListener.onSavingsCircleClick(circle));
    }

    /** Decide row color from goal/completed flags set in the ViewModel.
     * @param circle the savings circle
     * @param ctx the context
     * @return the color
     * */
    private int getGroupStatusColor(SavingsCircle circle, Context ctx) {
        if (circle.isGoalMet()) {
            // Group goal met (after end date)
            return ContextCompat.getColor(ctx, R.color.green);
        } else if (circle.isCompleted()) {
            // End date passed, group goal NOT met
            return ContextCompat.getColor(ctx, R.color.red);
        } else {
            // Still within the time window (in progress)
            return ContextCompat.getColor(ctx, R.color.blue);
        }
    }

    static class SavingsCircleViewHolder extends RecyclerView.ViewHolder {
        private final TextView groupName;
        private final TextView groupTitle;
        private final TextView groupGoal;
        private final TextView groupFrequency;

        public SavingsCircleViewHolder(@NonNull View itemView) {
            super(itemView);
            groupName = itemView.findViewById(R.id.textGroupName);
            groupTitle = itemView.findViewById(R.id.textGroupTitle);
            groupGoal = itemView.findViewById(R.id.textGroupGoal);
            groupFrequency = itemView.findViewById(R.id.textGroupFrequency);
        }
    }

    public interface OnSavingsCircleClickListener {
        void onSavingsCircleClick(SavingsCircle savingsCircle);
    }
}

