package com.example.bafp;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

public class PopUpAlertActivity extends Activity {

    private Button okButton;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make activity full screen without title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_popup_alert);

        // Initialize vibrator
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Start non-stop vibrations
        startVibrations();

        // OK button click listener
        okButton = findViewById(R.id.ok_button);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopVibrations(); // Stop vibrations when OK button is clicked
                finish(); // Close the popup alert activity
            }
        });
    }

    private void startVibrations() {
        // Vibration pattern (0ms delay, 1s vibration)
        long[] pattern = {0, 1000};

        // Check API level for compatibility
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // Use vibration effect for API level 26+
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
        } else {
            // Fallback for older API levels
            vibrator.vibrate(pattern, 0);
        }
    }

    private void stopVibrations() {
        // Stop vibrations
        vibrator.cancel();
    }
}
