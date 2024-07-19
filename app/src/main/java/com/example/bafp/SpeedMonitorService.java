package com.example.bafp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
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
    private static final String PREFS_NAME = "com.example.bafp.PREFS";
    private static final String KEY_MONITORING_TOGGLE = "monitoringToggle";

    private SharedPreferences sharedPreferences;
    private boolean isMonitoringEnabled;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isMonitoringEnabled = sharedPreferences.getBoolean(KEY_MONITORING_TOGGLE, true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("STOP_SERVICE".equals(action)) {
                stopMonitoringSpeed();
                return START_NOT_STICKY;
            } else if ("STOP_ALARM".equals(action)) {
                stopAlarmSound();
                if (checkLocationPermission() && checkNotificationPermission()) {
                    if (isMonitoringEnabled) {
                        startMonitoringSpeed();
                    }
                } else {
                    stopSelf();
                }
                return START_STICKY;
            }

            minSpeed = intent.getDoubleExtra("minSpeed", 30);
            timer = intent.getLongExtra("timer", 180000);

            createNotificationChannel();
            startForeground(NOTIFICATION_ID, createNotification());

            if (checkLocationPermission() && checkNotificationPermission()) {
                if (isMonitoringEnabled) {
                    startMonitoringSpeed();
                }
            } else {
                stopSelf();
            }
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
        if (locationManager == null) {
            Log.e("SpeedMonitorService", "LocationManager is null");
            return;
        }

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location == null) {
                    return;
                }
                double speedKmh = location.getSpeed() * 3.6;
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
            return;
        }
        isAlarmActive = true;

        Log.d("SpeedMonitorService", "Alarm triggered, launching PopUpAlertActivity.");

        Intent broadcastIntent = new Intent(this, AlertReceiver.class);
        sendBroadcast(broadcastIntent);

        showNotification(null);
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
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
        if (handler != null && checkSpeedRunnable != null) {
            handler.removeCallbacks(checkSpeedRunnable);
        }
        stopForeground(true);
        stopSelf();
    }

    private void showNotification(String contentText) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Speed Monitor Service")
                .setContentText(contentText != null ? contentText : "Speed monitoring in progress")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        Intent stopAlarmIntent = new Intent(this, SpeedMonitorService.class);
        stopAlarmIntent.setAction("STOP_ALARM");
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, stopAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(R.drawable.ic_stop, "Stop Alarm", pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Speed Monitor Channel";
            String description = "Channel for Speed Monitor Service notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Speed Monitor Service")
                .setContentText("Speed monitoring in progress")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        return builder.build();
    }

    private void simulateSpeedChange(double speedKmh) {
        long currentTime = System.currentTimeMillis();
        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setSpeed((float) (speedKmh / 3.6));
        location.setTime(currentTime);
        locationListener.onLocationChanged(location);
    }

    @Override
    public void onDestroy() {
        stopMonitoringSpeed();
        stopAlarmSound();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
