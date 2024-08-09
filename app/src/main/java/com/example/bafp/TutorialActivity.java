package com.example.bafp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;

public class TutorialActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        TextView tutorialTextView = findViewById(R.id.tutorialTextView);
        tutorialTextView.setText(getTutorialText());
    }

    private String getTutorialText() {
        return "Welcome to the Child Safety and Location Monitoring App!\n\n" +
                "Here's how to use the app:\n\n" +
                "1. **Grant Permissions**: The app requires permissions to access your location, send notifications, and run in the background. Ensure these permissions are granted.\n\n" +
                "2. **Set Minimum Speed and Timer**: In the settings, you can define the minimum speed limit and the timer limit for monitoring. The speed is monitored in km/h, and the timer sets the duration for monitoring.\n\n" +
                "3. **Enable Monitoring**: Use the toggle button on the main screen to start or stop monitoring. When monitoring is active, the app will track your speed and alert you if the speed exceeds the set limit.\n\n" +
                "4. **Simulation Mode**: If you want to simulate speed monitoring, enable the simulation mode in the main screen. This is useful for testing the app without actually moving.\n\n" +
                "5. **Alerts**: The app will notify you if your speed exceeds the set limit. Ensure that notifications are enabled for the app to receive alerts.\n\n" +
                "6. **Automatic Alarm Handling**: If an alarm is triggered and you do not press the 'Stop Alarm' button within 45 seconds, the alarm will automatically stop and monitoring will restart. This feature is especially useful in scenarios like traffic jams where you may not immediately be able to respond to the alarm.\n\n" +
                "7. **Exit Monitoring**: You can choose whether monitoring continues when the app is closed by using the toggle button on the main screen. If monitoring is active when you close the app, it will continue to track your speed.\n\n" +
                "Thank you for using our app! Stay safe.";
    }

}
