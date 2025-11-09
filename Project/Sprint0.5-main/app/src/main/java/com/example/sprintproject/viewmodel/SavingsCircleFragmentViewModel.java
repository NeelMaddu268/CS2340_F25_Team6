package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.model.SavingsCircle;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class SavingsCircleFragmentViewModel extends ViewModel {

    private final MutableLiveData<List<SavingsCircle>> savingsCircleLiveData =
            new MutableLiveData<>(new ArrayList<>());

    private ListenerRegistration activeListener;

    public LiveData<List<SavingsCircle>> getSavingsCircle() {
        return savingsCircleLiveData;
    }

    private void detachActiveListener() {
        if (activeListener != null) {
            activeListener.remove();
            activeListener = null;
        }
    }

    public void loadSavingsCircle() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            savingsCircleLiveData.postValue(new ArrayList<>());
            return;
        }
        String uid = auth.getCurrentUser().getUid();

        detachActiveListener();

        activeListener = FirestoreManager.getInstance()
                .userSavingsCirclePointers(uid)
                .addSnapshotListener((qs, e) -> {
                    if (e != null || qs == null) {
                        savingsCircleLiveData.postValue(new ArrayList<>());
                        return;
                    }
                    if (qs.isEmpty()) {
                        savingsCircleLiveData.postValue(new ArrayList<>());
                        return;
                    }

                    // Collect circle IDs from pointers
                    List<String> circleIds = new ArrayList<>();
                    qs.getDocuments().forEach(d -> {
                        String id = d.getString("circleId");
                        if (id != null) {
                            circleIds.add(id);
                        }
                    });

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
                    // track which IDs we found
                    java.util.Set<String> found = new java.util.HashSet<>();

                    for (Object obj : results) {
                        QuerySnapshot snapshot = (QuerySnapshot) obj;
                        snapshot.getDocuments().forEach(doc -> {
                            SavingsCircle circle = doc.toObject(SavingsCircle.class);
                            if (circle != null) {
                                circle.setId(doc.getId());
                                list.add(circle);
                                found.add(doc.getId());
                            }
                        });
                    }

                    java.util.Set<String> randoms = new java.util.HashSet<>(ids);
                    randoms.removeAll(found);
                    if (!randoms.isEmpty()) {
                        cleanupRandomPointers(uid, randoms);
                    }
                    savingsCircleLiveData.postValue(list);
                })
                .addOnFailureListener(err -> {
                    savingsCircleLiveData.postValue(new ArrayList<>());
                });
    }

    private void cleanupRandomPointers(String uid, java.util.Set<String> random) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<String> toDelete = new ArrayList<>(random);
        for (int i = 0; i < toDelete.size(); i += 10) {
            List<String> chunk = toDelete.subList(i, Math.min(i + 10, toDelete.size()));
            db.collection("users")
                    .document(uid)
                    .collection("savingsCirclePointers")
                    .whereIn("circleId", chunk)
                    .get()
                    .addOnSuccessListener(qs -> {
                        qs.getDocuments().forEach(d -> d.getReference().delete());
                    });
        }
    }
}