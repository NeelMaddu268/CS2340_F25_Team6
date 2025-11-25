// The adapter shows the incoming savings circle invites and lets the user accept or decline the offer.
// It observes the current date when responding to the invites and updates the firestore data accordingly, while displaying confirmation messages.

package com.example.sprintproject.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sprintproject.R;
import com.example.sprintproject.viewmodel.DateViewModel;
import com.example.sprintproject.viewmodel.InvitationsViewModel;

import java.util.List;
import java.util.Map;

public class InvitationsAdapter extends RecyclerView.Adapter<InvitationsAdapter.InviteViewHolder> {

    private List<Map<String, Object>> invites;
    private final InvitationsViewModel invitationsViewModel;
    private final DateViewModel dateViewModel;
    private final Context context;
    private final LifecycleOwner lifecycleOwner;

    public InvitationsAdapter(List<Map<String, Object>> invites,
                              InvitationsViewModel invitationsViewModel,
                              DateViewModel dateViewModel, Context context,
                              LifecycleOwner lifecycleOwner) {
        this.invites = invites;
        this.invitationsViewModel = invitationsViewModel;
        this.dateViewModel = dateViewModel;
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
    }

    public void updateInvites(List<Map<String, Object>> newInvites) {
        this.invites = newInvites;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public InviteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_invite, parent, false);
        return new InviteViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull InviteViewHolder holder, int position) {
        Map<String, Object> invite = invites.get(position);
        String circleName = (String) invite.get("circleName");
        String fromEmail = (String) invite.get("fromEmail");
        String inviteId = (String) invite.get("id");

        holder.circleName.setText(circleName);
        holder.fromEmail.setText("Invited by: " + fromEmail);

        holder.acceptBtn.setOnClickListener(v ->
            dateViewModel.getCurrentDate().observe(lifecycleOwner, appDate -> {
                if (appDate == null) {
                    return;
                }
                invitationsViewModel.respondToInvite(inviteId, true, appDate);
                Toast.makeText(context, "Joined " + circleName + "!", Toast.LENGTH_SHORT).show();
            })
        );

        holder.declineBtn.setOnClickListener(v ->
            dateViewModel.getCurrentDate().observe(lifecycleOwner, appDate -> {
                if (appDate == null) {
                    return;
                }
                invitationsViewModel.respondToInvite(inviteId, false, appDate);
                Toast.makeText(context, "Declined invite.", Toast.LENGTH_SHORT).show();
            })
        );
    }

    @Override
    public int getItemCount() {
        return invites != null ? invites.size() : 0;
    }

    static class InviteViewHolder extends RecyclerView.ViewHolder {
        private TextView circleName;
        private TextView fromEmail;
        private Button acceptBtn;
        private Button declineBtn;

        public InviteViewHolder(@NonNull View itemView) {
            super(itemView);
            circleName = itemView.findViewById(R.id.inviteCircleName);
            fromEmail = itemView.findViewById(R.id.inviteFromEmail);
            acceptBtn = itemView.findViewById(R.id.acceptInviteButton);
            declineBtn = itemView.findViewById(R.id.declineInviteButton);
        }
    }
}