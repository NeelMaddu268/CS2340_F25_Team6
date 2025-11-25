// ViewModel that handles the firebase login, registration with email and password and displays errors through LiveData.
// On successful registrations it instanciates the user profile in firestore and triggers creation of sample budgets and expenses.

package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.sprintproject.model.ThemeManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;


public class AuthenticationViewModel extends ViewModel {
    private final MutableLiveData<FirebaseUser> userLiveData;
    private final MutableLiveData<String> errorMessage;
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static final String DARK_MODE = "darkMode";
    public static final String USERS = "users";

    public AuthenticationViewModel() {
        userLiveData = new MutableLiveData<>();
        errorMessage = new MutableLiveData<>();
        mAuth = FirebaseAuth.getInstance();
    }


    public LiveData<FirebaseUser> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    private boolean isEmailInvalid(String email) {
        return email == null || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isPasswordInvalid(String password) {
        return password == null || password.length() < 6;
    }


    public void login(String email, String password, Context context) {
        if (isEmailInvalid(email)) {
            errorMessage.setValue("Invalid email");
            return;
        }
        if (isPasswordInvalid(password)) {
            errorMessage.setValue("Password must be at least 6 characters");
            return;
        }
        //logic for login
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser firebaseUser = task.getResult().getUser();
                    userLiveData.setValue(mAuth.getCurrentUser());
                    errorMessage.setValue(null);
                    savedDarkMode(firebaseUser, context.getApplicationContext());
                } else {
                    Exception e = task.getException();
                    if (e != null) {
                        Log.w("AuthenticationViewModel", "Failed to login", e);
                        errorMessage.setValue("Email or Password is incorrect");
                    }
                }
            });
    }

    private void savedDarkMode(FirebaseUser firebaseUser, Context context) {
        if (firebaseUser == null) {
            return;
        }
        String uid = firebaseUser.getUid();
        db.collection(USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.contains(DARK_MODE)) {
                        boolean darkMode = doc.getBoolean(DARK_MODE);
                        ThemeManager.applyTheme(darkMode, context.getApplicationContext());
                    }
                });
    }

    public void register(String email, String password, Context context) {
        if (isEmailInvalid(email)) {
            errorMessage.setValue("Invalid email");
            return;
        }
        if (isPasswordInvalid(password)) {
            errorMessage.setValue("Password must be at least 6 characters");
            return;
        }
        //logic for register
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser firebaseUser = task.getResult().getUser();
                    userLiveData.setValue(mAuth.getCurrentUser());
                    errorMessage.setValue(null);
                    if (firebaseUser != null) {
                        createUserInFirestore(firebaseUser);

                        ThemeManager.applyTheme(false, context.getApplicationContext());

                        db.collection(USERS).document(firebaseUser.getUid())
                                .update(DARK_MODE, false)
                                .addOnSuccessListener(aVoid -> Log.d("AuthVM", "Default theme set"))
                                .addOnFailureListener(e -> Log.w("AuthVM", "Failed to set default theme", e));
                    }
                    BudgetCreationViewModel budgetCreationViewModel =
                            new BudgetCreationViewModel();
                    ExpenseCreationViewModel expenseCreationViewModel =
                            new ExpenseCreationViewModel();
                    //need to wait for budgets to be made for expenses to be made
                    //add some async/sync logic
                    //budget view model will call expense view model to make expenses
                    budgetCreationViewModel
                            .createSampleBudgets(expenseCreationViewModel::createSampleExpenses);
                } else {
                    Exception e = task.getException();
                    Log.w("AuthenticationViewModel",
                            "Failed to create an account", task.getException());
                    errorMessage.setValue("Registration failed");

                    if (e instanceof FirebaseAuthUserCollisionException) {
                        errorMessage.setValue("Email already in use");
                    }
                }
            });
    }

    public void logout(Context context) {
        mAuth.signOut();
        userLiveData.setValue(null);
        ThemeManager.clearTheme(context.getApplicationContext());
    }

    public void createUserInFirestore(FirebaseUser firebaseUser) {
        String uid = firebaseUser.getUid();

        Map<String, Object> userData = new HashMap<>();
        userData.put("email", firebaseUser.getEmail());
        userData.put("name", "User");

        FirestoreManager.getInstance().addUser(uid, userData);
    }
    public void toggleTheme(boolean darkMode, Context context) {
        ThemeManager.applyTheme(darkMode, context.getApplicationContext());

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection(USERS).document(user.getUid())
                    .update(DARK_MODE, darkMode);
        }
    }
}
