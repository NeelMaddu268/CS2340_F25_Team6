package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CategorySpendViewModel extends ViewModel {

    private final MutableLiveData<Map<String, Double>> totalsLive
            = new MutableLiveData<>(new HashMap<>());
    public LiveData<Map<String, Double>> getCategoryTotals() {
        return totalsLive; }

    public void loadForCurrentMonth() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            totalsLive.postValue(new HashMap<>());
            return;
        }
        String uid = auth.getCurrentUser().getUid();

        // Range: [monthStart, monthEnd]
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long start = c.getTimeInMillis();

        Calendar e = (Calendar) c.clone();
        e.add(Calendar.MONTH, 1);
        long end = e.getTimeInMillis();

        FirestoreManager.getInstance()
                .expensesReference(uid)
                .whereGreaterThanOrEqualTo("timestamp", start)
                .whereLessThan("timestamp", end)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(qs -> {
                    Map<String, Double> byCat = new HashMap<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        Double amt = d.getDouble("amount");
                        String cat = d.getString("category");
                        if (amt == null || cat == null) {
                            continue;
                        }
                        String key = cat.trim().toLowerCase(Locale.US);
                        byCat.put(key, byCat.getOrDefault(key, 0.0) + amt);
                    }
                    totalsLive.postValue(byCat);
                })
                .addOnFailureListener(e1 -> totalsLive.postValue(new HashMap<>()));
    }
}
