package com.community.attendance;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context c, Intent i) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    "reminders", "Attendance reminders", NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(ch);
            b = new Notification.Builder(c, "reminders");
        } else {
            b = new Notification.Builder(c);
        }

        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent tap = PendingIntent.getActivity(c, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        b.setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(i.getStringExtra("title"))
                .setContentText(i.getStringExtra("text"))
                .setContentIntent(tap)
                .setAutoCancel(true);

        nm.notify((int) (System.currentTimeMillis() % 100000), b.build());
    }
}
