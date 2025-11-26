package com.example.sprintproject.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sprintproject.R;
import com.example.sprintproject.viewmodel.FriendRequestsViewModel;

import java.util.List;
import java.util.Map;

public class FriendsListAdapter extends RecyclerView.Adapter<FriendsListAdapter.FriendViewHolder> {
    private List<Map<String, Object>> friends;
    private final FriendRequestsViewModel viewModel;

    public FriendsListAdapter(
            List<Map<String, Object>> friends,
            FriendRequestsViewModel viewModel
    ) {
        this.friends = friends;
        this.viewModel = viewModel;
    }

    public void updateFriendsList(List<Map<String, Object>> newFriends) {
        this.friends = newFriends;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friends, parent, false);
        return new FriendViewHolder(v);
    }

    public void onBindViewHolder(FriendViewHolder holder, int position) {
        Map<String, Object> friend = friends.get(position);

        String friendEmail = (String) friend.get("email");
        holder.friendEmail.setText(friendEmail != null ? friendEmail : "Unknown");

        holder.removeBtn.setOnClickListener(v -> {
            String friendId = (String) friend.get("uid");
            if (friendId != null) {
                viewModel.removeFriend(friendId);
            }
        });
    }

    public int getItemCount() {
        return friends != null ? friends.size() : 0;
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        private TextView friendEmail;
        private Button removeBtn;

        public FriendViewHolder(View itemView) {
            super(itemView);
            friendEmail = itemView.findViewById(R.id.friendEmail);
            removeBtn = itemView.findViewById(R.id.removeFriendButton);
        }
    }
}
