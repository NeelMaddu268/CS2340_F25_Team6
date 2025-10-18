package com.example.sprintproject.view;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.sprintproject.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class BudgetsFragment extends Fragment {

    private Button addBudget;

    public BudgetsFragment() {
        super(R.layout.fragment_budgets);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        addBudget = view.findViewById(R.id.addBudget);

        EdgeToEdge.enable(requireActivity());
        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.budgets_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        addBudget.setOnClickListener(v -> {
            View popupView = getLayoutInflater().inflate(R.layout.popup_budget_creation, null);

            AlertDialog dialog = new AlertDialog.Builder(requireActivity())
                    .setView(popupView)
                    .create();

            EditText name = popupView.findViewById(R.id.nameInput);
            EditText amountInput = popupView.findViewById(R.id.amountInput);
            EditText frequency = popupView.findViewById(R.id.frequencyInput);
            EditText startDateInput = popupView.findViewById(R.id.startDateInput);

            Button createButton = popupView.findViewById(R.id.createButton);
            createButton.setOnClickListener(
                    view1 -> {
                        String amountText = amountInput.getText().toString();
                        int amount = Integer.parseInt(amountText);
                        String startDateText = startDateInput.getText().toString();
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        Date currentDate = new Date();
                        Date startDate;
                        try {
                            startDate = format.parse(startDateText);
                            if (amount <= 0) {
                                amountInput.setError("Start date must be in the future");

                            } else if (startDate.compareTo(currentDate) < 0) {
                                startDateInput.setError("Start date must be in the future");
                            }
                        } catch (ParseException e) {
                            startDateInput.setError("Start date must be in correct format.");
                        }
                        dialog.dismiss();
                    }
            );

            Button cancelButton = popupView.findViewById(R.id.cancelButton);
            cancelButton.setOnClickListener(view1 -> dialog.dismiss());

            dialog.show();
        });

        return view;
    }
}
