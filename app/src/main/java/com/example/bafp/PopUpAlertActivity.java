package com.example.bafp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PopUpAlertActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popup_alert);

        // Show the activity even when the device is locked
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        Button okButton = findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissAlarm();
            }
        });
    }

    private void dismissAlarm() {
        // Stop the alarm sound and reset monitoring
        Intent intent = new Intent(PopUpAlertActivity.this, SpeedMonitorService.class);
        intent.setAction("STOP_ALARM");
        startService(intent);

        // Notify MainActivity that the alert has been dismissed
        Intent mainActivityIntent = new Intent(PopUpAlertActivity.this, MainActivity.class);
        mainActivityIntent.setAction("ALERT_DISMISSED");
        mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(mainActivityIntent);

        // Close the activity
        finish();
    }

    @Override
    public void onBackPressed() {
        // Prevent closing the activity
        super.onBackPressed();
        Toast.makeText(this, "Please press the OK button to dismiss the alarm", Toast.LENGTH_SHORT).show();
    }
}