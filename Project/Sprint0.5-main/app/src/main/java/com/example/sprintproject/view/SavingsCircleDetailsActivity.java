package com.example.sprintproject.view;

import androidx.appcompat.app.AppCompatActivity;
import com.example.sprintproject.R;
import java.util.Locale;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class SavingsCircleDetailsActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_savingscircle_details);

        // Get the group creation details from the intent
        String groupName = getIntent().getStringExtra("groupName");
        String groupEmail = getIntent().getStringExtra("groupEmail");
        String groupInvite = getIntent().getStringExtra("groupInvite");
        String groupChallengeTitle = getIntent().getStringExtra("groupChallengeTitle");
        String groupChallengeGoal = getIntent().getStringExtra("groupChallenegeGoal");
        String groupFrequency = getIntent().getStringExtra("groupFrequency");
        String groupNotes = getIntent().getStringExtra("groupNotes");

        // Update the UI with the provided details
        TextView groupNameTextView = findViewById(R.id.groupNameTextView);
        TextView groupEmailTextView = findViewById(R.id.groupEmailTextView);
        TextView groupInviteTextView = findViewById(R.id.groupInviteTextView);
        TextView groupChallengeTitleTextView = findViewById(R.id.groupChallengeTitleTextView);
        TextView groupChallengeGoalTextView = findViewById(R.id.groupChallengeGoalTextView);
        TextView groupFrequencyTextView = findViewById(R.id.groupFrequencyTextView);
        TextView groupNotesTextView = findViewById(R.id.groupNotesTextView);

        groupNameTextView.setText(groupName);
        groupEmailTextView.setText(groupEmail);
        groupInviteTextView.setText(groupInvite);
        groupChallengeTitleTextView.setText(groupChallengeTitle);
        groupChallengeGoalTextView.setText(groupChallengeGoal);
        groupFrequencyTextView.setText(groupFrequency);
        groupNotesTextView.setText(groupNotes);

        if (groupNotes == null || groupNotes.trim().isEmpty()) {
            groupNotesTextView.setText("Notes: None");
        } else {
            groupNotesTextView.setText("Notes: " + groupNotes);
        }

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(view -> finish());
    }
}
