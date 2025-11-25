package com.example.sprintproject.view;   // <-- match the folder path

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sprintproject.R;
import com.example.sprintproject.ui.chat.ChatAdapter;
import com.example.sprintproject.viewmodel.ChatViewModel;

import java.util.ArrayList;

public class ChatbotFragment extends Fragment {

    private ChatViewModel chatVM;
    private ChatAdapter adapter;

    private RecyclerView recycler;
    private EditText input;
    private View sendBtn;
    private View newChatBtn;
    private ProgressBar progress;
    private TextView errorTxt;

    public ChatbotFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_chatbot, container, false);

        // ---- view bindings (must match IDs in fragment_chatbot.xml) ----
        recycler = v.findViewById(R.id.recyclerChat);
        input = v.findViewById(R.id.editChatInput);
        sendBtn = v.findViewById(R.id.btnSend);
        newChatBtn = v.findViewById(R.id.btnNewChat);
        progress = v.findViewById(R.id.progressChat);
        errorTxt = v.findViewById(R.id.txtChatError);
        // ----------------------------------------------------------------

        chatVM = new ViewModelProvider(requireActivity()).get(ChatViewModel.class);

        adapter = new ChatAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        // Start a new chat the first time fragment is opened
        if (savedInstanceState == null) {
            chatVM.startNewChat();
        }

        sendBtn.setOnClickListener(view -> {
            String text = input.getText().toString().trim();
            if (text.isEmpty()) {
                return;
            }
            input.setText("");
            chatVM.sendUserMessage(text);
        });

        newChatBtn.setOnClickListener(view -> chatVM.startNewChat());

        // -------- observers --------
        chatVM.getMessages().observe(getViewLifecycleOwner(), msgs -> {
            // Defensive copy for ListAdapter / DiffUtil
            if (msgs == null) {
                adapter.submitList(new ArrayList<>());
            } else {
                adapter.submitList(new ArrayList<>(msgs));
                recycler.scrollToPosition(msgs.size() - 1);
            }
        });

        chatVM.getLoading().observe(getViewLifecycleOwner(), isLoading ->
                progress.setVisibility(Boolean.TRUE.equals(isLoading)
                        ? View.VISIBLE : View.GONE)
        );

        chatVM.getError().observe(getViewLifecycleOwner(), err -> {
            if (err == null || err.trim().isEmpty()) {
                errorTxt.setVisibility(View.GONE);
            } else {
                errorTxt.setVisibility(View.VISIBLE);
                errorTxt.setText(err);
            }
        });
        // ---------------------------

        return v;
    }
}
