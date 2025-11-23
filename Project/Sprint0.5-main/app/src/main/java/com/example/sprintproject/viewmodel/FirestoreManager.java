package com.example.sprintproject.viewmodel;

import com.example.sprintproject.model.Budget;
import com.example.sprintproject.model.Expense;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FirestoreManager {
    private static volatile FirestoreManager instance;
    private final FirebaseFirestore db;

    private FirestoreManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirestoreManager getInstance() {
        if (instance == null) {
            synchronized (FirestoreManager.class) {
                if (instance == null) {
                    instance = new FirestoreManager();
                }
            }
        }
        return instance;
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    private static final String SAVINGS_CIRCLE_STRING = "savingsCircles";
    private static final String USERS_STRING = "users";

    public CollectionReference savingsCircleReference(String uid) {
        return db.collection(USERS_STRING).document(uid).collection(SAVINGS_CIRCLE_STRING);
    }

    public CollectionReference budgetsReference(String uid) {
        return db.collection(USERS_STRING).document(uid).collection("budgets");
    }

    public CollectionReference expensesReference(String uid) {
        return db.collection(USERS_STRING).document(uid).collection("expenses");
    }

    public CollectionReference categoriesReference(String uid) {
        return db.collection(USERS_STRING).document(uid).collection("categories");
    }

    public CollectionReference savingsCirclesGlobalReference() {
        return db.collection(SAVINGS_CIRCLE_STRING);
    }

    public DocumentReference savingsCircleDoc(String circleId) {
        return db.collection(SAVINGS_CIRCLE_STRING).document(circleId);
    }

    public CollectionReference userSavingsCirclePointers(String uid) {
        return db.collection(USERS_STRING).document(uid).collection("savingsCirclePointers");
    }

    public CollectionReference invitationsReference() {
        return db.collection("invitations");
    }

    public void addUser(String uid, Map<String, Object> userData) {
        db.collection(USERS_STRING).document(uid).set(userData);
    }

    public Query invitationsForUser(String uid) {
        return invitationsReference().whereEqualTo("toUid", uid);
    }

    public String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return user.getUid();
        } else {
            return null;
        }
    }

    public String getCurrentUserEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return user.getEmail();
        }
        throw new IllegalStateException("User not logged in");
    }

    public Task<Void> deleteSavingsCircle(String circleId, String requesterUid) {
        DocumentReference circleRef = db.collection(SAVINGS_CIRCLE_STRING).document(circleId);

        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();

        circleRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                tcs.setException(new IllegalStateException("Circle not found"));
                return;
            }

            String creatorId = snapshot.getString("creatorId");
            if (creatorId == null || !creatorId.equals(requesterUid)) {
                tcs.setException(new SecurityException("Only the creator can delete this circle"));
                return;
            }

            List<String> memberIds = (List<String>) snapshot.get("memberIds");
            Set<String> allUids = new HashSet<>();
            allUids.add(creatorId);
            if (memberIds != null) {
                allUids.addAll(memberIds);
            }

            // Fetch invites and all pointer docs first
            List<Task<QuerySnapshot>> fetches = new ArrayList<>();
            Task<QuerySnapshot> invitesTask = db.collection("invitations")
                    .whereEqualTo("circleId", circleId)
                    .get();
            fetches.add(invitesTask);

            for (String uid : allUids) {
                fetches.add(
                        db.collection(USERS_STRING)
                                .document(uid)
                                .collection("savingsCirclePointers")
                                .whereEqualTo("circleId", circleId)
                                .get()
                );
            }

            Tasks.whenAllSuccess(fetches).addOnSuccessListener(results -> {
                WriteBatch batch = db.batch();

                // delete the circle doc itself
                batch.delete(circleRef);

                int idx = 0;
                // invitations
                QuerySnapshot invitesSnap = (QuerySnapshot) results.get(idx++);
                for (DocumentSnapshot d : invitesSnap.getDocuments()) {
                    batch.delete(d.getReference());
                }

                // user pointers
                for (; idx < results.size(); idx++) {
                    QuerySnapshot snap = (QuerySnapshot) results.get(idx);
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        batch.delete(d.getReference());
                    }
                }

                batch.commit()
                        .addOnSuccessListener(aVoid -> tcs.setResult(null))
                        .addOnFailureListener(tcs::setException);

            }).addOnFailureListener(tcs::setException);

        }).addOnFailureListener(tcs::setException);

        return tcs.getTask();
    }

    public void addBudget(String uid, Budget budget) {
        budgetsReference(uid).add(budget);
    }

    public void addExpense(String uid, Expense expense) {
        expensesReference(uid).add(expense);
    }
}