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
    private LocationManager locationManager;
    private boolean notificationsEnabled = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        startMonitoringSpeed();
        return START_STICKY;
    }

    private void startMonitoringSpeed() {
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double speed = location.getSpeed() * 3.6; // Convert m/s to km/h
                if (speed > 30 && notificationsEnabled) {
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

        // Check for ACCESS_FINE_LOCATION permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
        } else {
            // Handle case where permission is not granted
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
            stopSelf();
            return;
        }

        // Check for POST_NOTIFICATIONS permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationsEnabled = true;
        } else {
            // Request POST_NOTIFICATIONS permission if not granted
            requestPostNotificationsPermission();
        }
    }

    private void requestPostNotificationsPermission() {
        // Implement proper permission request flow here
        // This is a simplified example using Toast for demonstration
        Toast.makeText(this, "Requesting POST_NOTIFICATIONS permission", Toast.LENGTH_SHORT).show();
        // You should implement a proper permission request flow (e.g., using ActivityCompat.requestPermissions())
        // Example:
        // ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
    }

    private void checkForStoppedCar() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // Add logic to get the current speed
                float speed = 0; // Placeholder for actual speed fetching logic
                if (speed < 5) { // Assuming 5 km/h as stationary
                    triggerAlarm();
                }
            }
        }, 300000); // 5 minutes
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

        // Build the notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Check the Car")
                .setContentText("Please check if any child is left in the car.")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVibrate(new long[]{0, 1000, 500, 1000}) // Vibration pattern
                .setAutoCancel(true); // Auto-cancel the notification when clicked

        // Notify if notifications are enabled
        if (notificationsEnabled) {
            notificationManager.notify(1, notificationBuilder.build());
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
