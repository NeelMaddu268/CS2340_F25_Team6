package com.example.sprintproject.viewmodel;

import android.util.Log;

import com.example.sprintproject.model.Budget;
import com.example.sprintproject.model.Expense;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FirestoreManager {

    private final FirebaseFirestore db;

    // Collection name constants (avoid magic strings)
    private static final String SAVINGS_CIRCLE_STRING = "savingsCircles";
    private static final String USERS_STRING = "users";
    private static final String BUDGETS_STRING = "budgets";
    private static final String EXPENSES_STRING = "expenses";
    private static final String CATEGORIES_STRING = "categories";
    private static final String POINTERS_STRING = "savingsCirclePointers";
    private static final String INVITATIONS_STRING = "invitations";
    private static final String FRIENDS_STRING = "friends";
    private static final String FRIEND_REQUESTS_STRING = "friendRequests";

    private FirestoreManager() {
        db = FirebaseFirestore.getInstance();
    }

    /** Singleton pattern with lazy initialization */
    private static class Holder {
        private static final FirestoreManager INSTANCE = new FirestoreManager();
    }

    public static FirestoreManager getInstance() {
        return Holder.INSTANCE;
    }

    public FirebaseFirestore getDb() {
        return db;
    }


    public CollectionReference savingsCircleReference(String uid) {
        return db.collection(USERS_STRING).document(uid).collection(SAVINGS_CIRCLE_STRING);
    }

    public CollectionReference budgetsReference(String uid) {
        return db.collection(USERS_STRING).document(uid).collection(BUDGETS_STRING);
    }

    public CollectionReference expensesReference(String uid) {
        return db.collection(USERS_STRING).document(uid).collection(EXPENSES_STRING);
    }

    public CollectionReference categoriesReference(String uid) {
        return db.collection(USERS_STRING).document(uid).collection(CATEGORIES_STRING);
    }

    public CollectionReference savingsCirclesGlobalReference() {
        return db.collection(SAVINGS_CIRCLE_STRING);
    }

    public DocumentReference savingsCircleDoc(String circleId) {
        return db.collection(SAVINGS_CIRCLE_STRING).document(circleId);
    }

    public CollectionReference userSavingsCirclePointers(String uid) {
        return db.collection(USERS_STRING).document(uid).collection(POINTERS_STRING);
    }

    public CollectionReference invitationsReference() {
        return db.collection(INVITATIONS_STRING);
    }

    public CollectionReference friendRequestsReference() {
        return db.collection(FRIEND_REQUESTS_STRING);
    }

    public CollectionReference friendsReference(String uid) {
        return db.collection(USERS_STRING).document(uid).collection(FRIENDS_STRING);
    }

    public void addUser(String uid, Map<String, Object> userData) {
        db.collection(USERS_STRING).document(uid).set(userData);
    }

    public Query invitationsForUser(String uid) {
        return invitationsReference().whereEqualTo("toUid", uid);
    }

    public String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return (user != null) ? user.getUid() : null;
    }

    public String getCurrentUserEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return user.getEmail();
        }
        return null;
    }

    /**
     * Deletes a savings circle and related data:
     * - circle doc
     * - all invitations for the circle
     * - all users' pointer docs referencing the circle
     *
     * Only creator can delete.
     */
    public Task<Void> deleteSavingsCircle(String circleId, String requesterUid) {
        DocumentReference circleRef = savingsCircleDoc(circleId);
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();

        circleRef.get()
                .addOnSuccessListener(snapshot -> {
                    if (!validateCircleAndPermission(snapshot, requesterUid, tcs)) return;

                    String creatorId = snapshot.getString("creatorId");
                    Set<String> allUids = collectAllUids(snapshot, creatorId);

                    List<Task<QuerySnapshot>> fetches = buildFetchTasks(circleId, allUids);

                    Tasks.whenAllSuccess(fetches)
                            .addOnSuccessListener(results -> commitDeleteBatch(circleRef, results, tcs))
                            .addOnFailureListener(tcs::setException);
                })
                .addOnFailureListener(tcs::setException);

        return tcs.getTask();
    }

    private boolean validateCircleAndPermission(
            DocumentSnapshot snapshot,
            String requesterUid,
            TaskCompletionSource<Void> tcs
    ) {
        if (snapshot == null || !snapshot.exists()) {
            tcs.setException(new IllegalStateException("Circle not found"));
            return false;
        }

        String creatorId = snapshot.getString("creatorId");
        if (creatorId == null || !creatorId.equals(requesterUid)) {
            tcs.setException(new SecurityException("Only the creator can delete this circle"));
            return false;
        }

        return true;
    }

    private Set<String> collectAllUids(DocumentSnapshot snapshot, String creatorId) {
        Set<String> allUids = new HashSet<>();
        allUids.add(creatorId);

        List<String> memberIds = (List<String>) snapshot.get("memberIds");
        if (memberIds != null) {
            allUids.addAll(memberIds);
        }

        return allUids;
    }

    private List<Task<QuerySnapshot>> buildFetchTasks(String circleId, Set<String> allUids) {
        List<Task<QuerySnapshot>> fetches = new ArrayList<>();

        // invitations
        fetches.add(
                invitationsReference()
                        .whereEqualTo("circleId", circleId)
                        .get()
        );

        // each user's pointers
        for (String uid : allUids) {
            fetches.add(fetchUserPointers(uid, circleId));
        }

        return fetches;
    }

    private Task<QuerySnapshot> fetchUserPointers(String uid, String circleId) {
        return userSavingsCirclePointers(uid)
                .whereEqualTo("circleId", circleId)
                .get();
    }

    private void commitDeleteBatch(
            DocumentReference circleRef,
            List<Object> results,
            TaskCompletionSource<Void> tcs
    ) {
        WriteBatch batch = db.batch();

        batch.delete(circleRef);

        deleteInvitations(batch, results);
        deleteUserPointers(batch, results);

        batch.commit()
                .addOnSuccessListener(v -> tcs.setResult(null))
                .addOnFailureListener(tcs::setException);
    }

    private void deleteInvitations(WriteBatch batch, List<Object> results) {
        QuerySnapshot invitesSnap = (QuerySnapshot) results.get(0);
        for (DocumentSnapshot d : invitesSnap.getDocuments()) {
            batch.delete(d.getReference());
        }
    }

    private void deleteUserPointers(WriteBatch batch, List<Object> results) {
        for (int i = 1; i < results.size(); i++) {
            QuerySnapshot snap = (QuerySnapshot) results.get(i);
            for (DocumentSnapshot d : snap.getDocuments()) {
                batch.delete(d.getReference());
            }
        }
    }

    public void addBudget(String uid, Budget budget) {
        budgetsReference(uid).add(budget);
    }

    public void addExpense(String uid, Expense expense) {
        expensesReference(uid).add(expense);
    }

    public void incrementField(String uid, String fieldName) {
        db.collection(USERS_STRING)
                .document(uid)
                .update(fieldName, FieldValue.increment(1));
    }

   public Query friendRequests(String uid) {
       return friendRequestsReference().whereEqualTo("approverUid", uid);
   }

    public Query searchByEmail(String email) {
        return db.collection(USERS_STRING).whereEqualTo("email", email);
    }

    public void sendFriendRequest(String requesterUid, String approverUid, String requesterEmail, String approverEmail) {
        Map<String, Object> request = new HashMap<>();
        request.put("requesterUid", requesterUid);
        request.put("approverUid", approverUid);
        request.put("requesterEmail", requesterEmail);
        request.put("approverEmail", approverEmail);
        request.put("status", "pending");

        FirebaseFirestore.getInstance()
                .collection("friendRequests")
                .add(request)
                .addOnSuccessListener(documentReference -> {
                    Log.d("Firestore", "Friend request sent: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to send friend request", e);
                });
    }

    public void approveFriendRequest(String requestId) {
       DocumentReference request = friendRequestsReference().document(requestId);
       TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();

       request.get().addOnSuccessListener(snapshot -> {
           if (snapshot == null || !snapshot.exists()) {
               tcs.setException(new IllegalStateException("Request not found"));
               return;
           }

           String requesterUid = snapshot.getString("requesterUid");
           String approverUid = snapshot.getString("approverUid");
           String requesterEmail = snapshot.getString("requesterEmail");
           String approverEmail = snapshot.getString("approverEmail");

           if (requesterUid == null || approverUid == null) {
               tcs.setException(new IllegalStateException("Invalid request"));
               return;
           }

           WriteBatch batch = db.batch();

           Map<String, Object> friend1 = new HashMap<>();
           friend1.put("uid", approverUid);
           friend1.put("email", approverEmail);

           Map<String, Object> friend2 = new HashMap<>();
           friend2.put("uid", requesterUid);
           friend2.put("email", requesterEmail);

           batch.set(friendsReference(requesterUid).document(approverUid), friend1);
           batch.set(friendsReference(approverUid).document(requesterUid), friend2);

           batch.delete(request);

           batch.commit()
                   .addOnSuccessListener(v -> tcs.setResult(null))
                   .addOnFailureListener(tcs::setException);
       }).addOnFailureListener(tcs::setException);
    }

    public void declineFriendRequest(String requestId) {
        DocumentReference request = friendRequestsReference().document(requestId);
        WriteBatch batch = db.batch();
        batch.delete(request);
        batch.commit();
    }

    public void removeFriend(String uid1, String uid2) {
        WriteBatch batch = db.batch();
        batch.delete(friendsReference(uid1).document(uid2));
        batch.delete(friendsReference(uid2).document(uid1));

        List<Task<QuerySnapshot>> fetches = new ArrayList<>();
        fetches.add(friendRequestsReference()
                .whereEqualTo("requesterUid", uid1)
                .whereEqualTo("approverUid", uid2)
                .get());
        fetches.add(friendRequestsReference()
                .whereEqualTo("requesterUid", uid2)
                .whereEqualTo("approverUid", uid1)
                .get());

        Tasks.whenAllSuccess(fetches)
                .addOnSuccessListener(results -> {
                    for (Object object : results) {
                        QuerySnapshot snap = (QuerySnapshot) object;
                        for (DocumentSnapshot ds : snap.getDocuments()) {
                            batch.delete(ds.getReference());
                        }
                    }
                    batch.commit();
                });
    }
}
