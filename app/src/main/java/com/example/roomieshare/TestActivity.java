package com.example.roomieshare;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class TestActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_RoomieShare);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
    }
} 