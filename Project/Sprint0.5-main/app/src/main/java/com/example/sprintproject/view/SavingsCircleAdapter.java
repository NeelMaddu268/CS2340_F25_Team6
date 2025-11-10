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
    public void onBindViewHolder(@NonNull SavingsCircleViewHolder holder, int position) {
        SavingsCircle circle = getItem(position);
        holder.groupName.setText(circle.getName());
        holder.groupTitle.setText(circle.getTitle());
        holder.groupGoal.setText("Goal: $" + circle.getGoal());
        holder.groupContribution.setText("Group Contribution: $" + circle.getSpent());
        holder.groupFrequency.setText(circle.getFrequency());

        holder.itemView.setOnClickListener(v ->
                onSavingsCircleClickListener.onSavingsCircleClick(circle));
    }

    static class SavingsCircleViewHolder extends RecyclerView.ViewHolder {
        private TextView groupName;
        private TextView groupTitle;
        private TextView groupGoal;
        private TextView groupContribution;
        private TextView groupFrequency;

        public SavingsCircleViewHolder(View itemView) {
            super(itemView);
            groupName = itemView.findViewById(R.id.textGroupName);
            groupTitle = itemView.findViewById(R.id.textGroupTitle);
            groupGoal = itemView.findViewById(R.id.textGroupGoal);
            groupContribution = itemView.findViewById(R.id.textGroupContribution);
            groupFrequency = itemView.findViewById(R.id.textGroupFrequency);
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



