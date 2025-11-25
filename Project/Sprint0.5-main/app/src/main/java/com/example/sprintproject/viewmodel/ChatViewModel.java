package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.logic.FinancialInsightsEngine;
import com.example.sprintproject.model.AppDate;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatViewModel extends ViewModel {

    public static final String ASSISTANT_PARTIAL = "assistant_partial";
    public static final String CONTENT = "content";
    public static final String ASSISTANT = "assistant";

    public static class UiMessage {
        public final String role;
        public final String content;
        public final long localTime;

        public UiMessage(String role, String content) {
            this(role, content, System.currentTimeMillis());
        }

        public UiMessage(String role, String content, long localTime) {
            this.role = role;
            this.content = content;
            this.localTime = localTime;
        }
    }

    private final MutableLiveData<List<UiMessage>> messages =
            new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>(null);
    private final MutableLiveData<String> currentChatId = new MutableLiveData<>(null);

    public LiveData<List<UiMessage>> getMessages() {
        return messages;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<String> getCurrentChatId() {
        return currentChatId;
    }

    private final ChatRepository repo = new ChatRepository();
    private final OllamaClient ollama = new OllamaClient();
    private final FinancialInsightsEngine engine = new FinancialInsightsEngine();

    private AppDate currentAppDate;

    private String activeChatId;
    private boolean titleGenerated = false;
    private final List<String> selectedReferenceChatIds = new ArrayList<>();

    private ListenerRegistration msgListener;

    public List<String> getStarterCommands() {
        return Arrays.asList(
                "Track my weekly expenses",
                "Create a sustainable housing budget",
                "Plan for daily essentials",
                "Summarize my monthly spending habits",
                "Give me insights about my savings circles"
        );
    }

    public void setCurrentAppDate(AppDate appDate) {
        this.currentAppDate = appDate;
    }

    private long nowFromAppDate() {
        if (currentAppDate == null) {
            return System.currentTimeMillis();
        }

        Calendar now = Calendar.getInstance();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, currentAppDate.getYear());
        c.set(Calendar.MONTH, currentAppDate.getMonth() - 1);
        c.set(Calendar.DAY_OF_MONTH, currentAppDate.getDay());
        c.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY));
        c.set(Calendar.MINUTE, now.get(Calendar.MINUTE));
        c.set(Calendar.SECOND, now.get(Calendar.SECOND));
        c.set(Calendar.MILLISECOND, now.get(Calendar.MILLISECOND));
        return c.getTimeInMillis();
    }

    private String isoNow() {
        long millis = nowFromAppDate();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
        return sdf.format(new Date(millis));
    }

    private long isoToMillis(String iso) {
        if (iso == null || iso.isEmpty()) {
            return System.currentTimeMillis();
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
            sdf.setLenient(false);
            Date d = sdf.parse(iso);
            return d != null ? d.getTime() : System.currentTimeMillis();
        } catch (ParseException e) {
            return System.currentTimeMillis();
        }
    }

    private List<UiMessage> currentListOrEmpty() {
        List<UiMessage> cur = messages.getValue();
        return cur == null ? new ArrayList<>() : new ArrayList<>(cur);
    }

    private void addLocalMessage(String role, String content) {
        List<UiMessage> copy = currentListOrEmpty();
        copy.add(new UiMessage(role, content, nowFromAppDate()));
        messages.postValue(copy);
    }

    private void updateLastPartial(String newContent, String role) {
        List<UiMessage> copy = currentListOrEmpty();
        int last = copy.size() - 1;
        if (last >= 0 && ASSISTANT_PARTIAL.equals(copy.get(last).role)) {
            UiMessage prev = copy.get(last);
            copy.set(last, new UiMessage(role, newContent, prev.localTime));
        } else {
            copy.add(new UiMessage(role, newContent, nowFromAppDate()));
        }
        messages.postValue(copy);
    }

    private void replaceWithFirestoreSnapshot(List<UiMessage> snapshotList) {
        snapshotList.sort(Comparator.comparingLong(m -> m.localTime));
        messages.postValue(snapshotList);
    }

    public void startNewChat() {
        loading.setValue(true);
        String iso = isoNow();

        repo.createNewChat(iso)
                .addOnSuccessListener(chatId -> {
                    activeChatId = chatId;
                    currentChatId.postValue(chatId);
                    titleGenerated = false;
                    replaceWithFirestoreSnapshot(new ArrayList<>());
                    listenToMessagesInternal(chatId);
                    loading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    loading.postValue(false);
                    error.postValue("Failed to start new chat: " + e.getMessage());
                });
    }

    public void openExistingChat(String chatId) {
        activeChatId = chatId;
        currentChatId.postValue(chatId);
        titleGenerated = true;
        listenToMessagesInternal(chatId);
    }

    public Task<List<ChatRepository.ChatDoc>> getChatDocs() {
        return repo.loadChatDocs();
    }

    public void setReferenceChats(List<String> chatIds) {
        selectedReferenceChatIds.clear();
        if (chatIds != null) {
            selectedReferenceChatIds.addAll(chatIds);
        }
        if (activeChatId != null) {
            repo.setReferencedChats(activeChatId, selectedReferenceChatIds);
        }
    }

    private void listenToMessagesInternal(String chatId) {
        detachListener();
        if (chatId == null) {
            replaceWithFirestoreSnapshot(new ArrayList<>());
            return;
        }

        msgListener = repo.listenToMessages(chatId, (snapshot, e) -> {
            if (e != null || snapshot == null) {
                return;
            }
            List<UiMessage> ui = new ArrayList<>();
            for (DocumentSnapshot d : snapshot.getDocuments()) {
                String role = d.getString("role");
                String content = d.getString("content");
                String tsIso = d.getString("timestamp");
                if (role != null && content != null) {
                    long ts = isoToMillis(tsIso);
                    ui.add(new UiMessage(role, content, ts));
                }
            }
            replaceWithFirestoreSnapshot(ui);
        });
    }

    private void detachListener() {
        if (msgListener != null) {
            msgListener.remove();
            msgListener = null;
        }
    }

    public void sendUserMessage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        if (activeChatId == null) {
            error.postValue("No active chat. Tap 'New' to start a conversation.");
            return;
        }

        addLocalMessage("user", text);
        String isoTs = isoNow();
        repo.addUserMessage(activeChatId, text, isoTs);

        loading.postValue(true);
        loadDataThenRespond(text);
    }

    public void sendMessage(String text, AppDate appDate) {
        setCurrentAppDate(appDate);
        sendUserMessage(text);
    }

    public void addMemoryNote(String note) {
        if (note == null || note.trim().isEmpty() || activeChatId == null) {
            return;
        }
        addLocalMessage("user", note);
        String isoTs = isoNow();
        repo.addUserMessage(activeChatId, note, isoTs);
    }

    public void listenToMessages(String chatId) {
        openExistingChat(chatId);
    }

    private void loadDataThenRespond(String userText) {
        Task<QuerySnapshot> expT = repo.loadExpenses();
        Task<QuerySnapshot> budT = repo.loadBudgets();

        Tasks.whenAllSuccess(expT, budT)
                .addOnSuccessListener(results -> {
                    List<Expense> expenses =
                            expT.getResult() != null
                                    ? expT.getResult().toObjects(Expense.class)
                                    : new ArrayList<>();
                    List<Budget> budgets =
                            budT.getResult() != null
                                    ? budT.getResult().toObjects(Budget.class)
                                    : new ArrayList<>();

                    FinancialInsightsEngine.InsightResult ir =
                            engine.tryHandle(userText, expenses, budgets);

                    String promptToAI = ir.getHandled() ? ir.getAiFollowupPrompt() : userText;

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
        if (selectedReferenceChatIds.isEmpty()) {
            return Tasks.forResult(prompt);
        }

        List<Task<String>> summaryTasks = new ArrayList<>();
        for (String id : selectedReferenceChatIds) {
            summaryTasks.add(repo.getSummary(id));
        }

        return Tasks.whenAllSuccess(summaryTasks).continueWith(t -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Context from previous chats:\n");
            for (Object s : t.getResult()) {
                if (s != null) {
                    sb.append("- ").append(s).append("\n");
                }
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
                    "You are SpendWise, a concise financial advisor. "
                            + "Only use numeric facts provided from the user's real data. "
                            + "Give practical, actionable tips.");
            arr.put(sys);

            List<UiMessage> ui = messages.getValue();
            if (ui != null) {
                int start = Math.max(0, ui.size() - 20);
                for (int i = start; i < ui.size(); i++) {
                    UiMessage m = ui.get(i);
                    JSONObject o = new JSONObject();
                    boolean isAssistant =
                            m.role != null && m.role.startsWith(ASSISTANT);
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
                if (token == null) {
                    return;
                }
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

                String isoTs = isoNow();
                repo.addAssistantMessage(activeChatId, fullReply, isoTs);

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

                String isoTs = isoNow();
                repo.addAssistantMessage(activeChatId,
                        "I couldn’t reach the AI right now.",
                        isoTs);
            }
        });
    }

    private void generateTitle(String firstPrompt) {
        try {
            JSONArray arr = new JSONArray();
            arr.put(new JSONObject()
                    .put("role", "user")
                    .put(CONTENT,
                            "Create a short 2-5 word title for this financial chat, "
                                    + "based on this first user message:\n" + firstPrompt));

            ollama.chat(arr, new OllamaClient.ChatCallback() {
                @Override
                public void onSuccess(String reply) {
                    String title = (reply == null || reply.trim().isEmpty())
                            ? "Chat"
                            : reply.trim();
                    repo.updateChatTitle(activeChatId, title);
                }

                @Override
                public void onError(String errorMsg) {
                }
            });

        } catch (Exception ignored) {
        }
    }

    private void generateAndStoreSummary() {
        List<UiMessage> ui = messages.getValue();
        if (ui == null || ui.isEmpty() || activeChatId == null) {
            return;
        }

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
                            "Summarize this SpendWise conversation in 1-2 factual sentences "
                                    + "so it can be used as memory:\n" + convo));

            ollama.chat(arr, new OllamaClient.ChatCallback() {
                @Override
                public void onSuccess(String reply) {
                    if (reply != null && !reply.trim().isEmpty()) {
                        repo.updateChatSummary(activeChatId, reply.trim());
                    }
                }

                @Override
                public void onError(String errorMsg) {
                }
            });

        } catch (Exception ignored) {
        }
    }

    public void refreshAllUntitledChatTitles() {
        String uid = FirestoreManager.getInstance().getCurrentUserId();
        if (uid == null) {
            return;
        }

        FirestoreManager.getInstance()
                .userChatsReference(uid)
                .get()
                .addOnSuccessListener(qs -> {
                    if (qs == null || qs.isEmpty()) {
                        return;
                    }

                    for (DocumentSnapshot chatDoc : qs.getDocuments()) {
                        String chatId = chatDoc.getId();
                        String title = chatDoc.getString("title");

                        if (!needsTitle(title)) {
                            continue;
                        }

                        FirestoreManager.getInstance()
                                .chatMessagesReference(uid, chatId)
                                .orderBy("timestamp")
                                .limit(1)
                                .get()
                                .addOnSuccessListener(msgSnap -> {
                                    if (msgSnap == null || msgSnap.isEmpty()) {
                                        return;
                                    }

                                    DocumentSnapshot first = msgSnap.getDocuments().get(0);
                                    String content = first.getString(CONTENT);
                                    if (content == null || content.trim().isEmpty()) {
                                        return;
                                    }

                                    generateTitleForSpecificChat(chatId, content);
                                });
                    }
                });
    }

    private boolean needsTitle(String title) {
        if (title == null) {
            return true;
        }
        String t = title.trim().toLowerCase(Locale.US);
        if (t.isEmpty()) {
            return true;
        }
        return t.equals("new chat") || t.equals("chat");
    }

    private void generateTitleForSpecificChat(String chatId, String firstMessage) {
        try {
            JSONArray arr = new JSONArray();
            arr.put(new JSONObject()
                    .put("role", "user")
                    .put(CONTENT,
                            "Create a short 2-5 word title for this financial conversation. "
                                    + "Do NOT include quotes, just the raw title:\n" + firstMessage));

            ollama.chat(arr, new OllamaClient.ChatCallback() {
                @Override
                public void onSuccess(String reply) {
                    if (reply == null) {
                        return;
                    }
                    String title = reply.trim();
                    if (title.isEmpty()) {
                        return;
                    }
                    repo.updateChatTitle(chatId, title);
                }

                @Override
                public void onError(String errorMsg) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        detachListener();
        ollama.cancelActive();
    }
}
