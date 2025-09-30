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


    public void login(String email, String password){
        //logic for login
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    userLiveData.setValue(mAuth.getCurrentUser());
                    errorMessage.setValue(null);
                } else {
                    userLiveData.setValue(null);
                    errorMessage.setValue("Invalid email or password");
                    Log.w("AuthenticationViewModel", "Failed to login", task.getException());
                }
            });
    }

    public void register(String email, String password){
        //logic for register
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    userLiveData.setValue(mAuth.getCurrentUser());
                    errorMessage.setValue(null);
                } else {
                    userLiveData.setValue(null);
                    errorMessage.setValue("Invalid email or password");
                    Log.w("AuthenticationViewModel", "Failed to create an account", task.getException());
                }
            });
    }



//    public void logout(){
//        //logic for logout
//    }
}
