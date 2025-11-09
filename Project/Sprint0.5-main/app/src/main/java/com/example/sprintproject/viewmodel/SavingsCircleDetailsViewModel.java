package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class SavingsCircleDetailsViewModel extends ViewModel {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration listener;
    private final MutableLiveData<Map<String, Double>> contributionsLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<Map<String, String>> membersLiveData = new MutableLiveData<>();
    private final MutableLiveData<Map<String, String>> memberUidLiveData = new MutableLiveData<>();
    private final MutableLiveData<Map<String, String>> memberJoinDatesLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();

    public LiveData<Map<String, Double>> getContributions() {
        return contributionsLiveData;
    }

    public LiveData<Map<String, String>> getMembers() {
        return membersLiveData;
    }

    public LiveData<Map<String, String>> getMemberUids() {
        return memberUidLiveData;
    }

    public LiveData<Map<String, String>> getMemberJoinDates() {
        return memberJoinDatesLiveData;
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    public void listenToSavingsCircle(String circleId) {
        DocumentReference circleRef = db.collection("savingsCircles").document(circleId);
        listener = circleRef.addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null || !snapshot.exists()) {
                return;
            }

            Map<String, Double> contributions = (Map<String, Double>) snapshot.get("contributions");
            if (contributions != null) {
                contributionsLiveData.setValue(contributions);
            }
            Map<String, String> datesJoined = (Map<String, String>) snapshot.get("datesJoined");
            if (datesJoined != null) {
                memberJoinDatesLiveData.setValue(datesJoined);
            }
            Object rawMembers = snapshot.get("memberEmails");
            if (rawMembers instanceof Map) {
                membersLiveData.setValue((Map<String, String>) rawMembers);
            } else if (rawMembers instanceof java.util.List) {
                Map<String, String> emailMap = new HashMap<>();
                for (int i = 0; i < ((java.util.List<?>) rawMembers).size(); i++) {
                    Object email = ((java.util.List<?>) rawMembers).get(i);
                    if (email != null) {
                        emailMap.put(String.valueOf(i), email.toString());
                    }
                }
                membersLiveData.setValue(emailMap);
            }
            Object rawMembersUids = snapshot.get("memberIds");
            if (rawMembersUids instanceof Map) {
                memberUidLiveData.setValue((Map<String, String>) rawMembersUids);
            } else if (rawMembersUids instanceof java.util.List) {
                Map<String, String> uidMap = new HashMap<>();
                for (int i = 0; i < ((java.util.List<?>) rawMembersUids).size(); i++) {
                    Object uid = ((java.util.List<?>) rawMembersUids).get(i);
                    if (uid != null) {
                        uidMap.put(String.valueOf(i), uid.toString());
                    }
                }
                memberUidLiveData.setValue(uidMap);
            }
        });
    }

    protected void onCleared() {
        super.onCleared();
        if (listener != null) {
            listener.remove();
        }
    }

    public void sendInvite(String circleId, String circleName, String inviteeEmail) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            statusMessage.postValue("You must be logged in to send invites.");
            return;
        }

        String fromUid = auth.getCurrentUser().getUid();
        String fromEmail = auth.getCurrentUser().getEmail();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .whereEqualTo("email", inviteeEmail)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        statusMessage.postValue("No user found with that email.");
                        return;
                    }

                    String toUid = snap.getDocuments().get(0).getId();
                    System.out.println("[sendInvite] Sending invite for circleId="
                            + circleId + " to " + inviteeEmail);
                    Map<String, Object> invite = new HashMap<>();
                    invite.put("circleId", circleId);
                    invite.put("circleName", circleName);
                    invite.put("fromUid", fromUid);
                    invite.put("fromEmail", fromEmail);
                    invite.put("toUid", toUid);
                    invite.put("toEmail", inviteeEmail);
                    invite.put("status", "pending");

                    FirestoreManager.getInstance()
                            .invitationsReference()
                            .add(invite)
                            .addOnSuccessListener(ref -> {
                                statusMessage.postValue("Invite sent successfully!");
                                System.out.println("[sendInvite] Invite created: " + ref.getId());
                            })
                            .addOnFailureListener(e -> {
                                statusMessage.postValue("Failed to send invite: " + e.getMessage());
                                System.err.println("[sendInvite] Failed: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> statusMessage.postValue("Error finding user: "
                        + e.getMessage()));
    }
}
