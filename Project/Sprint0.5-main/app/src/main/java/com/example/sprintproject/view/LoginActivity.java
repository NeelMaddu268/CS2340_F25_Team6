package com.example.sprintproject.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sprintproject.R;
import com.example.sprintproject.viewmodel.AuthenticationViewModel;

public class LoginActivity extends AppCompatActivity {

    private AuthenticationViewModel authenticationViewModel;

    private String email;
    private String password;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Log.d("LoginActivity", "onCreate called");


        EditText emailInput = findViewById(R.id.email);
        EditText passwordInput = findViewById(R.id.password);
        Button loginButton = findViewById(R.id.btnLogin);
        Button createAccount = findViewById(R.id.btnCreateAccount);
        TextView error = findViewById(R.id.error);

        authenticationViewModel = new AuthenticationViewModel();

        loginButton.setOnClickListener(v -> {
            email = emailInput.getText().toString();
            password = passwordInput.getText().toString();
            authenticationViewModel.login(email, password);
            if (authenticationViewModel.getErrorMessage() != null) {
                error.setVisibility(View.VISIBLE);
                error.setText(authenticationViewModel.getErrorMessage().getValue());
            }
        });

        createAccount.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );
    }
}