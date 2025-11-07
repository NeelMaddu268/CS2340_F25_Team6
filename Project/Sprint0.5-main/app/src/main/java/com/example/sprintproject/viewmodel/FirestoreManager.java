package com.example.sprintproject.viewmodel;

import com.example.sprintproject.model.Budget;
import com.example.sprintproject.model.Expense;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Map;

public class FirestoreManager {
    private static volatile FirestoreManager instance;
    private final FirebaseFirestore db;

    private FirestoreManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirestoreManager getInstance() {
        if (instance == null) {
            synchronized (FirestoreManager.class) {
                if (instance == null) {
                    instance = new FirestoreManager();
                }
            }
        }
        return instance;
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    public CollectionReference budgetsReference(String uid) {
        return db.collection("users").document(uid).collection("budgets");
    }

    public CollectionReference expensesReference(String uid) {
        return db.collection("users").document(uid).collection("expenses");
    }

    public CollectionReference categoriesReference(String uid) {
        return db.collection("users").document(uid).collection("categories");
    }

    public CollectionReference savingsCirclesGlobalReference() {
        return db.collection("savingsCircles");
    }

    public CollectionReference userSavingsCirclePointers(String uid) {
        return db.collection("users").document(uid).collection("savingsCirclePointers");
    }

    public CollectionReference invitationsReference() {
        return db.collection("invitations");
    }

    public void addUser(String uid, Map<String, Object> userData) {
        db.collection("users").document(uid).set(userData);
    }

    public Query invitationsForUser(String uid) {
        return invitationsReference().whereEqualTo("toUid", uid);
    }

    public Task<Void> deleteSavingsCircle(String circleId, String currentUserId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference circleRef = db.collection("savingsCircles").document(circleId);

        return circleRef.get().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                throw new Exception("Can't find circle");
            }

            DocumentSnapshot doc = task.getResult();
            if (!doc.exists()) {
                throw new Exception("Can't find circle");
            }

            String creatorId = doc.getString("creatorId");
            if (creatorId == null || !creatorId.equals(currentUserId)) {
                throw new Exception("Only creator can delete this circle");
            }

            return circleRef.delete().addOnSuccessListener(aVoid -> {
                System.out.println("Circle deleted: " + circleId);

                db.collection("invitations")
                        .whereEqualTo("circleId", circleId)
                        .get()
                        .addOnSuccessListener(qs -> {
                            for (DocumentSnapshot d : qs.getDocuments()) {
                                d.getReference().delete();
                            }
                        });

                db.collection("users")
                        .get()
                        .addOnSuccessListener(users -> {
                            for (DocumentSnapshot userDoc : users) {
                                userDoc.getReference()
                                        .collection("userSavingsCirclePointers")
                                        .whereEqualTo("circleId", circleId)
                                        .get()
                                        .addOnSuccessListener(pointerDocs -> {
                                            for (DocumentSnapshot pointer
                                                    : pointerDocs.getDocuments()) {
                                                pointer.getReference().delete();
                                            }
                                        });
                            }
                        });

            });
        });
    }



    public void addBudget(String uid, Budget budget) {
        budgetsReference(uid).add(budget);
    }

    public void addExpense(String uid, Expense expense) {
        expensesReference(uid).add(expense);
    }

}
