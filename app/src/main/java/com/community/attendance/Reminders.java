package com.community.attendance;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

public class Reminders {

    private static PendingIntent pending(Context c, int code, String title, String text, int flags) {
        Intent i = new Intent(c, AlarmReceiver.class);
        i.putExtra("title", title);
        i.putExtra("text", text);
        return PendingIntent.getBroadcast(c, code, i, flags | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void cancelAll(Context c, SharedPreferences sp) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        int old = sp.getInt("count", 0);
        for (int k = 0; k < old; k++) {
            PendingIntent p = pending(c, k, "", "", PendingIntent.FLAG_NO_CREATE);
            if (p != null) {
                am.cancel(p);
                p.cancel();
            }
        }
    }

    /* Replaces all earlier reminders with the list sent by the web page */
    public static int schedule(Context c, String json) {
        SharedPreferences sp = c.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE);
        cancelAll(c, sp);
        int n = 0;
        try {
            JSONArray arr = new JSONArray(json);
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            long now = System.currentTimeMillis();
            for (int i = 0; i < arr.length() && n < 60; i++) {
                JSONObject o = arr.getJSONObject(i);
                long at = o.getLong("at");
                if (at <= now) continue;
                PendingIntent pi = pending(c, n,
                        o.optString("title", "Attendance"),
                        o.optString("text", "Attendance window is open."),
                        PendingIntent.FLAG_UPDATE_CURRENT);
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                n++;
            }
        } catch (Exception e) {
            // keep what was scheduled so far
        }
        sp.edit().putString("reminders", json).putInt("count", n).apply();
        return n;
    }

    /* A test reminder that rings in about one minute */
    public static void test(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = pending(c, 9999, "Test reminder",
                "This is how your attendance reminders will look.",
                PendingIntent.FLAG_UPDATE_CURRENT);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 60000, pi);
    }
}
