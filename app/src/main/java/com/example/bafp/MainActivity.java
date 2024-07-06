package com.example.bafp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int SETTINGS_REQUEST_CODE = 2;
    private boolean isServiceRunning = false;
    private TextView speedTextView;
    private Button startStopButton;
    private TextView settingsTextView;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private int minSpeed = 15; // Default min speed in km/h
    private int timerLimit = 5; // Default timer limit in minutes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        speedTextView = findViewById(R.id.speedTextView);
        startStopButton = findViewById(R.id.startStopButton);
        settingsTextView = findViewById(R.id.settingsTextView);

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
                if (isServiceRunning) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            speedTextView.setText(String.format("Current Speed: %.2f km/h", speed));
                        }
                    });
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        // Check for location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }

        // Initialize with default values or saved settings
        updateUI(minSpeed, timerLimit);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // Handle settings click
            Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
            // Start SettingsActivity for result
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra("MIN_SPEED", minSpeed);
            intent.putExtra("TIMER_LIMIT", timerLimit);
            startActivityForResult(intent, SETTINGS_REQUEST_CODE);
            return true;
        } else if (id == R.id.action_tutorial) {
            // Handle tutorial click
            Toast.makeText(this, "Tutorial clicked", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_about) {
            // Handle about click
            Toast.makeText(this, "About clicked", Toast.LENGTH_SHORT).show();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0f, locationListener);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void stopLocationUpdates() {
        locationManager.removeUpdates(locationListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SETTINGS_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                minSpeed = data.getIntExtra("MIN_SPEED", minSpeed);
                timerLimit = data.getIntExtra("TIMER_LIMIT", timerLimit);

                // Update UI with new settings
                updateUI(minSpeed, timerLimit);
            }
        }
    }

    private void updateUI(int minSpeed, int timerLimit) {
        // Update UI elements with new settings
        // For example, update text views or other UI components
        // Here's a placeholder for updating settingsTextView
        settingsTextView.setText(String.format("Minimum Speed: %d km/h\nTimer Limit: %d minutes", minSpeed, timerLimit));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
