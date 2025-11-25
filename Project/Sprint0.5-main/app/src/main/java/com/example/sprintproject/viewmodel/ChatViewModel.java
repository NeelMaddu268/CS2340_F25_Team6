// This view model manages the chatbots full work flow from loading
// the app data, to generating insights and building AI prompts.

package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.logic.FinancialInsightsEngine;
import com.example.sprintproject.model.Budget;
import com.example.sprintproject.model.Expense;
import com.example.sprintproject.network.OllamaClient;
import com.example.sprintproject.repository.ChatRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChatViewModel extends ViewModel {
    public static final String ASSISTANT_PARTIAL = "assistant_partial";
    public static final String CONTENT = "content";
    public static final String ASSISTANT = "assistant";
    public static class UiMessage {
        public final String role;
        public final String content;
        public final long localTime;

        public UiMessage(String role, String content) {
            this.role = role;
            this.content = content;
            this.localTime = System.currentTimeMillis();
        }
    }

    private final MutableLiveData<List<UiMessage>> messages =
            new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>(null);

    public LiveData<List<UiMessage>> getMessages() { return messages; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }

    private final ChatRepository repo = new ChatRepository();
    private final OllamaClient ollama = new OllamaClient();
    private final FinancialInsightsEngine engine = new FinancialInsightsEngine();

    private String activeChatId;
    private boolean titleGenerated = false;
    private final List<String> selectedReferenceChatIds = new ArrayList<>();

    private ListenerRegistration msgListener;


    private List<UiMessage> currentListOrEmpty() {
        List<UiMessage> cur = messages.getValue();
        return cur == null ? new ArrayList<>() : new ArrayList<>(cur);
    }

    private void addLocalMessage(String role, String content) {
        List<UiMessage> copy = currentListOrEmpty();
        copy.add(new UiMessage(role, content));
        messages.postValue(copy);
    }

    private void updateLastPartial(String newContent, String role) {
        List<UiMessage> copy = currentListOrEmpty();
        int last = copy.size() - 1;
        if (last >= 0 && ASSISTANT_PARTIAL.equals(copy.get(last).role)) {
            copy.set(last, new UiMessage(role, newContent));
        } else {
            copy.add(new UiMessage(role, newContent));
        }
        messages.postValue(copy);
    }

    private void replaceWithFirestoreSnapshot(List<UiMessage> snapshotList) {
        messages.postValue(snapshotList);
    }

    public void startNewChat() {
        repo.createChatSkeleton()
                .addOnSuccessListener(ref -> {
                    activeChatId = ref.getId();
                    titleGenerated = false;
                    listenMessages();
                })
                .addOnFailureListener(e ->
                        error.postValue("Failed to create chat.")
                );
    }

    public void openExistingChat(String chatId) {
        activeChatId = chatId;
        titleGenerated = true;
        listenMessages();
    }


    private void listenMessages() {
        if (msgListener != null) {
            msgListener.remove();
            msgListener = null;
        }
        if (activeChatId == null) {
            return;
        }

        String uid = FirestoreManager.getInstance().getCurrentUserId();
        if (uid == null) {
            messages.postValue(new ArrayList<>());
            return;
        }

        msgListener = FirestoreManager.getInstance()
                .chatMessagesReference(uid, activeChatId)
                .orderBy("timestamp")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) {
                        return;
                    }

                    List<UiMessage> ui = new ArrayList<>();
                    for (DocumentSnapshot d : snapshot.getDocuments()) {
                        String role = d.getString("role");
                        String content = d.getString(CONTENT);
                        if (role != null && content != null) {
                            ui.add(new UiMessage(role, content));
                        }
                    }
                    replaceWithFirestoreSnapshot(ui);
                });
    }

    public Task<List<ChatRepository.ChatDoc>> getChatDocs() {
        return repo.loadChatDocs();
    }

    public void setReferenceChats(List<String> chatIds) {
        selectedReferenceChatIds.clear();
        if (chatIds != null) selectedReferenceChatIds.addAll(chatIds);

        if (activeChatId != null) {
            repo.setReferencedChats(activeChatId, selectedReferenceChatIds);
        }
    }

    public void sendUserMessage(String userText) {
        if (activeChatId == null || userText == null) return;

        error.postValue(null);
        loading.postValue(true);

        addLocalMessage("user", userText);

        repo.addMessage(activeChatId, "user", userText);

        loadDataThenRespond(userText);
    }

    private void loadDataThenRespond(String userText) {
        Task<QuerySnapshot> expT  = repo.loadExpenses();
        Task<QuerySnapshot> budT  = repo.loadBudgets();

        Tasks.whenAllSuccess(expT, budT)
                .addOnSuccessListener(results -> {
                    List<Expense> expenses = expT.getResult().toObjects(Expense.class);
                    List<Budget> budgets   = budT.getResult().toObjects(Budget.class);

                    FinancialInsightsEngine.InsightResult ir =
                            engine.tryHandle(userText, expenses, budgets);

                    String promptToAI = ir.handled ? ir.aiFollowupPrompt : userText;

                    buildFinalPrompt(promptToAI)
                            .addOnSuccessListener(fullPrompt -> {
                                JSONArray msgArr = buildMessagesArray(fullPrompt);
                                streamAssistant(msgArr, userText);
                            });
                })
                .addOnFailureListener(e -> {
                    loading.postValue(false);
                    error.postValue("Couldn’t load your financial data.");
                });
    }

    private Task<String> buildFinalPrompt(String prompt) {
        if (selectedReferenceChatIds.isEmpty())
            return Tasks.forResult(prompt);

        List<Task<String>> summaryTasks = new ArrayList<>();
        for (String id : selectedReferenceChatIds) {
            summaryTasks.add(repo.getSummary(id));
        }

        return Tasks.whenAllSuccess(summaryTasks).continueWith(t -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Context from previous chats:\n");
            for (Object s : t.getResult()) {
                if (s != null) sb.append("- ").append(s).append("\n");
            }
            sb.append("\nUser prompt:\n").append(prompt);
            return sb.toString();
        });
    }

    private JSONArray buildMessagesArray(String fullPrompt) {
        JSONArray arr = new JSONArray();
        try {
            JSONObject sys = new JSONObject();
            sys.put("role", "system");
            sys.put(CONTENT,
                    "You are SpendWise, a concise financial advisor. " +
                            "Only use the numeric facts provided. Give practical tips.");
            arr.put(sys);

            List<UiMessage> ui = messages.getValue();
            if (ui != null) {
                int start = Math.max(0, ui.size() - 20);
                for (int i = start; i < ui.size(); i++) {
                    UiMessage m = ui.get(i);
                    JSONObject o = new JSONObject();

                    boolean isAssistant = m.role != null && m.role.startsWith(ASSISTANT);
                    o.put("role", isAssistant ? ASSISTANT : "user");
                    o.put(CONTENT, m.content);

                    arr.put(o);
                }
            }

            JSONObject user = new JSONObject();
            user.put("role", "user");
            user.put(CONTENT, fullPrompt);
            arr.put(user);

        } catch (Exception ignored) {
            // intentionally ignored - invalid JSON format should not stop the execution
        }
        return arr;
    }


    private void streamAssistant(JSONArray msgArr, String rawUserText) {
        if (activeChatId == null) {
            loading.postValue(false);
            return;
        }

        addLocalMessage(ASSISTANT_PARTIAL, "");

        final StringBuilder streaming = new StringBuilder();

        ollama.chatStream(msgArr, new OllamaClient.StreamCallback() {
            @Override
            public void onToken(String token) {
                streaming.append(token);
                updateLastPartial(streaming.toString(), ASSISTANT_PARTIAL);
            }

            @Override
            public void onComplete(String fullReply) {
                loading.postValue(false);

                if (fullReply == null || fullReply.trim().isEmpty()) {
                    fullReply = "Sorry, I couldn’t fetch a response. Try again.";
                }

                updateLastPartial(fullReply, ASSISTANT);

                repo.addMessage(activeChatId, ASSISTANT, fullReply);

                if (!titleGenerated) {
                    titleGenerated = true;
                    generateTitle(rawUserText);
                }

                generateAndStoreSummary();
            }

            @Override
            public void onError(String err) {
                loading.postValue(false);
                error.postValue("AI response failed.");

                updateLastPartial("I couldn’t reach the AI right now.", ASSISTANT);

                repo.addMessage(activeChatId, ASSISTANT,
                        "I couldn’t reach the AI right now.");
            }
        });
    }


    private void generateTitle(String firstPrompt) {
        try {
            JSONArray arr = new JSONArray();
            arr.put(new JSONObject()
                    .put("role", "user")
                    .put(CONTENT,
                            "Create a short 2-5 word title for this chat based on: "
                                    + firstPrompt));

            ollama.chat(arr, new OllamaClient.ChatCallback() {
                @Override public void onSuccess(String reply) {
                    String title = reply == null ? "Chat" : reply.trim();
                    repo.updateChatTitle(activeChatId, title);
                }
                @Override public void onError(String error) {
                    // intentionally ignored - title generation is not critical
                }
            });

        } catch (Exception ignored) {
            // intentionally ignored - invalid JSON format should not stop the execution
        }
    }

    private void generateAndStoreSummary() {
        List<UiMessage> ui = messages.getValue();
        if (ui == null || ui.isEmpty()) return;

        StringBuilder convo = new StringBuilder();
        int start = Math.max(0, ui.size() - 10);
        for (int i = start; i < ui.size(); i++) {
            UiMessage m = ui.get(i);
            convo.append(m.role).append(": ").append(m.content).append("\n");
        }

        try {
            JSONArray arr = new JSONArray();
            arr.put(new JSONObject()
                    .put("role", "user")
                    .put(CONTENT,
                            "Summarize this conversation in 1-2 sentences " +
                                    "for memory. Be factual:\n" + convo));

            ollama.chat(arr, new OllamaClient.ChatCallback() {
                @Override public void onSuccess(String reply) {
                    if (reply != null) repo.updateChatSummary(activeChatId, reply.trim());
                }
                @Override public void onError(String error) {
                    // intentionally ignored - summary generation is not critical
                }
            });

        } catch (Exception ignored) {
            // intentionally ignored - invalid JSON format should not stop the execution
        }
    }

    @Override
    protected void onCleared() {
        if (msgListener != null) msgListener.remove();
        ollama.cancelActive();
    }
}
