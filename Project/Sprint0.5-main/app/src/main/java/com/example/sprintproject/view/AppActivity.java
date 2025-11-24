package com.example.sprintproject.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.sprintproject.R;
import com.example.sprintproject.databinding.ActivityAppBinding;
import androidx.lifecycle.ViewModelProvider;

import com.example.sprintproject.model.NotificationData;
import com.example.sprintproject.viewmodel.DateViewModel;
import com.example.sprintproject.viewmodel.InvitationsViewModel;
import com.example.sprintproject.viewmodel.NotificationQueueManager;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;


public class AppActivity extends AppCompatActivity {
    private ViewGroup reminderContainer;
    private View reminderView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ActivityAppBinding binding;

        super.onCreate(savedInstanceState);
        binding = ActivityAppBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        DateViewModel dateVM = new ViewModelProvider(this).get(DateViewModel.class);
        dateVM.getCurrentDate().observe(this, d -> {

        });

        InvitationsViewModel invitationsViewModel = new InvitationsViewModel();
        invitationsViewModel.startListening();

        BottomNavigationView nav = binding.bottomNav;
        BadgeDrawable inviteBadge = nav.getOrCreateBadge(R.id.Invites);
        inviteBadge.setVisible(true);
        inviteBadge.setNumber(0);

        invitationsViewModel.getInvites().observe(this, invites -> {
            boolean hasPending = invites != null && !invites.isEmpty();
            inviteBadge.setVisible(hasPending);
            if (!hasPending) {
                inviteBadge.clearNumber();
            }
        });

        // Default fragment
        replaceFragment(new DashboardFragment());


        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.Dashboard) {
                replaceFragment(new DashboardFragment());
            } else if (id == R.id.Budgets) {
                replaceFragment(new BudgetsFragment());
            } else if (id == R.id.ExpenseLogs) {
                replaceFragment(new ExpensesFragment());
            } else if (id == R.id.SavingsCircles) {
                replaceFragment(new SavingsCircleFragment());
            }  else if (id == R.id.Invites) {
                replaceFragment(new InvitationsFragment());
            }  else if (id == R.id.Chatbot) {
                replaceFragment(new ChatbotFragment());
            }
            return true;
        });

        reminderContainer = findViewById(R.id.reminder_container);
        setupReminderListener(nav);

        // Check for missed logs as soon as user gets to dashboard page
        NotificationQueueManager.getInstance().registerDateObserver(dateVM);
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    /**
     * Observes the NotificationQueueManager and handles displaying the pop-up.
     * @param nav Bottom navigation view
     */
    private void setupReminderListener(BottomNavigationView nav) {
        NotificationQueueManager.getInstance().getCurrentReminder().observe(this, reminder -> {
            if (reminder != null) {
                displayReminder(reminder, nav);
            } else {
                dismissReminder();
            }
        });
    }

    /**
     * Displays the small reminder window
     * @param nav Bottom navigation view
     * @param reminder The notification data
     */
    private void displayReminder(NotificationData reminder, BottomNavigationView nav) {
        if (reminderView == null && reminderContainer != null) {
            LayoutInflater inflater = LayoutInflater.from(this);
            reminderView = inflater.inflate(R.layout.popup_reminder, reminderContainer, false);
            reminderContainer.addView(reminderView);
        }

        if (reminderView == null) {
            return;
        }

        TextView titleText = reminderView.findViewById(R.id.reminderTitle);
        TextView messageText = reminderView.findViewById(R.id.reminderMessage);
        Button actionButton = reminderView.findViewById(R.id.reminderActionButton);
        Button dismissButton = reminderView.findViewById(R.id.reminderDismissButton);

        titleText.setText(reminder.getTitle());
        messageText.setText(reminder.getMessage());

        dismissButton.setOnClickListener(v -> NotificationQueueManager
                .getInstance().dismissCurrentReminder());

        if (reminder.getType() == NotificationData.Type.MISSED_LOG) {
            actionButton.setText("Log Expenses");
            actionButton.setVisibility(View.VISIBLE);
            actionButton.setOnClickListener(v -> {
                NotificationQueueManager.getInstance().dismissCurrentReminder();
                nav.setSelectedItemId(R.id.ExpenseLogs);
            });
        } else {
            actionButton.setVisibility(View.GONE);
        }

        if (reminderContainer != null) {
            reminderContainer.setVisibility(View.VISIBLE);
        }
    }

    private void dismissReminder() {
        if (reminderContainer != null && reminderView != null) {
            reminderContainer.removeView(reminderView);
            reminderView = null;
            reminderContainer.setVisibility(View.GONE);
        }
    }
    
}
