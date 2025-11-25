package com.example.sprintproject.repository;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatRepository {

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public ChatRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }


    private String requireUid() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("User not logged in");
        }
        return user.getUid();
    }

    private CollectionReference chatsCollection() {
        return db.collection("users")
                .document(requireUid())
                .collection("chats");
    }

    private CollectionReference expensesCollection() {
        return db.collection("users")
                .document(requireUid())
                .collection("expenses");
    }

    private CollectionReference budgetsCollection() {
        return db.collection("users")
                .document(requireUid())
                .collection("budgets");
    }

    public static class ChatDoc {
        public final String id;
        public final String title;
        public final String summary;

        public ChatDoc(String id, String title, String summary) {
            this.id = id;
            this.title = title;
            this.summary = summary;
        }
    }

    public Task<DocumentReference> createChatSkeleton() {
        try {
            CollectionReference chats = chatsCollection();
            DocumentReference ref = chats.document();

            long now = System.currentTimeMillis();

            Map<String, Object> data = new HashMap<>();
            data.put("title", "New chat");
            data.put("summary", "");
            data.put("createdAt", now);
            data.put("updatedAt", now);
            data.put("referencedChatIds", new ArrayList<String>());

            return ref.set(data).continueWith(t -> ref);
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    public Task<List<ChatDoc>> loadChatDocs() {
        try {
            return chatsCollection()
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .continueWith(t -> {
                        List<ChatDoc> out = new ArrayList<>();
                        if (!t.isSuccessful() || t.getResult() == null) {
                            return out;
                        }
                        for (DocumentSnapshot d : t.getResult().getDocuments()) {
                            String id = d.getId();
                            String title = d.getString("title");
                            String summary = d.getString("summary");
                            if (title == null || title.trim().isEmpty()) title = "Chat";
                            if (summary == null) summary = "";
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
            update.put("updatedAt", System.currentTimeMillis());

            chatsCollection().document(chatId).update(update);
        } catch (IllegalStateException ignored) { }
    }

    public void addMessage(String chatId, String role, String content) {
        try {
            DocumentReference chatRef = chatsCollection().document(chatId);

            Map<String, Object> msg = new HashMap<>();
            msg.put("role", role);
            msg.put("content", content);
            msg.put("timestamp", System.currentTimeMillis());

            chatRef.collection("messages").add(msg);
            chatRef.update("updatedAt", System.currentTimeMillis());
        } catch (IllegalStateException ignored) { }
    }

    public ListenerRegistration listenMessages(
            String chatId,
            EventListener<QuerySnapshot> listener
    ) {
        try {
            return chatsCollection()
                    .document(chatId)
                    .collection("messages")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .addSnapshotListener(listener);
        } catch (IllegalStateException e) {
            return null;
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


    public Task<String> getSummary(String chatId) {
        try {
            return chatsCollection()
                    .document(chatId)
                    .get()
                    .continueWith(t -> {
                        DocumentSnapshot d = t.getResult();
                        if (d == null) return "";
                        String s = d.getString("summary");
                        return s == null ? "" : s;
                    });
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    public void updateChatTitle(String chatId, String title) {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("title", title);
            update.put("updatedAt", System.currentTimeMillis());
            chatsCollection().document(chatId).update(update);
        } catch (IllegalStateException ignored) { }
    }

    public void updateChatSummary(String chatId, String summary) {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("summary", summary);
            update.put("updatedAt", System.currentTimeMillis());
            chatsCollection().document(chatId).update(update);
        } catch (IllegalStateException ignored) { }
    }
}
