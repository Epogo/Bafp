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
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP_ALARM".equals(intent.getAction())) {
            stopAlarmSound();
            isAlarmActive = false;
            resetMonitoring();
            return START_NOT_STICKY;
        }

        minSpeed = intent.getDoubleExtra("minSpeed", 30); // Default to 30 km/h if not provided
        timer = intent.getLongExtra("timer", 180000); // Default to 3 minutes (180000 ms) if not provided

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

        if (checkLocationPermission() && checkNotificationPermission()) {
            startMonitoringSpeed();

            // For testing purposes, simulate speed changes
            handler.postDelayed(() -> simulateSpeed(minSpeed + 10, 10000), 5000); // Simulate speed above threshold for 10 seconds
            handler.postDelayed(() -> simulateSpeed(minSpeed - 5, timer + 1000), 11000); // Simulate speed below threshold
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

                if (hasMovedAboveThreshold && !isAlarmActive && totalTimeBelowThreshold >= 1000) {
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
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
            }
        } catch (SecurityException e) {
            Log.e("SpeedMonitorService", "Location permission not granted");
        }
    }
    private void triggerAlarm() {
        if (isAlarmActive) {
            return; // Only trigger the alarm once
        }
        isAlarmActive = true;

        Log.d("SpeedMonitorService", "Alarm triggered, launching PopUpAlertActivity.");

        Intent broadcastIntent = new Intent(this, AlertReceiver.class);
        sendBroadcast(broadcastIntent);

        showNotification(null); // No PendingIntent required
        playAlarmSound();
    }

    private void playAlarmSound() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.alert_sound);
            mediaPlayer.setLooping(true);
        }
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

    private void showNotification(PendingIntent contentIntent) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Child Safety Alerts", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Alerts for child safety");
            channel.enableLights(true);
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Child Safety Alert")
                .setContentText("Vehicle has stopped. Check on your child!")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(contentIntent, true)
                .setAutoCancel(true);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
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

    private void simulateSpeed(final double speedKmh, long durationMillis) {
        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }

        final Location mockLocation = new Location(LocationManager.GPS_PROVIDER);
        mockLocation.setLatitude(0.0); // dummy latitude
        mockLocation.setLongitude(0.0); // dummy longitude
        mockLocation.setAccuracy(1);
        mockLocation.setTime(System.currentTimeMillis());
        mockLocation.setSpeed((float) (speedKmh / 3.6)); // speed in m/s

        handler.postDelayed(() -> {
            mockLocation.setSpeed(0.0f); // set speed to 0
            if (locationListener != null) {
                locationListener.onLocationChanged(mockLocation);
            }
        }, durationMillis);

        long interval = 1000; // 1 second interval
        for (long i = 0; i < durationMillis; i += interval) {
            handler.postDelayed(() -> {
                if (locationListener != null) {
                    locationListener.onLocationChanged(mockLocation);
                }
            }, i);
        }
    }
}
