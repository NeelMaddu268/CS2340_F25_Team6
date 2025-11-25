// This ViewModel tracks all of the saving circles the user is a part of and keeps them updated in real time,
// while filtering their status based on the selected AppDate. Also calculates each circles progress and updates
// the circle's goal has been met.

package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.model.AppDate;
import com.example.sprintproject.model.SavingsCircle;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class SavingsCircleFragmentViewModel extends ViewModel {

    private final MutableLiveData<List<SavingsCircle>> savingsCircleLiveData =
            new MutableLiveData<>(new ArrayList<>());

    private ListenerRegistration activeListener;

    private final List<SavingsCircle> cache = new ArrayList<>();
    private String currentUidCached = null;
    private AppDate currentAppDate = null;

    public LiveData<List<SavingsCircle>> getSavingsCircle() {
        return savingsCircleLiveData;
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

    public void loadSavingsCircle() {
        loadSavingsCircleFor(currentAppDate);
    }

    public void loadSavingsCircleFor(AppDate appDate) {

        String uid = getUidOrClear();
        if (uid == null) return;

        currentUidCached = uid;
        currentAppDate = appDate;

        detachActiveListener();

        activeListener = FirestoreManager.getInstance()
                .userSavingsCirclePointers(uid)
                .addSnapshotListener(this::handleSnapshot);
    }

    private void handleSnapshot(QuerySnapshot qs,
                                com.google.firebase.firestore.FirebaseFirestoreException e) {

        if (snapshotInvalid(qs, e)) {
            publishEmpty();
            return;
        }

        List<String> circleIds = extractValidCircleIds(qs.getDocuments());
        if (circleIds.isEmpty()) {
            publishEmpty();
            return;
        }

        fetchAllCircles(circleIds);
    }

    private boolean snapshotInvalid(QuerySnapshot qs,
                                    com.google.firebase.firestore.FirebaseFirestoreException e) {
        return e != null || qs == null || qs.isEmpty();
    }

    /* ---------------- extract IDs ---------------- */

    private List<String> extractValidCircleIds(List<DocumentSnapshot> docs) {
        List<String> ids = new ArrayList<>();
        for (DocumentSnapshot d : docs) {
            String id = d.getString("circleId");
            if (id != null && !id.trim().isEmpty()) ids.add(id);
        }
        return ids;
    }

    private void fetchAllCircles(List<String> circleIds) {
        List<SavingsCircle> acc = new ArrayList<>();
        Pending p = new Pending(circleIds.size(), () -> publish(acc));

        for (String id : circleIds) {
            fetchOneCircle(id, acc, p);
        }
    }

    private void fetchOneCircle(String id,
                                List<SavingsCircle> acc,
                                Pending p) {
        FirestoreManager.getInstance()
                .savingsCirclesGlobalReference()
                .document(id)
                .get()
                .addOnSuccessListener(snap -> {
                    SavingsCircle c = buildCircle(snap);
                    if (c != null) acc.add(c);
                    p.done();
                })
                .addOnFailureListener(err -> p.done());
    }


    private SavingsCircle buildCircle(DocumentSnapshot snap) {
        SavingsCircle c = snap.toObject(SavingsCircle.class);
        if (c == null) return null;

        c.setId(snap.getId());
        c.setContributions(resolveContrib(c.getContributions(), snap));

        evaluatePersonalAgainstAppDate(c, currentUidCached, currentAppDate);
        return c;
    }

    private Map<String, Double> resolveContrib(Map<String, Double> existing,
                                               DocumentSnapshot snap) {

        if (existing != null && !existing.isEmpty()) return existing;

        @SuppressWarnings("unchecked")
        Map<String, Object> raw = (Map<String, Object>) snap.get("contributions");

        Map<String, Double> coerced = new HashMap<>();
        if (raw != null) {
            for (Map.Entry<String, Object> e : raw.entrySet()) {
                Object v = e.getValue();
                if (v instanceof Number) {
                    coerced.put(e.getKey(), ((Number) v).doubleValue());
                }
            }
        }
        return coerced;
    }

    private void publish(List<SavingsCircle> list) {
        cache.clear();
        cache.addAll(list);
        savingsCircleLiveData.postValue(new ArrayList<>(list));
    }

    private void publishEmpty() {
        cache.clear();
        savingsCircleLiveData.postValue(new ArrayList<>());
    }

    private String getUidOrClear() {
        FirebaseAuth a = FirebaseAuth.getInstance();
        if (a.getCurrentUser() == null) {
            publishEmpty();
            return null;
        }
        return a.getCurrentUser().getUid();
    }

    public void setAppDate(AppDate appDate) {
        this.currentAppDate = appDate;
        recomputeFor(appDate);
    }

    public void recomputeFor(AppDate appDate) {
        if (cache.isEmpty()) {
            publishEmpty();
            return;
        }

        String uid = currentUidCached;
        if (uid == null) {
            FirebaseAuth a = FirebaseAuth.getInstance();
            if (a.getCurrentUser() != null) uid = a.getCurrentUser().getUid();
        }

        for (SavingsCircle c : cache) {
            evaluatePersonalAgainstAppDate(c, uid, appDate);
        }
        savingsCircleLiveData.postValue(new ArrayList<>(cache));
    }


    private void evaluatePersonalAgainstAppDate(
            SavingsCircle circle,
            String uid,
            AppDate appDate
    ) {
        if (circle == null || uid == null) return;

        // 1) personal end (joined + 7 days)
        long personalEndTs = computePersonalWindowEnd(circle, uid);

        // 2) AppDate -> millis midnight
        long appTs = (appDate != null) ? appDateStartMillis(appDate) : 0L;

        boolean ended = (personalEndTs > 0) && (appTs >= personalEndTs);
        circle.setCompleted(ended);

        // 3) sum contributions
        double total = sumContributions(circle);
        boolean groupMet = total >= circle.getGoal();

        circle.setGoalMet(ended && groupMet);
    }

    private long computePersonalWindowEnd(SavingsCircle c, String uid) {
        Map<String, String> joinedMap = c.getDatesJoined();
        if (joinedMap == null) return 0L;

        String joined = joinedMap.get(uid);
        return (joined != null) ? add7Days(joined) : 0L;
    }

    private double sumContributions(SavingsCircle c) {
        double total = 0;
        if (c.getContributions() != null) {
            for (Double v : c.getContributions().values()) {
                if (v != null) total += v;
            }
        }
        return total;
    }


    private long add7Days(String ymd) {
        if (ymd == null) return 0L;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            sdf.setLenient(false);
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(ymd));
            midnight(cal);
            cal.add(Calendar.DAY_OF_YEAR, 7);
            return cal.getTimeInMillis();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private long appDateStartMillis(AppDate a) {
        if (a == null) return 0L;
        Calendar c = Calendar.getInstance();
        c.set(a.getYear(), a.getMonth() - 1, a.getDay());
        midnight(c);
        return c.getTimeInMillis();
    }

    private void midnight(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }


    private static class Pending {
        private int left;
        private final Runnable onZero;

        Pending(int count, Runnable onZero) {
            this.left = count;
            this.onZero = onZero;
            if (left == 0) onZero.run();
        }

        void done() {
            left--;
            if (left == 0) onZero.run();
        }
    }
}
