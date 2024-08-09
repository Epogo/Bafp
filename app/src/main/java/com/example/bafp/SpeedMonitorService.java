package com.example.bafp;

import static java.lang.Boolean.FALSE;

import android.Manifest;
import android.app.ActivityManager;
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
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

public class SpeedMonitorService extends Service {
    private static final String TAG = "SpeedMonitorService";
    private static final String CHANNEL_ID = "ChildSafetyChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final String PREFS_NAME = "com.example.bafp.PREFS";
    private static final String KEY_MONITORING_TOGGLE = "monitoringToggle";

    private LocationManager locationManager;
    private boolean notificationsEnabled = false;
    private double minSpeed;
    private long timer;
    private boolean isAlarmActive = false;
    private LocationListener locationListener;
    private MediaPlayer mediaPlayer;
    private Handler handler;
    private Runnable checkSpeedRunnable;

    private long continuousTimeBelowThreshold = 0;
    private long lastProcessedTime = 0;

    private SharedPreferences sharedPreferences;
    private boolean isMonitoringEnabled;

    private PowerManager.WakeLock wakeLock;

    public static boolean isRunning = false;

    public boolean isSpecificRunning = true;
    private boolean isSimulationMode = false; // Add this flag

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
        // Ensure the intent is not null before accessing its data
        if (intent == null) {
            Log.w(TAG, "Received null intent, stopping service.");
            stopSelf();
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        isRunning = true;
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isMonitoringEnabled = sharedPreferences.getBoolean(KEY_MONITORING_TOGGLE, true);

        // Handle the "STOP_SERVICE" action
        if ("STOP_SERVICE".equals(action)) {
            stopMonitoringSpeed();
            stopSelf(); // Explicitly stop the service
            return START_NOT_STICKY;
        }

        // Handle the "STOP_ALARM" action
        else if ("STOP_ALARM".equals(action)) {
            resetAlarmState();
            if (checkLocationPermission() && checkNotificationPermission()) {
                if (isMonitoringEnabled) {
                    startMonitoringSpeed();
                } else {
                    stopSelf();
                }
            } else {
                stopSelf();
            }
            return START_STICKY;
        }

        // Read configuration parameters from the intent
        minSpeed = intent.getIntExtra("minSpeed", 20); // Default to 20 km/h
        timer = intent.getLongExtra("timer", 180000); // 3 minutes in milliseconds
        isSimulationMode = intent.getBooleanExtra("isSimulationMode", false); // Read the simulation mode flag
        continuousTimeBelowThreshold = 0;
        lastProcessedTime = 0;

        isSpecificRunning = true;

        // Initialize the Handler if it's null
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }

