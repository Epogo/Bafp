package com.example.bafp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private SeekBar minSpeedSeekBar;
    private SeekBar timerLimitSeekBar;
    private TextView minSpeedValueTextView;
    private TextView timerLimitValueTextView;
    private Button setButton;

    private static final String PREFS_NAME = "com.example.bafp.PREFS";
    private static final String KEY_MIN_SPEED = "min_speed";
    private static final String KEY_TIMER_LIMIT = "timer_limit";

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        minSpeedSeekBar = findViewById(R.id.minSpeedSeekBar);
        timerLimitSeekBar = findViewById(R.id.timerLimitSeekBar);
        minSpeedValueTextView = findViewById(R.id.minSpeedValueTextView);
        timerLimitValueTextView = findViewById(R.id.timerLimitValueTextView);
        setButton = findViewById(R.id.setButton);

        // Load saved settings or use defaults
        int currentMinSpeed = sharedPreferences.getInt(KEY_MIN_SPEED, 15);
        int currentTimerLimit = sharedPreferences.getInt(KEY_TIMER_LIMIT, 5);

        // Update minSpeedSeekBar max value
        minSpeedSeekBar.setMax(25 - 0); // Max progress is 25 - 0 = 25
        minSpeedSeekBar.setProgress(currentMinSpeed); // No need to adjust progress here
        minSpeedValueTextView.setText(String.valueOf(currentMinSpeed));

        timerLimitSeekBar.setMax(10 - 1); // Max progress is 10 - 1 = 9
        timerLimitSeekBar.setProgress(currentTimerLimit - 1);
        timerLimitValueTextView.setText(String.valueOf(currentTimerLimit));

        minSpeedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int minSpeed = progress; // Use progress directly for speed (0 + progress)
                minSpeedValueTextView.setText(String.valueOf(minSpeed));
                saveSettings();
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
                saveSettings();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Set click listener for the Set button
        setButton.setOnClickListener(view -> {
            saveSettings();
            // Prepare data intent
            Intent data = new Intent();
            data.putExtra("MIN_SPEED", Integer.parseInt(minSpeedValueTextView.getText().toString()));
            data.putExtra("TIMER_LIMIT", Integer.parseInt(timerLimitValueTextView.getText().toString()));
            setResult(RESULT_OK, data);
            finish(); // Close the activity and return to MainActivity
        });
    }

    private void saveSettings() {
        int minSpeed = Integer.parseInt(minSpeedValueTextView.getText().toString());
        int timerLimit = Integer.parseInt(timerLimitValueTextView.getText().toString());

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_MIN_SPEED, minSpeed);
        editor.putInt(KEY_TIMER_LIMIT, timerLimit);
        editor.apply();
    }
}
