package com.example.media_converter_app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationClass {
    public static void pushNotification(Context context, String type, String filename) {
        if (context == null) return;

        String CHANNEL_ID = "conversion_channel";

        String title = "";
        String message = "";
        String description = "";

        if (type.equals("conversion")) {
            title = "Conversion Completed";
            message = "Converted: " + filename + " !";
            description = "Notification when conversion is completed";
        } else if (type.equals("download")) {
            title = "Download Completed";
            message = "Downloaded: " + filename + " !";
            description = "Notification when download is completed";
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Media Converter Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            channel.setDescription(description);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 100, 500});

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVibrate(new long[]{0, 500, 100, 500})
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(1001, builder.build());
    }
}
