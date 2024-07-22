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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 2;
    private static final int SETTINGS_REQUEST_CODE = 3;
    private static final int REQUEST_OVERLAY_PERMISSION = 1234;
    private static final int PERMISSIONS_REQUEST_CODE = 5678;
    private static final String ACTION_REQUEST_PERMISSION = "com.example.bafp.REQUEST_PERMISSION";
    private static final String PREFS_NAME = "com.example.bafp.PREFS";
    private static final String KEY_MONITORING_TOGGLE = "monitoringToggle";
    private static final String KEY_MIN_SPEED = "min_speed";
    private static final String KEY_TIMER_LIMIT = "timer_limit";
    private static final long CHECK_INTERVAL = 5000; // Interval in milliseconds

    private TextView speedTextView;
    private TextView settingsTextView;
    private ToggleButton monitoringToggleButton;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private SharedPreferences sharedPreferences;
    private int minSpeed = 15; // Default min speed in km/h
    private int timerLimit = 5; // Default timer limit in minutes

    private Handler handler;
    private Runnable checkToggleRunnable;

    private BroadcastReceiver permissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_REQUEST_PERMISSION.equals(intent.getAction())) {
                checkAllPermissions();
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

        // Check if it's the first run
        boolean isFirstRun = sharedPreferences.getBoolean("IS_FIRST_RUN", true);
        if (isFirstRun) {
            showPermissionInstructions();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("IS_FIRST_RUN", false);
            editor.apply();
        } else {
            checkAllPermissions();
        }

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

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(ACTION_REQUEST_PERMISSION);
        registerReceiver(permissionReceiver, filter);

        // Initialize Handler and Runnable
        handler = new Handler();
        checkToggleRunnable = new Runnable() {
            @Override
            public void run() {
                boolean isMonitoringEnabled = sharedPreferences.getBoolean(KEY_MONITORING_TOGGLE, true);
                if (!isMonitoringEnabled) {
                    stopSpeedMonitorService();
                }
                handler.postDelayed(this, CHECK_INTERVAL); // Check periodically
            }
        };
        handler.post(checkToggleRunnable);

        // Initialize with default values or saved settings
        updateUI(minSpeed, timerLimit);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if the monitoring toggle is enabled and restart the service if necessary
        boolean isMonitoringEnabled = sharedPreferences.getBoolean(KEY_MONITORING_TOGGLE, true);
        if (isMonitoringEnabled) {
            startSpeedMonitorService();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && checkToggleRunnable != null) {
            handler.removeCallbacks(checkToggleRunnable);
        }
        locationManager.removeUpdates(locationListener);
        unregisterReceiver(permissionReceiver);

        // Check the toggle state before stopping the service
        if (!sharedPreferences.getBoolean(KEY_MONITORING_TOGGLE, true)) {
            stopSpeedMonitorService();
        }
    }

    private void checkAllPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), PERMISSIONS_REQUEST_CODE);
        }

        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission();
        }

        if (permissionsToRequest.isEmpty() && Settings.canDrawOverlays(this)) {
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
        Intent intent = new Intent(this, SpeedMonitorService.class);
        boolean stopped = stopService(intent);
        if (stopped) {
            Log.d("MainActivity", "Service stopped successfully");
        } else {
            Log.d("MainActivity", "Failed to stop service");
        }
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
        } else if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                if (monitoringToggleButton.isChecked()) {
                    startSpeedMonitorService();
                }
            } else {
                Toast.makeText(this, "Overlay permission is required for alerts to work properly.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateUI(int minSpeed, int timerLimit) {
        settingsTextView.setText(String.format("Minimum Speed: %d km/h\nTimer Limit: %d minutes", minSpeed, timerLimit));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && "ALERT_DISMISSED".equals(intent.getAction())) {
            onPopUpAlertDismissed();
        }
    }

    private void showPermissionInstructions() {
        new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("This app needs the following permissions to function properly:\n\n" +
                        "- Location access\n" +
                        "- Display over other apps\n" +
                        "- Notifications\n" +
                        "- Run in background\n\n" +
                        "We'll guide you through enabling these permissions.")
                .setPositiveButton("Continue", (dialog, which) -> checkAllPermissions())
                .setCancelable(false)
                .show();
    }

    private void requestOverlayPermission() {
        new AlertDialog.Builder(this)
                .setTitle("Overlay Permission Required")
                .setMessage("This app needs permission to display over other apps. Please enable it in the next screen.")
                .setPositiveButton("OK", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                if (monitoringToggleButton.isChecked()) {
                    startSpeedMonitorService();
                }
            } else {
                Toast.makeText(this, "All permissions are required for the app to function properly.", Toast.LENGTH_LONG).show();
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