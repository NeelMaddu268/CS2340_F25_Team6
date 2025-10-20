package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.FirestoreManager;
import com.example.sprintproject.model.AppDate;
import com.example.sprintproject.model.Budget;
import com.example.sprintproject.model.Expense;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads total spent and remaining budgets for the current period (weekly/monthly),
 * based on Firestore data and the app's selected date.
 */
public class DashboardViewModel extends ViewModel {

    private final MutableLiveData<Double> totalSpent = new MutableLiveData<>(0.0);
    private final MutableLiveData<Map<String, Double>> categoryTotals =
            new MutableLiveData<>(new LinkedHashMap<>());

    public LiveData<Double> getTotalSpent() { return totalSpent; }
    public LiveData<Map<String, Double>> getCategoryTotals() { return categoryTotals; }

    public void loadDashboardData(AppDate currentDate) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            totalSpent.postValue(0.0);
            categoryTotals.postValue(new LinkedHashMap<>());
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        FirestoreManager.getInstance().budgetsReference(uid)
                .get()
                .addOnSuccessListener(budgetSnap -> {
                    final Map<String, Double> remainingByCat = new LinkedHashMap<>();
                    final double[] spentTotal = {0.0};
                    List<Task<QuerySnapshot>> expenseTasks = new ArrayList<>();

                    for (DocumentSnapshot doc : budgetSnap.getDocuments()) {
                        Budget b = doc.toObject(Budget.class);
                        if (b == null) continue;

                        CycleBounds bounds = computeCycleBounds(b, currentDate);
                        if (bounds == null) {
                            remainingByCat.put(b.getCategory(), b.getAmount());
                            continue;
                        }

                        Task<QuerySnapshot> t = FirestoreManager.getInstance()
                                .expensesReference(uid)
                                .whereEqualTo("category", b.getCategory())
                                .whereGreaterThanOrEqualTo("timestamp", bounds.start)
                                .whereLessThan("timestamp", bounds.end)
                                .get()
                                .addOnSuccessListener(es -> {
                                    double spent = 0.0;
                                    for (DocumentSnapshot eDoc : es.getDocuments()) {
                                        Expense e = eDoc.toObject(Expense.class);
                                        if (e != null) spent += e.getAmount();
                                    }
                                    double remaining = Math.max(0.0, b.getAmount() - spent);
                                    remainingByCat.put(b.getCategory(), remaining);
                                    spentTotal[0] += spent;
                                });
                        expenseTasks.add(t);
                    }

                    Tasks.whenAllComplete(expenseTasks)
                            .addOnCompleteListener(done -> {
                                categoryTotals.postValue(remainingByCat);
                                totalSpent.postValue(spentTotal[0]);
                            });
                })
                .addOnFailureListener(err -> {
                    totalSpent.postValue(0.0);
                    categoryTotals.postValue(new LinkedHashMap<>());
                });
    }

    /** Compute the active weekly/monthly window for a budget. */
    private CycleBounds computeCycleBounds(Budget b, AppDate current) {
        long start = b.getStartDateTimestamp();
        if (start <= 0) return null;

        Calendar currentCal = Calendar.getInstance();
        currentCal.set(current.getYear(), current.getMonth() - 1, current.getDay(), 0, 0, 0);
        long now = currentCal.getTimeInMillis();

        if ("Weekly".equalsIgnoreCase(b.getFrequency())) {
            long period = 7L * 24 * 60 * 60 * 1000L;
            if (now < start) return new CycleBounds(start, start + period);
            long diff = now - start;
            long offset = (diff / period) * period;
            return new CycleBounds(start + offset, start + offset + period);
        } else if ("Monthly".equalsIgnoreCase(b.getFrequency())) {
            Calendar s = Calendar.getInstance();
            s.setTimeInMillis(start);
            while (now >= s.getTimeInMillis()) {
                Calendar next = (Calendar) s.clone();
                next.add(Calendar.MONTH, 1);
                if (now < next.getTimeInMillis()) {
                    return new CycleBounds(s.getTimeInMillis(), next.getTimeInMillis());
                }
                s = next;
            }
            return null;
        }
        return null;
    }

    private static class CycleBounds {
        final long start;
        final long end;
        CycleBounds(long s, long e) { start = s; end = e; }
    }
}
