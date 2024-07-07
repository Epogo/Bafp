package com.example.bafp;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

public class SpeedMonitorService extends Service {

    private static final String CHANNEL_ID = "ChildSafetyChannel";
    private LocationManager locationManager;
    private boolean notificationsEnabled = false;
    private double minSpeed;
    private long timer;
    private Vibrator vibrator;
    private boolean isAlarmActive = false;
    private LocationListener locationListener;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission not granted", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        notificationsEnabled = true;
        return true;
    }

    private void startMonitoringSpeed() {
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location.getSpeed() * 3.6 > minSpeed) {
                    // Speed exceeds minSpeed, stop monitoring
                    stopMonitoringSpeed();
                } else {
                    // Start a timer to check when speed drops below minSpeed
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
            Log.e("SpeedMonitorService", "Location permission not granted");
        }
    }

    private void checkForStoppedCar() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (lastKnownLocation != null && lastKnownLocation.getSpeed() * 3.6 < minSpeed) {
                        triggerAlarm();
                    } else {
                        // Continue checking if speed drops below minSpeed
                        checkForStoppedCar();
                    }
                } catch (SecurityException e) {
                    Log.e("SpeedMonitorService", "Location permission not granted");
                }
            }
        }, timer); // Use the provided timer value
    }

    private void triggerAlarm() {
        if (isAlarmActive) {
            return; // Only trigger the alarm once
        }
        isAlarmActive = true;

        // Launch PopUpAlertActivity
        Intent popupIntent = new Intent(this, PopUpAlertActivity.class);
        popupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(popupIntent);

        // Vibrate indefinitely until the user interacts with the popup
        startVibrations();

        // Show notification if notifications are enabled
        showNotification();
    }

    private void startVibrations() {
        // Vibration pattern (0ms delay, 1s vibration)
        long[] pattern = {0, 1000};

        // Check API level for compatibility
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Use vibration effect for API level 26+
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
        } else {
            // Fallback for older API levels
            vibrator.vibrate(pattern, 0);
        }
    }

    private void stopMonitoringSpeed() {
        locationManager.removeUpdates(locationListener);
        stopVibrations();
        stopSelf();
    }

    private void stopVibrations() {
        vibrator.cancel();
    }

    private void showNotification() {
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
                .setContentTitle("Child Left in Car")
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
                notificationManager.notify(1, notificationBuilder.build());
                startVibrations();
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



