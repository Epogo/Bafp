package com.example.bafp;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class SpeedMonitorService extends Service {
    private static final String CHANNEL_ID = "CHILD_SAFETY_CHANNEL";
    public static final String ACTION_REQUEST_PERMISSION = "com.example.bafp.REQUEST_PERMISSION";
    private LocationManager locationManager;
    private boolean notificationsEnabled = false;
    private double minSpeed;
    private long timer;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Extract minSpeed and timer from the intent
        minSpeed = intent.getDoubleExtra("minSpeed", 30); // Default to 30 km/h if not provided
        timer = intent.getLongExtra("timer", 300000); // Default to 5 minutes (300000 ms) if not provided

        if (checkLocationPermission() && checkNotificationPermission()) {
            startMonitoringSpeed();
        } else {
            stopSelf();
        }
        return START_STICKY;
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Request POST_NOTIFICATIONS permission if not granted
                Intent requestPermissionIntent = new Intent(ACTION_REQUEST_PERMISSION);
                sendBroadcast(requestPermissionIntent);
                return false;
            }
        }
        notificationsEnabled = true;
        return true;
    }

    private void startMonitoringSpeed() {
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double speed = location.getSpeed() * 3.6; // Convert m/s to km/h
                if (speed > minSpeed && notificationsEnabled) {
                    // Start a timer or mechanism to check if speed drops below a threshold
                    checkForStoppedCar();
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
        } catch (SecurityException e) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkForStoppedCar() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // Add logic to get the current speed
                float speed = 0; // Placeholder for actual speed fetching logic
                if (speed < 6) { // Assuming 5 km/h as stationary
                    triggerAlarm();
                }
            }
        }, timer); // Use the provided timer value
    }

    private void triggerAlarm() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            CharSequence name = "Child Safety Notifications";
            String description = "Notifications for child safety reminders";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true); // Ensure vibration is enabled
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000}); // Vibration pattern
            notificationManager.createNotificationChannel(channel);
        }

        Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.alert_sound);

        // Build the notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Check the Car")
                .setContentText("Please check if any child is left in the car.")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVibrate(new long[]{0, 1000, 500, 1000, 500, 10000}) // Vibration pattern
                .setSound(soundUri) // Set the custom sound
                .setAutoCancel(true); // Auto-cancel the notification when clicked

        // Notify if notifications are enabled
        try {
            if (notificationsEnabled) {
                NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
                notificationManagerCompat.notify(1, notificationBuilder.build());
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Notification permission not granted", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
