// The fragment shos the user's invitations using a RecyclerView and
// updates the UI as the invites are accepted or declined.
// Listens for real time invite changes and updates the UI accordingly.

package com.example.sprintproject.view;

import static android.app.ProgressDialog.show;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sprintproject.R;
import com.example.sprintproject.viewmodel.DateViewModel;
import com.example.sprintproject.viewmodel.FriendRequestsViewModel;
import com.example.sprintproject.viewmodel.InvitationsViewModel;

import java.util.ArrayList;

public class InvitationsFragment extends Fragment {

    private InvitationsViewModel invitationsViewModel;
    private FriendRequestsViewModel friendRequestsViewModel;

    public InvitationsFragment() {
        super(R.layout.fragment_invitations);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        //InvitationsAdapter adapter;
        //TextView noInvitesText;
        super.onViewCreated(view, savedInstanceState);
        RecyclerView recyclerView = view.findViewById(R.id.invitationsRecyclerView);
        TextView noInvitesText = view.findViewById(R.id.noInvitesText);

        invitationsViewModel = new ViewModelProvider(this).get(InvitationsViewModel.class);
        friendRequestsViewModel = new ViewModelProvider(this).get(FriendRequestsViewModel.class);

        DateViewModel dateViewModel = new ViewModelProvider(this).get(DateViewModel.class);

        InvitationsAdapter circleAdapter = new InvitationsAdapter(
                new ArrayList<>(),
                invitationsViewModel,
                dateViewModel,
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

        FriendRequestsAdapter friendAdapter = new FriendRequestsAdapter(
                new ArrayList<>(),
                friendRequestsViewModel,
                requireContext(),
                getViewLifecycleOwner()
        );

        ConcatAdapter combinedAdapter = new ConcatAdapter(circleAdapter, friendAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(combinedAdapter);

        friendRequestsViewModel.startListeningForRequests();
        invitationsViewModel.startListening();

        friendRequestsViewModel.getFriendRequests().observe(getViewLifecycleOwner(), requests -> {
            friendAdapter.updateRequests(requests);
            checkEmptyText(noInvitesText);

            if (requests != null & !requests.isEmpty()) {
                Toast.makeText(requireContext(),
                        "You have " + requests.size() +  "pending friend request(s)!",
                        Toast.LENGTH_SHORT).show();
            }
        });

        invitationsViewModel.getInvites().observe(getViewLifecycleOwner(), invites -> {
            circleAdapter.updateInvites(invites);
            checkEmptyText(noInvitesText);
        invitationsViewModel.getInvites().observe(getViewLifecycleOwner(), invites -> {
            adapter.updateInvites(invites);
            noInvitesText.setVisibility(invites == null || invites.isEmpty()
                    ? View.VISIBLE : View.GONE);
        });
    }

    private void checkEmptyText(TextView noInvitesText) {
        boolean noFriendRequests = friendRequestsViewModel.getFriendRequests().getValue() == null
                || friendRequestsViewModel.getFriendRequests().getValue().isEmpty();
        boolean noCircleInvites = invitationsViewModel.getInvites().getValue() == null
                || invitationsViewModel.getInvites().getValue().isEmpty();
        noInvitesText.setVisibility((noFriendRequests && noCircleInvites) ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        friendRequestsViewModel.stopListeningForRequests();
        invitationsViewModel.stopListening();
    }
    public void onStart() {
        super.onStart();
        if (invitationsViewModel != null) {
            invitationsViewModel.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (invitationsViewModel != null) {
            invitationsViewModel.stopListening();
        }
    }
}
