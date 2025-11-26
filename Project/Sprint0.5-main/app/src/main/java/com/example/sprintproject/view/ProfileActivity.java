package com.example.sprintproject.view;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.example.sprintproject.viewmodel.FriendRequestsViewModel;
import com.google.firebase.firestore.FirebaseFirestore;

import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sprintproject.R;
import com.example.sprintproject.viewmodel.FirestoreManager;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class ProfileActivity extends AppCompatActivity {
    //private static final int IMAGE_REQUEST = 1;
    //private static final int PERMISSION_REQUEST = 101;
    private int[] animalIcons = {
            R.drawable.cat, R.drawable.monkey, R.drawable.panda,
            R.drawable.lion, R.drawable.bear, R.drawable.dog,
            R.drawable.mouse, R.drawable.bunny
    };
    private ImageView profileImage;
    private int placeholderIcon = R.drawable.baseline_account_circle_24;

    private FriendRequestsViewModel viewModel;
    private FriendsListAdapter friendsAdapter;

    private RecyclerView friendsRecyclerView;
    private EditText emailInput;
    private Button sendRequestButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        TextView userEmail = findViewById(R.id.userEmail);
        TextView totalExpenses = findViewById(R.id.totalExpenses);
        TextView totalBudgets = findViewById(R.id.totalBudgets);
        profileImage = findViewById(R.id.profileImage);
        loadIcon(profileImage);

        emailInput = findViewById(R.id.emailInput);
        sendRequestButton = findViewById(R.id.sendRequestButton);
        friendsRecyclerView = findViewById(R.id.friendsRecyclerView);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userEmail.setText("Email: " + FirebaseAuth.getInstance().getCurrentUser().getEmail());

            loadUserTotals(totalExpenses, totalBudgets);
        }

        profileImage.setOnClickListener(v -> iconPicker());

        ImageButton backBtn = findViewById(R.id.btnBack);
        backBtn.setOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this).get(FriendRequestsViewModel.class);

        friendsAdapter = new FriendsListAdapter(new ArrayList<>(), viewModel, this, this);
        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        friendsRecyclerView.setAdapter(friendsAdapter);

        viewModel.getFriends().observe(this, friends -> friendsAdapter.updateFriendsList(friends));

        viewModel.startListeningForFriends();

        sendRequestButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            if (!email.isEmpty()) {
                FirestoreManager.getInstance().searchByEmail(email).get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (!querySnapshot.isEmpty()) {
                                String approverUid = querySnapshot.getDocuments().get(0).getId();
                                String requesterUid = FirebaseAuth.getInstance().getUid();
                                String requesterEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

                                FirestoreManager.getInstance().sendFriendRequest(
                                        requesterUid,
                                        approverUid,
                                        requesterEmail,
                                        email
                                );
                                Toast.makeText(this, "Request sent to: " + email, Toast.LENGTH_SHORT).show();
                                emailInput.setText("");
                            } else {
                                Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Failed to send request", Toast.LENGTH_SHORT).show()
                        );
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.startListeningForFriends();
        viewModel.stopListeningForRequests();
    }

    private void iconPicker() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(16, 16, 16, 16);

        for (int icon : animalIcons) {
            ImageView imageView = new ImageView(this);
            imageView.setImageResource(icon);
            imageView.setPadding(8, 8, 8, 8);
            imageView.setLayoutParams(new LinearLayout.LayoutParams(100, 100));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setOnClickListener(v -> {
                profileImage.setImageResource(icon);
                saveIcon(icon);
            });
            layout.addView(imageView);
        }
        new AlertDialog.Builder(this)
                .setTitle("Choose a Profile Icon")
                .setView(layout)
                .show();
    }

    private void saveIcon(int iconId) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            return;
        }

        String iconName = getResources().getResourceEntryName(iconId);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("profileIcon", iconName)
                .addOnSuccessListener(e -> Toast.makeText(this, "Profile icon updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update icon", Toast.LENGTH_SHORT).show());
    }

    private void loadIcon(ImageView imageView) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        imageView.setImageResource(placeholderIcon);
                        return;
                    }

                    String iconName = document.getString("profileIcon");
                    if (iconName != null) {
                        int iD = getResources().getIdentifier(iconName, "drawable", getPackageName());
                        imageView.setImageResource(iD);
                    } else {
                        imageView.setImageResource(placeholderIcon);
                    }
                })
                .addOnFailureListener(e -> imageView.setImageResource(placeholderIcon));
    }

    // Extracted to reduce Cognitive Complexity in onCreate()
    private void loadUserTotals(TextView totalExpenses, TextView totalBudgets) {
        String uid = FirestoreManager.getInstance().getCurrentUserId();
        if (uid == null) {
            return;
        }

        FirestoreManager.getInstance().getDb()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        return;
                    }

                    long expenses = document.getLong("totalExpenses") != null
                            ? document.getLong("totalExpenses") : 0;
                    long budgets = document.getLong("totalBudgets") != null
                            ? document.getLong("totalBudgets") : 0;

                    totalExpenses.setText("Expenses: " + expenses);
                    totalBudgets.setText("Budgets: " + budgets);
                });
    }
}

