package com.community.attendance;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/* Saves a file made by a web page (PDF, Excel) and offers Share / Open. */
final class FileSaver {

    private FileSaver() {}

    private static String guessMime(String name, String mime) {
        if (mime != null && !mime.isEmpty() && !mime.equals("application/octet-stream")) return mime;
        String n = name.toLowerCase();
        if (n.endsWith(".pdf")) return "application/pdf";
        if (n.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (n.endsWith(".csv")) return "text/csv";
        return "application/octet-stream";
    }

    static void save(final Activity act, String b64, String name, String mime) {
        try {
            final byte[] data = Base64.decode(b64, Base64.DEFAULT);
            String safe = (name == null ? "" : name).replaceAll("[^A-Za-z0-9_.-]", "_");
            if (safe.isEmpty()) safe = "file";
            final String type = guessMime(safe, mime);
            final String fileName = safe;

            // a private copy, used for Share and Open
            File dir = new File(act.getCacheDir(), "shared");
            dir.mkdirs();
            File f = new File(dir, safe);
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(data);
            fo.close();

            // a public copy in the Downloads folder (Android 10 and newer)
            String where = "";
            if (Build.VERSION.SDK_INT >= 29) {
                try {
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.Downloads.DISPLAY_NAME, safe);
                    v.put(MediaStore.Downloads.MIME_TYPE, type);
                    v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    Uri u = act.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                    if (u != null) {
                        OutputStream os = act.getContentResolver().openOutputStream(u);
                        if (os != null) {
                            os.write(data);
                            os.close();
                            where = "\nIt is also in your Downloads folder.";
                        }
                    }
                } catch (Exception e) {
                    // the private copy still works for Share and Open
                }
            }

            final Uri uri = FileProvider.getUriForFile(act, act.getPackageName() + ".files", f);
            final String message = fileName + where;
            act.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (act.isFinishing()) return;
                    new AlertDialog.Builder(act)
                            .setTitle("File ready")
                            .setMessage(message)
                            .setPositiveButton("Share", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface d, int w) {
                                    Intent i = new Intent(Intent.ACTION_SEND);
                                    i.setType(type);
                                    i.putExtra(Intent.EXTRA_STREAM, uri);
                                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    act.startActivity(Intent.createChooser(i, "Share"));
                                }
                            })
                            .setNeutralButton("Open", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface d, int w) {
                                    Intent i = new Intent(Intent.ACTION_VIEW);
                                    i.setDataAndType(uri, type);
                                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    try {
                                        act.startActivity(i);
                                    } catch (ActivityNotFoundException e) {
                                        Toast.makeText(act, "No app on this phone can open this file.",
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            })
                            .setNegativeButton("Close", null)
                            .show();
                }
            });
        } catch (Exception e) {
            act.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(act, "Could not save the file.", Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
