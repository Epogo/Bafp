package com.example.bafp;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
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
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
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
    private boolean isAlarmActive = false;
    private LocationListener locationListener;
    private MediaPlayer mediaPlayer;
    private Handler handler;
    private Runnable checkSpeedRunnable;

    private boolean hasMovedAboveThreshold = false;
    private long lastBelowThresholdTime = 0;
    private long totalTimeBelowThreshold = 0;

    private static final int NOTIFICATION_ID = 1;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP_ALARM".equals(intent.getAction())) {
            stopAlarmSound();
            isAlarmActive = false;
            resetMonitoring();
            return START_NOT_STICKY;
        }

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        minSpeed = intent.getDoubleExtra("minSpeed", 30); // Default to 30 km/h if not provided
        timer = intent.getLongExtra("timer", 180000); // Default to 3 minutes (180000 ms) if not provided

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission not granted", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        notificationsEnabled = true;
        return true;
    }

    private void startMonitoringSpeed() {
        handler = new Handler(Looper.getMainLooper());

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double speedKmh = location.getSpeed() * 3.6; // Convert m/s to km/h
                long currentTime = System.currentTimeMillis();

                if (speedKmh > minSpeed) {
                    hasMovedAboveThreshold = true;
                    totalTimeBelowThreshold = 0;
                    lastBelowThresholdTime = 0;
                } else if (hasMovedAboveThreshold) {
                    if (lastBelowThresholdTime == 0) {
                        lastBelowThresholdTime = currentTime;
                    } else {
                        totalTimeBelowThreshold = currentTime - lastBelowThresholdTime;
                    }
                }

                // Check if we should trigger the alarm
                if (hasMovedAboveThreshold && !isAlarmActive && totalTimeBelowThreshold >= timer) {
                    triggerAlarm();
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

    private void triggerAlarm() {
        if (isAlarmActive) {
            return; // Only trigger the alarm once
        }
        isAlarmActive = true;

        // Launch PopUpAlertActivity
        Intent popupIntent = new Intent(this, PopUpAlertActivity.class);
        popupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(popupIntent);

        // Play MP3 file
        playAlarmSound();

        // Show notification if notifications are enabled
        showNotification();
    }

    private void playAlarmSound() {
        mediaPlayer = MediaPlayer.create(this, R.raw.alert_sound);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
    }

    private void stopAlarmSound() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void stopMonitoringSpeed() {
        if (locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
        if (handler != null && checkSpeedRunnable != null) {
            handler.removeCallbacks(checkSpeedRunnable);
        }
        stopAlarmSound();
        stopSelf();
    }

    private void resetMonitoring() {
        hasMovedAboveThreshold = false;
        lastBelowThresholdTime = 0;
        totalTimeBelowThreshold = 0;
        isAlarmActive = false;
        if (handler != null && checkSpeedRunnable != null) {
            handler.removeCallbacks(checkSpeedRunnable);
            handler.postDelayed(checkSpeedRunnable, 1000);
        }
    }

    private void showNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Child Safety Notifications";
            String description = "Notifications for child safety reminders";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            notificationManager.createNotificationChannel(channel);
        }

        Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.alert_sound);

        Intent stopIntent = new Intent(this, SpeedMonitorService.class);
        stopIntent.setAction("STOP_ALARM");
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Child Left in Car")
                .setContentText("Please check if any child is left in the car.")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSound(soundUri)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_stop, "Stop Alarm", stopPendingIntent);

        if (notificationsEnabled) {
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        }
    }

    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Speed Monitor Service")
                .setContentText("Monitoring vehicle speed...")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Child Safety Notifications";
            String description = "Notifications for child safety reminders";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());
        PendingIntent restartServicePendingIntent = PendingIntent.getService(
                getApplicationContext(), 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, restartServicePendingIntent);
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAlarmSound();
        stopMonitoringSpeed();
    }
}