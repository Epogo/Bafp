package com.example.bafp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class PopUpAlertActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popup_alert);

        Button stopAlertButton = findViewById(R.id.stop_alert_button);
        stopAlertButton.setOnClickListener(v -> {
            // Send an intent to the service to stop the alarm
            Intent stopIntent = new Intent(this, SpeedMonitorService.class);
            stopIntent.setAction("STOP_ALARM");
            startService(stopIntent);
            finish(); // Close the activity
        });
    }
}
