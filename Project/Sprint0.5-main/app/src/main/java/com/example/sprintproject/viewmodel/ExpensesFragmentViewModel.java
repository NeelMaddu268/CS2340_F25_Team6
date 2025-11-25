// This ViewModel loads all the expenses and filters them by a selected AppDate,
// using FireStore operations to stay updated.
// Includes flexible date-parsing logic so that expenses are
// handled correctly even if stored in different formats.

package com.example.sprintproject.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.model.AppDate;
import com.example.sprintproject.model.Expense;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpensesFragmentViewModel extends ViewModel {

    private final MutableLiveData<List<Expense>> expensesLiveData =
            new MutableLiveData<>(new ArrayList<>());

    private ListenerRegistration activeListener;

    // Avoid re-allocating these on every parse call (Sonar "avoid repeated allocations")
    private static final List<String> FULL_DATE_FORMATS = Arrays.asList(
            "yyyy-MM-dd", "MM/dd/yyyy", "yyyy/MM/dd", "dd-MM-yyyy",
            "MMM dd, yyyy", "MMM d, yyyy", "MMMM dd, yyyy", "MMMM d, yyyy"
    );
    private static final List<String> MONTH_ONLY_FORMATS = Arrays.asList(
            "yyyy-MM", "yyyy/MM", "MM-yyyy", "MM/yyyy", "MMM yyyy", "MMMM yyyy"
    );

    public LiveData<List<Expense>> getExpenses() {
        return expensesLiveData;
    }

    private void detachActiveListener() {
        if (activeListener != null) {
            activeListener.remove();
            activeListener = null;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        detachActiveListener();
    }

    /** Load all expenses (no filtering). */
    public void loadExpenses() {
        String uid = getUidOrPostEmpty();
        if (uid == null) return;

        detachActiveListener();

        activeListener = FirestoreManager.getInstance()
                .expensesReference(uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((qs, e) -> handleAllExpensesSnapshot(qs, e));
    }

    private void handleAllExpensesSnapshot(QuerySnapshot qs, FirebaseFirestoreException e) {
        if (hasSnapshotError(qs, e)) {
            expensesLiveData.postValue(new ArrayList<>());
            return;
        }

        List<Expense> list = new ArrayList<>();
        for (DocumentSnapshot doc : qs.getDocuments()) {
            Expense exp = doc.toObject(Expense.class);
            if (exp != null) list.add(exp);
        }
        expensesLiveData.postValue(list);
    }

    /**
     * Loads expenses whose date is on or before the given app date.
     * Works even if the date field is stored as a String (client-side filtering).
     *
     * @param appDate The selected date used to filter which expenses to show.
     */
    public void loadExpensesFor(@NonNull AppDate appDate) {
        String uid = getUidOrPostEmpty();
        if (uid == null) return;

        detachActiveListener();

        activeListener = FirestoreManager.getInstance()
                .expensesReference(uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((qs, e) -> handleFilteredExpensesSnapshot(qs, e, appDate));
    }

    /** ---------------- Snapshot helpers ---------------- */

    private void handleFilteredExpensesSnapshot(
            QuerySnapshot qs,
            FirebaseFirestoreException e,
            AppDate appDate
    ) {
        if (hasSnapshotError(qs, e)) {
            expensesLiveData.postValue(new ArrayList<>());
            return;
        }

        List<Expense> filtered = filterExpenses(qs, appDate);
        expensesLiveData.postValue(filtered);
    }

    private boolean hasSnapshotError(QuerySnapshot qs, FirebaseFirestoreException e) {
        return e != null || qs == null;
    }

    private String getUidOrPostEmpty() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            expensesLiveData.postValue(new ArrayList<>());
            return null;
        }
        return auth.getCurrentUser().getUid();
    }

    /** ---------------- Filtering helpers ---------------- */

    private List<Expense> filterExpenses(QuerySnapshot qs, AppDate appDate) {
        List<Expense> filtered = new ArrayList<>();
        for (DocumentSnapshot doc : qs.getDocuments()) {
            Expense exp = doc.toObject(Expense.class);
            if (exp == null) continue;

            if (shouldIncludeExpense(doc, exp, appDate)) {
                filtered.add(exp);
            }
        }
        return filtered;
    }

    private boolean shouldIncludeExpense(DocumentSnapshot doc, Expense exp, AppDate appDate) {
        YMD when = getExpenseYMD(doc, exp);
        return when != null && onOrBefore(when, appDate);
    }

    private YMD getExpenseYMD(DocumentSnapshot doc, Expense exp) {
        Object raw = doc.get("date");
        String fallback = getDateFallbackViaReflection(exp);
        return extractYMD(raw, fallback);
    }

    private String getDateFallbackViaReflection(Expense exp) {
        try {
            Method m = exp.getClass().getMethod("getDate");
            Object val = m.invoke(exp);
            return (val instanceof String) ? (String) val : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /** ---------------- Date comparison/parsing ---------------- */

    private boolean onOrBefore(YMD item, AppDate selected) {
        Calendar sel = Calendar.getInstance();
        sel.set(selected.getYear(), selected.getMonth() - 1, selected.getDay(), 0, 0, 0);
        sel.set(Calendar.MILLISECOND, 0);

        Calendar it = Calendar.getInstance();
        it.set(item.year, item.month - 1, item.day, 0, 0, 0);
        it.set(Calendar.MILLISECOND, 0);

        return !it.after(sel);
    }

    private YMD extractYMD(Object raw, String fallbackStr) {
        if (raw instanceof Timestamp) {
            return fromDate(((Timestamp) raw).toDate());
        }
        if (raw instanceof Date) {
            return fromDate((Date) raw);
        }
        if (raw instanceof Long) {
            return fromDate(new Date((Long) raw));
        }
        if (raw instanceof String) {
            YMD parsed = parseYMDFromString((String) raw);
            if (parsed != null) return parsed;
        }
        return (fallbackStr != null) ? parseYMDFromString(fallbackStr) : null;
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

    /**
     * Parses a date string into a YMD object.
     * Tries full-date formats first; if only a month is provided, defaults the day to 1.
     *
     * @param s The date string to parse.
     * @return A YMD object if parsing succeeds, or null if parsing fails.
     */
    private YMD parseYMDFromString(String s) {
        String t = safeTrim(s);
        if (t.isEmpty()) return null;

        YMD fullParsed = tryParseWithFormats(t, FULL_DATE_FORMATS, false);
        if (fullParsed != null) return fullParsed;

        YMD monthParsed = tryParseWithFormats(t, MONTH_ONLY_FORMATS, true);
        if (monthParsed != null) return monthParsed;

        return tryParseLooselyAsYearMonth(t);
    }

    private YMD tryParseWithFormats(String t, List<String> formats, boolean monthOnly) {
        for (String f : formats) {
            Date d = tryParseDate(t, f);
            if (d == null) continue;

            if (!monthOnly) return fromDate(d);

            Calendar c = Calendar.getInstance();
            c.setTime(d);
            return new YMD(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, 1);
        }
        return null;
    }

    private Date tryParseDate(String t, String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
            sdf.setLenient(false);
            return sdf.parse(t);
        } catch (ParseException ignored) {
            return null;
        }
    }

    private YMD tryParseLooselyAsYearMonth(String t) {
        String cleaned = t.replace('/', '-');
        String[] parts = cleaned.split("-");
        if (parts.length < 2) return null;

        try {
            int y = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            if (m >= 1 && m <= 12) return new YMD(y, m, 1);
        } catch (NumberFormatException ignored) {
            // ignore
        }
        return null;
    }

    private String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private static final class YMD {
        private final int year;
        private final int month;
        private final int day;

        YMD(int y, int m, int d) {
            year = y;
            month = m;
            day = d;
        }
    }
}


