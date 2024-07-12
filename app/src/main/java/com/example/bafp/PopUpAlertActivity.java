package com.example.bafp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class PopUpAlertActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popup_alert);

        // Prevent dismissing the dialog by tapping outside
        setFinishOnTouchOutside(false);

        Button okButton = findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
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
}