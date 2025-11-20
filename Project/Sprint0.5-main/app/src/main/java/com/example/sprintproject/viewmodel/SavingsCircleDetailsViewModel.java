package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.model.AppDate;
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

    @Override
    protected void onCleared() {
        super.onCleared();
        if (listener != null) {
            listener.remove();
        }
    }

    public void sendInvite(String circleId, String circleName,
                           String inviteeEmail, String appDateIso) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            statusMessage.postValue("You must be logged in to send invites.");
            return;
        }

        final String fromUid = auth.getCurrentUser().getUid();
        final String fromEmail = auth.getCurrentUser().getEmail();

        if (inviteeEmail == null || inviteeEmail.trim().isEmpty()) {
            statusMessage.postValue("Enter a valid email.");
            return;
        }
        if (fromEmail != null && inviteeEmail.equalsIgnoreCase(fromEmail)) {
            statusMessage.postValue("You’re already in this circle.");
            return;
        }
        if (appDateIso == null || appDateIso.isEmpty()) {
            statusMessage.postValue("Internal error: app date unavailable.");
            return;
        }

        // Loads the circle
        final DocumentReference circleRef = db.collection("savingsCircles").document(circleId);
        circleRef.get()
                .addOnSuccessListener(circleSnap -> {
                    if (!circleSnap.exists()) {
                        statusMessage.postValue("Circle not found.");
                        return;
                    }

                    String frequency = circleSnap.getString("frequency");
                    String creatorId = circleSnap.getString("creatorId");

                    Map<String, String> datesJoined =
                            (Map<String, String>) circleSnap.get("datesJoined");

                    if (creatorId != null && datesJoined != null) {
                        String creatorJoinIso = datesJoined.get(creatorId);
                        if (creatorJoinIso != null) {
                            String creatorEndIso = "Weekly".equals(frequency)
                                    ? AppDate.addDays(creatorJoinIso, 7, 0)
                                    : AppDate.addDays(creatorJoinIso, 0, 1);
                            if (appDateIso.compareTo(creatorEndIso) > 0) {
                                statusMessage.postValue("Invites are closed for this circle.");
                                return;
                            }
                        }
                    }

                    java.util.List<String> memberEmails =
                            (java.util.List<String>) circleSnap.get("memberEmails");
                    java.util.List<String> memberIds =
                            (java.util.List<String>) circleSnap.get("memberIds");

                    if (memberEmails != null) {
                        for (String email : memberEmails) {
                            if (email != null && email.equalsIgnoreCase(inviteeEmail)) {
                                statusMessage.postValue("That user is already a member.");
                                return;
                            }
                        }
                    }

                    // Check if there’s a pending invite from this creator to this email
                    db.collection("invitations")
                            .whereEqualTo("circleId", circleId)
                            .whereEqualTo("fromUid", fromUid)
                            .whereEqualTo("toEmail", inviteeEmail)
                            .whereEqualTo("status", "pending")
                            .limit(1)
                            .get()
                            .addOnSuccessListener(invSnap -> {
                                if (!invSnap.isEmpty()) {
                                    statusMessage.postValue(
                                            "You already sent a pending invite to this user.");
                                    return;
                                }

                                db.collection("users")
                                        .whereEqualTo("email", inviteeEmail)
                                        .limit(1)
                                        .get()
                                        .addOnSuccessListener(userSnap -> {
                                            if (userSnap.isEmpty()) {
                                                statusMessage.postValue(
                                                        "No user found with that email.");
                                                return;
                                            }

                                            String toUid = userSnap.getDocuments().get(0).getId();

                                            if (memberIds != null && memberIds.contains(toUid)) {
                                                statusMessage.postValue(
                                                        "That user is already a member.");
                                                return;
                                            }

                                            // Create the invite
                                            Map<String, Object> invite = new HashMap<>();
                                            invite.put("circleId", circleId);
                                            invite.put("circleName", circleName);
                                            invite.put("fromUid", fromUid);
                                            invite.put("fromEmail", fromEmail);
                                            invite.put("toUid", toUid);
                                            invite.put("toEmail", inviteeEmail);
                                            invite.put("status", "pending");
                                            invite.put("appDateIso", appDateIso);

                                            FirestoreManager.getInstance()
                                                    .invitationsReference()
                                                    .add(invite)
                                                    .addOnSuccessListener(ref ->
                                                        statusMessage.postValue(
                                                                "Invite sent successfully!")
                                                    )
                                                    .addOnFailureListener(e ->
                                                        statusMessage.postValue(
                                                                "Failed to send invite: "
                                                                        + e.getMessage())
                                                    );
                                        })
                                        .addOnFailureListener(e ->
                                                statusMessage.postValue("Error finding user: "
                                                        + e.getMessage())
                                    );
                            })
                            .addOnFailureListener(e ->
                                    statusMessage.postValue("Error checking pending invites: "
                                            + e.getMessage())
                        );
                })
                .addOnFailureListener(e ->
                        statusMessage.postValue("Error loading circle: " + e.getMessage())
            );
    }
}
