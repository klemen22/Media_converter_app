package com.example.media_converter_app;

import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.media_converter_app.adapter.PagerAdapter;

public class MainActivity extends AppCompatActivity {


    private ViewPager2 viewPager2;

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

        //define viewPager2
        viewPager2 = findViewById(R.id.viewPager);
        PagerAdapter adapter = new PagerAdapter(this);
        viewPager2.setAdapter(adapter);


    }
}