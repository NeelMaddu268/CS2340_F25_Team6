package com.example.sprintproject.view;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.sprintproject.R;

public class Chatbot extends AppCompatActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Enables Android's edge-to-edge display mode
        EdgeToEdge.enable(this);
        // Sets the layout for this activity using the corresponding XML file
        setContentView(R.layout.activity_chatbot);

        // Adjusts the view's padding so that the content does not overlap with system UI elements
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            // Get the size of the system bars
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Apply padding to the view
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            // Return the insets
            return insets;
        });
    }
}