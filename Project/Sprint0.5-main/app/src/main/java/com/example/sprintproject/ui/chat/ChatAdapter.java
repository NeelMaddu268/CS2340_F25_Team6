package com.example.sprintproject.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sprintproject.R;
import com.example.sprintproject.viewmodel.ChatViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 0;
    private static final int TYPE_BOT = 1;

    private final List<ChatViewModel.UiMessage> items = new ArrayList<>();

    public void submitList(List<ChatViewModel.UiMessage> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        ChatViewModel.UiMessage msg = items.get(position);
        if (msg.role != null && msg.role.startsWith("assistant")) {
            return TYPE_BOT;
        }
        return TYPE_USER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_USER) {
            View v = inflater.inflate(R.layout.item_message_user, parent, false);
            return new UserHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_message_bot, parent, false);
            return new BotHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position
    ) {
        ChatViewModel.UiMessage msg = items.get(position);
        String timeText = formatTime(msg.localTime);

        if (holder instanceof UserHolder) {
            ((UserHolder) holder).bubble.setText(msg.content);
            ((UserHolder) holder).time.setText(timeText);
        } else if (holder instanceof BotHolder) {
            ((BotHolder) holder).bubble.setText(msg.content);
            ((BotHolder) holder).time.setText(timeText);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatTime(long millis) {
        Date d = new Date(millis);
        SimpleDateFormat sdf =
                new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault());
        return sdf.format(d);
    }

    static class UserHolder extends RecyclerView.ViewHolder {
        final TextView bubble;
        final TextView time;

        UserHolder(@NonNull View itemView) {
            super(itemView);
            bubble = itemView.findViewById(R.id.txtBubble);
            time = itemView.findViewById(R.id.userMsgTime);
        }
    }

    static class BotHolder extends RecyclerView.ViewHolder {
        final TextView bubble;
        final TextView time;

        BotHolder(@NonNull View itemView) {
            super(itemView);
            bubble = itemView.findViewById(R.id.txtBubble);
            time = itemView.findViewById(R.id.botMsgTime);
        }
    }
}
