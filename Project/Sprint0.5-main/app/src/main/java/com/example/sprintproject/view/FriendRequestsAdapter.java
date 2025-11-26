package com.example.sprintproject.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sprintproject.R;
import com.example.sprintproject.viewmodel.FriendRequestsViewModel;

import java.util.List;
import java.util.Map;

public class FriendRequestsAdapter
        extends RecyclerView.Adapter<FriendRequestsAdapter.RequestViewHolder> {
    private List<Map<String, Object>> requests;
    private final FriendRequestsViewModel viewModel;
    private final Context context;

    public FriendRequestsAdapter(
            List<Map<String, Object>> requests,
            FriendRequestsViewModel viewModel,
            Context context
    ) {
        this.requests = requests;
        this.viewModel = viewModel;
        this.context = context;
    }

    public void updateRequests(List<Map<String, Object>> newRequests) {
        this.requests = newRequests;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_requests, parent, false);
        return new RequestViewHolder(v);
    }

    public void onBindViewHolder(RequestViewHolder holder, int position) {
        Map<String, Object> request = requests.get(position);

        String requesterEmail = (String) request.get("requesterEmail");
        String requestId = (String) request.get("id");

        holder.requesterEmail.setText("Friend request from: " + requesterEmail);

        holder.acceptBtn.setOnClickListener(v -> {
            if (requestId != null) {
                viewModel.acceptFriendRequest(requestId);
                Toast.makeText(context, "Friend request accepted.", Toast.LENGTH_SHORT).show();
            }
        });

        holder.declineBtn.setOnClickListener(v -> {
            viewModel.response(requestId, false);
            Toast.makeText(context, "Friend request declined.", Toast.LENGTH_SHORT).show();
        });
    }

    public int getItemCount() {
        return requests != null ? requests.size() : 0;
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        private TextView requesterEmail;
        private Button acceptBtn;
        private Button declineBtn;

        public RequestViewHolder(View itemView) {
            super(itemView);
            requesterEmail = itemView.findViewById(R.id.requesterEmail);
            acceptBtn = itemView.findViewById(R.id.acceptRequestButton);
            declineBtn = itemView.findViewById(R.id.declineRequestButton);
        }
    }
}
