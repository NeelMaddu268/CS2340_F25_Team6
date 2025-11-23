package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.model.AppDate;
import com.example.sprintproject.model.SavingsCircle;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class SavingsCircleCreationViewModel extends ViewModel {
    private final MutableLiveData<String> text = new MutableLiveData<>();

    public SavingsCircleCreationViewModel() {
        // Just sets a sample value (not used for logic)
    }

    public LiveData<String> getText() {
        return text;
    }

    public void createUserSavingsCircle(String name,
            String title, String goalString, String frequency, String notes, AppDate dateJoined) {

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            text.setValue("User not logged in");
            return;
        }

        Double goal = parseAmount(goalString);
        if (goal == null) {
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        SavingsCircle circle = new SavingsCircle();
        circle.setName(name);
        circle.setCreatorId(uid);
        circle.setDatesJoined(Collections.singletonMap(uid, dateJoined.toIso()));
        circle.setCreatorDateJoined(dateJoined);
        circle.setTitle(title);
        circle.setGoal(goal);
        circle.setFrequency(frequency);
        circle.setNotes(notes);
        circle.setMemberIds(Collections.singletonList(uid));
        String currentUserEmail = FirestoreManager.getInstance().getCurrentUserEmail();
        circle.setMemberEmails(Collections.singletonList(currentUserEmail));
        circle.setContributions(Collections.singletonMap(uid, 0.0));
        circle.setInvite("active");
        circle.setSpent(0.0);

        FirestoreManager.getInstance().savingsCirclesGlobalReference()
                .add(circle)
                .addOnSuccessListener(docRef -> {
                    String circleId = docRef.getId();

                    Map<String, Object> pointerData = new HashMap<>();
                    pointerData.put("circleId", circleId);
                    pointerData.put("name", name);
                    pointerData.put("title", title);
                    pointerData.put("goal", goal);

                    FirestoreManager.getInstance()
                            .userSavingsCirclePointers(uid)
                            .add(pointerData);

                });
    }

    private Double parseAmount(String goalString) {
        try {
            double amount = Double.parseDouble(goalString);
            if (amount <= 0) {
                text.setValue("Amount must be greater than 0");
                return null;
            }
            return amount;
        } catch (NumberFormatException e) {
            text.setValue("Invalid amount");
            return null;
        }
    }
}