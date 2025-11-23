package com.example.sprintproject.view;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.sprintproject.R;
import com.example.sprintproject.model.AppDate;
import com.example.sprintproject.viewmodel.DateViewModel;
import com.example.sprintproject.viewmodel.FirestoreManager;
import com.example.sprintproject.viewmodel.SavingsCircleDetailsViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class SavingsCircleDetailsActivity extends AppCompatActivity {

    // UI
    private TextView groupContributionsTextView;
    private TextView groupEmailTextView;
    // Single-line status under Contributions
    private TextView statusLineTextView;

    // ViewModels
    private SavingsCircleDetailsViewModel detailsViewModel;
    private DateViewModel dateViewModel;

    // Intent data
    private String circleId;
    private String groupName;
    private double groupChallengeGoal;
    @SuppressWarnings("unchecked") private HashMap<String, String> datesJoined;
    @SuppressWarnings("unchecked") private HashMap<String, Double> contributions;

    // VM data
    private Map<String, Double> vmContributions; // uid/email -> amount
    private Map<String, String> vmMembers;       // idx -> email/label
    private Map<String, String> vmMemberUids;    // idx -> uid

    // State
    private String currentUid;
    private String currentEmail;
    private AppDate lastAppDate; // NOT device clock

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_savingscircle_details);

        String groupChallengeTitle;
        String groupFrequency;
        String groupNotes;
        String creationDate;
        String creatorId;
        TextView groupEndingTextView;
        TextView groupJoinedTextView;
        TextView groupChallengeGoalTextView;
        ArrayList<String> groupEmails;

        String weeklyText = "Weekly";



        // --------- ViewModels ----------
        detailsViewModel = new ViewModelProvider(this).get(SavingsCircleDetailsViewModel.class);
        dateViewModel = new ViewModelProvider(this).get(DateViewModel.class);

        // --------- Intent extras ----------
        groupName = getIntent().getStringExtra("groupName");
        groupEmails = getIntent().getStringArrayListExtra("groupEmails");
        groupChallengeTitle = getIntent().getStringExtra("groupChallengeTitle");
        groupChallengeGoal = getIntent().getDoubleExtra("groupChallengeGoal", 0.0);
        groupFrequency = getIntent().getStringExtra("groupFrequency");
        groupNotes = getIntent().getStringExtra("groupNotes");
        creationDate = getIntent().getStringExtra("creationDate");
        datesJoined = (HashMap<String, String>) getIntent().getSerializableExtra("datesJoined");
        contributions = (HashMap<String, Double>) getIntent().getSerializableExtra("contributions");
        circleId = getIntent().getStringExtra("circleId");
        creatorId = getIntent().getStringExtra("creatorId");

        int ay = getIntent().getIntExtra("appYear", -1);
        int am = getIntent().getIntExtra("appMonth", -1);
        int ad = getIntent().getIntExtra("appDay", -1);
        if (ay > 0 && am > 0 && ad > 0) {
            lastAppDate = new AppDate(ay, am, ad);
        }

        currentUid = FirestoreManager.getInstance().getCurrentUserId();
        currentEmail = (FirebaseAuth.getInstance().getCurrentUser() != null)
                ? FirebaseAuth.getInstance().getCurrentUser().getEmail()
                : null;

        // --------- Wire UI ----------
        TextView groupNameTextView = findViewById(R.id.groupNameTextView);
        TextView groupChallengeTitleTextView = findViewById(R.id.groupChallengeTitleTextView);
        groupChallengeGoalTextView = findViewById(R.id.groupChallengeGoalTextView);
        TextView groupFrequencyTextView = findViewById(R.id.groupFrequencyTextView);
        TextView groupNotesTextView = findViewById(R.id.groupNotesTextView);
        TextView groupCreationTextView = findViewById(R.id.groupCreationTextView);
        groupJoinedTextView = findViewById(R.id.groupJoinedTextView);
        groupContributionsTextView = findViewById(R.id.groupContributionsTextView);
        groupEndingTextView = findViewById(R.id.groupEndingTextView);
        groupEmailTextView = findViewById(R.id.groupEmailTextView);
        statusLineTextView = findViewById(R.id.statusLineTextView);

        statusLineTextView.setText("status ready");
        statusLineTextView.setVisibility(View.VISIBLE);

        // --------- Populate static text ----------
        groupNameTextView.setText(groupName);
        groupChallengeTitleTextView.setText(groupChallengeTitle);
        groupChallengeGoalTextView.setText("$" + groupChallengeGoal);
        groupFrequencyTextView.setText(groupFrequency);
        groupNotesTextView.setText(
                (groupNotes == null || groupNotes.trim().isEmpty())
                        ? "Notes: None"
                        : "Notes: " + groupNotes
        );
        groupCreationTextView.setText(creationDate);

        if (groupEmails != null) {
            groupEmailTextView.setText(TextUtils.join(", ", groupEmails));
        }

        String dateJoinedInitial = safeGetJoinedDate(datesJoined, currentUid, currentEmail);
        groupJoinedTextView.setText(dateJoinedInitial != null
                ? dateJoinedInitial : "Date not available");

        // Initial group end date from creation + frequency
        if (weeklyText.equals(groupFrequency)) {
            groupEndingTextView.setText(AppDate.addDays(creationDate, 7, 0));
        } else if ("Monthly".equals(groupFrequency)) {
            groupEndingTextView.setText(AppDate.addDays(creationDate, 0, 1));
        }

        // Initial contributions text if we have basic data
        if (groupEmails != null && contributions != null) {
            StringBuilder sb = new StringBuilder();
            for (String email : groupEmails) {
                Double c = contributions.get(email);
                sb.append(email).append(": $").append(c != null ? c : 0).append("\n");
            }
            groupContributionsTextView.setText(sb.toString());
        }

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        EditText inviteEmailInput = findViewById(R.id.inviteEmailInput);
        Button inviteButton = findViewById(R.id.inviteButton);
        Button deleteCircleButton = findViewById(R.id.deleteCircleButton);

        // Hide invite/delete for non-creator
        if (creatorId == null || !creatorId.equals(currentUid)) {
            inviteEmailInput.setVisibility(View.GONE);
            inviteButton.setVisibility(View.GONE);
            deleteCircleButton.setVisibility(View.GONE);
        } else {
            deleteCircleButton.setVisibility(View.VISIBLE);
            deleteCircleButton.setOnClickListener(v ->
                    new AlertDialog.Builder(this)
                            .setTitle("Delete Savings Circle")
                            .setMessage("Are you sure you want to delete this savings circle?")
                            .setPositiveButton("Delete", (dialog, which) ->
                                    FirestoreManager.getInstance()
                                            .deleteSavingsCircle(circleId, currentUid)
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(this, "Circle deleted successfully",
                                                        Toast.LENGTH_SHORT).show();
                                                finish();
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(this, "Failed to delete: "
                                                            + e.getMessage(),
                                                            Toast.LENGTH_SHORT).show())
                            )
                            .setNegativeButton("Cancel", null)
                            .show()
            );
        }

        // --------- Invite gate based on AppDate + creator's end date ----------
        final String freq = groupFrequency;
        final String circleCreatorId = creatorId;
        final EditText inviteInputFinal = inviteEmailInput;
        final Button inviteButtonFinal = inviteButton;

        final AtomicReference<Map<String, String>> lastDatesRef =
                new AtomicReference<>();

        final Runnable reevaluateInviteGate = new Runnable() {
            @Override
            public void run() {
                // Only creator can invite
                if (circleCreatorId == null || !circleCreatorId.equals(currentUid)) {
                    setInviteControls(false, inviteInputFinal, inviteButtonFinal);
                    return;
                }

                Map<String, String> dates = lastDatesRef.get();
                if (dates == null) {
                    return;
                }

                AppDate appDate = dateViewModel.getCurrentDate().getValue();
                if (appDate == null) {
                    return;
                }

                String creatorJoinIso = dates.get(circleCreatorId);
                if (creatorJoinIso == null) {
                    return;
                }

                String creatorEndIso = weeklyText.equals(freq)
                        ? AppDate.addDays(creatorJoinIso, 7, 0)
                        : AppDate.addDays(creatorJoinIso, 0, 1);

                boolean invitesOpen = appDate.toIso().compareTo(creatorEndIso) <= 0;
                setInviteControls(invitesOpen, inviteInputFinal, inviteButtonFinal);
            }
        };

        // Reevaluate gate whenever AppDate changes
        dateViewModel.getCurrentDate().observe(this, d -> reevaluateInviteGate.run());

        // --------- Invite button: use AppDate when sending invite ----------
        inviteButton.setOnClickListener(v -> {
            String inviteEmail = inviteEmailInput.getText().toString().trim();
            if (inviteEmail.isEmpty()) {
                inviteEmailInput.setError("Enter an email");
                return;
            }

            // Get current AppDate once, then send invite with appDate iso string
            androidx.lifecycle.Observer<AppDate> once = new androidx.lifecycle.Observer<AppDate>() {
                @Override
                public void onChanged(AppDate appDate) {
                    if (appDate == null) {
                        return;
                    }
                    dateViewModel.getCurrentDate().removeObserver(this);
                    getDetailsVM().sendInvite(circleId, groupName, inviteEmail, appDate.toIso());
                }
            };
            dateViewModel.getCurrentDate().observe(this, once);
        });

        // --------- ViewModels & observers for contributions + status line ----------
        Observer<Object> dataObserver = ignored -> {
            vmContributions = detailsViewModel.getContributions().getValue();
            vmMembers = detailsViewModel.getMembers().getValue();
            vmMemberUids = detailsViewModel.getMemberUids().getValue();
            updateUIWithAppDate();
        };
        detailsViewModel.getContributions().observe(this, dataObserver);
        detailsViewModel.getMembers().observe(this, dataObserver);
        detailsViewModel.getMemberUids().observe(this, dataObserver);

        detailsViewModel.getStatusMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });

        // Member join dates: update end date + reevaluate invite gate
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

        dateViewModel.getCurrentDate().observe(this, appDate -> {
            lastAppDate = appDate;
            updateUIWithAppDate();
        });

        // Start listening after observers are wired
        detailsViewModel.listenToSavingsCircle(circleId);

        if (lastAppDate == null) {
            lastAppDate = dateViewModel.getCurrentDate().getValue();
        }
        updateUIWithAppDate();
        groupContributionsTextView.post(this::updateUIWithAppDate);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUIWithAppDate();
    }

    private SavingsCircleDetailsViewModel getDetailsVM() {
        if (detailsViewModel == null) {
            detailsViewModel = new ViewModelProvider(this)
                    .get(SavingsCircleDetailsViewModel.class);
        }
        return detailsViewModel;
    }

    private String safeGetJoinedDate(Map<String, String> map, String uid, String email) {
        if (map == null) {
            return null;
        }
        if (uid != null && map.containsKey(uid)) {
            return map.get(uid);
        }
        if (email != null && map.containsKey(email)) {
            return map.get(email);
        }
        return null;
    }

    /** Re-render contributions and the single colored status TEXT using AppDate. */
    private void updateUIWithAppDate() {

        if (statusLineTextView == null) {
            return;
        }

        statusLineTextView.setText("Calculating goal status…");
        statusLineTextView.setTextColor(ContextCompat.getColor(this, R.color.Accent));
        statusLineTextView.setVisibility(View.VISIBLE);

        // 1) Rebuild contributions list
        if (groupContributionsTextView != null
                && vmContributions != null
                && vmMembers != null
                && vmMemberUids != null) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : vmMembers.entrySet()) {
                String idx = entry.getKey();
                String label = entry.getValue();
                String uid = vmMemberUids.get(idx);
                Double amt = (uid != null) ? vmContributions.get(uid) : null;
                sb.append(label).append(": $").append(amt != null ? amt : 0.0).append("\n");
            }
            groupContributionsTextView.setText(sb.toString());
            if (groupEmailTextView != null) {
                groupEmailTextView.setText(TextUtils.join(", ", vmMembers.values()));
            }
        }

        // 3) Compute my target and contribution
        int people = (vmMembers != null && !vmMembers.isEmpty()) ? vmMembers.size() : 1;
        double personalTarget = groupChallengeGoal / Math.max(people, 1);

        Double myContribution = null;
        if (vmContributions != null) {
            if (currentUid != null) {
                myContribution = vmContributions.get(currentUid);
            }
            if (myContribution == null && currentEmail != null) {
                myContribution = vmContributions.get(currentEmail);
            }
        }
        double myAmt = myContribution != null ? myContribution : 0.0;

        // 4) Build the TEXT and color for statusLineTextView
        String text;
        int color;

        if (myAmt >= personalTarget) {
            text = "Goal met";
            color = safeColor(R.color.green, 0xFF3DB85D);
        } else {
            text = "Goal not reached yet";
            color = safeColor(R.color.red, 0xFFE53935);
        }

        statusLineTextView.setText(text);
        statusLineTextView.setTextColor(color);
        statusLineTextView.setVisibility(View.VISIBLE);
    }

    /** Return theme color or hex fallback if missing.
     * @param resId the resource ID of the color to retrieve
     * @param fallback the fallback color value to return if the resource is missing
     * @return theme color
     * */
    private int safeColor(int resId, int fallback) {
        try {
            return ContextCompat.getColor(this, resId);
        } catch (Exception e) {
            return fallback;
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











