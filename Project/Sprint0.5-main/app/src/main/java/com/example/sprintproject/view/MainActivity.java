package com.example.sprintproject.view;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.example.sprintproject.R;

public class MainActivity extends AppCompatActivity {

    private boolean isLoading = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        splashScreen.setKeepOnScreenCondition(() -> isLoading);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            isLoading = false;
        }, 1500);

        Button btnStart = findViewById(R.id.btnStart);
        Button btnQuit = findViewById(R.id.btnQuit);


        btnStart.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });


        btnQuit.setOnClickListener(v -> finishAffinity());
    }
}