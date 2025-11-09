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
    private InvitationsAdapter adapter;
    private TextView noInvitesText;

    public InvitationsFragment() {
        super(R.layout.fragment_invitations);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.invitationsRecyclerView);
        noInvitesText = view.findViewById(R.id.noInvitesText);
        invitationsViewModel = new InvitationsViewModel();
        adapter = new InvitationsAdapter(new ArrayList<>(),
                invitationsViewModel, new ViewModelProvider(this)
                .get(DateViewModel.class), requireContext(), getViewLifecycleOwner());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        invitationsViewModel.startListening();

        invitationsViewModel.getInvites().observe(getViewLifecycleOwner(), invites -> {
            adapter.updateInvites(invites);
            noInvitesText.setVisibility(invites == null
                    || invites.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        invitationsViewModel.stopListening();
    }
}