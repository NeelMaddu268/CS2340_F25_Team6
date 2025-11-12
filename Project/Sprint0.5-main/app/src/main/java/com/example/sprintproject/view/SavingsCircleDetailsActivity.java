package com.example.sprintproject.view;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.sprintproject.R;
import com.example.sprintproject.model.AppDate;
import com.example.sprintproject.viewmodel.DateViewModel;
import com.example.sprintproject.viewmodel.FirestoreManager;
import com.example.sprintproject.viewmodel.SavingsCircleDetailsViewModel;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SavingsCircleDetailsActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_savingscircle_details);
        DateViewModel dateVM = new ViewModelProvider(this).get(DateViewModel.class);


        // get the group creation details from the intent
        String groupName = getIntent().getStringExtra("groupName");
        ArrayList<String> groupEmails = getIntent().getStringArrayListExtra("groupEmails");
        String groupChallengeTitle = getIntent().getStringExtra("groupChallengeTitle");
        double groupChallengeGoal = getIntent().getDoubleExtra("groupChallengeGoal", 0.0);
        String groupFrequency = getIntent().getStringExtra("groupFrequency");
        String groupNotes = getIntent().getStringExtra("groupNotes");



        String creationDate = getIntent().getStringExtra("creationDate");
        HashMap<String, String> datesJoined = (HashMap<String, String>)
                getIntent().getSerializableExtra("datesJoined");
        HashMap<String, Double> contributions = (HashMap<String, Double>)
                getIntent().getSerializableExtra("contributions");
        String circleId = getIntent().getStringExtra("circleId");

        // update the UI with the provided details
        TextView groupNameTextView =
                findViewById(R.id.groupNameTextView);
        TextView groupChallengeTitleTextView =
                findViewById(R.id.groupChallengeTitleTextView);
        TextView groupChallengeGoalTextView = findViewById(R.id.groupChallengeGoalTextView);
        TextView groupFrequencyTextView = findViewById(R.id.groupFrequencyTextView);
        TextView groupNotesTextView = findViewById(R.id.groupNotesTextView);
        TextView groupCreationTextView = findViewById(R.id.groupCreationTextView);
        TextView groupJoinedTextView = findViewById(R.id.groupJoinedTextView);
        TextView groupContributionsTextView = findViewById(R.id.groupContributionsTextView);
        TextView goalComplete = findViewById(R.id.goalComplete);
        TextView groupEndingTextView = findViewById(R.id.groupEndingTextView);
        TextView groupEmailTextView = findViewById(R.id.groupEmailTextView);

        String currentUid = FirestoreManager.getInstance().getCurrentUserId();
        String dateJoined = datesJoined != null ? datesJoined.get(currentUid) : null;


        groupNameTextView.setText(groupName);
        groupEmailTextView.setText(String.join(", ", groupEmails));
        Button deleteCircleButton = findViewById(R.id.deleteCircleButton);


        groupNameTextView.setText(groupName);
        groupChallengeTitleTextView.setText(groupChallengeTitle);
        groupChallengeGoalTextView.setText("$" + groupChallengeGoal);
        groupFrequencyTextView.setText(groupFrequency);
        groupNotesTextView.setText(groupNotes);
        groupCreationTextView.setText(creationDate);

        if (dateJoined != null) {
            groupJoinedTextView.setText(dateJoined);
        } else {
            groupJoinedTextView.setText("Date not available");
        }

        EditText inviteEmailInput = findViewById(R.id.inviteEmailInput);
        Button inviteButton = findViewById(R.id.inviteButton);

        final String freq = groupFrequency;
        final String circleCreatorId = getIntent().getStringExtra("creatorId");

        // keep the latest dates for when AppDate changes
        final java.util.concurrent.atomic.AtomicReference<Map<String, String>> lastDatesRef =
                new java.util.concurrent.atomic.AtomicReference<>();

        // one place to toggle the controls based on app date + creator end
        final Runnable reevaluateInviteGate = new Runnable() {
            @Override public void run() {

                if (!currentUid.equals(circleCreatorId)) {
                    setInviteControls(false, inviteEmailInput, inviteButton);
                    return;
                }

                Map<String, String> dates = lastDatesRef.get();
                if (dates == null || circleCreatorId == null) {
                    return;
                }

                AppDate appDate = dateVM.getCurrentDate().getValue();
                if (appDate == null) {
                    return;
                }

                String creatorJoinIso = dates.get(circleCreatorId);
                if (creatorJoinIso == null) {
                    return;
                }

                String creatorEndIso = "Weekly".equals(freq)
                        ? AppDate.addDays(creatorJoinIso, 7, 0)
                        : AppDate.addDays(creatorJoinIso, 0, 1);

                boolean invitesOpen = appDate.toIso().compareTo(creatorEndIso) <= 0;
                setInviteControls(invitesOpen, inviteEmailInput, inviteButton);
            }
        };

        dateVM.getCurrentDate().observe(this, d -> {
            reevaluateInviteGate.run();
        });

        reevaluateInviteGate.run();

        String creatorId = getIntent().getStringExtra("creatorId");

        if (creatorId == null || !creatorId.equals(currentUid)) {
            inviteEmailInput.setVisibility(View.GONE);
            inviteButton.setVisibility(View.GONE);
            deleteCircleButton.setVisibility(View.GONE);
        } else {
            deleteCircleButton.setVisibility(View.VISIBLE);

            deleteCircleButton.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Delete Savings Circle")
                        .setMessage("Are you sure you want to delete this savings circle?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            FirestoreManager.getInstance()
                                    .deleteSavingsCircle(circleId, currentUid)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Circle deleted successfully",
                                                Toast.LENGTH_SHORT).show();
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Failed to delete: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        String circleName = groupName;

        SavingsCircleDetailsViewModel detailsViewModel =
                new ViewModelProvider(this).get(SavingsCircleDetailsViewModel.class);

        detailsViewModel.listenToSavingsCircle(circleId);

        Observer<Object> observer = ignored ->
                updateUI(detailsViewModel, currentUid, groupChallengeGoal,
                        goalComplete, groupContributionsTextView, groupEmailTextView);
        detailsViewModel.getContributions().observe(this, observer);
        detailsViewModel.getMembers().observe(this, observer);
        detailsViewModel.getMemberUids().observe(this, observer);

        detailsViewModel.getStatusMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });

        detailsViewModel.getMemberJoinDates().observe(this, dates -> {
            lastDatesRef.set(dates);

            if (dates != null && !dates.isEmpty()) {
                String maxEndIso = null;
                for (String joinIso : dates.values()) {
                    if (joinIso == null || joinIso.isEmpty()) {
                        continue;
                    }
                    String endIso = "Weekly".equals(freq)
                            ? AppDate.addDays(joinIso, 7, 0)
                            : AppDate.addDays(joinIso, 0, 1);
                    if (maxEndIso == null || endIso.compareTo(maxEndIso) > 0) {
                        maxEndIso = endIso;
                    }
                }
                if (maxEndIso != null) {
                    groupEndingTextView.setText(maxEndIso);
                }
            }

            reevaluateInviteGate.run();
        });



        inviteButton.setOnClickListener(v -> {
            String inviteEmail = inviteEmailInput.getText().toString().trim();
            if (inviteEmail.isEmpty()) {
                inviteEmailInput.setError("Enter an email");
                return;
            }

            androidx.lifecycle.Observer<AppDate> once = new androidx.lifecycle.Observer<AppDate>() {
                @Override public void onChanged(AppDate appDate) {
                    if (appDate == null) {
                        return;
                    }
                    dateVM.getCurrentDate().removeObserver(this);
                    detailsViewModel.sendInvite(circleId, circleName, inviteEmail, appDate.toIso());
                }
            };
            dateVM.getCurrentDate().observe(this, once);

        });


        if (groupNotes == null || groupNotes.trim().isEmpty()) {
            groupNotesTextView.setText("Notes: None");
        } else {
            groupNotesTextView.setText("Notes: " + groupNotes);
        }

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(view -> finish());
    }
    private void updateUI(SavingsCircleDetailsViewModel vm, String currentUid,
                          double groupChallengeGoal, TextView goalComplete,
                          TextView groupContributionsTextView, TextView groupEmailTextView) {

        Map<String, Double> contributions = vm.getContributions().getValue();
        Map<String, String> members = vm.getMembers().getValue();
        Map<String, String> memberUids = vm.getMemberUids().getValue();

        // Only update when both contributions and members are loaded
        if (contributions == null || members == null || memberUids == null) {
            return;
        }

        StringBuilder sb = new StringBuilder();

        int n = memberUids.size();
        boolean usedIndexedLoop = true;
        for (int i = 0; i < n; i++) {
            String idx  = String.valueOf(i);
            String uid  = memberUids.get(idx);
            String mail = members.get(idx);

            if (uid == null || mail == null) {
                usedIndexedLoop = false;
                break;
            }
            Double amt = contributions.get(uid);
            sb.append(mail)
                    .append(": $")
                    .append(amt != null ? amt : 0)
                    .append("\n");
        }

        if (!usedIndexedLoop) {
            for (Map.Entry<String, String> e : members.entrySet()) {
                String idx  = e.getKey();
                String mail = e.getValue();
                String uid  = memberUids.get(idx);
                Double amt  = uid != null ? contributions.get(uid) : null;
                sb.append(mail)
                        .append(": $")
                        .append(amt != null ? amt : 0)
                        .append("\n");
            }
        }

        groupContributionsTextView.setText(sb.toString());

        // Update members emails
        StringBuilder emailsSb = new StringBuilder();
        int nn = memberUids.size();
        for (int i = 0; i < nn; i++) {
            if (i > 0) {
                emailsSb.append(", ");
            }
            String mail = members.get(String.valueOf(i));
            if (mail != null) {
                emailsSb.append(mail);
            }
        }
        groupEmailTextView.setText(emailsSb.toString());


        // Update goal complete
        Double myContribution = contributions.get(currentUid);
        int people = Math.max(1, members.size());
        double myTarget = groupChallengeGoal / people;
        if (myContribution != null && myContribution >= myTarget) {
            goalComplete.setVisibility(View.VISIBLE);
        } else {
            goalComplete.setVisibility(View.GONE);
        }
    }

    private void setInviteControls(boolean open, EditText input, Button button) {
        if (open) {
            input.setVisibility(View.VISIBLE);
            button.setVisibility(View.VISIBLE);
            input.setEnabled(true);
            button.setEnabled(true);
            button.setText("Invite");
        } else {
            input.setVisibility(View.GONE);
            button.setVisibility(View.GONE);
        }
    }
}
