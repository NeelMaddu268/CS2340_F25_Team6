package com.example.sprintproject.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.sprintproject.R;
import com.example.sprintproject.viewmodel.DateViewModel;
import com.example.sprintproject.viewmodel.InvitationsViewModel;

import java.util.List;
import java.util.Map;

public class InvitationsFragment extends Fragment {

    private InvitationsViewModel invitationsViewModel;
    private DateViewModel dateViewModel;
    private LinearLayout invitesContainer;

    public InvitationsFragment() {
        super(R.layout.fragment_invitations);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_invitations, container, false);

        invitesContainer = view.findViewById(R.id.invitesContainer);
        invitationsViewModel = new ViewModelProvider(
                requireActivity()).get(InvitationsViewModel.class);
        dateViewModel = new ViewModelProvider(this)
                .get(DateViewModel.class);

        // start listening for invites
        invitationsViewModel.startListening();

        // observe the changes in invites list
        invitationsViewModel.getInvites().observe(getViewLifecycleOwner(), invites -> {
            displayInvites(invites);
        });

        return view;
    }

    private void displayInvites(List<Map<String, Object>> invites) {
        invitesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        if (invites == null || invites.isEmpty()) {
            TextView none = new TextView(requireContext());
            none.setText("No pending invites");
            none.setTextSize(18);
            invitesContainer.addView(none);
            return;
        }

        for (Map<String, Object> invite : invites) {
            View inviteView = inflater.inflate(R.layout.item_invite, invitesContainer, false);

            TextView circleName = inviteView.findViewById(R.id.inviteCircleName);
            TextView fromEmail = inviteView.findViewById(R.id.inviteFromEmail);
            Button acceptBtn = inviteView.findViewById(R.id.acceptInviteButton);
            Button declineBtn = inviteView.findViewById(R.id.declineInviteButton);

            String id = (String) invite.get("circleId");
            String name = (String) invite.get("circleName");
            String from = (String) invite.get("fromEmail");
            String inviteId = (String) invite.get("id");

            circleName.setText("Circle: " + name);
            fromEmail.setText("Invited by: " + from);

            acceptBtn.setOnClickListener(v -> {
                dateViewModel.getCurrentDate().observe(getViewLifecycleOwner(), appDate -> {
                    if (appDate == null) return;
                    invitationsViewModel.respondToInvite(inviteId, true, appDate);
                    Toast.makeText(requireContext(), "Joined " + name + "!", Toast.LENGTH_SHORT).show();
                });
            });

            declineBtn.setOnClickListener(v -> {
                dateViewModel.getCurrentDate().observe(getViewLifecycleOwner(), appDate -> {
                    if (appDate == null) return;
                    invitationsViewModel.respondToInvite(inviteId, false, appDate);
                    Toast.makeText(requireContext(), "Declined invite.", Toast.LENGTH_SHORT).show();
                });
            });

            invitesContainer.addView(inviteView);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        invitationsViewModel.stopListening();
    }
}
