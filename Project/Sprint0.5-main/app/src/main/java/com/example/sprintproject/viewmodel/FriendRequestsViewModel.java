package com.example.sprintproject.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendRequestsViewModel extends ViewModel {
    private final MutableLiveData<List<Map<String, Object>>> friendRequestsLiveData
            = new MutableLiveData<>();
    private final MutableLiveData<List<Map<String, Object>>> friendsLiveData
            = new MutableLiveData<>();
    private ListenerRegistration requestsListener;
    private ListenerRegistration friendsListener;
    private static final String USERS = "users";
    private static final String FRIENDS = "friends";
    private static final String FIRESTORE = "Firestore";

    public LiveData<List<Map<String, Object>>> getFriendRequests() {
        return friendRequestsLiveData;
    }

    public LiveData<List<Map<String, Object>>> getFriends() {
        return friendsLiveData;
    }

    public void startListeningForRequests() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            return;
        }
        String uid = auth.getCurrentUser().getUid();

        stopListeningForRequests();

        requestsListener = FirestoreManager.getInstance()
                .friendRequests(uid)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) {
                        return;
                    }
                    List<Map<String, Object>> requests = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            String status = (String) data.get("status");
                            if (status != null && !status.equals("pending")) {
                                continue;
                            }
                            data.put("id", doc.getId());
                            requests.add(data);
                        }
                    }
                    friendRequestsLiveData.postValue(requests);
                });
    }

    public void stopListeningForRequests() {
        if (requestsListener != null) {
            requestsListener.remove();
            requestsListener = null;
        }
    }

    public void response(String requestId, boolean accept) {
        if (accept) {
            FirestoreManager.getInstance().approveFriendRequest(requestId);
        } else {
            FirestoreManager.getInstance().declineFriendRequest(requestId);
        }
    }

    public void startListeningForFriends() {
        String userId = FirestoreManager.getInstance().getCurrentUserId();
        if (userId == null) {
            return;
        }

        stopListeningForFriends();

        friendsListener = FirebaseFirestore.getInstance().collection(USERS)
                .document(userId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) {
                        return;
                    }
                    List<Map<String, Object>> friends = (List<Map<String, Object>>) snapshot.get(FRIENDS);
                    if (friends == null) {
                        friends = new ArrayList<>();
                    }
                    friendsLiveData.postValue(friends);
                });
    }

    public void stopListeningForFriends() {
        if (friendsListener != null) {
            friendsListener.remove();
            friendsListener = null;
        }
    }

    /**
     * Refactored to reduce cognitive complexity:
     * now delegates to a reusable helper for each side of the friendship.
     */
    public void removeFriend(String friendId) {
        String currentUid = FirestoreManager.getInstance().getCurrentUserId();
        if (currentUid == null || friendId == null) {
            return;
        }

        // Remove the friend from the current user's friends list
        removeFriendRelation(currentUid, friendId);

        // Remove the current user from the friend's friends list
        removeFriendRelation(friendId, currentUid);
    }

    /**
     * Helper that removes the friend entry with targetUid
     * from the FRIENDS array of the ownerUid document.
     */
    private void removeFriendRelation(String ownerUid, String targetUid) {
        if (ownerUid == null || targetUid == null) {
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(USERS).document(ownerUid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || !snapshot.exists()) {
                        return;
                    }

                    List<Map<String, Object>> friends =
                            (List<Map<String, Object>>) snapshot.get(FRIENDS);
                    if (friends == null || friends.isEmpty()) {
                        return;
                    }

                    Map<String, Object> friendToRemove = findFriendByUid(friends, targetUid);
                    if (friendToRemove == null) {
                        return;
                    }

                    db.collection(USERS).document(ownerUid)
                            .update(FRIENDS, FieldValue.arrayRemove(friendToRemove));
                });
    }

    /**
     * Finds the friend map whose "uid" equals the given uid.
     */
    private Map<String, Object> findFriendByUid(List<Map<String, Object>> friends, String uid) {
        if (friends == null || uid == null) {
            return null;
        }
        for (Map<String, Object> friend : friends) {
            if (uid.equals(friend.get("uid"))) {
                return friend;
            }
        }
        return null;
    }

    public void acceptFriendRequest(String requestId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("friendRequests").document(requestId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        Log.e(FIRESTORE, "Friend request not found: " + requestId);
                        return;
                    }

                    String requesterUid = document.getString("requesterUid");
                    String approverUid = document.getString("approverUid");
                    String requesterEmail = document.getString("requesterEmail");
                    String approverEmail = document.getString("approverEmail");

                    if (requesterUid == null || approverUid == null) {
                        Log.e(FIRESTORE, "Invalid friend request: " + requestId);
                        return;
                    }

                    db.collection("friendRequests").document(requestId)
                            .update("status", "accepted")
                            .addOnSuccessListener(e -> {
                                Log.d(FIRESTORE, "Friend request accepted: " + requestId);

                                Map<String, Object> requesterData = new HashMap<>();
                                requesterData.put("uid", approverUid);
                                requesterData.put("email", approverEmail);

                                Map<String, Object> approverData = new HashMap<>();
                                approverData.put("uid", requesterUid);
                                approverData.put("email", requesterEmail);

                                db.collection(USERS).document(requesterUid)
                                        .update(FRIENDS, FieldValue.arrayUnion(requesterData));

                                db.collection(USERS).document(approverUid)
                                        .update(FRIENDS, FieldValue.arrayUnion(approverData));
                            })
                            .addOnFailureListener(e ->
                                    Log.e(FIRESTORE, "Failed to accept friend request", e)
                            );
                })
                .addOnFailureListener(e ->
                        Log.e(FIRESTORE, "Failed to get friend request", e)
                );
    }

    public void sendFriendRequest(String approverEmail) {
        String requesterUid = FirestoreManager.getInstance().getCurrentUserId();
        String requesterEmail = FirestoreManager.getInstance().getCurrentUserEmail();

        if (requesterUid == null || requesterEmail == null) {
            return;
        }

        Log.d(FIRESTORE, "Sending friend request to: " + approverEmail);

        FirestoreManager.getInstance().searchByEmail(approverEmail).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        Log.d(FIRESTORE, "No user found with that email: " + approverEmail);
                        return;
                    }
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        String approverUid = document.getId();
                        FirestoreManager.getInstance().sendFriendRequest(
                                requesterUid,
                                approverUid,
                                requesterEmail,
                                approverEmail
                        );
                        Log.d(FIRESTORE, "Friend request created for: " + approverEmail);
                    }
                })
                .addOnFailureListener(e ->
                        Log.d(FIRESTORE, "Failed to search for user by email", e)
                );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopListeningForRequests();
        stopListeningForFriends();
    }
}
