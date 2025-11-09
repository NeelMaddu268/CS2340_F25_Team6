package com.example.sprintproject.view;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.sprintproject.R;
import com.example.sprintproject.model.AppDate;
import com.example.sprintproject.viewmodel.FirestoreManager;
import com.example.sprintproject.viewmodel.SavingsCircleDetailsViewModel;
import com.google.firebase.auth.FirebaseAuth;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SavingsCircleDetailsActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_savingscircle_details);

        // get the group creation details from the intent
        String groupName = getIntent().getStringExtra("groupName");
        ArrayList<String> groupEmails = getIntent().getStringArrayListExtra("groupEmails");
        String groupChallengeTitle = getIntent().getStringExtra("groupChallengeTitle");
        double groupChallengeGoal = getIntent().getDoubleExtra("groupChallengeGoal", 0.0);
        String groupFrequency = getIntent().getStringExtra("groupFrequency");
        String groupNotes = getIntent().getStringExtra("groupNotes");

        String creationDate = getIntent().getStringExtra("creationDate");
        HashMap<String, String> datesJoined = (HashMap<String, String>) getIntent().getSerializableExtra("datesJoined");
        HashMap<String, Double> contributions = (HashMap<String, Double>) getIntent().getSerializableExtra("contributions");
        String circleId = getIntent().getStringExtra("circleId");

        // update the UI with the provided details
        TextView groupNameTextView = findViewById(R.id.groupNameTextView);
        TextView groupChallengeTitleTextView = findViewById(R.id.groupChallengeTitleTextView);
        TextView groupChallengeGoalTextView = findViewById(R.id.groupChallengeGoalTextView);
        TextView groupFrequencyTextView = findViewById(R.id.groupFrequencyTextView);
        TextView groupNotesTextView = findViewById(R.id.groupNotesTextView);
        TextView groupCreationTextView = findViewById(R.id.groupCreationTextView);
        TextView groupJoinedTextView = findViewById(R.id.groupJoinedTextView);
        TextView groupContributionsTextView = findViewById(R.id.groupContributionsTextView);
        TextView goalComplete = findViewById(R.id.goalComplete);
        TextView groupEndingTextView = findViewById(R.id.groupEndingTextView);
        TextView groupEmailTextView = findViewById(R.id.groupEmailTextView);

        StringBuilder sb = new StringBuilder();
        for (String email : groupEmails) {
            Double contribution = contributions.get(email);
            sb.append(email).append(": $").append(contribution != null ? contribution : 0);
            sb.append("\n");
        }

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
        groupContributionsTextView.setText(sb.toString());
        if (dateJoined != null) {
            groupJoinedTextView.setText(dateJoined);
        } else {
            groupJoinedTextView.setText("Date not available");
        }

        if (groupFrequency.equals("Weekly")) {
            groupEndingTextView.setText(AppDate.addDays(creationDate, 7, 0));
        } else if (groupFrequency.equals("Monthly")) {
            groupEndingTextView.setText(AppDate.addDays(creationDate, 0, 1));
        }

        EditText inviteEmailInput = findViewById(R.id.inviteEmailInput);
        Button inviteButton = findViewById(R.id.inviteButton);

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

        detailsViewModel.getContributions().observe(this, contributionss -> {
            Map<String, String> members = detailsViewModel.getMembers().getValue();
            Map<String, String> memberUids = detailsViewModel.getMemberUids().getValue();
            if (contributionss == null || members == null || memberUids == null) return;

            Double myContribution = contributionss.get(currentUid);
            if (myContribution != null) {
                int memberCount = members.size();
                if (myContribution / memberCount >= groupChallengeGoal) {
                    goalComplete.setVisibility(View.VISIBLE);
                }
            }

            StringBuilder mb = new StringBuilder();
            for (Map.Entry<String, String> email : members.entrySet()) {
                String index = email.getKey();
                String uid = memberUids.get(index);
                Double contribution = contributionss.get(uid);
                mb.append(email.getValue()).append(": $").append(contribution != null ? contribution : 0);
                mb.append("\n");
            }
            groupContributionsTextView.setText(mb.toString());
        });

        detailsViewModel.getMembers().observe(this, members -> {
            if (members != null) {
                groupEmailTextView.setText(String.join(", ", members.values()));
            }
        });

        detailsViewModel.getStatusMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });


        inviteButton.setOnClickListener(v -> {
            String inviteEmail = inviteEmailInput.getText().toString().trim();
            if (inviteEmail.isEmpty()) {
                inviteEmailInput.setError("Enter an email");
                return;
            }

            detailsViewModel.sendInvite(circleId, circleName, inviteEmail);
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
        if (contributions == null || members == null || memberUids == null) return;

        // Update contributions text
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : members.entrySet()) {
            String index = entry.getKey();
            String uid = memberUids.get(index);
            Double contribution = contributions.get(uid);
            sb.append(entry.getValue())
                    .append(": $")
                    .append(contribution != null ? contribution : 0)
                    .append("\n");
        }
        groupContributionsTextView.setText(sb.toString());

        // Update members emails
        groupEmailTextView.setText(String.join(", ", members.values()));

        // Update goal complete
        Double myContribution = contributions.get(currentUid);
        if (myContribution != null && myContribution / members.size() >= groupChallengeGoal) {
            goalComplete.setVisibility(View.VISIBLE);
        } else {
            goalComplete.setVisibility(View.GONE);
        }
    }


}
