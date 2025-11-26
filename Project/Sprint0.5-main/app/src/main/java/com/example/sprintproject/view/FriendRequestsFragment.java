package com.example.sprintproject.view;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sprintproject.R;
import com.example.sprintproject.viewmodel.FriendRequestsViewModel;

import java.util.ArrayList;


public class FriendRequestsFragment extends Fragment {
    private FriendRequestsViewModel friendRequestsViewModel;

    public FriendRequestsFragment() {
        super(R.layout.fragment_invitations);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.invitationsRecyclerView);
        TextView noInvites = view.findViewById(R.id.noInvitesText);

        friendRequestsViewModel = new ViewModelProvider(requireActivity())
                .get(FriendRequestsViewModel.class);

        FriendRequestsAdapter adapter = new FriendRequestsAdapter(
                new ArrayList<>(),
                friendRequestsViewModel,
                requireContext()
        );

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        friendRequestsViewModel.getFriendRequests().observe(getViewLifecycleOwner(), requests -> {
            adapter.updateRequests(requests);
            noInvites.setVisibility((requests == null
                    || requests.isEmpty()) ? View.VISIBLE : View.GONE);
        });

        friendRequestsViewModel.startListeningForRequests();
    }

    public void onDestroyView() {
        super.onDestroyView();
        friendRequestsViewModel.stopListeningForRequests();
    }
}
