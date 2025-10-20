package com.example.sprintproject.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sprintproject.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RemainingBudgetAdapter
        extends RecyclerView.Adapter<RemainingBudgetAdapter.ViewHolder> {

    private final List<Map.Entry<String, Double>> entries = new ArrayList<>();

    public void updateData(Map<String, Double> map) {
        entries.clear();
        entries.addAll(map.entrySet());
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_remaining_budget, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map.Entry<String, Double> entry = entries.get(position);
        holder.categoryText.setText(entry.getKey());
        holder.remainingText.setText(String.format("$%.2f", entry.getValue()));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView categoryText;
        private TextView remainingText;

        ViewHolder(View v) {
            super(v);
            categoryText = v.findViewById(R.id.textRemainingCategory);
            remainingText = v.findViewById(R.id.textRemainingAmount);
        }

        public TextView getCategoryText() {
            return categoryText;
        }

        public TextView getRemainingText() {
            return remainingText;
        }
    }
}
