package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.auth.AuthResult;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.DatabaseReference;
import android.util.Log;


public class AuthenticationViewModel extends ViewModel {
    private final MutableLiveData<FirebaseUser> userLiveData;
    private final FirebaseAuth mAuth;
    private final MutableLiveData<String> errorMessage;

    public AuthenticationViewModel() {
        //DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        userLiveData = new MutableLiveData<>();
        mAuth = FirebaseAuth.getInstance();
        errorMessage = new MutableLiveData<>();
    }

//    public LiveData<FirebaseUser> getUserLiveData(){
//        return userLiveData;
//    }

    public LiveData<String> getErrorMessage(){
        return errorMessage;
    }

    private boolean isEmailValid(String email) {
        return email != null && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isPasswordValid(String password) {
        return password != null && password.length() >= 6;
    }


    public void login(String email, String password){
        if (!isEmailValid(email)) {
            errorMessage.setValue("Invalid email");
            return;
        }
        if (!isPasswordValid(password)) {
            errorMessage.setValue("Password must be at least 6 characters");
            return;
        }
        //logic for login
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    userLiveData.setValue(mAuth.getCurrentUser());
                    errorMessage.setValue(null);
                } else {
                    Exception e = task.getException();
                    if (e != null) {
                        Log.w("AuthenticationViewModel", "Failed to login", e);
                        errorMessage.setValue("Email or Password is incorrect");
                    }
                }
            });
    }

    public void register(String email, String password){
        if (!isEmailValid(email)) {
            errorMessage.setValue("Invalid email");
            return;
        }
        if (!isPasswordValid(password)) {
            errorMessage.setValue("Password must be at least 6 characters");
            return;
        }
        //logic for register
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    userLiveData.setValue(mAuth.getCurrentUser());
                    errorMessage.setValue(null);
                } else {
                    Exception e = task.getException();
                    if (e != null) {
                        Log.w("AuthenticationViewModel", "Failed to create an account", task.getException());
                        errorMessage.setValue("Registration failed");
                    }
                }
            });
    }



//    public void logout(){
//        //logic for logout
//    }
}
