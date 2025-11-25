// The activity handles the user login by taking in the email and password of the user
// and interacting with the authentication view model. Automatically navigates
// the user to main app when login is successful.

package com.example.sprintproject.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.sprintproject.R;
import com.example.sprintproject.model.ThemeManager;
import com.example.sprintproject.viewmodel.AuthenticationViewModel;

public class LoginActivity extends AppCompatActivity {


    private String email;
    private String password;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean darkMode = ThemeManager.isDarkModeEnabled(this);

        AppCompatDelegate.setDefaultNightMode(
                darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        AuthenticationViewModel authenticationViewModel;

        Log.d("LoginActivity", "onCreate called");


        EditText emailInput = findViewById(R.id.creatorEmail);
        EditText passwordInput = findViewById(R.id.password);
        Button loginButton = findViewById(R.id.btnLogin);
        Button createAccount = findViewById(R.id.btnCreateAccount);
        TextView error = findViewById(R.id.error);

        authenticationViewModel = new AuthenticationViewModel();

        loginButton.setOnClickListener(v -> {
            email = emailInput.getText().toString();
            password = passwordInput.getText().toString();
            authenticationViewModel.login(email, password, this);
        });

        createAccount.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );

        authenticationViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                error.setText(errorMessage);
                error.setVisibility(View.VISIBLE);
            } else {
                error.setVisibility(View.GONE);
                Intent intent = new Intent(LoginActivity.this, AppActivity.class);
                startActivity(intent);
                finish();

            }
        });
    }
}