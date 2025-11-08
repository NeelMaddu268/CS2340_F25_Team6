package com.example.sprintproject.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.sprintproject.R;
import com.example.sprintproject.viewmodel.SavingsCircleDetailsViewModel;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

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

        // update the UI with the provided details
        TextView groupNameTextView = findViewById(R.id.groupNameTextView);
        TextView groupEmailTextView = findViewById(R.id.groupEmailTextView);
        TextView groupChallengeTitleTextView = findViewById(R.id.groupChallengeTitleTextView);
        TextView groupChallengeGoalTextView = findViewById(R.id.groupChallengeGoalTextView);
        TextView groupFrequencyTextView = findViewById(R.id.groupFrequencyTextView);
        TextView groupNotesTextView = findViewById(R.id.groupNotesTextView);
        TextView groupCreationTextView = findViewById(R.id.groupCreationTextView);

        groupNameTextView.setText(groupName);
        groupEmailTextView.setText(String.join(", ", groupEmails));
        groupChallengeTitleTextView.setText(groupChallengeTitle);
        groupChallengeGoalTextView.setText("$" + groupChallengeGoal);
        groupFrequencyTextView.setText(groupFrequency);
        groupNotesTextView.setText(groupNotes);
        groupCreationTextView.setText(creationDate);

        EditText inviteEmailInput = findViewById(R.id.inviteEmailInput);
        Button inviteButton = findViewById(R.id.inviteButton);

        String circleId = getIntent().getStringExtra("circleId");
        String circleName = groupName;

        SavingsCircleDetailsViewModel detailsViewModel =
                new ViewModelProvider(this).get(SavingsCircleDetailsViewModel.class);

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
