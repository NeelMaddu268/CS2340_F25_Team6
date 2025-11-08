package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.model.SavingsCircle;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
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
                .addSnapshotListener((QuerySnapshot qs, FirebaseFirestoreException e) -> {
                    if (e != null || qs == null) {
                        savingsCircleLiveData.postValue(new ArrayList<>());
                        return;
                    }
                    List<SavingsCircle> list = new ArrayList<>();
                    for (DocumentSnapshot pointerDoc : qs.getDocuments()) {
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
                                        list.add(circle);

                                        savingsCircleLiveData.postValue(new ArrayList<>(list));
                                    }
                                });
                    }
                });
    }

    //    public void deleteCircle(String circleId) {
    //        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
    //
    //        FirestoreManager.getInstance()
    //                .deleteSavingsCircle(circleId, currentUid)
    //                .addOnSuccessListener(aVoid ->
    //                        System.out.println("Circle deleted successfully"))
    //                .addOnFailureListener(e ->
    //                        System.err.println("Delete failed: " + e.getMessage()));
    //    }

}