        // Initialize the LocationManager if it's null
        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }

        // Initialize the MediaPlayer if it's null
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.alert_sound);
        }

        // Initialize and acquire the WakeLock
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SpeedMonitorService::WakeLock");
        }
        try {
            wakeLock.acquire();
        } catch (SecurityException e) {
            Log.e(TAG, "WakeLock permission not granted", e);
            stopSelf(); // Stop the service if WakeLock acquisition fails
            return START_NOT_STICKY;
        }

        Log.d(TAG, "Service started with minSpeed: " + minSpeed + " km/h, timer: " + timer + " ms, simulationMode: " + isSimulationMode);

        // Create a notification channel if it hasn't been created already
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

        // Check permissions before starting monitoring
        if (checkLocationPermission() && checkNotificationPermission()) {
            if (isMonitoringEnabled) {
                startMonitoringSpeed();
            } else {
                stopSelf(); // Stop the service if monitoring is not enabled
            }
        } else {
            stopSelf(); // Stop the service if permissions are not granted
        }

        return START_STICKY;
    }

    private boolean checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Location permission not granted");
            return false;
        }
        return true;
    }

    private boolean checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission not granted", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Notification permission not granted");
                return false;
            }
        }
        notificationsEnabled = true;
        return true;
    }

    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Speed Monitor Service")
                .setContentText("Speed monitoring in progress")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);

        return builder.build();
    }

    private void startMonitoringSpeed() {
        isSpecificRunning = true;
        if (isSimulationMode) {
            simulateTravel();
        }
        else
        {
            if (locationManager != null && locationListener != null) {
                locationManager.removeUpdates(locationListener);
            }

            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    handleRealLocation(location);
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
                Log.e(TAG, "Location permission not granted", e);
            }
        }
    }

    private void handleRealLocation(Location location) {
        isMonitoringEnabled = sharedPreferences.getBoolean(KEY_MONITORING_TOGGLE, true);
        if(!isMonitoringEnabled)
        {
            stopSelf();
            return;
        }

        if (!isSpecificRunning || location == null) {
            return;
        }
        double speedKmh = location.getSpeed() * 3.6;
        processSpeed(speedKmh);
    }

    private void processSpeed(double speedKmh) {
        long currentTime = System.currentTimeMillis();

        if (lastProcessedTime == 0) {
            lastProcessedTime = currentTime;
        }

        long timeDelta = currentTime - lastProcessedTime;
        lastProcessedTime = currentTime;

        Log.d(TAG, "Processing speed: " + speedKmh + " km/h at time: " + currentTime);

        if (speedKmh <= minSpeed) {
            continuousTimeBelowThreshold += timeDelta;
            Log.d(TAG, "Speed below threshold. Continuous time below: " + continuousTimeBelowThreshold + " ms");
        } else {
            continuousTimeBelowThreshold = 0;
            Log.d(TAG, "Speed above threshold. Reset continuous time below threshold.");
        }

        if (continuousTimeBelowThreshold >= 3000) { //TODO: replace with timer.
            triggerAlarm();
        }

        Log.d(TAG, "Current speed: " + speedKmh + " km/h, Continuous time below threshold: " + continuousTimeBelowThreshold + " ms");
    }

    private void triggerAlarm() {
        if (isAlarmActive) {
            return;
        }
        isAlarmActive = true;

        Log.d(TAG, "Alarm triggered at " + System.currentTimeMillis());

        Intent broadcastIntent = new Intent(this, AlertReceiver.class);
        sendBroadcast(broadcastIntent);

        showNotification("Speed below threshold for too long!");
        playAlarmSound();
    }

    private void playAlarmSound() {
        mediaPlayer = MediaPlayer.create(this, R.raw.alert_sound);
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
            Log.d(TAG, "Alarm sound started");
        }

    }

    private void stopAlarmSound() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            Log.d(TAG, "Alarm sound stopped");
        }
    }

    // Update resetAlarmState to log when alarm is reset
    private void resetAlarmState() {
        isAlarmActive = false;
        continuousTimeBelowThreshold = 0;
        lastProcessedTime = 0;
        stopAlarmSound();
        showNotification("Speed monitoring in progress");
        Log.d(TAG, "Alarm state reset at " + System.currentTimeMillis());
    }
    private void stopMonitoringSpeed() {
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
            Log.d(TAG, "Location updates removed");
        }
        if (handler != null && checkSpeedRunnable != null) {
            handler.removeCallbacks(checkSpeedRunnable);
        }
        stopForeground(true);
        stopSelf();
        isSpecificRunning = false;
    }

    private void showNotification(String contentText) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Speed Monitor Service")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
        Log.d(TAG, "Notification shown: " + contentText);
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

    private void simulateTravel() {
        // Speed in km/h
        double[] speeds = {40, 3, 25, 2};
        // Duration in milliseconds
        long[] durations = {3000, 3000, 3000, 180000}; // 0.5 min, 0.5 min, 0.5 min, 3 min

        handler.post(new Runnable() {
            private int index = 0;
            private long elapsedTime = 0;
            private boolean isSimulationComplete = false;

            @Override
            public void run() {
                if (index < speeds.length) {
                    Log.d(TAG, "Simulating travel at " + speeds[index] + " km/h for " + durations[index] + " ms");
                    Location location = new Location(LocationManager.GPS_PROVIDER);
                    location.setSpeed((float) (speeds[index] / 3.6)); // Convert km/h to m/s
                    handleRealLocation(location);

                    elapsedTime += 1000; // Simulate 1 second passing
                    if (elapsedTime >= durations[index]) {
                        index++;
                        elapsedTime = 0;
                    }

                    handler.postDelayed(this, 1000); // Update every second
                } else if (!isSimulationComplete) {
                    Log.d(TAG, "Simulation completed. Waiting for user to press OK...");
                    isSimulationComplete = true;
                    // Wait for user to press OK before restarting the simulation
                    handler.postDelayed(this, 1000);
                } else if (!isAlarmActive) {
                    Log.d(TAG, "Restarting simulation");
                    index = 0;
                    elapsedTime = 0;
                    isSimulationComplete = false;
                    handler.post(this);
                } else {
                    // If alarm is still active, wait and check again
                    handler.postDelayed(this, 1000);
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "SpeedMonitorService onDestroy called");

        // Release WakeLock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null; // Nullify the reference
            Log.d(TAG, "WakeLock released");
        }

        // Stop location updates
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
            locationListener = null; // Nullify the reference
            Log.d(TAG, "Location updates removed");
        }

        // Remove Handlers and Runnables
        if (handler != null && checkSpeedRunnable != null) {
            handler.removeCallbacks(checkSpeedRunnable);
            checkSpeedRunnable = null; // Nullify the reference
            Log.d(TAG, "Handler callbacks removed");
        }

        // Stop the foreground service
        stopForeground(true);
        Log.d(TAG, "Foreground service stopped");

        // Stop the service
        stopSelf();
        isSpecificRunning = false;
        isRunning = false;
        Log.d(TAG, "Service stopped");
        // Directly stop the service
        stopMonitoringSpeed();

        // Reset alarm state
        isAlarmActive = false;
        continuousTimeBelowThreshold = 0;
        lastProcessedTime = 0;
        stopAlarmSound();
        Log.d(TAG, "Alarm state reset");

        // Ensure media player is released
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null; // Nullify the reference
            Log.d(TAG, "Media player released");
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
