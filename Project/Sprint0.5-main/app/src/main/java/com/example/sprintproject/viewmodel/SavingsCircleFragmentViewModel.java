package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.model.AppDate;
import com.example.sprintproject.model.SavingsCircle;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        String uid = auth.getCurrentUser().getUid();
        currentUidCached = uid;
        currentAppDate = appDate;

        detachActiveListener();

        activeListener = FirestoreManager.getInstance()
                .userSavingsCirclePointers(uid)
                .addSnapshotListener((qs, e) -> {
                    if (e != null || qs == null || qs.isEmpty()) {
                        savingsCircleLiveData.postValue(new ArrayList<>());
                        return;
                    }

                    List<String> circleIds = new ArrayList<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        String id = d.getString("circleId");
                        if (id != null) {
                            circleIds.add(id);
                        }
                    }

                    fetchCirclesByIds(uid, circleIds);
                });
    }

    private void fetchCirclesByIds(String uid, List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            savingsCircleLiveData.postValue(new ArrayList<>());
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();

        // Chunking by 10 for Firestore's whereIn limit
        for (int i = 0; i < ids.size(); i += 10) {
            List<String> chunk = ids.subList(i, Math.min(i + 10, ids.size()));
            tasks.add(
                    db.collection("savingsCircles")
                            .whereIn(FieldPath.documentId(), chunk)
                            .get()
            );
        }

        Tasks.whenAllSuccess(tasks)
                .addOnSuccessListener(results -> {
                    List<SavingsCircle> list = new ArrayList<>();
                    Set<String> found = new HashSet<>();

                    for (Object obj : results) {
                        QuerySnapshot snapshot = (QuerySnapshot) obj;
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            SavingsCircle circle = doc.toObject(SavingsCircle.class);
                            if (circle != null) {
                                circle.setId(doc.getId());
                                list.add(circle);
                                found.add(doc.getId());

                                // Optional: evaluate circle status
                                evaluatePersonalAgainstAppDate(circle, uid, currentAppDate);
                            }
                        }
                    }

                    // Clean up dangling pointers
                    Set<String> missing = new HashSet<>(ids);
                    missing.removeAll(found);
                    if (!missing.isEmpty()) {
                        cleanupRandomPointers(uid, missing);
                    }

                    cache.clear();
                    cache.addAll(list);
                    savingsCircleLiveData.postValue(list);
                })
                .addOnFailureListener(err -> savingsCircleLiveData.postValue(new ArrayList<>()));
    }

    private void cleanupRandomPointers(String uid, Set<String> random) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<String> toDelete = new ArrayList<>(random);

        for (int i = 0; i < toDelete.size(); i += 10) {
            List<String> chunk = toDelete.subList(i, Math.min(i + 10, toDelete.size()));
            db.collection("users")
                    .document(uid)
                    .collection("savingsCirclePointers")
                    .whereIn("circleId", chunk)
                    .get()
                    .addOnSuccessListener(qs ->
                            qs.getDocuments().forEach(d -> d.getReference().delete())
                    );
        }
    }

    /** Placeholder: evaluate circle status against AppDate (implement as needed). */
    private void evaluatePersonalAgainstAppDate(SavingsCircle circle, String uid, AppDate appDate) {
        // Implement any logic for “ended”, “joined”, or “goal reached” state checks here.
        // Example: circle.setEnded(appDate != null && appDate.isAfter(circle.getEndDate()));
    }
}
