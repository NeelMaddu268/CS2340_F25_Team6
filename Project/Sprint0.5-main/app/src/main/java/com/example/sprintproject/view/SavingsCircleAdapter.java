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
    public void onBindViewHolder(@NonNull VH h, int position) {
        SavingsCircle c = items.get(position);

        // Bind text fields (null-safe)
        h.name.setText(c.getName() == null ? "" : c.getName());
        h.title.setText(c.getTitle() == null ? "" : c.getTitle());
        h.goal.setText(String.format(Locale.US, "$%.1f", c.getGoal()));
        h.frequency.setText(c.getFrequency() == null ? "" : c.getFrequency());

        // Background color logic (matches your rollover palette)
        int colorRes;
        if (c.isGoalMet()) {
            colorRes = R.color.green;     // #3DB85D
        } else if (c.isCompleted()) {
            colorRes = R.color.red;       // #E53935
        } else {
            colorRes = R.color.blue;      // #357DED
        }
    }

    public interface OnSavingsCircleClickListener {
        void onSavingsCircleClick(SavingsCircle savingsCircle);
    }
}
