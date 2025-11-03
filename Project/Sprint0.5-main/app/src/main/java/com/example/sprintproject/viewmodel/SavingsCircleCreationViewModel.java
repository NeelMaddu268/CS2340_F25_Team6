package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.FirestoreManager;
import com.example.sprintproject.model.Budget;
import com.example.sprintproject.model.Expense;
import com.example.sprintproject.model.SavingsCircle;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class SavingsCircleCreationViewModel extends ViewModel {
    private final MutableLiveData<String> text = new MutableLiveData<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final MutableLiveData<List<String>> categoriesLiveData =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<String>> frequenciesLiveData =
            new MutableLiveData<>(new ArrayList<>());

    public SavingsCircleCreationViewModel() {
        // Just sets a sample value (not used for logic)
        text.setValue("Hello from ViewModel (placeholder)");
    }

    public LiveData<String> getText() {
        return text;
    }

    public LiveData<List<String>> getCategories() {
        return categoriesLiveData;
    }

    public LiveData<List<String>> getFrequencies() {
        return frequenciesLiveData;
    }

    public void loadCategories() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirestoreManager.getInstance().categoriesReference(uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> categoryNames = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        if (doc.getString("name") != null) {
                            categoryNames.add(doc.getString("name"));
                        }
                    }
                    categoriesLiveData.setValue(categoryNames);
                })
                .addOnFailureListener(e -> {
                    categoriesLiveData.setValue(new ArrayList<>());
                });
    }

    public void loadFrequencies() {
        List<String> defaultFrequencies = new ArrayList<>();
        defaultFrequencies.add("Weekly");
        defaultFrequencies.add("Monthly");

        frequenciesLiveData.setValue(defaultFrequencies);
    }

    public void createSavingsCircle(
            String name, String email,
            String title, String goalString, String frequency, String notes) {

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            text.setValue("User not logged in");
            return;
        }

        Double amount = parseAmount(goalString);
        if (amount == null) {
            return;
        }

        SavingsCircle savingsCircle = new SavingsCircle(name, email, "", title, amount, frequency, notes);

        String uid = auth.getCurrentUser().getUid();

        FirestoreManager.getInstance().savingsCircleReference(uid)
                .add(savingsCircle)
                .addOnSuccessListener(docRef -> {
                    String newSavingsCircleId = docRef.getId();
                    System.out.println("[createSavingsCircle] SavingsCircle added successfully! ID="
                            + newSavingsCircleId);
                })
                .addOnFailureListener(e -> {
                    System.out.println("[createSavingsCircle] Failed to add savingsCircle: "
                            + e.getMessage());
                    e.printStackTrace();
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