package com.example.roomieshare;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import androidx.fragment.app.FragmentActivity;

public class HomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        ImageView profilePlaceholder = findViewById(R.id.profilePlaceholder);
        profilePlaceholder.setOnClickListener(v ->
            startActivity(new Intent(HomeActivity.this, ProfileActivity.class))
        );

        // Show current date in a TextView
        TextView currentDateText = findViewById(R.id.currentDateText);
        String currentDate = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(new Date());
        currentDateText.setText(currentDate);

        findViewById(R.id.cardChoreTracker).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ChoreListHostActivity.class);
            startActivity(intent);
        });
    }
} 