// The fragment shos the user's invitations using a RecyclerView and updates the UI as the invites are accepted or declined.
// Listens for real time invite changes and updates the UI accordingly.

package com.example.sprintproject.view;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sprintproject.R;
import com.example.sprintproject.viewmodel.DateViewModel;
import com.example.sprintproject.viewmodel.InvitationsViewModel;

import java.util.ArrayList;

public class InvitationsFragment extends Fragment {

    private InvitationsViewModel invitationsViewModel;
    public InvitationsFragment() {
        super(R.layout.fragment_invitations);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.invitationsRecyclerView);
        final TextView noInvitesText = view.findViewById(R.id.noInvitesText);

        //use provider, not new()
        invitationsViewModel = new ViewModelProvider(requireActivity())
                .get(InvitationsViewModel.class);

        //shared app date VM
        DateViewModel dateVM = new ViewModelProvider(requireActivity())
                .get(DateViewModel.class);

        final InvitationsAdapter adapter = new InvitationsAdapter(
                new ArrayList<>(),
                invitationsViewModel,
                dateVM,
                requireContext(),
                getViewLifecycleOwner()
        );

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        invitationsViewModel.getInvites().observe(getViewLifecycleOwner(), invites -> {
            adapter.updateInvites(invites);
            noInvitesText.setVisibility(invites == null || invites.isEmpty()
                    ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (invitationsViewModel != null) {
            invitationsViewModel.startListening();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (invitationsViewModel != null) {
            invitationsViewModel.stopListening();
        }
    }
}
