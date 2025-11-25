package com.example.sprintproject.ui.chat;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;

import com.example.sprintproject.R;
import com.example.sprintproject.viewmodel.ChatViewModel;

import java.util.*;

public class ChatAdapter extends ListAdapter<ChatViewModel.UiMessage, RecyclerView.ViewHolder> {

    private static final int USER = 0;
    private static final int ASSISTANT = 1;

    public ChatAdapter() {
        super(DIFF);
    }

    private static final DiffUtil.ItemCallback<ChatViewModel.UiMessage> DIFF =
            new DiffUtil.ItemCallback<ChatViewModel.UiMessage>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull ChatViewModel.UiMessage oldItem,
                        @NonNull ChatViewModel.UiMessage newItem
                ) {
                    return oldItem.localTime == newItem.localTime
                            && Objects.equals(oldItem.role, newItem.role);
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull ChatViewModel.UiMessage oldItem,
                        @NonNull ChatViewModel.UiMessage newItem
                ) {
                    return Objects.equals(oldItem.content, newItem.content)
                            && Objects.equals(oldItem.role, newItem.role);
                }
            };

    @Override
    public int getItemViewType(int position) {
        ChatViewModel.UiMessage m = getItem(position);
        boolean isAssistant = m.role != null && m.role.startsWith("assistant");
        return isAssistant ? ASSISTANT : USER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType
    ) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        View v;
        if (viewType == USER) {
            v = inf.inflate(R.layout.item_message_user, parent, false);
        } else {
            v = inf.inflate(R.layout.item_message_bot, parent, false);
        }
        return new BubbleVH(v);
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder, int position
    ) {
        BubbleVH vh = (BubbleVH) holder;
        ChatViewModel.UiMessage m = getItem(position);
        vh.txt.setText(m.content == null ? "" : m.content);
    }

    static class BubbleVH extends RecyclerView.ViewHolder {
        TextView txt;
        BubbleVH(@NonNull View itemView) {
            super(itemView);
            txt = itemView.findViewById(R.id.txtBubble);
        }
    }
}
