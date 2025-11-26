// This ViewModel handles the loading and
// updating of all the user budgets, keeping the relevant totals
// and remaining amounts up to date.
// Automatically pushes the budgets into new cycles
// when they expire for their initial weekly/monthly cycles.

package com.example.sprintproject.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.model.AppDate;
import com.example.sprintproject.model.Budget;
import com.example.sprintproject.model.Expense;
import com.example.sprintproject.model.FinanceVisitor;   // ⬅️ NEW IMPORT
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BudgetsFragmentViewModel extends ViewModel {

    private final MutableLiveData<List<Budget>> budgetsLiveData =
            new MutableLiveData<>(new ArrayList<>());
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final MutableLiveData<Double> totalRemainingLiveData = new MutableLiveData<>(0.0);

    private final MutableLiveData<Double> totalSpentAllTimeLiveData = new MutableLiveData<>(0.0);

    private static final String TAG = "BudgetsVM";

    private static final String START_DATE_TIMESTAMP_STRING = "startDateTimestamp";

    /** Keep exactly one active Firestore listener at a time. */
    private ListenerRegistration activeListener;

    public LiveData<List<Budget>> getBudgets() {
        return budgetsLiveData;
    }

    private void detachActiveListener() {
        if (activeListener != null) {
            activeListener.remove();
            activeListener = null;
        }
    }

    public LiveData<Double> getTotalRemaining() {
        return totalRemainingLiveData;
    }

    public LiveData<Double> getTotalSpentAllTime() {
        return totalSpentAllTimeLiveData;
    }

    private void computeAllTimeSpent() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            totalSpentAllTimeLiveData.postValue(0.0);
            return;
        }
        String uid = auth.getCurrentUser().getUid();

        FirestoreManager.getInstance().expensesReference(uid)
                .whereLessThanOrEqualTo("timestamp", System.currentTimeMillis())
                .get()
                .addOnSuccessListener(qs -> {
                    double total = 0;
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        Expense e = doc.toObject(Expense.class);
                        if (e != null) {
                            total += e.getAmount();
                        }
                    }
                    totalSpentAllTimeLiveData.postValue(total);
                })
                .addOnFailureListener(e ->
                        totalSpentAllTimeLiveData.postValue(0.0)
                );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        detachActiveListener();
    }

    private boolean isBudgetExpired(Budget budget, Date compareDate) {
        try {
            Date startDate;
            if (budget.getStartDateTimestamp() > 0) {
                startDate = new Date(budget.getStartDateTimestamp());
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
                startDate = sdf.parse(budget.getStartDate());
            }

            if (startDate == null) {
                return false;
            }

            Calendar start = Calendar.getInstance();
            start.setTime(startDate);
            Calendar end = (Calendar) start.clone();

            if ("Weekly".equalsIgnoreCase(budget.getFrequency())) {
                end.add(Calendar.DAY_OF_YEAR, 7);
            } else if ("Monthly".equalsIgnoreCase(budget.getFrequency())) {
                end.add(Calendar.MONTH, 1);
            }

            return compareDate.after(end.getTime());
        } catch (Exception e) {
            return false;
        }
    }


    // Load all budgets (no filtering)
    public void loadBudgets() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            budgetsLiveData.postValue(new ArrayList<>());
            totalRemainingLiveData.postValue(0.0);
            return;
        }
        String uid = auth.getCurrentUser().getUid();

        detachActiveListener();

        activeListener = FirestoreManager.getInstance()
                .budgetsReference(uid)
                .orderBy(START_DATE_TIMESTAMP_STRING, Query.Direction.DESCENDING)
                .addSnapshotListener((qs, e) -> {
                    if (e != null || qs == null) {
                        budgetsLiveData.postValue(new ArrayList<>());
                        totalRemainingLiveData.postValue(0.0);
                        return;
                    }

                    List<Budget> list = new ArrayList<>();
                    Date today = new Date();

                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        Budget b = toBudgetWithId(doc);
                        if (b == null) {
                            continue;
                        }

                        if (isBudgetExpired(b, today)) {
                            applyRollover(b, today);
                        }

                        list.add(b);
                    }

                    list.sort((b1, b2) ->
                            Long.compare(b2.getStartDateTimestamp(), b1.getStartDateTimestamp()));

                    budgetsLiveData.postValue(list);

                    // Use Visitor instead of manual summation
                    TotalRemainingVisitor visitor = new TotalRemainingVisitor();
                    for (Budget b : list) {
                        b.accept(visitor);
                    }
                    totalRemainingLiveData.postValue(visitor.getTotal());

                    computeAllTimeSpent();
                });
    }

    /**
     * Loads budgets for a specific date.
     *
     * @param appDate The selected date used to filter budgets.
     */
    public void loadBudgetsFor(@NonNull AppDate appDate) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            budgetsLiveData.postValue(new ArrayList<>());
            totalRemainingLiveData.postValue(0.0);
            return;
        }
        String uid = auth.getCurrentUser().getUid();

        Calendar selected = Calendar.getInstance();
        selected.set(appDate.getYear(), appDate.getMonth() - 1, appDate.getDay(), 0, 0, 0);
        selected.set(Calendar.MILLISECOND, 0);

        detachActiveListener();

        activeListener = FirestoreManager.getInstance()
                .budgetsReference(uid)
                .orderBy(START_DATE_TIMESTAMP_STRING, Query.Direction.DESCENDING)
                .addSnapshotListener((qs, e) -> {
                    if (e != null || qs == null) {
                        budgetsLiveData.postValue(new ArrayList<>());
                        totalRemainingLiveData.postValue(0.0);
                        return;
                    }

                    List<Budget> list = new ArrayList<>();

                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        Budget b = toBudgetWithId(doc);
                        if (b == null) {
                            continue;
                        }

                        boolean expired = isBudgetExpired(b, selected.getTime());
                        if (expired) {
                            applyRollover(b, selected.getTime());
                        }

                        list.add(b);
                    }

                    list.sort((b1, b2) ->
                            Long.compare(b2.getStartDateTimestamp(), b1.getStartDateTimestamp()));

                    budgetsLiveData.postValue(list);

                    // Use Visitor here too
                    TotalRemainingVisitor visitor = new TotalRemainingVisitor();
                    for (Budget b : list) {
                        b.accept(visitor);
                    }
                    totalRemainingLiveData.postValue(visitor.getTotal());

                    computeAllTimeSpent();
                });
    }

    /**
     * Applies a rollover to a budget if its period has expired.
     *
     * @param budget      The budget object to roll over to a new period.
     * @param targetDate  The current date used to determine expiration and new start date.
     */
    public void applyRollover(Budget budget, Date targetDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);

            Date startDate = (budget.getStartDateTimestamp() > 0)
                    ? new Date(budget.getStartDateTimestamp())
                    : sdf.parse(budget.getStartDate());

            if (startDate == null) {
                return;
            }

            Calendar currentStart = Calendar.getInstance();
            currentStart.setTime(startDate);

            boolean rolledAny = false;
            int safety = 0;

            while (isBudgetExpired(budget, targetDate) && safety < 36) {
                safety++;

                Calendar nextStart = (Calendar) currentStart.clone();
                if ("Weekly".equalsIgnoreCase(budget.getFrequency())) {
                    nextStart.add(Calendar.DAY_OF_YEAR, 7);
                } else if ("Monthly".equalsIgnoreCase(budget.getFrequency())) {
                    nextStart.add(Calendar.MONTH, 1);
                } else {
                    break;
                }

                boolean wasOver = budget.getMoneyRemaining() < 0;
                budget.setPreviousCycleOverBudget(wasOver);
                budget.setPreviousCycleEndTimestamp(System.currentTimeMillis());

                double newTotal = budget.getAmount() + budget.getMoneyRemaining();

                budget.setSpentToDate(0);
                budget.setMoneyRemaining(newTotal);
                budget.setStartDate(sdf.format(nextStart.getTime()));
                budget.setStartDateTimestamp(nextStart.getTimeInMillis());
                budget.setHasPreviousCycle(true);

                currentStart = nextStart;
                rolledAny = true;
            }

            if (rolledAny) {
                updateBudget(budget);
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "applyRollover(): Exception for " + budget.getName(), e);
        }
    }

    /**
     * Retrieves a single budget document by its ID.
     *
     * @param budgetId The unique identifier of the budget to fetch.
     * @return A LiveData object that emits updates for the requested budget.
     */
    public LiveData<Budget> getBudgetById(String budgetId) {
        MutableLiveData<Budget> live = new MutableLiveData<>();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            live.setValue(null);
            return live;
        }
        String uid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("budgets")
                .document(budgetId)
                .addSnapshotListener((snap, err) -> {
                    if (err != null) {
                        return;
                    }
                    if (snap != null && snap.exists()) {
                        live.setValue(toBudgetWithId(snap));
                    }
                });
        return live;
    }

    /**
     * Updates an existing budget document in Firestore.
     * This requires that the budget has a valid ID and that a user is authenticated.
     *
     * @param budget The budget object containing updated fields such as
     *               start date, spent amount, and remaining balance.
     */
    public void updateBudget(Budget budget) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            return;
        }
        if (budget.getId() == null || budget.getId().isEmpty()) {
            return;
        }
        String uid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("budgets")
                .document(budget.getId())
                .update(
                        "startDate", budget.getStartDate(),
                        START_DATE_TIMESTAMP_STRING, budget.getStartDateTimestamp(),
                        "spentToDate", budget.getSpentToDate(),
                        "moneyRemaining", budget.getMoneyRemaining(),
                        "hasPreviousCycle", budget.isHasPreviousCycle(),
                        "previousCycleOverBudget", budget.isPreviousCycleOverBudget(),
                        "previousCycleEndTimestamp", budget.getPreviousCycleEndTimestamp()
                )
                .addOnSuccessListener(v ->
                        android.util.Log.d(TAG, "updateBudget(): SUCCESS for " + budget.getName()))
                .addOnFailureListener(e ->
                        android.util.Log.e(TAG, "updateBudget(): FAIL for " + budget.getName(), e));
    }

    private Budget toBudgetWithId(@NonNull DocumentSnapshot doc) {
        if (doc.getId().trim().isEmpty()) {
            return null;
        }
        Budget b = doc.toObject(Budget.class);
        if (b == null) {
            return null;
        }
        b.setId(doc.getId());
        return b;
    }

    // ===== Visitor implementation for total remaining =====
    private static final class TotalRemainingVisitor implements FinanceVisitor {
        private double total = 0.0;

        @Override
        public void visit(Budget budget) {
            if (budget != null) {
                total += budget.getMoneyRemaining();
            }
        }

        @Override
        public void visit(Expense expense) {
            // Not used for this visitor
        }

        double getTotal() {
            return total;
        }
    }
}
