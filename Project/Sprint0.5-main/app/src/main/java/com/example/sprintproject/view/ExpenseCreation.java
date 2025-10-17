package com.example.sprintproject.view;

import android.os.Bundle;
import android.view.View;
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
    private EditText expenseName, expenseAmount, expenseDate;
    private Button createBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.popup_expense_creation);

        expenseName = findViewById(R.id.ExpenseName);
        expenseAmount = findViewById(R.id.ExpenseAmount);
        //expenseCategory = findViewById(R.id.ExpenseCategory);
        expenseDate = findViewById(R.id.ExpenseDate);
        //expenseOther = findViewById(R.id.expenseOther);
        //expenseNotes = findViewById(R.id.expenseNotes);
        createBtn = findViewById(R.id.createExpenseButton);


        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button createExpense = findViewById(R.id.createExpenseButton);
        createBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                String name = expenseName.getText().toString();
                String date = expenseDate.getText().toString();
                String amount = expenseAmount.getText().toString();

//                if(name.isEmpty() || date.isEmpty() || amount.isEmpty()) {
//                    expenseCreationViewModel.createExpense(name, date, amount);
//                }
            }
        });
    }
}