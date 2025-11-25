package com.example.sprintproject.view;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.example.sprintproject.model.AppDate;
import com.example.sprintproject.viewmodel.ChatViewModel;
import com.example.sprintproject.viewmodel.DateViewModel;

public class ChatbotFragment extends Fragment {

    private ChatViewModel chatVM;
    private DateViewModel dateVM;

    private com.example.sprintproject.ui.chat.ChatAdapter adapter;
    private RecyclerView recycler;
    private ProgressBar progress;
    private TextView txtError;
    private EditText editInput;
    private View commandRow;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_chatbot, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        chatVM = new ViewModelProvider(requireActivity()).get(ChatViewModel.class);
        dateVM = new ViewModelProvider(requireActivity()).get(DateViewModel.class);

        recycler = view.findViewById(R.id.recyclerChat);
        progress = view.findViewById(R.id.progressChat);
        txtError = view.findViewById(R.id.txtChatError);
        editInput = view.findViewById(R.id.editChatInput);
        commandRow = view.findViewById(R.id.layoutCommandRow);

        Button btnSend = view.findViewById(R.id.btnSend);
        Button btnNewChat = view.findViewById(R.id.btnNewChat);
        Button btnCmdWeekly = view.findViewById(R.id.btnCmdWeekly);
        Button btnCmdHousing = view.findViewById(R.id.btnCmdHousing);
        Button btnCmdEssentials = view.findViewById(R.id.btnCmdEssentials);

        adapter = new com.example.sprintproject.ui.chat.ChatAdapter();
        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        lm.setStackFromEnd(true);
        recycler.setLayoutManager(lm);
        recycler.setAdapter(adapter);

        // Keep ChatViewModel synced with the current app date (for timestamps)
        dateVM.getCurrentDate().observe(getViewLifecycleOwner(), appDate -> {
            chatVM.setCurrentAppDate(appDate);
        });

        chatVM.getMessages().observe(getViewLifecycleOwner(), msgs -> {
            adapter.submitList(msgs);
            if (adapter.getItemCount() > 0) {
                recycler.scrollToPosition(adapter.getItemCount() - 1);
            }

            // show starter commands only if there are no messages in this chat
            if (commandRow != null) {
                if (msgs == null || msgs.isEmpty()) {
                    commandRow.setVisibility(View.VISIBLE);
                } else {
                    commandRow.setVisibility(View.GONE);
                }
            }
        });

        chatVM.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progress.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE);
        });

        chatVM.getError().observe(getViewLifecycleOwner(), err -> {
            if (TextUtils.isEmpty(err)) {
                txtError.setVisibility(View.GONE);
            } else {
                txtError.setVisibility(View.VISIBLE);
                txtError.setText(err);
            }
        });

        btnSend.setOnClickListener(v -> {
            String text = editInput.getText().toString().trim();
            if (text.isEmpty()) {
                return;
            }
            chatVM.sendUserMessage(text);
            editInput.setText("");
        });

        btnNewChat.setOnClickListener(v -> {
            chatVM.startNewChat();
            editInput.setText("");
        });

        // Command buttons: all personal prompts, no fake numbers
        btnCmdWeekly.setOnClickListener(v -> {
            AppDate appDate = dateVM.getCurrentDate().getValue();
            String prompt =
                    "Using ONLY my real SpendWise data, help me track my weekly expenses. " +
                            "Focus on this app date: " +
                            (appDate != null ? appDate.toIso() : "today") +
                            ". Don’t invent sample numbers; work only with my budgets and expenses.";
            editInput.setText("");
            chatVM.sendUserMessage(prompt);
        });

        btnCmdHousing.setOnClickListener(v -> {
            AppDate appDate = dateVM.getCurrentDate().getValue();
            String prompt =
                    "Using my current income and real expense history in SpendWise, " +
                            "help me create a sustainable housing budget for the app date " +
                            (appDate != null ? appDate.toIso() : "today") +
                            ". Do NOT make up any dollar amounts; base everything on my existing data.";
            editInput.setText("");
            chatVM.sendUserMessage(prompt);
        });

        btnCmdEssentials.setOnClickListener(v -> {
            AppDate appDate = dateVM.getCurrentDate().getValue();
            String prompt =
                    "Plan my daily essentials (food, transport, basic needs) using only my " +
                            "actual SpendWise budgets and expenses for " +
                            (appDate != null ? appDate.toIso() : "today") +
                            ". No sample numbers—only the data you can infer from my account.";
            editInput.setText("");
            chatVM.sendUserMessage(prompt);
        });
    }
}