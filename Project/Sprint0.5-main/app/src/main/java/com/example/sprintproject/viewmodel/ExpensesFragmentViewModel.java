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
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            expensesLiveData.postValue(new ArrayList<>());
            return;
        }
        String uid = auth.getCurrentUser().getUid();

        detachActiveListener();

        activeListener = FirestoreManager.getInstance()
                .expensesReference(uid)
                .orderBy("timestamp",
                        Query.Direction.DESCENDING)
                .addSnapshotListener((QuerySnapshot qs, FirebaseFirestoreException e) -> {
                    if (e != null || qs == null) {
                        expensesLiveData.postValue(new ArrayList<>());
                        return;
                    }
                    List<Expense> list = new ArrayList<>();
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        Expense exp = doc.toObject(Expense.class);
                        if (exp != null) {
                            list.add(exp);
                        }
                    }
                    expensesLiveData.postValue(list);
                });
    }

    /**
     * Loads expenses whose date is on or before the given app date.
     * Works even if the date field is stored as a String (client-side filtering).
     *
     * @param appDate The selected date used to filter which expenses to show.
     */
    public void loadExpensesFor(@NonNull AppDate appDate) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            expensesLiveData.postValue(new ArrayList<>());
            return;
        }
        String uid = auth.getCurrentUser().getUid();

        detachActiveListener();

        activeListener = FirestoreManager.getInstance()
                .expensesReference(uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((QuerySnapshot qs, FirebaseFirestoreException e) -> {
                    if (e != null || qs == null) {
                        expensesLiveData.postValue(new ArrayList<>());
                        return;
                    }
                    List<Expense> filtered = new ArrayList<>();
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        Expense exp = doc.toObject(Expense.class);
                        if (exp == null) {
                            continue;
                        }

                        Object raw = doc.get("date");
                        String fallback = null;
                        try {
                            java.lang.reflect.Method m = exp.getClass().getMethod("getDate");
                            Object val = m.invoke(exp);
                            if (val instanceof String) {
                                fallback = (String) val;
                            }
                        } catch (Exception ignored) {
                            //ignore
                        }

                        YMD when = extractYMD(raw, fallback);
                        if (when != null && onOrBefore(when, appDate)) {
                            filtered.add(exp);
                        }
                    }
                    expensesLiveData.postValue(filtered);
                });
    }


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
        if (raw instanceof Date)      {
            return fromDate((Date) raw);
        }
        if (raw instanceof Long)      {
            return fromDate(new Date((Long) raw));
        }
        if (raw instanceof String) {
            YMD parsed = parseYMDFromString((String) raw);
            if (parsed != null) {
                return parsed;
            }
        }
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

    /**
     * Parses a date string into a YMD object.
     * Tries full-date formats first; if only a month is provided, defaults the day to 1.
     *
     * @param s The date string to parse.
     * @return A YMD object if parsing succeeds, or null if parsing fails.
     */
    private YMD parseYMDFromString(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }

        List<String> full = Arrays.asList(
                "yyyy-MM-dd", "MM/dd/yyyy", "yyyy/MM/dd", "dd-MM-yyyy",
                "MMM dd, yyyy", "MMM d, yyyy", "MMMM dd, yyyy", "MMMM d, yyyy"
        );
        for (String f : full) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(f, Locale.US);
                sdf.setLenient(false);
                Date d = sdf.parse(t);
                if (d != null) {
                    return fromDate(d);
                }
            } catch (ParseException ignored) {
                //ignore
            }
        }

        List<String> monthOnly = Arrays.asList(
                "yyyy-MM", "yyyy/MM", "MM-yyyy", "MM/yyyy", "MMM yyyy", "MMMM yyyy"
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
            } catch (ParseException ignored) {
                //ignore
            }
        }

        String cleaned = t.replace('/', '-');
        String[] parts = cleaned.split("-");
        if (parts.length >= 2) {
            try {
                int y = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]);
                if (m >= 1 && m <= 12) {
                    return new YMD(y, m, 1);
                }
            } catch (NumberFormatException ignored) {
                //ignore
            }
        }
        return null;
    }

    private static final class YMD {
        private final int  year;
        private final int month;
        private final int day;
        YMD(int y, int m, int d) {
            year = y;
            month = m;
            day = d;
        }
    }
}

