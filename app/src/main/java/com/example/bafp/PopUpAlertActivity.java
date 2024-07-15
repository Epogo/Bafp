package com.example.bafp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

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
        });
    }

    @Override
    public void onBackPressed() {
        // Prevent closing the activity
        Toast.makeText(this, "Please press the OK button to dismiss the alarm", Toast.LENGTH_SHORT).show();
    }
}