package com.example.bafp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 2;
    private static final int SETTINGS_REQUEST_CODE = 3;
    private static final String ACTION_REQUEST_PERMISSION = "com.example.bafp.REQUEST_PERMISSION";
    private static final String PREFS_NAME = "com.example.bafp.PREFS";
    private static final String KEY_MONITORING_TOGGLE = "monitoringToggle";
    private static final String KEY_MIN_SPEED = "min_speed";
    private static final String KEY_TIMER_LIMIT = "timer_limit";

    private TextView speedTextView;
    private TextView settingsTextView;
    private ToggleButton monitoringToggleButton;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private SharedPreferences sharedPreferences;
    private int minSpeed = 15; // Default min speed in km/h
    private int timerLimit = 5; // Default timer limit in minutes

    private BroadcastReceiver permissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_REQUEST_PERMISSION.equals(intent.getAction())) {
                requestPostNotificationsPermission();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        speedTextView = findViewById(R.id.speedTextView);
        settingsTextView = findViewById(R.id.settingsTextView);
        monitoringToggleButton = findViewById(R.id.monitoringToggleButton);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        minSpeed = sharedPreferences.getInt(KEY_MIN_SPEED, 15);
        timerLimit = sharedPreferences.getInt(KEY_TIMER_LIMIT, 5);
        updateUI(minSpeed, timerLimit);
        boolean isMonitoringEnabled = sharedPreferences.getBoolean(KEY_MONITORING_TOGGLE, true);
        monitoringToggleButton.setChecked(isMonitoringEnabled);

        monitoringToggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_MONITORING_TOGGLE, isChecked);
            editor.apply();

            if (isChecked) {
                startSpeedMonitorService();
            } else {
                stopSpeedMonitorService();
            }
        });

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double speed = location.getSpeed() * 3.6; // Convert m/s to km/h
                runOnUiThread(() -> speedTextView.setText(String.format("Current Speed: %.2f km/h", speed)));
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        checkAndRequestPermissions();

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(ACTION_REQUEST_PERMISSION);
        registerReceiver(permissionReceiver, filter);

        // Initialize with default values or saved settings
        updateUI(minSpeed, timerLimit);
    }

    @Override
    protected void onResume() {
        super.onResume();
        minSpeed = sharedPreferences.getInt(KEY_MIN_SPEED, 15);
        timerLimit = sharedPreferences.getInt(KEY_TIMER_LIMIT, 5);
        updateUI(minSpeed, timerLimit);
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
        } else {
            if (monitoringToggleButton.isChecked()) {
                startSpeedMonitorService();
            }
        }
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
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra("MIN_SPEED", minSpeed);
            intent.putExtra("TIMER_LIMIT", timerLimit);
            startActivityForResult(intent, SETTINGS_REQUEST_CODE);
            return true;
        } else if (id == R.id.action_tutorial) {
            Toast.makeText(this, "Tutorial clicked", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_about) {
            Toast.makeText(this, "About clicked", Toast.LENGTH_SHORT).show();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void startSpeedMonitorService() {
        Intent intent = new Intent(this, SpeedMonitorService.class);
        intent.putExtra("minSpeed", minSpeed);
        intent.putExtra("timer", timerLimit * 60000L); // Convert minutes to milliseconds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, intent);
        } else {
            startService(intent);
        }

        startLocationUpdates();
    }

    private void stopSpeedMonitorService() {
        stopService(new Intent(this, SpeedMonitorService.class));
    }

    private void stopAlarm() {
        Intent intent = new Intent(this, SpeedMonitorService.class);
        intent.setAction("STOP_ALARM");
        startService(intent);
    }

    private void startLocationUpdates() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0f, locationListener);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SETTINGS_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            minSpeed = data.getIntExtra("MIN_SPEED", minSpeed);
            timerLimit = data.getIntExtra("TIMER_LIMIT", timerLimit);
            updateUI(minSpeed, timerLimit);

            // Stop the alarm and restart the service with new settings
            stopAlarm();
            if (monitoringToggleButton.isChecked()) {
                startSpeedMonitorService();
            }
        }
    }

    private void updateUI(int minSpeed, int timerLimit) {
        settingsTextView.setText(String.format("Minimum Speed: %d km/h\nTimer Limit: %d minutes", minSpeed, timerLimit));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop location updates
        locationManager.removeUpdates(locationListener);
        // Unregister the BroadcastReceiver
        unregisterReceiver(permissionReceiver);

        // Stop the alarm
        stopAlarm();

        // Check the toggle state before stopping the service
        if (!sharedPreferences.getBoolean(KEY_MONITORING_TOGGLE, true)) {
            // Stop the SpeedMonitorService
            stopSpeedMonitorService();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && "ALERT_DISMISSED".equals(intent.getAction())) {
            onPopUpAlertDismissed();
        }
    }

    private void requestPostNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE || requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkAndRequestPermissions();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onPopUpAlertDismissed() {
        stopAlarm();
        if (monitoringToggleButton.isChecked()) {
            startSpeedMonitorService();
        }
    }
}