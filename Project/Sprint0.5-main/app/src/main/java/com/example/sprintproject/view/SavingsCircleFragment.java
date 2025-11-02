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
import com.example.sprintproject.model.SavingsCircle;
import com.example.sprintproject.viewmodel.BudgetsFragmentViewModel;
import com.example.sprintproject.viewmodel.ExpensesFragmentViewModel;
import com.example.sprintproject.viewmodel.SavingsCircleCreationViewModel;
import com.example.sprintproject.viewmodel.SavingsCircleFragmentViewModel;

import java.util.ArrayList;
import java.util.List;

public class SavingsCircleFragment extends Fragment {

    private RecyclerView recyclerView;
    private SavingsCircleFragmentViewModel savingsCircleFragmentViewModel;
    private SavingsCircleAdapter adapter;
    private BudgetsFragmentViewModel budgetsFragmentViewModel;

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
            intent.putExtra("groupName", savings.getName());
            intent.putExtra("groupEmail", savings.getEmail());
            intent.putExtra("groupInvite", savings.getInvite());
            intent.putExtra("groupChallengeTitle", savings.getTitle());
            intent.putExtra("groupChallengeGoal", savings.getGoal());
            intent.putExtra("groupFrequency", savings.getFrequency());
            intent.putExtra("groupNotes", savings.getNotes());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        savingsCircleFragmentViewModel = new ViewModelProvider(requireActivity())
                .get(SavingsCircleFragmentViewModel.class);
        Button addGroup = view.findViewById(R.id.addGroup);
        addGroup.setOnClickListener(v -> {
            View popupView = getLayoutInflater().inflate(R.layout.popup_savingscircle_creation, null);
            AlertDialog dialog = new AlertDialog.Builder(requireActivity())
                    .setView(popupView)
                    .create();
            EditText groupName = popupView.findViewById(R.id.GroupName);
            EditText groupEmail = popupView.findViewById(R.id.GroupEmail);
            EditText groupInvite = popupView.findViewById(R.id.GroupInvite);
            EditText groupChallengeTitle = popupView.findViewById(R.id.GroupChallengeTitle);
            EditText groupChallengeGoal = popupView.findViewById(R.id.GroupChallengeGoal);
            EditText groupFrequency = popupView.findViewById(R.id.GroupFrequency);
            EditText groupNotes = popupView.findViewById(R.id.GroupNotes);
            Button createBtn = popupView.findViewById(R.id.createGroupButton);
            Button closeButton = popupView.findViewById(R.id.closeButton);
            SavingsCircleCreationViewModel savingsCreationViewModel = new SavingsCircleCreationViewModel();

            closeButton.setOnClickListener(view1 -> dialog.dismiss());

            createBtn.setOnClickListener(view1 -> {
                String name = groupName.getText().toString();
                String email = groupEmail.getText().toString();
                String invite = groupInvite.getText().toString();
                String title = groupChallengeTitle.getText().toString();
                String goal = groupChallengeGoal.getText().toString();
                String frequency = groupFrequency.getText().toString();
                String notes = groupNotes.getText().toString();

                if (name.isEmpty()) {
                    groupName.setError("Please enter a name");
                    return;
                }
                if (email.isEmpty()) {
                    groupEmail.setError("Please enter an email");
                    return;
                }
                if (invite.isEmpty()) {
                    groupInvite.setError("Please enter an invite");
                    return;
                }
                if (title.isEmpty()) {
                    groupInvite.setError("Please enter a challenge title");
                }
                if (goal.isEmpty()) {
                    groupInvite.setError("Please enter a challenge goal");
                }

                if (frequency.isEmpty()) {
                    groupInvite.setError("Please enter a frequency");
                }

                dialog.dismiss();

//                boolean isValid = true;
//                try {
//                    if (name.equals("")) {
//                        expenseName.setError("Please enter a name");
//                        isValid = false;
//                    }
//
//                } catch (NumberFormatException e) {
//                    expenseAmount.setError("Amount must be a number");
//                    isValid = false;
//                }
//                if (isValid) {
//                    savingsCircleCreationViewModel.createSavingsCircle(name, email, invite, title,
//                            goal, frequency, notes, () -> {
//                                budgetsFragmentViewModel.loadBudgets(); //Refresh UI properly
//                            });
//                    dialog.dismiss();
//                    groupName.setText("");
//                    groupEmail.setText("");
//                    groupInvite.setText("");
//                    groupChallengeTitle.setText("");
//                    groupChallengeGoal.setText("");
//                    groupFrequency.setText("");
//                    groupNotes.setText("");
//                }
            });
            dialog.show();
        });
        return view;
    }
}