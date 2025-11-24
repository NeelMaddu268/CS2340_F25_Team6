package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.model.AppDate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SavingsCircleDetailsViewModel extends ViewModel {

    private static final String SAVINGS_CIRCLES = "savingsCircles";
    private static final String INVITATIONS = "invitations";
    private static final String USERS = "users";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration listener;

    private final MutableLiveData<Map<String, Double>> contributionsLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<Map<String, String>> membersLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<Map<String, String>> memberUidLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<Map<String, String>> memberJoinDatesLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<String> statusMessage =
            new MutableLiveData<>();

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

    /** ------------------------ Smell #1 fixed: low cognitive complexity ------------------------ */
    public void listenToSavingsCircle(String circleId) {
        DocumentReference circleRef = db.collection(SAVINGS_CIRCLES).document(circleId);
        listener = circleRef.addSnapshotListener(this::handleCircleSnapshot);
    }

    private void handleCircleSnapshot(DocumentSnapshot snapshot, FirebaseFirestoreException e) {
        if (snapshotInvalid(snapshot, e)) return;

        publishIfPresent(snapshot, "contributions", contributionsLiveData);
        publishIfPresent(snapshot, "datesJoined", memberJoinDatesLiveData);

        Map<String, String> members =
                coerceMapOrListToStringMap(snapshot.get("memberEmails"));
        if (members != null) {
            membersLiveData.setValue(members);
        }

        Map<String, String> memberUids =
                coerceMapOrListToStringMap(snapshot.get("memberIds"));
        if (memberUids != null) {
            memberUidLiveData.setValue(memberUids);
        }
    }

    private boolean snapshotInvalid(DocumentSnapshot snapshot, FirebaseFirestoreException e) {
        return e != null || snapshot == null || !snapshot.exists();
    }

    @SuppressWarnings("unchecked")
    private <T> void publishIfPresent(
            DocumentSnapshot snapshot,
            String field,
            MutableLiveData<Map<String, T>> liveData
    ) {
        Object raw = snapshot.get(field);
        if (raw instanceof Map) {
            liveData.setValue((Map<String, T>) raw);
        }
    }

    /**
     * Accepts either:
     *  - Map<?,?> already (casts keys/values to String),
     *  - List<?> (indexes -> String keys),
     *  - or null/other (returns null).
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> coerceMapOrListToStringMap(Object raw) {
        if (raw == null) return null;

        if (raw instanceof Map) {
            Map<Object, Object> m = (Map<Object, Object>) raw;
            Map<String, String> out = new HashMap<>();
            for (Map.Entry<Object, Object> en : m.entrySet()) {
                Object k = en.getKey();
                Object v = en.getValue();
                if (k != null && v != null) {
                    out.put(k.toString(), v.toString());
                }
            }
            return out;
        }

        if (raw instanceof List) {
            List<?> list = (List<?>) raw;
            Map<String, String> out = new HashMap<>();
            for (int i = 0; i < list.size(); i++) {
                Object v = list.get(i);
                if (v != null) {
                    out.put(String.valueOf(i), v.toString());
                }
            }
            return out;
        }

        return null;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (listener != null) {
            listener.remove();
            listener = null;
        }
    }

    /** ------------------------ Smell #2 fixed: low cognitive complexity ------------------------ */
    public void sendInvite(String circleId,
                           String circleName,
                           String inviteeEmail,
                           String appDateIso) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (!validateInviteInputs(user, inviteeEmail, appDateIso)) return;

        final String fromUid = user.getUid();
        final String fromEmail = user.getEmail();
        final String targetEmail = inviteeEmail.trim();

        DocumentReference circleRef = db.collection(SAVINGS_CIRCLES).document(circleId);
        circleRef.get()
                .addOnSuccessListener(circleSnap ->
                        handleCircleLoaded(circleSnap, circleId, circleName,
                                fromUid, fromEmail, targetEmail, appDateIso))
                .addOnFailureListener(e ->
                        statusMessage.postValue("Error loading circle: " + e.getMessage()));
    }

    private boolean validateInviteInputs(FirebaseUser user,
                                         String inviteeEmail,
                                         String appDateIso) {
        if (user == null) {
            statusMessage.postValue("You must be logged in to send invites.");
            return false;
        }
        if (inviteeEmail == null || inviteeEmail.trim().isEmpty()) {
            statusMessage.postValue("Enter a valid email.");
            return false;
        }
        String fromEmail = user.getEmail();
        if (fromEmail != null && inviteeEmail.equalsIgnoreCase(fromEmail)) {
            statusMessage.postValue("You’re already in this circle.");
            return false;
        }
        if (appDateIso == null || appDateIso.isEmpty()) {
            statusMessage.postValue("Internal error: app date unavailable.");
            return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private void handleCircleLoaded(DocumentSnapshot circleSnap,
                                    String circleId,
                                    String circleName,
                                    String fromUid,
                                    String fromEmail,
                                    String inviteeEmail,
                                    String appDateIso) {

        if (circleSnap == null || !circleSnap.exists()) {
            statusMessage.postValue("Circle not found.");
            return;
        }

        if (invitesClosed(circleSnap, appDateIso)) {
            statusMessage.postValue("Invites are closed for this circle.");
            return;
        }

        List<String> memberEmails = (List<String>) circleSnap.get("memberEmails");
        List<String> memberIds = (List<String>) circleSnap.get("memberIds");

        if (emailAlreadyMember(memberEmails, inviteeEmail)) {
            statusMessage.postValue("That user is already a member.");
            return;
        }

        ensureNoPendingInvite(circleId, fromUid, inviteeEmail,
                () -> findInviteeUserAndSend(circleId, circleName, fromUid, fromEmail,
                        inviteeEmail, appDateIso, memberIds));
    }

    @SuppressWarnings("unchecked")
    private boolean invitesClosed(DocumentSnapshot circleSnap, String appDateIso) {
        String frequency = circleSnap.getString("frequency");
        String creatorId = circleSnap.getString("creatorId");
        Map<String, String> datesJoined =
                (Map<String, String>) circleSnap.get("datesJoined");

        if (creatorId == null || datesJoined == null) return false;

        String creatorJoinIso = datesJoined.get(creatorId);
        if (creatorJoinIso == null) return false;

        String creatorEndIso = computeCreatorEndIso(creatorJoinIso, frequency);
        return appDateIso.compareTo(creatorEndIso) > 0;
    }

    private String computeCreatorEndIso(String creatorJoinIso, String frequency) {
        boolean weekly = "Weekly".equalsIgnoreCase(frequency);
        return weekly
                ? AppDate.addDays(creatorJoinIso, 7, 0)
                : AppDate.addDays(creatorJoinIso, 0, 1);
    }

    private boolean emailAlreadyMember(List<String> memberEmails, String inviteeEmail) {
        if (memberEmails == null) return false;
        for (String email : memberEmails) {
            if (email != null && email.equalsIgnoreCase(inviteeEmail)) {
                return true;
            }
        }
        return false;
    }

    private void ensureNoPendingInvite(String circleId,
                                       String fromUid,
                                       String inviteeEmail,
                                       Runnable onAllowed) {
        db.collection(INVITATIONS)
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
                    onAllowed.run();
                })
                .addOnFailureListener(e ->
                        statusMessage.postValue("Error checking pending invites: "
                                + e.getMessage()));
    }

    private void findInviteeUserAndSend(String circleId,
                                        String circleName,
                                        String fromUid,
                                        String fromEmail,
                                        String inviteeEmail,
                                        String appDateIso,
                                        List<String> memberIds) {
        db.collection(USERS)
                .whereEqualTo("email", inviteeEmail)
                .limit(1)
                .get()
                .addOnSuccessListener(userSnap -> {
                    if (userSnap.isEmpty()) {
                        statusMessage.postValue("No user found with that email.");
                        return;
                    }

                    String toUid = userSnap.getDocuments().get(0).getId();
                    if (memberIds != null && memberIds.contains(toUid)) {
                        statusMessage.postValue("That user is already a member.");
                        return;
                    }

                    createInvitationDoc(circleId, circleName, fromUid, fromEmail,
                            toUid, inviteeEmail, appDateIso);
                })
                .addOnFailureListener(e ->
                        statusMessage.postValue("Error finding user: " + e.getMessage()));
    }

    private void createInvitationDoc(String circleId,
                                     String circleName,
                                     String fromUid,
                                     String fromEmail,
                                     String toUid,
                                     String toEmail,
                                     String appDateIso) {
        Map<String, Object> invite = new HashMap<>();
        invite.put("circleId", circleId);
        invite.put("circleName", circleName);
        invite.put("fromUid", fromUid);
        invite.put("fromEmail", fromEmail);
        invite.put("toUid", toUid);
        invite.put("toEmail", toEmail);
        invite.put("status", "pending");
        invite.put("appDateIso", appDateIso);

        FirestoreManager.getInstance()
                .invitationsReference()
                .add(invite)
                .addOnSuccessListener(ref ->
                        statusMessage.postValue("Invite sent successfully!"))
                .addOnFailureListener(e ->
                        statusMessage.postValue("Failed to send invite: " + e.getMessage()));
    }
}
