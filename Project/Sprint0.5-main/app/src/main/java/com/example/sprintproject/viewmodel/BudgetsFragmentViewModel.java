package com.example.sprintproject.viewmodel;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.FirestoreManager;
import com.example.sprintproject.model.AppDate;
import com.example.sprintproject.model.Budget;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BudgetsFragmentViewModel extends ViewModel {

    private final MutableLiveData<List<Budget>> budgetsLiveData =
            new MutableLiveData<>(new ArrayList<>());
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

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

    /** Load all budgets (no filtering). */
    public void loadBudgets() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            budgetsLiveData.postValue(new ArrayList<>());
            return;
        }
        String uid = auth.getCurrentUser().getUid();

        detachActiveListener();

        activeListener = FirestoreManager.getInstance()
                .budgetsReference(uid)
                .orderBy("startDate", Query.Direction.DESCENDING)
                .addSnapshotListener((QuerySnapshot qs, FirebaseFirestoreException e) -> {
                    if (e != null || qs == null) {
                        budgetsLiveData.postValue(new ArrayList<>());
                        return;
                    }
                    List<Budget> list = new ArrayList<>();
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        Budget b = toBudgetWithId(doc);
                        if (b != null) list.add(b);
                    }
                    budgetsLiveData.postValue(list);
                });
    }

    /**
     * Load budgets where budget.startDate <= selected (day-aware).
     * Works even if startDate is stored as a String (client-side filtering).
     */
    public void loadBudgetsFor(@NonNull AppDate appDate) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            budgetsLiveData.postValue(new ArrayList<>());
            return;
        }
        String uid = auth.getCurrentUser().getUid();

        detachActiveListener();
        activeListener = null;

        FirestoreManager.getInstance()
                .budgetsReference(uid)
                .orderBy("startDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(qs -> {
                    List<Budget> filtered = new ArrayList<>();
                    Calendar current = Calendar.getInstance();
                    current.set(appDate.getYear(), appDate.getMonth() - 1, appDate.getDay(), 0, 0, 0);

                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        Budget b = toBudgetWithId(doc);
                        if (b == null) continue;

                        Object raw = doc.get("startDate");   // Timestamp/Date/Long/String
                        String fallback = b.getStartDate();  // model’s string if present
                        YMD start = extractYMD(raw, fallback);

                        if (start != null && startedOnOrBefore(start, appDate)) {

                            boolean expired = isBudgetExpired(b, current.getTime());

                            if (expired) {
                                applyRollover(b, current.getTime());
                                filtered.add(b);
                            } else {
                                filtered.add(b);
                            }
                        }
                    }
                    budgetsLiveData.postValue(filtered);
                })
                .addOnFailureListener(err -> {
                    budgetsLiveData.postValue(new ArrayList<>());
                    System.err.println("Failed to load budgets: " + err.getMessage());
                });
    }

    public void applyRollover(Budget budget, Date currentDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
            Date startDate = sdf.parse(budget.getStartDate());
            if (startDate == null) {
                System.out.println("Skipping rollover for " + budget.getName() + " (invalid start date)");
                return;
            }

            if (startDate.after(currentDate)) {
                System.out.println("Skipping rollover for " + budget.getName() + " (start date is in the future)");
                return;
            }

            // Don't roll over the same budget multiple times within the same session
            if (budget.isHasPreviousCycle() && !isBudgetExpired(budget, currentDate)) {
                System.out.println("Skipping rollover for " + budget.getName() + " (already rolled over)");
                return;
            }

            Calendar nextStart = Calendar.getInstance();
            nextStart.setTime(startDate);


            if (budget.getFrequency().equalsIgnoreCase("Weekly")) {
                nextStart.add(Calendar.DAY_OF_YEAR, 7);
            } else if (budget.getFrequency().equalsIgnoreCase("Monthly")) {
                nextStart.add(Calendar.MONTH, 1);
            }


            //Add the rollover
            double remaining = budget.getMoneyRemaining();
            double newTotal = budget.getAmount() + remaining;

            // current startDate is the old one — safe to log first
            System.out.println("Rolling over budget: " + budget.getName());
            System.out.println("  Previous start date: " + budget.getStartDate());
            System.out.println("  Remaining money: " + remaining);
            System.out.println("  New total (with rollover): " + newTotal);

            budget.setSpentToDate(0);
            budget.setMoneyRemaining(budget.getAmount() + remaining);
            budget.setStartDate(sdf.format(nextStart.getTime()));
            budget.setStartDateTimestamp(nextStart.getTimeInMillis());
            budget.setHasPreviousCycle(true);

            updateBudget(budget);
            System.out.println("Rolled over budget: " + budget.getName());


        } catch (ParseException e){
            System.err.println("❌ Failed to apply rollover for " + budget.getName());
            e.printStackTrace();
        }
    }

    /** Get single budget by id. */
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
                    if (err != null) return;
                    if (snap != null && snap.exists()) {
                        live.setValue(toBudgetWithId(snap));
                    }
                });
        return live;
    }

    /** Update an existing budget (requires id). */
    public void updateBudget(Budget budget) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            System.err.println("UpdateBudget: no verified user found!");
            return;
        }
        if (budget.getId() == null || budget.getId().isEmpty()) {
            System.err.println("UpdateBudget: no id found!");
            return;
        }
        String uid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("budgets")
                .document(budget.getId())
                .update(
                        "startDate", budget.getStartDate(),
                        "startDateTimestamp", budget.getStartDateTimestamp(),
                        "spentToDate", budget.getSpentToDate(),
                        "moneyRemaining", budget.getMoneyRemaining(),
                        "hasPreviousCycle", budget.isHasPreviousCycle()
                )
                .addOnSuccessListener(v -> System.out.println("Budget updated successfully"))
                .addOnFailureListener(e -> System.err.println("Budget failed to update"));
    }

    // -------- Helpers --------

    private Budget toBudgetWithId(@NonNull DocumentSnapshot doc) {
        Budget b = doc.toObject(Budget.class);
        if (b == null) return null;
        b.setId(doc.getId());
        return b;
    }

    /** Day-aware compare: true if startDate <= selected AppDate. */
    private boolean startedOnOrBefore(YMD start, AppDate selected) {
        Calendar sel = Calendar.getInstance();
        sel.set(selected.getYear(), selected.getMonth() - 1, selected.getDay(), 0, 0, 0);
        sel.set(Calendar.MILLISECOND, 0);

        Calendar st = Calendar.getInstance();
        st.set(start.year, start.month - 1, start.day, 0, 0, 0);
        st.set(Calendar.MILLISECOND, 0);

        return !st.after(sel);
    }

    /** Extract a year-month-day from raw Firestore value or fallback string. */
    private YMD extractYMD(Object rawStartDate, String fallbackStr) {
        // Native types
        if (rawStartDate instanceof Timestamp) return fromDate(((Timestamp) rawStartDate).toDate());
        if (rawStartDate instanceof Date)      return fromDate((Date) rawStartDate);
        if (rawStartDate instanceof Long)      return fromDate(new Date((Long) rawStartDate));

        // String in the doc field
        if (rawStartDate instanceof String) {
            YMD ymd = parseYMDFromString((String) rawStartDate);
            if (ymd != null) return ymd;
        }
        // Fallback to model field
        if (fallbackStr != null) {
            return parseYMDFromString(fallbackStr);
        }
        return null;
    }

    private YMD fromDate(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return new YMD(
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH)
        );
    }

    /** Try strict parses; if month-only, default day=1. */
    private YMD parseYMDFromString(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;

        // Full date formats (contain a day)
        List<String> fullFormats = Arrays.asList(
                "yyyy-MM-dd",
                "MM/dd/yyyy",
                "yyyy/MM/dd",
                "dd-MM-yyyy",
                "MMM dd, yyyy",
                "MMM d, yyyy",
                "MMMM dd, yyyy",
                "MMMM d, yyyy"
        );
        for (String f : fullFormats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(f, Locale.US);
                sdf.setLenient(false);
                Date d = sdf.parse(t);
                if (d != null) return fromDate(d);
            } catch (ParseException ignored) {}
        }

        // Month-only formats (default day = 1)
        List<String> monthOnly = Arrays.asList(
                "yyyy-MM",
                "yyyy/MM",
                "MM-yyyy",
                "MM/yyyy",
                "MMM yyyy",
                "MMMM yyyy"
        );
        for (String f : monthOnly) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(f, Locale.US);
                sdf.setLenient(false);
                Date d = sdf.parse(t);
                if (d != null) {
                    Calendar c = Calendar.getInstance();
                    c.setTime(d);
                    return new YMD(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, 1);
                }
            } catch (ParseException ignored) {}
        }

        // Loose numeric fallbacks like "2023-05" or "2023/05"
        String cleaned = t.replace('/', '-');
        String[] parts = cleaned.split("-");
        if (parts.length >= 2) {
            try {
                int y = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]);
                if (m >= 1 && m <= 12) return new YMD(y, m, 1);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    /** Simple holder for a full date (year, month, day). month = 1..12 */
    private static final class YMD {
        final int year;
        final int month;
        final int day;
        YMD(int year, int month, int day) {
            this.year = year;
            this.month = month;
            this.day = day;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        detachActiveListener();
    }

    private boolean isBudgetExpired(Budget budget, Date currentDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        try {
            Date startDate = sdf.parse(budget.getStartDate());
            if (startDate == null) {
                return false;
            }

            Calendar start = Calendar.getInstance();
            start.setTime(startDate);
            Calendar end = Calendar.getInstance();

            if (budget.getFrequency().equalsIgnoreCase("Weekly")) {
                end.setTime(start.getTime());
                end.add(Calendar.DAY_OF_YEAR, 7);
            } else if (budget.getFrequency().equalsIgnoreCase("Monthly")) {
                end.setTime(start.getTime());
                end.add(Calendar.MONTH, 1);
            }
            return currentDate.after(end.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }
}

