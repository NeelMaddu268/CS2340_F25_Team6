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

import java.util.Locale;

public class SavingsCircleAdapter extends ListAdapter<SavingsCircle, SavingsCircleAdapter.SavingsCircleViewHolder> {
    private final SavingsCircleAdapter.onSavingsCircleClickListener onSavingsCircleClickListener;

    private static final DiffUtil.ItemCallback<SavingsCircle> SAVINGS_CIRCLE_DIFF_CALLBACK =
            new DiffUtil.ItemCallback<SavingsCircle>() {
                @Override
                public boolean areItemsTheSame(@NonNull SavingsCircle oldItem, @NonNull SavingsCircle newItem) {
                    return oldItem.getName().equals(newItem.getName())
                            && oldItem.getEmail().equals(newItem.getEmail());
                }

                public boolean areContentsTheSame(
                        @NonNull SavingsCircle oldItem, @NonNull SavingsCircle newItem) {
                    return oldItem.getName().equals(newItem.getName())
                            && oldItem.getEmail().equals(newItem.getEmail())
                            && oldItem.getTitle().equals(newItem.getTitle())
                            && oldItem.getGoal().equals(newItem.getGoal())
                            && oldItem.getFrequency().equals(newItem.getFrequency())
                            && oldItem.getNotes().equals(newItem.getNotes());
                }
            };

    public SavingsCircleAdapter(SavingsCircleAdapter.OnSavingsCircleClickListener onSavingsCircleClickListener) {
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
    public void onBindViewHolder(@NonNull SavingsCircleAdapter holder, int position) {
        SavingsCircle circle = getItem(position);
        holder.groupName.setText(circle.getName());
        holder.groupTitle.setText("Challenge: " + circle.getTitle());
        holder.groupGoal.setText("Goal: " + circle.getGoal());
        holder.groupFrequency.setText("Goal: " + circle.getFrequency());

        holder.itemView.setOnClickListener(v ->
                clickListener.onSavingsCircleClick(circle));
    }

    static class SavingsCircleViewHolder extends RecyclerView.ViewHolder {
        private TextView groupName;
        private TextView groupEmail;
        private TextView groupTitle;
        private TextView groupGoal;
        private TextView groupFrequency;
        private TextView groupNotes;

        public SavingsCircleViewHolder(View itemView) {
            super(itemView);
            groupName = itemView.findViewById(R.id.textGroupName);
        }
    }

    public interface OnSavingsCircleClickListener {
        void onSavingsCircleClick(SavingsCircle savingsCircle);
    }
}
