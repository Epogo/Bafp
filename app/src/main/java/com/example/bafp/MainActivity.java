package com.example.bafp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
    private boolean isServiceRunning = false;
    private TextView speedTextView;
    private Button startStopButton;
    private LocationManager locationManager;
    private LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        speedTextView = findViewById(R.id.speedTextView);
        startStopButton = findViewById(R.id.startStopButton);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isServiceRunning) {
                    stopSpeedMonitorService();
                } else {
                    startSpeedMonitorService();
                }
                isServiceRunning = !isServiceRunning;
                startStopButton.setText(isServiceRunning ? "Stop" : "Start");
            }
        });

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double speed = location.getSpeed() * 3.6; // Convert m/s to km/h
                speedTextView.setText(String.format("Current Speed: %.2f km/h", speed));
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };
    }

    private void startSpeedMonitorService() {
        Intent intent = new Intent(this, SpeedMonitorService.class);
        startService(intent);
        startLocationUpdates();
    }

    private void stopSpeedMonitorService() {
        Intent intent = new Intent(this, SpeedMonitorService.class);
        stopService(intent);
        stopLocationUpdates();
    }

    private void startLocationUpdates() {
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0f, locationListener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void stopLocationUpdates() {
        locationManager.removeUpdates(locationListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }
}
