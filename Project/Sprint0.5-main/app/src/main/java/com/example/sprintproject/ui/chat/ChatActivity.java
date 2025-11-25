package com.example.sprintproject.ui.chat;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sprintproject.R;
import com.example.sprintproject.viewmodel.ChatViewModel;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private ChatViewModel vm;
    private ChatAdapter adapter;
    private RecyclerView recycler;
    private TextView errorText;
    private EditText input;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // layout MUST contain: chatRecycler, errorText, inputBox, sendBtn
        setContentView(R.layout.fragment_chatbot);

        vm = new ViewModelProvider(this).get(ChatViewModel.class);

        recycler = findViewById(R.id.recyclerChat);
        errorText = findViewById(R.id.txtChatError);
        input = findViewById(R.id.editChatInput);
        Button send = findViewById(R.id.btnSend);

        adapter = new ChatAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        vm.getMessages().observe(this, msgs -> {
            adapter.submitList(msgs);   // <- use submitList, not submit
            if (msgs != null && !msgs.isEmpty()) {
                recycler.scrollToPosition(msgs.size() - 1);
            }
        });

        vm.getError().observe(this, err -> {
            if (err == null || err.trim().isEmpty()) {
                errorText.setVisibility(View.GONE);   // <- View.GONE
                errorText.setText("");
            } else {
                errorText.setVisibility(View.VISIBLE);
                errorText.setText(err);
            }
        });

        // start a new chat each time you open screen
        vm.startNewChat();

        send.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (text.isEmpty()) {
                return;
            }
            askIncludePreviousThenSend(text);
        });
    }

    private void askIncludePreviousThenSend(String userText) {
        new AlertDialog.Builder(this)
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
                // ChatRepository.ChatDoc has 'title' field
                titles[i] = docs.get(i).title;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Pick chats")
                    .setMultiChoiceItems(titles, checked,
                            (dlg, idx, isChecked) -> checked[idx] = isChecked)
                    .setPositiveButton("Use Selected", (dlg, w) -> {
                        List<String> selected = new ArrayList<>();
                        for (int i = 0; i < checked.length; i++) {
                            if (checked[i]) {
                                selected.add(docs.get(i).id);
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
