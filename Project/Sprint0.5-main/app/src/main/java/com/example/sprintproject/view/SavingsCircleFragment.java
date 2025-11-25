// The fragment displays all of the savings circles that the user is a part of and updates each circle based on the app's data
// and lets the user view more details for each circle through clicks. It also allows the user to create new circles.

package com.example.sprintproject.view;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sprintproject.R;
import com.example.sprintproject.model.AppDate;
import com.example.sprintproject.viewmodel.DateViewModel;
import com.example.sprintproject.viewmodel.SavingsCircleCreationViewModel;
import com.example.sprintproject.viewmodel.SavingsCircleFragmentViewModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SavingsCircleFragment extends Fragment {

    private SavingsCircleFragmentViewModel savingsCircleFragmentViewModel;
    private SavingsCircleCreationViewModel savingsCircleCreationViewModel;
    private DateViewModel dateViewModel;

    public SavingsCircleFragment() {
        super(R.layout.fragment_savingscircle);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view == null) return null;

        setupInsets(view);
        initViewModels();
        SavingsCircleAdapter adapter = setupRecycler(view);

        observeSavingsCircles(adapter);
        observeAppDateForRowUpdates();
        loadInitialCircles();
        observeCreationMessages();   // observe once, not inside click

        setupAddGroupButton(view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        AppDate d = (dateViewModel != null) ? dateViewModel.getCurrentDate().getValue() : null;
        if (savingsCircleFragmentViewModel != null) {
            savingsCircleFragmentViewModel.setAppDate(d);
        }
    }

    /* ------------------------- setup helpers ------------------------- */

    private void setupInsets(View view) {
        EdgeToEdge.enable(requireActivity());
        ViewCompat.setOnApplyWindowInsetsListener(
                view.findViewById(R.id.savings_circle_layout),
                (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                }
        );
    }

    private void initViewModels() {
        savingsCircleFragmentViewModel =
                new ViewModelProvider(requireActivity()).get(SavingsCircleFragmentViewModel.class);
        savingsCircleCreationViewModel =
                new ViewModelProvider(this).get(SavingsCircleCreationViewModel.class);
        dateViewModel =
                new ViewModelProvider(requireActivity()).get(DateViewModel.class);
    }

    private SavingsCircleAdapter setupRecycler(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.savingsCircleRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        SavingsCircleAdapter adapter = new SavingsCircleAdapter(savings -> {
            Intent intent = new Intent(requireContext(), SavingsCircleDetailsActivity.class);
            putSavingsCircleExtras(intent, savings);
            putCurrentAppDateExtras(intent);
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
        return adapter;
    }

    private void observeSavingsCircles(SavingsCircleAdapter adapter) {
        savingsCircleFragmentViewModel.getSavingsCircle().observe(
                getViewLifecycleOwner(),
                list -> adapter.submitList(list == null ? null : new ArrayList<>(list))
        );
    }

    private void observeAppDateForRowUpdates() {
        dateViewModel.getCurrentDate().observe(
                getViewLifecycleOwner(),
                appDate -> savingsCircleFragmentViewModel.setAppDate(appDate)
        );
    }

    private void loadInitialCircles() {
        AppDate initial = dateViewModel.getCurrentDate().getValue();
        if (initial != null) {
            savingsCircleFragmentViewModel.loadSavingsCircleFor(initial);
        } else {
            savingsCircleFragmentViewModel.loadSavingsCircle();
        }
    }

    private void setupAddGroupButton(View view) {
        Button addGroup = view.findViewById(R.id.addGroup);
        addGroup.setOnClickListener(v -> showCreateGroupDialog());
    }

    private void observeCreationMessages() {
        savingsCircleCreationViewModel.getText()
                .observe(getViewLifecycleOwner(), message -> {
                    if (message != null && !message.isEmpty()) {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /* ------------------------- navigation extras ------------------------- */

    private void putSavingsCircleExtras(Intent intent,
                                        com.example.sprintproject.model.SavingsCircle savings) {
        intent.putExtra("circleId", savings.getId());
        intent.putExtra("groupName", savings.getName());

        intent.putStringArrayListExtra(
                "groupEmails",
                savings.getMemberEmails() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(savings.getMemberEmails())
        );

        intent.putExtra("groupInvite", savings.getInvite());
        intent.putExtra("groupChallengeTitle", savings.getTitle());
        intent.putExtra("groupChallengeGoal", savings.getGoal());
        intent.putExtra("groupFrequency", savings.getFrequency());
        intent.putExtra("groupNotes", savings.getNotes());

        if (savings.getCreatorDateJoined() != null) {
            intent.putExtra("creationDate", savings.getCreatorDateJoined().toIso());
        }

        intent.putExtra("datesJoined", (Serializable) savings.getDatesJoined());
        intent.putExtra("contributions", (Serializable) savings.getContributions());
        intent.putExtra("creatorId", savings.getCreatorId());
    }

    private void putCurrentAppDateExtras(Intent intent) {
        AppDate cur = dateViewModel.getCurrentDate().getValue();
        if (cur == null) return;

        intent.putExtra("appYear", cur.getYear());
        intent.putExtra("appMonth", cur.getMonth());
        intent.putExtra("appDay", cur.getDay());
    }

    /* ------------------------- create group dialog ------------------------- */

    private void showCreateGroupDialog() {
        View popupView = getLayoutInflater()
                .inflate(R.layout.popup_savingscircle_creation, null);

        AlertDialog dialog = new AlertDialog.Builder(requireActivity())
                .setView(popupView)
                .create();

        CreateDialogViews dv = bindCreateDialogViews(popupView);
        setupFrequencySpinner(dv.groupFrequency);

        dv.closeButton.setOnClickListener(v -> dialog.dismiss());
        dv.createBtn.setOnClickListener(v -> onCreateGroupClicked(dv, dialog));

        dialog.show();
    }

    private CreateDialogViews bindCreateDialogViews(View popupView) {
        CreateDialogViews dv = new CreateDialogViews();
        dv.groupName = popupView.findViewById(R.id.GroupName);
        dv.groupChallengeTitle = popupView.findViewById(R.id.GroupChallengeTitle);
        dv.groupChallengeGoal = popupView.findViewById(R.id.GroupChallengeGoal);
        dv.groupNotes = popupView.findViewById(R.id.GroupNotes);
        dv.groupFrequency = popupView.findViewById(R.id.GroupFrequency);
        dv.createBtn = popupView.findViewById(R.id.createGroupButton);
        dv.closeButton = popupView.findViewById(R.id.closeButton);
        return dv;
    }

    private void setupFrequencySpinner(Spinner groupFrequency) {
        List<String> freqs = new ArrayList<>();
        freqs.add("Weekly");
        freqs.add("Monthly");

        ArrayAdapter<String> frequencyAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                freqs
        );
        frequencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        groupFrequency.setAdapter(frequencyAdapter);
    }

    private void onCreateGroupClicked(CreateDialogViews dv, AlertDialog dialog) {
        String name = dv.groupName.getText().toString().trim();
        String title = dv.groupChallengeTitle.getText().toString().trim();
        String goal = dv.groupChallengeGoal.getText().toString().trim();
        String frequency = String.valueOf(dv.groupFrequency.getSelectedItem());
        String notes = dv.groupNotes.getText().toString().trim();

        if (!validateCreateInputs(dv, name, title, goal, frequency)) {
            return;
        }

        AppDate appDate = dateViewModel.getCurrentDate().getValue();
        if (appDate == null) {
            Toast.makeText(requireContext(),
                    "Date not ready. Try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        savingsCircleCreationViewModel.createUserSavingsCircle(
                name, title, goal, frequency, notes, appDate
        );

        Toast.makeText(requireContext(),
                "Savings Circle created!", Toast.LENGTH_SHORT).show();

        dialog.dismiss();
        clearCreateInputs(dv);
    }

    private boolean validateCreateInputs(CreateDialogViews dv,
                                         String name,
                                         String title,
                                         String goal,
                                         String frequency) {
        if (!"Weekly".equals(frequency) && !"Monthly".equals(frequency)) {
            Toast.makeText(requireContext(),
                    "Invalid frequency", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (name.isEmpty()) {
            dv.groupName.setError("Please enter a name");
            return false;
        }

        if (title.isEmpty()) {
            dv.groupChallengeTitle.setError("Please enter a challenge title");
            return false;
        }

        if (goal.isEmpty()) {
            dv.groupChallengeGoal.setError("Please enter a challenge goal");
            return false;
        }

        try {
            double goalAmount = Double.parseDouble(goal);
            if (goalAmount <= 0) {
                dv.groupChallengeGoal.setError("Amount must be greater than 0");
                return false;
            }
        } catch (NumberFormatException e) {
            dv.groupChallengeGoal.setError("Invalid number");
            return false;
        }

        return true;
    }

    private void clearCreateInputs(CreateDialogViews dv) {
        dv.groupName.setText("");
        dv.groupChallengeTitle.setText("");
        dv.groupChallengeGoal.setText("");
        dv.groupNotes.setText("");
        dv.groupFrequency.setSelection(0);
    }

    private static class CreateDialogViews {
        EditText groupName;
        EditText groupChallengeTitle;
        EditText groupChallengeGoal;
        EditText groupNotes;
        Spinner groupFrequency;
        Button createBtn;
        Button closeButton;
    }
}




