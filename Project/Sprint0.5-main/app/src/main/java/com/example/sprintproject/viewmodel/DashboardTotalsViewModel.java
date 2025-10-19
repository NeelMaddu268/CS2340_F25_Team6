package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.FirestoreManager;
import com.example.sprintproject.model.AppDate;
import com.example.sprintproject.model.Budget;
import com.example.sprintproject.model.Expense;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardTotalsViewModel extends ViewModel {
    private final MutableLiveData<Double> totalSpent = new MutableLiveData<>(0.0);
    private final MutableLiveData<List<BudgetAggregate>> remainingByBudget = new MutableLiveData<>(new ArrayList<BudgetAggregate>());
    private ListenerRegistration budgetListener, expenseListener;
    private final List<Budget> allBudgets = new ArrayList<>();
    private final List<Expense> allExpenses = new ArrayList<>();
    private AppDate currentDate;

    public LiveData<Double> getTotalSpent() { return totalSpent; }
    public LiveData<List<BudgetAggregate>> getRemainingByBudget() { return remainingByBudget; }

    public void start(AppDate date) {
        currentDate = date;
        attachListeners();
    }

    public void setSelectedDate(AppDate date) {
        currentDate = date;
        computeTotals();
    }

    private void attachListeners() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirestoreManager fm = FirestoreManager.getInstance();
        if (budgetListener != null) budgetListener.remove();
        budgetListener = fm.budgetsReference(uid).addSnapshotListener((QuerySnapshot qs, com.google.firebase.firestore.FirebaseFirestoreException e) -> {
            allBudgets.clear();
            if (qs != null) {
                for (DocumentSnapshot d : qs.getDocuments()) {
                    Budget b = d.toObject(Budget.class);
                    if (b != null) { b.setId(d.getId()); allBudgets.add(b); }
                }
            }
            computeTotals();
        });
        if (expenseListener != null) expenseListener.remove();
        expenseListener = fm.expensesReference(uid).addSnapshotListener((QuerySnapshot qs, com.google.firebase.firestore.FirebaseFirestoreException e) -> {
            allExpenses.clear();
            if (qs != null) {
                for (DocumentSnapshot d : qs.getDocuments()) {
                    Expense x = d.toObject(Expense.class);
                    if (x != null) allExpenses.add(x);
                }
            }
            computeTotals();
        });
    }

    private void computeTotals() {
        if (currentDate == null) return;
        Calendar sel = Calendar.getInstance();
        sel.set(currentDate.getYear(), currentDate.getMonth() - 1, currentDate.getDay(), 0, 0, 0);
        Date selDate = sel.getTime();
        double total = 0;
        List<BudgetAggregate> rows = new ArrayList<>();
        for (Budget b : allBudgets) {
            double amt = nz(b.getAmount());
            String cat = s(b.getCategory());
            String freq = s(b.getFrequency());
            Date start = startOfPeriod(b, selDate, freq);
            double spent = 0;
            for (Expense x : allExpenses) {
                if (!cat.equalsIgnoreCase(s(x.getCategory()))) continue;
                if (withinRange(x.getDate(), start, selDate)) spent += nz(x.getAmount());
            }
            total += spent;
            rows.add(new BudgetAggregate(b.getId(), s(b.getName()), cat, freq, amt, round(spent), round(amt - spent)));
        }
        totalSpent.postValue(round(total));
        remainingByBudget.postValue(rows);
    }

    private Date startOfPeriod(Budget b, Date sel, String freq) {
        YMD s = extractYMD(b.getStartDate());
        if (s == null) return sel;
        Calendar c = Calendar.getInstance();
        c.set(s.year, s.month - 1, s.day, 0, 0, 0);
        if (freq != null && freq.equalsIgnoreCase("Weekly")) {
            while (c.getTime().before(sel)) c.add(Calendar.WEEK_OF_YEAR, 1);
            c.add(Calendar.WEEK_OF_YEAR, -1);
        } else if (freq != null && freq.equalsIgnoreCase("Monthly")) {
            while (c.getTime().before(sel)) c.add(Calendar.MONTH, 1);
            c.add(Calendar.MONTH, -1);
        } else {
            if (c.getTime().after(sel)) return sel;
        }
        return c.getTime();
    }

    private boolean withinRange(String expDate, Date start, Date end) {
        YMD e = extractYMD(expDate);
        if (e == null) return false;
        Calendar c = Calendar.getInstance();
        c.set(e.year, e.month - 1, e.day, 0, 0, 0);
        Date d = c.getTime();
        return !d.before(start) && !d.after(end);
    }

    private YMD extractYMD(Object o) {
        if (o == null) return null;
        if (o instanceof Timestamp) return fromDate(((Timestamp) o).toDate());
        if (o instanceof Date) return fromDate((Date) o);
        if (o instanceof Long) return fromDate(new Date((Long) o));
        return parseYMDFromString(String.valueOf(o));
    }

    private YMD parseYMDFromString(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        String[] fmts = {
                "yyyy-MM-dd","MM/dd/yyyy","yyyy/MM/dd","dd-MM-yyyy",
                "MMM dd, yyyy","MMM d, yyyy","MMMM dd, yyyy","MMMM d, yyyy",
                "yyyy-MM","yyyy/MM","MM-yyyy","MM/yyyy","MMM yyyy","MMMM yyyy"
        };
        for (String f : fmts) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(f, Locale.US);
                sdf.setLenient(false);
                Date d = sdf.parse(t);
                if (d != null) return fromDate(d);
            } catch (ParseException ignored) {}
        }
        String cleaned = t.replace('/', '-');
        String[] p = cleaned.split("-");
        if (p.length >= 2) {
            try {
                int y = Integer.parseInt(p[0]);
                int m = Integer.parseInt(p[1]);
                if (m >= 1 && m <= 12) return new YMD(y, m, 1);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private YMD fromDate(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return new YMD(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    private static double nz(Double d) { return d == null ? 0 : d; }
    private static String s(String s) { return s == null ? "" : s; }
    private static double round(double v) { return Math.round(v * 100) / 100.0; }

    public static class BudgetAggregate {
        public String id, name, category, frequency;
        public double amount, spent, remaining;
        public BudgetAggregate(String i, String n, String c, String f, double a, double s, double r) {
            id=i; name=n; category=c; frequency=f; amount=a; spent=s; remaining=r;
        }
    }

    private static final class YMD {
        final int year, month, day;
        YMD(int y, int m, int d) { year = y; month = m; day = d; }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (budgetListener != null) budgetListener.remove();
        if (expenseListener != null) expenseListener.remove();
    }
}


