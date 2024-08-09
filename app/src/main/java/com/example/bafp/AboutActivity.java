package com.example.bafp;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // Set up the TextView with the about information
        TextView aboutTextView = findViewById(R.id.aboutTextView);
        String aboutText = "This app was developed with a focus on child safety, particularly to prevent the tragic scenario of forgetting a child in a car. " +
                "It was developed by Evgeni Pogoster, with the first version released on August 9, 2024. " +
                "The app provides tools to monitor vehicle speed and ensure timely alerts to protect your loved ones.";

        aboutTextView.setText(aboutText);
    }
}
