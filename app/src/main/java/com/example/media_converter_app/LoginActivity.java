package com.example.media_converter_app;

import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        View rootView = findViewById(android.R.id.content);

        rootView.setOnApplyWindowInsetsListener((v, insets) -> {
            int topInset = insets.getInsets(android.view.WindowInsets.Type.systemBars()).top;
            int botInset = insets.getInsets(WindowInsets.Type.systemBars()).bottom;
            v.setPadding(0, topInset, 0, botInset);
            return insets;
        });
    }
}