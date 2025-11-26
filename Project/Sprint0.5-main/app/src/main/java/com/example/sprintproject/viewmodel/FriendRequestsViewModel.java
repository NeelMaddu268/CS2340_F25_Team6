package com.example.sprintproject.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.model.AppDate;
import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
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

        friendsListener = FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .addSnapshotListener((snapshot, e) -> {
                    List<Map<String, Object>> friends = (List<Map<String, Object>>) snapshot.get("friends");
                    if (friends == null) {
                        friends = new ArrayList<>();
                    }
                    friendsLiveData.postValue(friends);
                });

//        friendsListener = FirestoreManager.getInstance()
//                .friendsReference(userId)
//                .addSnapshotListener((snapshots, e) -> {
//                    if (e != null || snapshots == null) {
//                        return;
//                    }

//                    List<Map<String, Object>> friends = new ArrayList<>();
//                    for (DocumentSnapshot document : snapshots.getDocuments()) {
//                        Map<String, Object> data = document.getData();
//                        if (data != null) {
//                            data.put("id", document.getId());
//                            if (!data.containsKey("email")) {
//                                data.put("email", "Unknown");
//                            }
//                            friends.add(data);
//                        }
//                    }
//                    friendsLiveData.postValue(friends);
//                });
    }

    public void stopListeningForFriends() {
        if (friendsListener != null) {
            friendsListener.remove();
            friendsListener = null;
        }
    }

    public void removeFriend(String friendId) {
        String currentUid =  FirestoreManager.getInstance().getCurrentUserId();
        if (currentUid == null || friendId == null) {
            return;
        }

        FirebaseFirestore.getInstance().collection("users").document(currentUid)
                        .get()
                        .addOnSuccessListener(snapshot -> {
                            if (!snapshot.exists()) {
                                return;
                            }

                            List<Map<String, Object>> friends = (List<Map<String, Object>>) snapshot.get("friends");
                            if (friends == null) {
                                return;
                            }

                            for (Map<String, Object> friend : friends) {
                                if (friendId.equals(friend.get("uid"))) {
                                    FirebaseFirestore.getInstance().collection("users").document(currentUid)
                                            .update("friends", FieldValue.arrayRemove(friend));
                                    break;
                                }
                            }
                        });

        FirebaseFirestore.getInstance().collection("users").document(friendId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        return;
                    }

                    List<Map<String, Object>> friends = (List<Map<String, Object>>) snapshot.get("friends");
                    if (friends == null) {
                        return;
                    }

                    for (Map<String, Object> friend : friends) {
                        if (currentUid.equals(friend.get("uid"))) {
                            FirebaseFirestore.getInstance().collection("users").document(friendId)
                                    .update("friends", FieldValue.arrayRemove(friend));
                            break;
                        }
                    }
                });
    }

    public void acceptFriendRequest(String requestId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("friendRequests").document(requestId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        Log.e("Firestore", "Friend request not found: " + requestId);
                    }

                    String requesterUid = document.getString("requesterUid");
                    String approverUid = document.getString("approverUid");
                    String requesterEmail = document.getString("requesterEmail");
                    String approverEmail = document.getString("approverEmail");

                    if (requesterUid == null || approverUid == null) {
                        Log.e("Firestore", "Invalid friend request: " + requestId);
                        return;
                    }

                    db.collection("friendRequests").document(requestId)
                            .update("status", "accepted")
                            .addOnSuccessListener(e -> {
                                Log.d("Firestore", "Friend request accepted: " + requestId);

                                Map<String, Object> requesterData = new HashMap<>();
                                requesterData.put("uid", approverUid);
                                requesterData.put("email", approverEmail);

                                Map<String, Object> approverData = new HashMap<>();
                                approverData.put("uid", requesterUid);
                                approverData.put("email", requesterEmail);

                                db.collection("users").document(requesterUid)
                                        .update("friends", FieldValue.arrayUnion(requesterData));

                                db.collection("users").document(approverUid)
                                        .update("friends", FieldValue.arrayUnion(approverData));
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Firestore", "Failed to accept friend request", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to get friend request", e);
                });
    }

    public void sendFriendRequest(String approverEmail) {
        String requesterUid = FirestoreManager.getInstance().getCurrentUserId();
        String requesterEmail = FirestoreManager.getInstance().getCurrentUserEmail();

        if (requesterUid == null || requesterEmail == null) {
            return;
        }

        Log.d("Firestore", "Sending friend request to: " + approverEmail);

        FirestoreManager.getInstance().searchByEmail(approverEmail).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        Log.d("Firestore", "No user found with that email: " + approverEmail);
                        return;
                    }
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        String approverUid = document.getId();
                        FirestoreManager.getInstance().sendFriendRequest(requesterUid, approverUid,
                                requesterEmail, approverEmail);
                        Log.d("Firestore", "Friend request created for: " + approverEmail);

                    }
                })
                .addOnFailureListener(e ->
                        Log.d("Firestore", "Failed to search for user by email", e)
                );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopListeningForRequests();
        stopListeningForFriends();
    }
}
