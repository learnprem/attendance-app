package com.community.attendance;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/* Asks the web app which version is the newest, and offers the update. Never blocks the app if it cannot reach the server. */
final class Updater {

    private Updater() {}

    static long versionCode(Context c) {
        try {
            PackageInfo p = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return Build.VERSION.SDK_INT >= 28 ? p.getLongVersionCode() : p.versionCode;
        } catch (Exception e) {
            return 0;
        }
    }

    static void check(final Activity act, final String homeUrl) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String base = homeUrl.split("\\?")[0];
                    HttpURLConnection c = (HttpURLConnection) new URL(base + "?app=version").openConnection();
                    c.setConnectTimeout(8000);
                    c.setReadTimeout(8000);
                    BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                    r.close();
                    JSONObject j = new JSONObject(sb.toString());
                    final long latest = j.optLong("latest", 0);
                    final long min = j.optLong("min", 0);
                    final String notes = j.optString("notes", "");
                    String u = j.optString("url", "");
                    final String url = u.isEmpty() ? BuildConfig.UPDATE_URL : u;
                    final long cur = versionCode(act);
                    if (min > cur) {
                        act.runOnUiThread(new Runnable() {
                            @Override
                            public void run() { show(act, url, notes, latest, true); }
                        });
                    } else if (latest > cur && !snoozed(act, latest)) {
                        act.runOnUiThread(new Runnable() {
                            @Override
                            public void run() { show(act, url, notes, latest, false); }
                        });
                    }
                } catch (Exception e) {
                    // no internet or no version info: carry on without a message
                }
            }
        }).start();
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE);
    }

    private static boolean snoozed(Context c, long latest) {
        long until = prefs(c).getLong("snooze_" + latest, 0);
        return System.currentTimeMillis() < until;
    }

    private static void openUrl(Activity act, String url) {
        try {
            act.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            // no browser found
        }
    }

    private static void show(final Activity act, final String url, String notes,
                             final long latest, final boolean mandatory) {
        if (act.isFinishing()) return;
        String msg = mandatory
                ? "This version of the app is too old. Please update to continue."
                : "A new version of the app is ready.";
        if (notes != null && !notes.isEmpty()) msg = msg + "\n\n" + notes;
        msg = msg + "\n\nTap Update, then open the downloaded file to install it.";

        AlertDialog.Builder b = new AlertDialog.Builder(act);
        b.setTitle(mandatory ? "Please update the app" : "New version available");
        b.setMessage(msg);
        b.setCancelable(!mandatory);
        b.setPositiveButton("Update", null);
        if (!mandatory) {
            b.setNegativeButton("Later", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface d, int w) {
                    prefs(act).edit().putLong("snooze_" + latest,
                            System.currentTimeMillis() + 24L * 3600L * 1000L).apply();
                }
            });
        }
        final AlertDialog dlg = b.create();
        dlg.show();
        // the Update button opens the download and keeps the message on screen
        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                openUrl(act, url);
                if (!mandatory) dlg.dismiss();
            }
        });
    }
}
