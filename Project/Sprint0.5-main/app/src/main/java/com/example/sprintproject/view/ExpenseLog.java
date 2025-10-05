package com.example.sprintproject.view;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.sprintproject.R;

public class ExpenseLog extends AppCompatActivity {
    private Button addExpense;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_expense_log);
        addExpense = findViewById(R.id.addExpense);

        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        addExpense.setOnClickListener(v -> {
            View popupView = getLayoutInflater().inflate(R.layout.popup_expense_creation, null);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(popupView)
                    .create();

            Button closeButton = popupView.findViewById(R.id.closeButton);
            closeButton.setOnClickListener(view -> dialog.dismiss());

            dialog.show();
        });
    }
}