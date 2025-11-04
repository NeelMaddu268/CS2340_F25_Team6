package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.model.SavingsCircle;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class SavingsCircleFragmentViewModel extends ViewModel {

    private final MutableLiveData<List<SavingsCircle>> savingsCircleLiveData =
            new MutableLiveData<>(new ArrayList<>());
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
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
                .savingsCircleReference(uid)
                .addSnapshotListener((QuerySnapshot qs, FirebaseFirestoreException e) -> {
                    if (e != null || qs == null) {
                        savingsCircleLiveData.postValue(new ArrayList<>());
                        return;
                    }
                    List<SavingsCircle> list = new ArrayList<>();
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        SavingsCircle group = doc.toObject(SavingsCircle.class);
                        if (group != null) {
                            list.add(group);
                        }
                    }
                    savingsCircleLiveData.postValue(list);
                });
    }

//    public SavingsCircleFragmentViewModel() {
//        // Just sets a sample value (not used for logic)
//        text.setValue("Hello from ViewModel (placeholder)");
//    }
//
//    public LiveData<String> getText() {
//        return text;
//    }

//    public void doNothing() {
//        // This method does nothing
//    }
}
