// The activity displays info about a specific savings circle. It also manages the invitation process
// of new members to the savings circle along with other features such as deleting the circle, and reacts to
// live updates through firestore.

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
    private TextView statusLineTextView;
    private TextView groupEndingTextView;
    private TextView groupJoinedTextView;

    private EditText inviteEmailInput;
    private Button inviteButton;
    private Button deleteCircleButton;

    // ViewModels
    private SavingsCircleDetailsViewModel detailsViewModel;
    private DateViewModel dateViewModel;

    // Intent data
    private String circleId;
    private String groupName;
    private String groupChallengeTitle;
    private double groupChallengeGoal;
    private String groupFrequency;
    private String groupNotes;
    private String creationDate;
    private String creatorId;
    private ArrayList<String> groupEmails;

    @SuppressWarnings("unchecked")
    private HashMap<String, String> datesJoined;
    @SuppressWarnings("unchecked")
    private HashMap<String, Double> contributions;

    // VM data
    private Map<String, Double> vmContributions; // uid/email -> amount
    private Map<String, String> vmMembers;       // idx -> email/label
    private Map<String, String> vmMemberUids;    // idx -> uid

    // State
    private String currentUid;
    private String currentEmail;
    private AppDate lastAppDate; // NOT device clock

    // Invite gate state
    private final AtomicReference<Map<String, String>> lastDatesRef = new AtomicReference<>();
    // Sonar fix: removed field reevaluateInviteGate

    private static final String WEEKLY_TEXT = "Weekly";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_savingscircle_details);

        initViewModels();
        readIntentExtras();
        seedLastAppDateFromIntent();
        initCurrentUser();

        wireViews();
        populateStaticText();

        setupBackButton();
        setupInviteDeleteControls();

        // Sonar fix: reevaluateInviteGate is LOCAL now
        Runnable reevaluateInviteGate = setupInviteGateReevaluation();

        setupInviteButton();
        wireViewModelObservers(reevaluateInviteGate);

        startListeningAndInitialUI();
    }

    /* ------------------------- init / wiring ------------------------- */

    private void initViewModels() {
        detailsViewModel = new ViewModelProvider(this).get(SavingsCircleDetailsViewModel.class);
        dateViewModel = new ViewModelProvider(this).get(DateViewModel.class);
    }

    private void readIntentExtras() {
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
    }

    private void seedLastAppDateFromIntent() {
        int ay = getIntent().getIntExtra("appYear", -1);
        int am = getIntent().getIntExtra("appMonth", -1);
        int ad = getIntent().getIntExtra("appDay", -1);
        if (ay > 0 && am > 0 && ad > 0) {
            lastAppDate = new AppDate(ay, am, ad);
        }
    }

    private void initCurrentUser() {
        currentUid = FirestoreManager.getInstance().getCurrentUserId();
        currentEmail = (FirebaseAuth.getInstance().getCurrentUser() != null)
                ? FirebaseAuth.getInstance().getCurrentUser().getEmail()
                : null;
    }

    private void wireViews() {
        TextView groupNameTextView = findViewById(R.id.groupNameTextView);
        TextView groupChallengeTitleTextView = findViewById(R.id.groupChallengeTitleTextView);
        TextView groupChallengeGoalTextView = findViewById(R.id.groupChallengeGoalTextView);
        TextView groupFrequencyTextView = findViewById(R.id.groupFrequencyTextView);
        TextView groupNotesTextView = findViewById(R.id.groupNotesTextView);
        TextView groupCreationTextView = findViewById(R.id.groupCreationTextView);
        groupJoinedTextView = findViewById(R.id.groupJoinedTextView);
        groupContributionsTextView = findViewById(R.id.groupContributionsTextView);
        groupEndingTextView = findViewById(R.id.groupEndingTextView);
        groupEmailTextView = findViewById(R.id.groupEmailTextView);
        statusLineTextView = findViewById(R.id.statusLineTextView);

        inviteEmailInput = findViewById(R.id.inviteEmailInput);
        inviteButton = findViewById(R.id.inviteButton);
        deleteCircleButton = findViewById(R.id.deleteCircleButton);

        // set static pieces right after wiring
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
    }

    private void populateStaticText() {
        if (groupEmails != null) {
            groupEmailTextView.setText(TextUtils.join(", ", groupEmails));
        }

        String joined = safeGetJoinedDate(datesJoined, currentUid, currentEmail);
        groupJoinedTextView.setText(joined != null ? joined : "Date not available");

        setInitialEndDateFromCreation();
        setInitialContributionsText();

        statusLineTextView.setText("status ready");
        statusLineTextView.setVisibility(View.VISIBLE);
    }

    private void setInitialEndDateFromCreation() {
        if (WEEKLY_TEXT.equals(groupFrequency)) {
            groupEndingTextView.setText(AppDate.addDays(creationDate, 7, 0));
        } else if ("Monthly".equals(groupFrequency)) {
            groupEndingTextView.setText(AppDate.addDays(creationDate, 0, 1));
        }
    }

    private void setInitialContributionsText() {
        if (groupEmails != null && contributions != null) {
            StringBuilder sb = new StringBuilder();
            for (String email : groupEmails) {
                Double c = contributions.get(email);
                sb.append(email).append(": $").append(c != null ? c : 0).append("\n");
            }
            groupContributionsTextView.setText(sb.toString());
        }
    }

    private void setupBackButton() {
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }

    private void setupInviteDeleteControls() {
        // Hide invite/delete for non-creator
        if (creatorId == null || !creatorId.equals(currentUid)) {
            inviteEmailInput.setVisibility(View.GONE);
            inviteButton.setVisibility(View.GONE);
            deleteCircleButton.setVisibility(View.GONE);
            return;
        }

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

    /* ------------------------- invite gate + invite ------------------------- */

    // Sonar fix: method now RETURNS a Runnable instead of storing a field
    private Runnable setupInviteGateReevaluation() {
        final String freq = groupFrequency;
        final String circleCreatorId = creatorId;

        Runnable reevaluateInviteGate = () -> {
            // Only creator can invite
            if (circleCreatorId == null || !circleCreatorId.equals(currentUid)) {
                setInviteControls(false, inviteEmailInput, inviteButton);
                return;
            }

            Map<String, String> dates = lastDatesRef.get();
            if (dates == null) return;

            AppDate appDate = dateViewModel.getCurrentDate().getValue();
            if (appDate == null) return;

            String creatorJoinIso = dates.get(circleCreatorId);
            if (creatorJoinIso == null) return;

            String creatorEndIso = WEEKLY_TEXT.equals(freq)
                    ? AppDate.addDays(creatorJoinIso, 7, 0)
                    : AppDate.addDays(creatorJoinIso, 0, 1);

            boolean invitesOpen = appDate.toIso().compareTo(creatorEndIso) <= 0;
            setInviteControls(invitesOpen, inviteEmailInput, inviteButton);
        };

        // Reevaluate gate whenever AppDate changes
        dateViewModel.getCurrentDate().observe(this, d -> reevaluateInviteGate.run());

        return reevaluateInviteGate;
    }

    private void setupInviteButton() {
        inviteButton.setOnClickListener(v -> {
            String inviteEmail = inviteEmailInput.getText().toString().trim();
            if (inviteEmail.isEmpty()) {
                inviteEmailInput.setError("Enter an email");
                return;
            }

            observeCurrentAppDateOnce(appDate -> {
                if (appDate == null) return;
                getDetailsVM().sendInvite(circleId, groupName, inviteEmail, appDate.toIso());
            });
        });
    }

    private void observeCurrentAppDateOnce(Observer<AppDate> consumer) {
        AtomicReference<Observer<AppDate>> ref = new AtomicReference<>();
        Observer<AppDate> once = appDate -> {
            dateViewModel.getCurrentDate().removeObserver(ref.get());
            consumer.onChanged(appDate);
        };
        ref.set(once);
        dateViewModel.getCurrentDate().observe(this, once);
    }

    /* ------------------------- vm observers + initial start ------------------------- */

    // Sonar fix: accept local reevaluateInviteGate
    private void wireViewModelObservers(Runnable reevaluateInviteGate) {
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

        detailsViewModel.getMemberJoinDates().observe(this, dates -> {
            lastDatesRef.set(dates);
            updateMaxEndDateFromJoinDates(dates);
            if (reevaluateInviteGate != null) reevaluateInviteGate.run();
        });

        dateViewModel.getCurrentDate().observe(this, appDate -> {
            lastAppDate = appDate;
            updateUIWithAppDate();
        });
    }

    private void updateMaxEndDateFromJoinDates(Map<String, String> dates) {
        if (dates == null || dates.isEmpty()) return;

        String maxEndIso = null;
        for (String joinIso : dates.values()) {
            if (joinIso == null || joinIso.isEmpty()) continue;

            String endIso = WEEKLY_TEXT.equals(groupFrequency)
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

    private void startListeningAndInitialUI() {
        detailsViewModel.listenToSavingsCircle(circleId);

        if (lastAppDate == null) {
            lastAppDate = dateViewModel.getCurrentDate().getValue();
        }
        updateUIWithAppDate();
        groupContributionsTextView.post(this::updateUIWithAppDate);
    }

    /* ------------------------- existing helpers unchanged ------------------------- */

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

    /** Sonar fix: split into helpers to reduce Cognitive Complexity. */
    private void updateUIWithAppDate() {
        if (statusLineTextView == null) {
            return;
        }

        showCalculatingStatus();
        updateContributionsSection();

        int people = (vmMembers != null && !vmMembers.isEmpty()) ? vmMembers.size() : 1;
        double personalTarget = groupChallengeGoal / Math.max(people, 1);

        double myAmt = computeMyContribution();
        updateGoalStatus(myAmt, personalTarget);
    }

    /* ------------------------- UI helpers (new) ------------------------- */

    private void showCalculatingStatus() {
        statusLineTextView.setText("Calculating goal status…");
        statusLineTextView.setTextColor(ContextCompat.getColor(this, R.color.Accent));
        statusLineTextView.setVisibility(View.VISIBLE);
    }

    private void updateContributionsSection() {
        if (groupContributionsTextView == null
                || vmContributions == null
                || vmMembers == null
                || vmMemberUids == null) {
            return;
        }

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

    private double computeMyContribution() {
        if (vmContributions == null) {
            return 0.0;
        }

        Double amt = null;
        if (currentUid != null) {
            amt = vmContributions.get(currentUid);
        }
        if (amt == null && currentEmail != null) {
            amt = vmContributions.get(currentEmail);
        }

        return amt != null ? amt : 0.0;
    }

    private void updateGoalStatus(double myAmt, double target) {
        boolean met = myAmt >= target;

        String text = met ? "Goal met" : "Goal not reached yet";
        int color = met
                ? safeColor(R.color.green, 0xFF3DB85D)
                : safeColor(R.color.red, 0xFFE53935);

        statusLineTextView.setText(text);
        statusLineTextView.setTextColor(color);
        statusLineTextView.setVisibility(View.VISIBLE);
    }

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














