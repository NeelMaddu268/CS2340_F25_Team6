package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

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

public class InvitationsViewModel extends ViewModel {
    private final MutableLiveData<List<Map<String, Object>>> invitesLiveData
            = new MutableLiveData<>();
    private ListenerRegistration listener;

    public LiveData<List<Map<String, Object>>> getInvites() {
        return invitesLiveData;
    }

    public void startListening() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            return;
        }
        String uid = auth.getCurrentUser().getUid();

        listener = FirestoreManager.getInstance()
                .invitationsForUser(uid)
                .addSnapshotListener((qs, e) -> {
                    if (e != null || qs == null) {
                        return;
                    }
                    List<Map<String, Object>> invites = new ArrayList<>();
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            data.put("id", doc.getId());
                            invites.add(data);
                        }
                    }
                    invitesLiveData.postValue(invites);
                });
    }

    public void stopListening() {
        if (listener != null) {
            listener.remove();
            listener = null;
        }
    }

    public void respondToInvite(String inviteId, boolean accept) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (inviteId == null || inviteId.trim().isEmpty()) {
            System.err.println("[respondToInvite] No inviteId provided");
            return;
        }

        DocumentReference ref = db.collection("invitations").document(inviteId);

        ref.update("status", accept ? "accepted" : "declined")
                .addOnSuccessListener(aVoid -> {
                    System.out.println("[respondToInvite] Invite " + inviteId
                            + " marked as " + (accept ? "accepted" : "declined"));

                    if (!accept) {
                        return;
                    }

                    ref.get().addOnSuccessListener(doc -> {
                        if (!doc.exists()) {
                            System.err.println("[respondToInvite] Invite doc not found!");
                            return;
                        }

                        String circleId = doc.getString("circleId");
                        String toUid = doc.getString("toUid");
                        String circleName = doc.getString("circleName");

                        if (circleId == null || toUid == null) {
                            System.err.println(
                                    "[respondToInvite] Missing circleId or toUid in invite doc");
                            return;
                        }

                        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        if (!currentUid.equals(toUid)) {
                            System.err.println("[respondToInvite] Auth user doesn't"
                                    + " match invite target (rejecting unauthorized join)");
                            return;
                        }

                        System.out.println("[respondToInvite] Adding user "
                                + currentUid + " to circle " + circleId);

                        db.collection("savingsCircles")
                                .document(circleId)
                                .update(
                                        "memberIds", FieldValue.arrayUnion(currentUid),
                                        "contributions." + currentUid, 0.0
                                )
                                .addOnSuccessListener(aVoid2 -> {
                                    System.out.println("[respondToInvite] User"
                                            + " added to circle successfully");

                                    Map<String, Object> pointer = new HashMap<>();
                                    pointer.put("circleId", circleId);
                                    pointer.put("name", circleName);

                                    FirestoreManager.getInstance()
                                            .userSavingsCirclePointers(currentUid)
                                            .add(pointer)
                                            .addOnSuccessListener(r ->
                                                    System.out.println("[respondToInvite] Added"
                                                            + " circle pointer for user"))
                                            .addOnFailureListener(e ->
                                                    System.err.println("[respondToInvite] Failed "
                                                            + "to add pointer: "
                                                            + e.getMessage()));
                                })
                                .addOnFailureListener(e ->
                                        System.err.println("[respondToInvite] Failed to"
                                                + " add user to circle: " + e.getMessage())
                            );

                    }).addOnFailureListener(e ->
                            System.err.println("[respondToInvite] Failed to fetch invite doc: "
                                    + e.getMessage())
                    );
                })
                .addOnFailureListener(e ->
                        System.err.println("[respondToInvite] Failed to update invite status: "
                                + e.getMessage())
            );
    }
}
