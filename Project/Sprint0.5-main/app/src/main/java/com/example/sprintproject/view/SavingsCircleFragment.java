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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sprintproject.R;
import com.example.sprintproject.viewmodel.SavingsCircleCreationViewModel;
import com.example.sprintproject.viewmodel.SavingsCircleFragmentViewModel;

import java.util.ArrayList;

public class SavingsCircleFragment extends Fragment {

    private RecyclerView recyclerView;
    private SavingsCircleAdapter adapter;
    private SavingsCircleFragmentViewModel savingsCircleFragmentViewModel;
    private SavingsCircleCreationViewModel savingsCircleCreationViewModel;

    public SavingsCircleFragment() {
        super(R.layout.fragment_savingscircle);
    }

    @Override

    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {

        View view = super.onCreateView(inflater, container, savedInstanceState);

        // Initialize UI elements here
        EdgeToEdge.enable(requireActivity());
        ViewCompat.setOnApplyWindowInsetsListener(
                view.findViewById(R.id.savings_circle_layout),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left,
                            systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                });

        recyclerView = view.findViewById(R.id.savingsCircleRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        savingsCircleFragmentViewModel = new ViewModelProvider(requireActivity())
                .get(SavingsCircleFragmentViewModel.class);
        adapter = new SavingsCircleAdapter(savings -> {
            Intent intent = new Intent(requireContext(), SavingsCircleDetailsActivity.class);
            intent.putExtra("circleId", savings.getId());
            intent.putExtra("groupName", savings.getName());
            intent.putExtra("groupChallengeTitle", savings.getTitle());
            intent.putExtra("groupChallengeGoal", savings.getGoal());
            intent.putExtra("groupFrequency", savings.getFrequency());
            intent.putExtra("groupNotes", savings.getNotes());
            intent.putExtra("creatorId", savings.getCreatorId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        savingsCircleFragmentViewModel = new ViewModelProvider(requireActivity())
                .get(SavingsCircleFragmentViewModel.class);
        savingsCircleFragmentViewModel.loadSavingsCircle();
        savingsCircleCreationViewModel = new ViewModelProvider(this)
                .get(SavingsCircleCreationViewModel.class);
        Button addGroup = view.findViewById(R.id.addGroup);
        savingsCircleFragmentViewModel.getSavingsCircle().observe(
                getViewLifecycleOwner(),
                list -> adapter.submitList(list == null ? null : new ArrayList<>(list))
        );
        addGroup.setOnClickListener(v -> {
            View popupView = getLayoutInflater().inflate(
                    R.layout.popup_savingscircle_creation, null);
            AlertDialog dialog = new AlertDialog.Builder(requireActivity())
                    .setView(popupView)
                    .create();
            EditText groupName = popupView.findViewById(R.id.GroupName);
            EditText groupChallengeTitle = popupView.findViewById(R.id.GroupChallengeTitle);
            EditText groupChallengeGoal = popupView.findViewById(R.id.GroupChallengeGoal);
            EditText groupNotes = popupView.findViewById(R.id.GroupNotes);
            Spinner groupFrequency = popupView.findViewById(R.id.GroupFrequency);
            ArrayAdapter<String> frequencyAdapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    new String[]{"Weekly", "Monthly"}
            );
            frequencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            groupFrequency.setAdapter(frequencyAdapter);
            Button createBtn = popupView.findViewById(R.id.createGroupButton);
            Button closeButton = popupView.findViewById(R.id.closeButton);

            closeButton.setOnClickListener(view1 -> dialog.dismiss());

            createBtn.setOnClickListener(view1 -> {
                String name = groupName.getText().toString().trim();
                String title = groupChallengeTitle.getText().toString().trim();
                String goal = groupChallengeGoal.getText().toString().trim();
                String frequency = groupFrequency.getSelectedItem().toString();
                String notes = groupNotes.getText().toString().trim();

                if (!frequency.equals("Weekly") && !frequency.equals("Monthly")) {
                    Toast.makeText(requireContext(),
                            "Invalid frequency", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (name.isEmpty()) {
                    groupName.setError("Please enter a name");
                    return;
                }
                if (title.isEmpty()) {
                    groupChallengeTitle.setError("Please enter a challenge title");
                    return;
                }
                if (goal.isEmpty()) {
                    groupChallengeGoal.setError("Please enter a challenge goal");
                    return;
                }

                try {
                    double goalAmount = Double.parseDouble(goal);
                    if (goalAmount <= 0) {
                        groupChallengeGoal.setError("Amount must be greater than 0");
                        return;
                    }
                } catch (NumberFormatException e) {
                    groupChallengeGoal.setError("Invalid number");
                    return;
                }

                savingsCircleCreationViewModel.createUserSavingsCircle(
                        name, title, goal, frequency, notes
                );

                savingsCircleCreationViewModel.getText()
                        .observe(getViewLifecycleOwner(), message -> {
                            if (message != null && !message.isEmpty()) {
                                Toast.makeText(requireContext(),
                                        message, Toast.LENGTH_SHORT).show();
                            }
                        });
                Toast.makeText(requireContext(),
                        "Savings Circle created!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
            dialog.show();
        });
        return view;
    }
}