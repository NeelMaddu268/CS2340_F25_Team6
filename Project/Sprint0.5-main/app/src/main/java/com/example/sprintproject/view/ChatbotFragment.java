// This fragment file implements the chatbot UI, connecting the
// viewModel, recyclerView and etc together. Also allows
// users to include context from previous chats.

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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sprintproject.R;
import com.example.sprintproject.model.AppDate;
import com.example.sprintproject.repository.ChatRepository;
import com.example.sprintproject.ui.chat.ChatAdapter;
import com.example.sprintproject.viewmodel.ChatViewModel;
import com.example.sprintproject.viewmodel.DateViewModel;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class ChatbotFragment extends Fragment {

    private ChatViewModel chatVM;
    private static final String TODAY = "today";
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
        final DateViewModel dateVM = new ViewModelProvider(requireActivity()).get(DateViewModel.class);

        final RecyclerView recycler = view.findViewById(R.id.recyclerChat);
        final ProgressBar progress = view.findViewById(R.id.progressChat);
        final TextView txtError = view.findViewById(R.id.txtChatError);
        final EditText editInput = view.findViewById(R.id.editChatInput);

        final View commandRow = view.findViewById(R.id.layoutCommandRow);

        Button btnSend = view.findViewById(R.id.btnSend);
        Button btnNewChat = view.findViewById(R.id.btnNewChat);
        Button btnCmdWeekly = view.findViewById(R.id.btnCmdWeekly);
        Button btnCmdHousing = view.findViewById(R.id.btnCmdHousing);
        Button btnCmdEssentials = view.findViewById(R.id.btnCmdEssentials);

        final ChatAdapter adapter = new ChatAdapter();
        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        lm.setStackFromEnd(true);
        recycler.setLayoutManager(lm);
        recycler.setAdapter(adapter);

        dateVM.getCurrentDate().observe(getViewLifecycleOwner(), chatVM::setCurrentAppDate);

        chatVM.getMessages().observe(getViewLifecycleOwner(), msgs -> {
            adapter.submitList(msgs);
            if (adapter.getItemCount() > 0) {
                recycler.scrollToPosition(adapter.getItemCount() - 1);
            }

            if (commandRow != null) {
                commandRow.setVisibility(
                        (msgs == null || msgs.isEmpty()) ? View.VISIBLE : View.GONE
                );
            }
        });

        chatVM.getLoading().observe(getViewLifecycleOwner(), isLoading ->
            progress.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE)
        );

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
            showMemoryPopup();
        });

        btnCmdWeekly.setOnClickListener(v -> {
            AppDate appDate = dateVM.getCurrentDate().getValue();
            String prompt =
                    "Summarize my spending this week and track my weekly expenses using ONLY my "
                            + "real SpendWise data. Focus on the app date "
                            + (appDate != null ? appDate.toIso() : TODAY)
                            + " and do NOT invent any sample numbers.";
            editInput.setText("");
            chatVM.sendUserMessage(prompt);
        });

        btnCmdHousing.setOnClickListener(v -> {
            AppDate appDate = dateVM.getCurrentDate().getValue();
            String prompt =
                    "Using my current income and real expense history in SpendWise, "
                            + "help me create a sustainable housing budget for the app date "
                            + (appDate != null ? appDate.toIso() : TODAY)
                            + ". Do NOT invent any new dollar amounts; base everything on my "
                            + "existing data and realistic housing guidelines.";
            editInput.setText("");
            chatVM.sendUserMessage(prompt);
        });

        btnCmdEssentials.setOnClickListener(v -> {
            AppDate appDate = dateVM.getCurrentDate().getValue();
            String prompt =
                    "Plan my daily essentials (food, transport, basic needs) using only my "
                            + "actual SpendWise budgets and expenses for "
                            + (appDate != null ? appDate.toIso() : TODAY)
                            + ". No sample numbers—only what you can infer from my real data.";
            editInput.setText("");
            chatVM.sendUserMessage(prompt);
        });
    }

    private void showMemoryPopup() {
        chatVM.getChatDocs().addOnSuccessListener(docs -> {
            if (docs == null || docs.isEmpty()) {
                chatVM.setReferenceChats(new ArrayList<>());
                return;
            }

            String[] titles = new String[docs.size()];
            boolean[] checked = new boolean[docs.size()];

            for (int i = 0; i < docs.size(); i++) {
                ChatRepository.ChatDoc doc = docs.get(i);
                titles[i] = doc.title;
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle("Include previous chats?")
                    .setMultiChoiceItems(titles, checked,
                            (dlg, idx, isChecked) -> checked[idx] = isChecked)
                    .setPositiveButton("Use Selected", (dlg, w) -> {
                        List<String> selectedIds = new ArrayList<>();
                        StringBuilder memoryNote = new StringBuilder();

                        memoryNote.append("Using these previous chats as context:\n");
                        for (int i = 0; i < checked.length; i++) {
                            if (!checked[i]) continue;

                            ChatRepository.ChatDoc doc = docs.get(i);
                            selectedIds.add(doc.id);

                            memoryNote.append("- ")
                                    .append(doc.title);

                            if (doc.summary != null && !doc.summary.trim().isEmpty()) {
                                memoryNote.append(": ")
                                        .append(doc.summary.trim());
                            }
                            memoryNote.append("\n");
                        }

                        chatVM.setReferenceChats(selectedIds);

                        if (!selectedIds.isEmpty()) {
                            chatVM.addMemoryNote(memoryNote.toString().trim());
                        }
                    })
                    .setNegativeButton("Skip", (dlg, w) ->
                        chatVM.setReferenceChats(new ArrayList<>())
                    )
                    .show();
        });
    }
}
