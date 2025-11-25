// This activity handles the users profile info that is pulled from firestore.
// Provides a back button to return to the previous screen.

package com.example.sprintproject.view;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sprintproject.R;
import com.example.sprintproject.viewmodel.FirestoreManager;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        TextView userEmail = findViewById(R.id.userEmail);
        TextView totalExpenses = findViewById(R.id.totalExpenses);
        TextView totalBudgets = findViewById(R.id.totalBudgets);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userEmail.setText("Email: " +
                    FirebaseAuth.getInstance().getCurrentUser().getEmail());

            loadUserTotals(totalExpenses, totalBudgets);
        }

        ImageButton backBtn = findViewById(R.id.btnBack);
        backBtn.setOnClickListener(v -> finish());
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

