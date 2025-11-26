// This repo manages the chat-related firestore operations such as
// updating any meta data and listening to any real time messages changes.
// Also provides chat logs with the users specific budgets and expenses.

package com.example.sprintproject.repository;

import androidx.annotation.Nullable;

import com.example.sprintproject.viewmodel.FirestoreManager;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatRepository {

    private static final String USERS = "users";
    private static final String TITLE = "title";
    private static final String SUMMARY = "summary";
    private static final String UPDATED_AT = "updatedAt";
    private static final String USER_NOT_LOGGED_IN = "User not logged in";
    private static final String CREATED_AT = "createdAt";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public ChatRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    private String requireUid() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            throw new IllegalStateException(USER_NOT_LOGGED_IN);
        }
        return user.getUid();
    }

    private @Nullable String getUid() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    private CollectionReference chatsCollection() {
        return db.collection(USERS)
                .document(requireUid())
                .collection("chats");
    }

    private CollectionReference expensesCollection() {
        return db.collection(USERS)
                .document(requireUid())
                .collection("expenses");
    }

    private CollectionReference budgetsCollection() {
        return db.collection(USERS)
                .document(requireUid())
                .collection("budgets");
    }

    public Task<String> createNewChat(String isoTimestamp) {
        String uid = getUid();
        if (uid == null) {
            TaskCompletionSource<String> tcs = new TaskCompletionSource<>();
            tcs.setException(new IllegalStateException(USER_NOT_LOGGED_IN));
            return tcs.getTask();
        }

        DocumentReference chatRef = FirestoreManager.getInstance()
                .userChatsReference(uid)
                .document();

        String chatId = chatRef.getId();

        Map<String, Object> data = new HashMap<>();
        data.put(TITLE, "New Chat");
        data.put(CREATED_AT, isoTimestamp);
        data.put(UPDATED_AT, isoTimestamp);

        return chatRef.set(data).continueWith(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return chatId;
        });
    }

    public Task<List<ChatDoc>> loadChatDocs() {
        try {
            return chatsCollection()
                    .orderBy(CREATED_AT, Query.Direction.DESCENDING)
                    .get()
                    .continueWith(t -> {
                        List<ChatDoc> out = new ArrayList<>();
                        if (!t.isSuccessful() || t.getResult() == null) {
                            return out;
                        }
                        for (DocumentSnapshot d : t.getResult().getDocuments()) {
                            String id = d.getId();
                            String title = d.getString(TITLE);
                            String summary = d.getString(SUMMARY);
                            if (title == null || title.trim().isEmpty()) {
                                title = "Chat";
                            }
                            if (summary == null) {
                                summary = "";
                            }
                            out.add(new ChatDoc(id, title, summary));
                        }
                        return out;
                    });
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    public void setReferencedChats(String chatId, List<String> referencedIds) {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("referencedChatIds",
                    referencedIds == null ? new ArrayList<>() : referencedIds);
            update.put(UPDATED_AT, System.currentTimeMillis());
            chatsCollection().document(chatId).update(update);
        } catch (IllegalStateException ignored) {
            // intentionally left blank - meant to ignore
        }
    }

    public Task<QuerySnapshot> loadExpenses() {
        try {
            return expensesCollection().get();
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    public Task<QuerySnapshot> loadBudgets() {
        try {
            return budgetsCollection().get();
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    public Task<Void> addUserMessage(String chatId, String content, String isoTimestamp) {
        return addMessage(chatId, content, "user", isoTimestamp);
    }

    public Task<Void> addAssistantMessage(String chatId, String content, String isoTimestamp) {
        return addMessage(chatId, content, "assistant", isoTimestamp);
    }

    private Task<Void> addMessage(String chatId,
                                  String content,
                                  String role,
                                  String isoTimestamp) {

        String uid = getUid();
        if (uid == null) {
            return Tasks.forException(new IllegalStateException(USER_NOT_LOGGED_IN));
        }

        CollectionReference msgRef = FirestoreManager.getInstance()
                .chatMessagesReference(uid, chatId);

        DocumentReference msgDoc = msgRef.document();

        Map<String, Object> data = new HashMap<>();
        data.put("role", role);
        data.put("content", content);
        data.put("timestamp", isoTimestamp);
        data.put(CREATED_AT, System.currentTimeMillis());

        return msgDoc.set(data);
    }

    public ListenerRegistration listenToMessages(
            String chatId,
            EventListener<QuerySnapshot> listener
    ) {
        String uid = getUid();
        if (uid == null) {
            return null;
        }

        return FirestoreManager.getInstance()
                .chatMessagesReference(uid, chatId)
                .orderBy(CREATED_AT, Query.Direction.ASCENDING)
                .addSnapshotListener(listener);
    }

    public Task<Void> saveChatTitle(String chatId, String title, String isoTimestamp) {
        String uid = getUid();
        if (uid == null) {
            return Tasks.forException(new IllegalStateException(USER_NOT_LOGGED_IN));
        }

        Map<String, Object> update = new HashMap<>();
        update.put(TITLE, title);
        update.put(UPDATED_AT, isoTimestamp);

        return FirestoreManager.getInstance()
                .userChatDoc(uid, chatId)
                .update(update);
    }

    public Task<String> getSummary(String chatId) {
        try {
            return chatsCollection()
                    .document(chatId)
                    .get()
                    .continueWith(t -> {
                        DocumentSnapshot d = t.getResult();
                        if (d == null) {
                            return "";
                        }
                        String s = d.getString(SUMMARY);
                        return s == null ? "" : s;
                    });
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    public void updateChatTitle(String chatId, String title) {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put(TITLE, title);
            update.put(UPDATED_AT, System.currentTimeMillis());
            chatsCollection().document(chatId).update(update);
        } catch (IllegalStateException ignored) {
            // intentionally left blank - meant to ignore
        }
    }

    public Task<Void> saveChatSummary(String chatId,
                                      String summary,
                                      String isoTimestamp) {
        String uid = getUid();
        if (uid == null) {
            return Tasks.forException(new IllegalStateException(USER_NOT_LOGGED_IN));
        }

        Map<String, Object> update = new HashMap<>();
        update.put(SUMMARY, summary);
        update.put(UPDATED_AT, isoTimestamp);

        return FirestoreManager.getInstance()
                .userChatDoc(uid, chatId)
                .update(update);
    }

    public ListenerRegistration listenToChats(
            EventListener<QuerySnapshot> listener
    ) {
        String uid = getUid();
        if (uid == null) {
            return null;
        }

        return FirestoreManager.getInstance()
                .userChatsReference(uid)
                .orderBy(UPDATED_AT, Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
    }

    public Task<String> fetchChatSummary(String chatId) {
        String uid = getUid();
        if (uid == null) {
            TaskCompletionSource<String> tcs = new TaskCompletionSource<>();
            tcs.setException(new IllegalStateException(USER_NOT_LOGGED_IN));
            return tcs.getTask();
        }

        return FirestoreManager.getInstance()
                .userChatDoc(uid, chatId)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    DocumentSnapshot snap = task.getResult();
                    return snap != null && snap.contains(SUMMARY)
                            ? snap.getString(SUMMARY)
                            : null;
                });
    }

    public void updateChatSummary(String chatId, String summary) {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put(SUMMARY, summary);
            update.put(UPDATED_AT, System.currentTimeMillis());
            chatsCollection().document(chatId).update(update);
        } catch (IllegalStateException ignored) {
            // intentionally left blank - meant to ignore
        }
    }

    public static class ChatDoc {
        private final String id;
        private final String title;
        private final String summary;

        public ChatDoc(String id, String title, String summary) {
            this.id = id;
            this.title = title;
            this.summary = summary;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getSummary() {
            return summary;
        }
    }
}
