package com.example.sprintproject.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.sprintproject.R;
import com.example.sprintproject.viewmodel.FirestoreManager;
import com.example.sprintproject.viewmodel.SavingsCircleDetailsViewModel;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
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
        String groupInvite = getIntent().getStringExtra("groupInvite");
        String groupChallengeTitle = getIntent().getStringExtra("groupChallengeTitle");
        double groupChallengeGoal = getIntent().getDoubleExtra("groupChallengeGoal", 0.0);
        String groupFrequency = getIntent().getStringExtra("groupFrequency");
        String groupNotes = getIntent().getStringExtra("groupNotes");
        String creationDate = getIntent().getStringExtra("creationDate");
        HashMap<String, String> datesJoined = (HashMap<String, String>) getIntent().getSerializableExtra("datesJoined");
        HashMap<String, Double> contributions = (HashMap<String, Double>) getIntent().getSerializableExtra("contributions");

        // update the UI with the provided details
        TextView groupNameTextView = findViewById(R.id.groupNameTextView);
        TextView groupEmailTextView = findViewById(R.id.groupEmailTextView);
        TextView groupChallengeTitleTextView = findViewById(R.id.groupChallengeTitleTextView);
        TextView groupChallengeGoalTextView = findViewById(R.id.groupChallengeGoalTextView);
        TextView groupFrequencyTextView = findViewById(R.id.groupFrequencyTextView);
        TextView groupNotesTextView = findViewById(R.id.groupNotesTextView);
        TextView groupCreationTextView = findViewById(R.id.groupCreationTextView);
        TextView groupJoinedTextView = findViewById(R.id.groupJoinedTextView);
        TextView groupContributionsTextView = findViewById(R.id.groupContributionsTextView);

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

        EditText inviteEmailInput = findViewById(R.id.inviteEmailInput);
        Button inviteButton = findViewById(R.id.inviteButton);

        String circleId = getIntent().getStringExtra("circleId");
        String circleName = groupName;

        SavingsCircleDetailsViewModel detailsViewModel =
                new ViewModelProvider(this).get(SavingsCircleDetailsViewModel.class);

        detailsViewModel.listenToSavingsCircle(circleId);

        detailsViewModel.getContributions().observe(this, contributionss -> {
            Map<String, String> members = detailsViewModel.getMembers().getValue();
            if (contributionss != null && members != null) {
                StringBuilder mb = new StringBuilder();
                for (String email : members.values()) {
                    Double contribution = contributionss.get(email);
                    mb.append(email).append(": $").append(contribution != null ? contribution : 0);
                    mb.append("\n");
                }
                groupContributionsTextView.setText(mb.toString());
            }
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
}
