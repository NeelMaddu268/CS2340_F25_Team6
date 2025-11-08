package com.example.sprintproject.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sprintproject.R;
import com.example.sprintproject.model.SavingsCircle;
import androidx.recyclerview.widget.ListAdapter;

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

                public boolean areContentsTheSame(
                        @NonNull SavingsCircle oldItem, @NonNull SavingsCircle newItem) {
                    return oldItem.getName().equals(newItem.getName())
                            && oldItem.getTitle().equals(newItem.getTitle())
                            && oldItem.getGoal() == (newItem.getGoal())
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
        holder.groupName.setText(circle.getName());
        holder.groupTitle.setText(circle.getTitle());
        holder.groupGoal.setText("$" + circle.getGoal());
        holder.groupFrequency.setText(circle.getFrequency());

        holder.itemView.setOnClickListener(v ->
                onSavingsCircleClickListener.onSavingsCircleClick(circle));
    }

    static class SavingsCircleViewHolder extends RecyclerView.ViewHolder {
        private TextView groupName;
        private TextView groupTitle;
        private TextView groupGoal;
        private TextView groupFrequency;

        public SavingsCircleViewHolder(View itemView) {
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
