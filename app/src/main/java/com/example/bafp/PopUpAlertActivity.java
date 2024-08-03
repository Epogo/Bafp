package com.example.bafp;

import static androidx.constraintlayout.motion.utils.Oscillator.TAG;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class PopUpAlertActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popup_alert);

        setFinishOnTouchOutside(false);

        Button stopAlertButton = findViewById(R.id.stop_alert_button);
        stopAlertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Stop the alarm sound
                Intent intent = new Intent(PopUpAlertActivity.this, SpeedMonitorService.class);
                intent.setAction("STOP_ALARM");
                startService(intent);

                // Close the activity
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        // Call the super method
        super.onBackPressed();

        // Prevent closing the activity
        // You can show a toast or dialog here to inform the user
        // that they need to press the OK button
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "PopUpAlertActivity destroyed");
    }
}
