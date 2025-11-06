package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SavingsCircleDetailsViewModel extends ViewModel {

    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    public LiveData<String> getStatusMessage() {
        return statusMessage;
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
