package com.example.sprintproject.view;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.sprintproject.R;
import com.example.sprintproject.viewmodel.ExpenseCreationViewModel;

public class ExpenseCreation extends AppCompatActivity {

    private ExpenseCreationViewModel expenseCreationViewModel;
    private EditText expenseName;
    private EditText expenseAmount;
    private EditText expenseDate;
    private EditText expenseCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.popup_expense_creation);

        expenseName = findViewById(R.id.ExpenseName);
        expenseAmount = findViewById(R.id.ExpenseAmount);
        expenseCategory = findViewById(R.id.ExpenseCategory);
        expenseDate = findViewById(R.id.ExpenseDate);
        Button createBtn = findViewById(R.id.createExpenseButton);

        expenseCreationViewModel = new ExpenseCreationViewModel();


        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}