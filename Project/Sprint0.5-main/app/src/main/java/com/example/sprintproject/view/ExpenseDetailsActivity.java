package com.example.sprintproject.view;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sprintproject.R;

import java.util.Locale;

public class ExpenseDetailsActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_details);

        // Get the budget details from the intent
        String expenseName = getIntent().getStringExtra("expenseName");
        double expenseAmount = getIntent().getDoubleExtra("expenseAmount", 0.0);
        String expenseCategory = getIntent().getStringExtra("expenseCategory");
        String expenseDate = getIntent().getStringExtra("expenseDate");
        String expenseNotes = getIntent().getStringExtra("expenseNotes");

        // Update the UI with the budget details
        TextView expenseNameTextView = findViewById(R.id.expenseNameTextView);
        TextView expenseAmountTextView = findViewById(R.id.expenseAmountTextView);
        TextView expenseCategoryTextView = findViewById(R.id.expenseCategoryTextView);
        TextView expenseDateTextView = findViewById(R.id.expenseDateTextView);
        TextView expenseNotesTextView = findViewById(R.id.expenseNotesTextView);


        expenseNameTextView.setText(expenseName);
        expenseAmountTextView.setText(String.format((Locale.US), "$%.2f", expenseAmount));
        expenseCategoryTextView.setText(expenseCategory);
        expenseDateTextView.setText(expenseDate);

        if (expenseNotes == null || expenseNotes.trim().isEmpty()) {
            expenseNotesTextView.setText("Notes: None");
        } else {
            expenseNotesTextView.setText("Notes: " + expenseNotes);
        }

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(view -> finish());
    }
}
