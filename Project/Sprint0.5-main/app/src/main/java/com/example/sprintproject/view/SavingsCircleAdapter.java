package com.example.sprintproject.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sprintproject.R;
import com.example.sprintproject.model.SavingsCircle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SavingsCircleAdapter extends RecyclerView.Adapter<SavingsCircleAdapter.VH> {

    public interface OnItemClick {
        void onClick(SavingsCircle item);
    }

    private final List<SavingsCircle> items = new ArrayList<>();
    private final OnItemClick click;

    public SavingsCircleAdapter(OnItemClick click) {
        this.click = click;
    }

    public void submitList(List<SavingsCircle> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                // If your item XML has a different name, change this:
                .inflate(R.layout.item_savingscircle, parent, false);
        return new VH(v);
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
        h.itemView.setBackgroundColor(ContextCompat.getColor(h.itemView.getContext(), colorRes));

        h.itemView.setOnClickListener(v -> {
            if (click != null) click.onClick(c);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView title;
        final TextView goal;
        final TextView frequency;

        VH(@NonNull View itemView) {
            super(itemView);
            // IDs from your item layout you shared
            name = itemView.findViewById(R.id.textGroupName);
            title = itemView.findViewById(R.id.textGroupTitle);
            goal = itemView.findViewById(R.id.textGroupGoal);
            frequency = itemView.findViewById(R.id.textGroupFrequency);
        }
    }
}



