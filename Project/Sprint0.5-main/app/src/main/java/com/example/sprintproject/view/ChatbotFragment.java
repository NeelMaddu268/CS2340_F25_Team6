package com.example.sprintproject.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sprintproject.R;
import com.example.sprintproject.ui.chat.ChatAdapter;
import com.example.sprintproject.viewmodel.ChatViewModel;
import com.example.sprintproject.repository.ChatRepository;

import java.util.ArrayList;
import java.util.List;

public class ChatbotFragment extends Fragment {

    private ChatViewModel vm;
    private ChatAdapter adapter;

    private RecyclerView recycler;
    private TextView errorText;
    private EditText input;
    private ProgressBar progress;
    private Button sendBtn;
    private Button newChatBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chatbot, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        vm = new ViewModelProvider(this).get(ChatViewModel.class);

        recycler   = view.findViewById(R.id.recyclerChat);
        errorText  = view.findViewById(R.id.txtChatError);
        input      = view.findViewById(R.id.editChatInput);
        progress   = view.findViewById(R.id.progressChat);
        sendBtn    = view.findViewById(R.id.btnSend);
        newChatBtn = view.findViewById(R.id.btnNewChat);

        adapter = new ChatAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        vm.getMessages().observe(getViewLifecycleOwner(), msgs -> {
            adapter.submitList(msgs == null ? new ArrayList<>() : new ArrayList<>(msgs));
            if (msgs != null && !msgs.isEmpty()) {
                recycler.scrollToPosition(msgs.size() - 1);
            }
        });

        vm.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null && isLoading) {
                progress.setVisibility(View.VISIBLE);
            } else {
                progress.setVisibility(View.GONE);
            }
        });

        vm.getError().observe(getViewLifecycleOwner(), err -> {
            if (err == null || err.trim().isEmpty()) {
                errorText.setVisibility(View.GONE);
                errorText.setText("");
            } else {
                errorText.setVisibility(View.VISIBLE);
                errorText.setText(err);
            }
        });

        vm.startNewChat();

        newChatBtn.setOnClickListener(v -> {
            vm.startNewChat();
            input.setText("");
        });

        sendBtn.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (text.isEmpty()) {
                return;
            }

            askIncludePreviousThenSend(text);
        });
    }

    private void askIncludePreviousThenSend(String userText) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Include previous context?")
                .setMessage("Use a previous chat summary to help the AI?")
                .setPositiveButton("Yes", (d, w) -> showChatPicker(userText))
                .setNegativeButton("No", (d, w) -> {
                    vm.setReferenceChats(new ArrayList<>());
                    vm.sendUserMessage(userText);
                    input.setText("");
                })
                .show();
    }

    private void showChatPicker(String userText) {
        vm.getChatDocs().addOnSuccessListener(docs -> {
            if (docs == null || docs.isEmpty()) {
                vm.sendUserMessage(userText);
                input.setText("");
                return;
            }

            String[] titles = new String[docs.size()];
            boolean[] checked = new boolean[docs.size()];

            for (int i = 0; i < docs.size(); i++) {
                ChatRepository.ChatDoc doc = docs.get(i);
                titles[i] = doc.getTitle() == null ? "Chat" : doc.getTitle();
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle("Pick chats")
                    .setMultiChoiceItems(titles, checked,
                            (dlg, idx, isChecked) -> checked[idx] = isChecked)
                    .setPositiveButton("Use Selected", (dlg, w) -> {
                        List<String> selected = new ArrayList<>();
                        for (int i = 0; i < checked.length; i++) {
                            if (checked[i]) {
                                selected.add(docs.get(i).getId());
                            }
                        }
                        vm.setReferenceChats(selected);
                        vm.sendUserMessage(userText);
                        input.setText("");
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }
}
