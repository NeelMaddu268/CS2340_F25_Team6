package com.example.sprintproject.view;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.sprintproject.R;
import com.example.sprintproject.viewmodel.FirestoreManager;
import com.example.sprintproject.viewmodel.SavingsCircleDetailsViewModel;
import com.google.firebase.auth.FirebaseAuth;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SavingsCircleDetailsActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_savingscircle_details);

        // get the group creation details from the intent
        String groupName = getIntent().getStringExtra("groupName");
        String groupChallengeTitle = getIntent().getStringExtra("groupChallengeTitle");
        double groupChallengeGoal = getIntent().getDoubleExtra("groupChallengeGoal", 0.0);
        String groupFrequency = getIntent().getStringExtra("groupFrequency");
        String groupNotes = getIntent().getStringExtra("groupNotes");
        String circleId = getIntent().getStringExtra("circleId");


        // update the UI with the provided details
        TextView groupNameTextView = findViewById(R.id.groupNameTextView);
        TextView groupChallengeTitleTextView = findViewById(R.id.groupChallengeTitleTextView);
        TextView groupChallengeGoalTextView = findViewById(R.id.groupChallengeGoalTextView);
        TextView groupFrequencyTextView = findViewById(R.id.groupFrequencyTextView);
        TextView groupNotesTextView = findViewById(R.id.groupNotesTextView);
        Button deleteCircleButton = findViewById(R.id.deleteCircleButton);


        groupNameTextView.setText(groupName);
        groupChallengeTitleTextView.setText(groupChallengeTitle);
        groupChallengeGoalTextView.setText(String.valueOf(groupChallengeGoal));
        groupFrequencyTextView.setText(groupFrequency);
        groupNotesTextView.setText(groupNotes);

        EditText inviteEmailInput = findViewById(R.id.inviteEmailInput);
        Button inviteButton = findViewById(R.id.inviteButton);

        String creatorId = getIntent().getStringExtra("creatorId");
        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

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
