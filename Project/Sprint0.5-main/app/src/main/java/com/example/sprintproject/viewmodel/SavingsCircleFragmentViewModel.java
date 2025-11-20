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

    // Cache to recompute against new AppDate without requerying
    private final List<SavingsCircle> cache = new ArrayList<>();
    private String currentUidCached = null;

    // App-controlled date (NOT system time). If null, nothing is “ended”.
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

    /** Load without forcing an AppDate (keeps previous AppDate if set). */
    public void loadSavingsCircle() {
        loadSavingsCircleFor(currentAppDate);
    }

    /** Load and evaluate circles for a specific AppDate. */
    public void loadSavingsCircleFor(AppDate appDate) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            cache.clear();
            savingsCircleLiveData.postValue(new ArrayList<>());
            return;
        }
        currentUidCached = auth.getCurrentUser().getUid();
        currentAppDate = appDate;

        detachActiveListener();

        activeListener = FirestoreManager.getInstance()
                .userSavingsCirclePointers(currentUidCached)
                .addSnapshotListener((QuerySnapshot qs,
                                      com.google.firebase.firestore
                                              .FirebaseFirestoreException e) -> {
                    if (e != null || qs == null) {
                        cache.clear();
                        savingsCircleLiveData.postValue(new ArrayList<>());
                        return;
                    }
                    if (qs.isEmpty()) {
                        cache.clear();
                        savingsCircleLiveData.postValue(new ArrayList<>());
                        return;
                    }

                    List<DocumentSnapshot> docs = qs.getDocuments();

                    // Count valid pointers
                    int tmp = 0;
                    for (DocumentSnapshot d : docs) {
                        if (d.getString("circleId") != null) {
                            tmp++;
                        }
                    }
                    if (tmp == 0) {
                        cache.clear();
                        savingsCircleLiveData.postValue(new ArrayList<>());
                        return;
                    }

                    final int expectedFinal = tmp; // effectively final for lambdas
                    final int[] seen = {0};
                    final List<SavingsCircle> acc = new ArrayList<>();

                    for (DocumentSnapshot pointerDoc : docs) {
                        String circleId = pointerDoc.getString("circleId");
                        if (circleId == null) {
                            continue;
                        }

                        FirestoreManager.getInstance()
                                .savingsCirclesGlobalReference()
                                .document(circleId)
                                .get()
                                .addOnSuccessListener(circleSnap -> {
                                    SavingsCircle circle = circleSnap.toObject(SavingsCircle.class);
                                    if (circle != null) {
                                        circle.setId(circleSnap.getId());

                                        Map<String, Double> contrib =
                                                circle.getContributions();
                                        if (contrib == null || contrib.isEmpty()) {
                                            @SuppressWarnings("unchecked")
                                            Map<String, Object> raw =
                                                    (Map<String, Object>) circleSnap
                                                            .get("contributions");
                                            Map<String, Double> coerced = new HashMap<>();
                                            if (raw != null) {
                                                for (Map.Entry<String, Object> en
                                                        : raw.entrySet()) {
                                                    Object v = en.getValue();
                                                    if (v instanceof Number) {
                                                        coerced.put(en.getKey(),
                                                                ((Number) v).doubleValue());
                                                    }
                                                }
                                            }
                                            circle.setContributions(coerced);
                                        }

                                        // Evaluate per-user goal status
                                        // against AppDate (join + 7 days rule)
                                        evaluatePersonalAgainstAppDate(circle,
                                                currentUidCached, currentAppDate);

                                        acc.add(circle);
                                    }

                                    if (++seen[0] == expectedFinal) {
                                        cache.clear();
                                        cache.addAll(acc);
                                        savingsCircleLiveData.postValue(new ArrayList<>(cache));
                                    }
                                })
                                .addOnFailureListener(err -> {
                                    if (++seen[0] == expectedFinal) {
                                        cache.clear();
                                        cache.addAll(acc);
                                        savingsCircleLiveData.postValue(new ArrayList<>(cache));
                                    }
                                });
                    }
                });
    }

    /** Update AppDate and recompute flags on the cached list. */
    public void setAppDate(AppDate appDate) {
        this.currentAppDate = appDate;
        recomputeFor(appDate);
    }

    /** Re-evaluate using the provided AppDate (no network). */
    public void recomputeFor(AppDate appDate) {
        if (cache.isEmpty()) {
            savingsCircleLiveData.postValue(new ArrayList<>());
            return;
        }
        String uid = currentUidCached;
        if (uid == null) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                uid = auth.getCurrentUser().getUid();
            }
        }
        for (SavingsCircle c : cache) {
            evaluatePersonalAgainstAppDate(c, uid, appDate);
        }
        savingsCircleLiveData.postValue(new ArrayList<>(cache));
    }

    /**
     * Personal rule (per member):
     *  - Window ends at (joinDate + 7 days).
     *  - "Ended" is true iff AppDate >= (joinDate + 7 days).
     *  - Target = totalGoal / memberCount (fallback to contributions.size or 1).
     *  - Flags:
     *      completed = ended
     *      goalMet   = ended && (myContribution >= personalTarget)
     */
    private void evaluatePersonalAgainstAppDate(SavingsCircle circle,
                                                String currentUid, AppDate appDate) {
        if (circle == null || currentUid == null) {
            return;
        }

        // 1) personal end timestamp from datesJoined[currentUid] + 7 days
        String joinedStr = null;
        Map<String, String> joinedMap = circle.getDatesJoined();
        if (joinedMap != null) {
            joinedStr = joinedMap.get(currentUid);
        }

        long personalEndTs = (joinedStr != null) ? add7Days(joinedStr) : 0L;

        // 2) AppDate -> millis (midnight local)
        long appTs = (appDate != null) ? appDateStartMillis(appDate) : 0L;

        boolean ended = (personalEndTs > 0) && (appTs >= personalEndTs);
        circle.setCompleted(ended);

        // 3) GROUP goal logic: sum all contributions
        double totalContributed = 0.0;
        if (circle.getContributions() != null) {
            for (Double v : circle.getContributions().values()) {
                if (v != null) {
                    totalContributed += v;
                }
            }
        }

        boolean groupMet = totalContributed >= circle.getGoal();

        // 4) Only mark goalMet when the 7-day window has ended AND group goal is met
        circle.setGoalMet(ended && groupMet);
    }


    /** yyyy-MM-dd -> millis for (date + 7 days), DST-safe. */
    private long add7Days(String ymd) {
        if (ymd == null) {
            return 0L;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            sdf.setLenient(false);
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(ymd));
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            cal.add(Calendar.DAY_OF_YEAR, 7);
            return cal.getTimeInMillis();
        } catch (Exception e) {
            return 0L;
        }
    }

    /** Convert AppDate (Y,M,D) to millis at local 00:00. */
    private long appDateStartMillis(AppDate a) {
        if (a == null) {
            return 0L;
        }
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, a.getYear());
        cal.set(Calendar.MONTH, a.getMonth() - 1);
        cal.set(Calendar.DAY_OF_MONTH, a.getDay());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}

