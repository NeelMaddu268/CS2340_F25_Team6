// This fragment displays the users budgets and the savings circles the user is a part of.
// This fragment also manages the navigation to detailed views of the different budgets,
// and allows users to add new budgets.

package com.example.sprintproject.view;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sprintproject.R;
import com.example.sprintproject.model.AppDate;
import com.example.sprintproject.viewmodel.BudgetCreationViewModel;
import com.example.sprintproject.viewmodel.BudgetsFragmentViewModel;
import com.example.sprintproject.viewmodel.DateViewModel;
import com.example.sprintproject.viewmodel.NotificationQueueManager;
import com.example.sprintproject.viewmodel.SavingsCircleFragmentViewModel;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class BudgetsFragment extends Fragment {

    private Button addBudget;
    private BudgetsFragmentViewModel budgetsFragmentViewModel;
    private DateViewModel dateVM;

    public BudgetsFragment() {
        super(R.layout.fragment_budgets);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view == null) {
            return null;
        }

        addBudget = view.findViewById(R.id.addBudget);

        EdgeToEdge.enable(requireActivity());
        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.budgets_layout),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top,
                            systemBars.right, systemBars.bottom);
                    return insets;
                });

        // --- adapters local (Sonar fix) ---
        final BudgetAdapter budgetAdapter = setupBudgetRecyclerView(view);
        final SavingsCircleAdapter savingsCircleAdapter = setupSavingsCircleRecyclerView(view);
        // ---------------------------------

        budgetsFragmentViewModel = new ViewModelProvider(requireActivity())
                .get(BudgetsFragmentViewModel.class);
        dateVM = new ViewModelProvider(requireActivity())
                .get(DateViewModel.class);

        budgetsFragmentViewModel.getBudgets().observe(
                getViewLifecycleOwner(),
                list -> {
                    budgetAdapter.submitList(list == null ? null : new ArrayList<>(list));
                    NotificationQueueManager.getInstance().checkForBudgetWarning(list);
                }
        );

        // local VM (Sonar already ok)
        SavingsCircleFragmentViewModel savingsCircleFragmentViewModel =
                new ViewModelProvider(requireActivity())
                        .get(SavingsCircleFragmentViewModel.class);
        savingsCircleFragmentViewModel.loadSavingsCircle();
        savingsCircleFragmentViewModel.getSavingsCircle().observe(
                getViewLifecycleOwner(),
                list -> savingsCircleAdapter.submitList(list == null ? null : new ArrayList<>(list))
        );

        AppDate seed = dateVM.getCurrentDate().getValue();
        if (seed == null || isToday(seed)) {
            budgetsFragmentViewModel.loadBudgets();
        } else {
            budgetsFragmentViewModel.loadBudgetsFor(seed);
        }

        dateVM.getCurrentDate().observe(getViewLifecycleOwner(), d -> {
            if (d == null || isToday(d)) {
                budgetsFragmentViewModel.loadBudgets();
            } else {
                budgetsFragmentViewModel.loadBudgetsFor(d);
            }
        });

        setupAddBudgetDialog();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dateVM != null) {
            AppDate d = dateVM.getCurrentDate().getValue();
            if (d == null || isToday(d)) {
                budgetsFragmentViewModel.loadBudgets();
            } else {
                budgetsFragmentViewModel.loadBudgetsFor(d);
            }
        }
    }

    private boolean isToday(AppDate d) {
        if (d == null) {
            return false;
        }
        Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH) + 1; // 1..12
        int day = c.get(Calendar.DAY_OF_MONTH);
        return d.getYear() == y && d.getMonth() == m && d.getDay() == day;
    }

    // return adapter instead of storing as field
    private BudgetAdapter setupBudgetRecyclerView(View view) {
        RecyclerView budgetRecyclerView = view.findViewById(R.id.budgetsRecyclerView);
        budgetRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        BudgetAdapter budgetAdapter = new BudgetAdapter(budget -> {
            Intent intent = new Intent(requireContext(), BudgetDetailsActivity.class);
            intent.putExtra("budgetId", budget.getId());
            intent.putExtra("budgetName", budget.getName());
            intent.putExtra("budgetAmount", budget.getAmount());
            intent.putExtra("budgetCategory", budget.getCategory());
            intent.putExtra("budgetFrequency", budget.getFrequency());
            intent.putExtra("budgetStartDate", budget.getStartDate());
            startActivity(intent);
        });

        budgetRecyclerView.setAdapter(budgetAdapter);
        return budgetAdapter;
    }

    // return adapter instead of storing as field
    private SavingsCircleAdapter setupSavingsCircleRecyclerView(View view) {
        RecyclerView savingsCircleRecyclerView = view.findViewById(R.id.savingsCircleRecyclerView);
        savingsCircleRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        SavingsCircleAdapter savingsCircleAdapter = new SavingsCircleAdapter(savings -> {
            Intent intent = new Intent(requireContext(), SavingsCircleDetailsActivity.class);
            intent.putExtra("circleId", savings.getId());
            intent.putExtra("groupName", savings.getName());
            intent.putStringArrayListExtra("groupEmails",
                    new ArrayList<>(savings.getMemberEmails()));
            intent.putExtra("groupInvite", savings.getInvite());
            intent.putExtra("groupChallengeTitle", savings.getTitle());
            intent.putExtra("groupChallengeGoal", savings.getGoal());
            intent.putExtra("groupFrequency", savings.getFrequency());
            intent.putExtra("groupNotes", savings.getNotes());
            intent.putExtra("creationDate", savings.getCreatorDateJoined().toIso());
            intent.putExtra("datesJoined", (Serializable) savings.getDatesJoined());
            intent.putExtra("contributions", (Serializable) savings.getContributions());
            intent.putExtra("creatorId", savings.getCreatorId());
            startActivity(intent);
        });

        savingsCircleRecyclerView.setAdapter(savingsCircleAdapter);
        return savingsCircleAdapter;
    }

    private void setupAddBudgetDialog() {
        addBudget.setOnClickListener(v -> {
            View popupView = getLayoutInflater().inflate(R.layout.popup_budget_creation, null);
            AlertDialog dialog =
                    new AlertDialog.Builder(requireActivity()).setView(popupView).create();
            setupDialogContents(popupView, dialog);
            dialog.show();
        });
    }

    private void setupDialogContents(View popupView, AlertDialog dialog) {
        DialogViews dv = bindBudgetDialogViews(popupView);

        BudgetCreationViewModel budgetCreationViewModel =
                new ViewModelProvider(requireActivity()).get(BudgetCreationViewModel.class);

        setupFrequencySpinner(popupView, dv.budgetFrequencyEntry, dv.budgetDateEntry);
        setupBudgetDatePicker(dv.budgetDateEntry, dv.budgetFrequencyEntry);

        dv.cancelButton.setOnClickListener(v -> dialog.dismiss());

        dv.createBudgetButton.setOnClickListener(v -> {
            BudgetInputs inputs = readBudgetInputs(dv);

            if (!validateBudgetInputs(dv, inputs)) {
                return;
            }

            long timestamp = parseBudgetTimestamp(inputs.date);

            budgetCreationViewModel.createBudget(
                    inputs.name,
                    inputs.date,
                    inputs.amount,
                    inputs.category,
                    inputs.frequency,
                    timestamp,
                    () -> {
                        AppDate appDate = dateVM.getCurrentDate().getValue();
                        if (appDate != null) {
                            budgetsFragmentViewModel.loadBudgetsFor(appDate);
                        }
                    });

            dialog.dismiss();
            clearBudgetDialogInputs(dv);
        });
    }

    // ---- helpers for dialog complexity ----

    private DialogViews bindBudgetDialogViews(View popupView) {
        DialogViews dv = new DialogViews();
        dv.budgetNameEntry = popupView.findViewById(R.id.BudgetNameEntry);
        dv.budgetAmountEntry = popupView.findViewById(R.id.BudgetAmountEntry);
        dv.budgetFrequencyEntry = popupView.findViewById(R.id.BudgetFrequencyEntry);
        dv.budgetDateEntry = popupView.findViewById(R.id.BudgetDateEntry);
        dv.budgetCategoryEntry = popupView.findViewById(R.id.BudgetCategoryEntry);
        dv.createBudgetButton = popupView.findViewById(R.id.createBudgetButton);
        dv.cancelButton = popupView.findViewById(R.id.cancelButton);
        return dv;
    }

    private void setupBudgetDatePicker(EditText budgetDateEntry, Spinner frequencySpinner) {
        budgetDateEntry.setOnClickListener(v -> {
            DateViewModel dateViewModel = new ViewModelProvider(requireActivity())
                    .get(DateViewModel.class);

            dateViewModel.getCurrentDate().observe(getViewLifecycleOwner(), appDate -> {
                if (appDate == null) {
                    return;
                }

                Calendar minCalendar = Calendar.getInstance();
                minCalendar.set(appDate.getYear(),
                        appDate.getMonth() - 1, appDate.getDay(), 0, 0, 0);
                minCalendar.set(Calendar.MILLISECOND, 0);

                final Calendar today = Calendar.getInstance();
                int year = today.get(Calendar.YEAR);
                int month = today.get(Calendar.MONTH);
                int day = today.get(Calendar.DAY_OF_MONTH);

                String selectedFrequency = frequencySpinner.getSelectedItem().toString();

                DatePickerDialog picker = new DatePickerDialog(
                        requireContext(),
                        (view, y, mZero, dd) -> {
                            int displayMonth = mZero + 1;
                            int displayDay = dd;
                            int displayYear = y;

                            if ("Monthly".equals(selectedFrequency)) {
                                displayDay = 1;
                                Calendar sel = Calendar.getInstance();
                                sel.set(y, mZero, 1, 0, 0, 0);
                                sel.set(Calendar.MILLISECOND, 0);
                                if (sel.before(today)) {
                                    sel.add(Calendar.MONTH, 1);
                                    displayMonth = sel.get(Calendar.MONTH) + 1;
                                    displayYear = sel.get(Calendar.YEAR);
                                }
                            }

                            String dateString = String.format(
                                    Locale.US, "%02d/%02d/%04d",
                                    displayMonth, displayDay, displayYear);
                            budgetDateEntry.setText(dateString);
                        },
                        year, month, day
                );

                picker.getDatePicker().setMinDate(minCalendar.getTimeInMillis());
                picker.show();
            });
        });
    }

    private BudgetInputs readBudgetInputs(DialogViews dv) {
        BudgetInputs in = new BudgetInputs();
        in.name = dv.budgetNameEntry.getText().toString();
        in.date = dv.budgetDateEntry.getText().toString();
        in.amount = dv.budgetAmountEntry.getText().toString();
        in.category = dv.budgetCategoryEntry.getText().toString();
        in.frequency = dv.budgetFrequencyEntry.getSelectedItem().toString();
        return in;
    }

    private boolean validateBudgetInputs(DialogViews dv, BudgetInputs in) {
        boolean isValid = true;

        // amount
        try {
            int intAmount = Integer.parseInt(in.amount);
            if (intAmount <= 0) {
                dv.budgetAmountEntry.setError("Amount must be greater than 0");
                isValid = false;
            }
        } catch (NumberFormatException e) {
            dv.budgetAmountEntry.setError("Amount must be a number");
            isValid = false;
        }

        // frequency
        if (dv.budgetFrequencyEntry.getSelectedItemPosition() == 0) {
            TextView errorText = (TextView) dv.budgetFrequencyEntry.getSelectedView();
            if (errorText != null) {
                errorText.setError("");
            }
            isValid = false;
        }

        // name/category/date
        if (TextUtils.isEmpty(in.name)) {
            dv.budgetNameEntry.setError("Please enter a name");
            isValid = false;
        }
        if (TextUtils.isEmpty(in.category)) {
            dv.budgetCategoryEntry.setError("Please enter a category");
            isValid = false;
        }
        if (TextUtils.isEmpty(in.date)) {
            dv.budgetDateEntry.setError("Please select a date");
            isValid = false;
        }

        return isValid;
    }

    private long parseBudgetTimestamp(String date) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date parsed = sdf.parse(date);
            return parsed != null ? parsed.getTime() : System.currentTimeMillis();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }

    private void clearBudgetDialogInputs(DialogViews dv) {
        dv.budgetNameEntry.setText("");
        dv.budgetDateEntry.setText("");
        dv.budgetAmountEntry.setText("");
        dv.budgetCategoryEntry.setText("");
        dv.budgetFrequencyEntry.setSelection(0);
    }

    // ---- original spinner helper unchanged ----
    private void setupFrequencySpinner(
            View popupView,
            Spinner budgetFrequencyEntry,
            EditText budgetDateEntry
    ) {
        String[] frequencies = {"Select a Frequency", "Weekly", "Monthly"};
        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(
                popupView.getContext(),
                android.R.layout.simple_spinner_item,
                frequencies
        );
        freqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        budgetFrequencyEntry.setAdapter(freqAdapter);

        budgetFrequencyEntry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                boolean enable = pos != 0;
                budgetDateEntry.setEnabled(enable);
                budgetDateEntry.setFocusable(false);
                budgetDateEntry.setClickable(enable);
                if (!enable) {
                    budgetDateEntry.setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                budgetDateEntry.setEnabled(false);
                budgetDateEntry.setFocusable(false);
                budgetDateEntry.setClickable(false);
            }
        });
    }

    // ---- tiny structs to support dialog helpers ----
    private static class DialogViews {
        private EditText budgetNameEntry;
        private EditText budgetAmountEntry;
        private Spinner budgetFrequencyEntry;
        private EditText budgetDateEntry;
        private EditText budgetCategoryEntry;
        private Button createBudgetButton;
        private Button cancelButton;

        public EditText getBudgetNameEntry() {
            return budgetNameEntry;
        }
        public EditText getBudgetAmountEntry() {
            return budgetAmountEntry;
        }
        public Spinner getBudgetFrequencyEntry() {
            return budgetFrequencyEntry;
        }
        public EditText getBudgetDateEntry() {
            return budgetDateEntry;
        }
        public EditText getBudgetCategoryEntry() {
            return budgetCategoryEntry;
        }
        public Button getCreateBudgetButton() {
            return createBudgetButton;
        }
        public Button getCancelButton() {
            return cancelButton;
        }
    }

    private static class BudgetInputs {
        private String name;
        private String date;
        private String amount;
        private String category;
        private String frequency;

        public String getName() {
            return name;
        }
        public String getDate() {
            return date;
        }
        public String getAmount() {
            return amount;
        }
        public String getCategory() {
            return category;
        }
        public String getFrequency() {
            return frequency;
        }
    }
}

