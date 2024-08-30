package com.example.bafp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.bafp.AlertReceiver;
import com.example.bafp.R;

public class AlarmUtils {

    private static final String TAG = "AlarmUtils";
    private static final String CHANNEL_ID = "ChildSafetyChannel";
    private static final long ALARM_TIMEOUT_MS = 45000; // 45 seconds

    private static MediaPlayer mediaPlayer;
    private static boolean isAlarmActive = false;

    public static void triggerAlarm(Context context) {
        if (isAlarmActive) {
            return;
        }
        isAlarmActive = true;

        Log.d(TAG, "Alarm triggered at " + System.currentTimeMillis());

        Intent broadcastIntent = new Intent(context, AlertReceiver.class);
        context.sendBroadcast(broadcastIntent);

        showNotification(context, "Speed below threshold for too long!");
        playAlarmSound(context);

        // Start the timeout runnable
        startAlarmTimeout(context);
    }

    private static void playAlarmSound(Context context) {
        mediaPlayer = MediaPlayer.create(context, R.raw.alert_sound);
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
            Log.d(TAG, "Alarm sound started");
        }
    }

    private static void stopAlarmSound() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            Log.d(TAG, "Alarm sound stopped");
        }
    }

    private static void showNotification(Context context, String contentText) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Speed Monitor Service")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(1, builder.build());
        }
        Log.d(TAG, "Notification shown: " + contentText);
    }

    private static void startAlarmTimeout(Context context) {
        // Implement timeout logic here
        // You can use a Handler or any other mechanism to execute after ALARM_TIMEOUT_MS
    }

    public static void resetAlarmState(Context context) {
        isAlarmActive = false;
        stopAlarmSound();
        showNotification(context, "Speed monitoring in progress");
        Log.d(TAG, "Alarm state reset at " + System.currentTimeMillis());

        // Stop the timeout runnable
        // Implement logic to stop the timeout runnable
    }
}
