package com.example.sprintproject.view;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.sprintproject.R;

public class ExpenseLogFragment extends Fragment {

    private Button addExpense;

    public ExpenseLogFragment() {
        super(R.layout.fragment_expenselog);
    }

    @Override

    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        
        View view = super.onCreateView(inflater, container, savedInstanceState);

        addExpense = view.findViewById(R.id.addExpense);

        EdgeToEdge.enable(requireActivity());
        ViewCompat.setOnApplyWindowInsetsListener(

                view.findViewById(R.id.expenselog_layout),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );
                    return insets;
                }
        );

        addExpense.setOnClickListener(v -> {
            View popupView = getLayoutInflater().inflate(R.layout.popup_expense_creation, null);

            AlertDialog dialog = new AlertDialog.Builder(requireActivity())
                    .setView(popupView)
                    .create();

            Button closeButton = popupView.findViewById(R.id.closeButton);
            closeButton.setOnClickListener(view1 -> dialog.dismiss());

            dialog.show();
        });

        return view;
    }
}
