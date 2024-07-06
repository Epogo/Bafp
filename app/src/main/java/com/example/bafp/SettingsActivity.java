package com.example.bafp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private SeekBar minSpeedSeekBar;
    private SeekBar timerLimitSeekBar;
    private TextView minSpeedValueTextView;
    private TextView timerLimitValueTextView;
    private Button setButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        minSpeedSeekBar = findViewById(R.id.minSpeedSeekBar);
        timerLimitSeekBar = findViewById(R.id.timerLimitSeekBar);
        minSpeedValueTextView = findViewById(R.id.minSpeedValueTextView);
        timerLimitValueTextView = findViewById(R.id.timerLimitValueTextView);
        setButton = findViewById(R.id.setButton);

        // Initialize with default values or saved settings
        int currentMinSpeed = getIntent().getIntExtra("MIN_SPEED", 15);
        int currentTimerLimit = getIntent().getIntExtra("TIMER_LIMIT", 5);

        minSpeedSeekBar.setMax(50 - 15); // Max progress is 50 - 15 = 35
        minSpeedSeekBar.setProgress(currentMinSpeed - 15);
        minSpeedValueTextView.setText(String.valueOf(currentMinSpeed));

        timerLimitSeekBar.setMax(10 - 1); // Max progress is 10 - 1 = 9
        timerLimitSeekBar.setProgress(currentTimerLimit - 1);
        timerLimitValueTextView.setText(String.valueOf(currentTimerLimit));

        minSpeedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int minSpeed = progress + 15; // Convert progress back to actual speed (15 + progress)
                minSpeedValueTextView.setText(String.valueOf(minSpeed));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        timerLimitSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int minutes = progress + 1; // Convert progress back to actual minutes (1 + progress)
                timerLimitValueTextView.setText(String.valueOf(minutes));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Set click listener for the Set button
        setButton.setOnClickListener(view -> {
            // Prepare data intent
            Intent data = new Intent();
            data.putExtra("MIN_SPEED", Integer.parseInt(minSpeedValueTextView.getText().toString()));
            data.putExtra("TIMER_LIMIT", Integer.parseInt(timerLimitValueTextView.getText().toString()));
            setResult(RESULT_OK, data);
            finish(); // Close the activity and return to MainActivity
        });
    }
}
