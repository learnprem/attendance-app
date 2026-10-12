package com.community.attendance;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.view.Gravity;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    static final String PREFS = "attendance";
    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        String home = prefs().getString("home", "");
        if (home.isEmpty()) {
            showSetup();
        } else {
            showWeb(home);
        }
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    /* First screen: the member pastes the personal link once */
    private void showSetup() {
        float d = getResources().getDisplayMetrics().density;
        int pad = (int) (24 * d);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, pad, pad, pad);
        box.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("Paste your personal attendance link");
        title.setTextSize(20);

        final EditText input = new EditText(this);
        input.setHint("https://script.google.com/...");
        input.setMinLines(3);

        final TextView err = new TextView(this);
        err.setTextColor(0xFFC62828);

        Button go = new Button(this);
        go.setText("Open");
        go.setOnClickListener(v -> {
            String s = input.getText().toString().trim();
            if (!s.startsWith("https://script.google.com/")) {
                err.setText("This does not look like your personal link. "
                        + "It should start with https://script.google.com/");
                return;
            }
            prefs().edit().putString("home", s).apply();
            showWeb(s);
        });

        box.addView(title);
        box.addView(input);
        box.addView(err);
        box.addView(go);
        setContentView(box);
    }

    private boolean isGoogle(Uri u) {
        String h = u.getHost();
        if (h == null) return false;
        return h.endsWith("google.com") || h.endsWith("googleusercontent.com");
    }

    private void openExternal(Uri u) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, u));
        } catch (Exception e) {
            // no app can open this link
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void showWeb(String url) {
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setSupportMultipleWindows(true);

        web.addJavascriptInterface(new Bridge(), "Android");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                Uri u = request.getUrl();
                if (isGoogle(u)) return false;
                openExternal(u);
                return true;
            }
        });

        // links that open in a new window (Join Zoom, WhatsApp) go to the right app
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog,
                                          boolean isUserGesture, Message resultMsg) {
                WebView temp = new WebView(MainActivity.this);
                temp.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                        openExternal(r.getUrl());
                        return true;
                    }

                    @Override
                    public void onPageStarted(WebView v, String u, Bitmap icon) {
                        if (u != null && !u.equals("about:blank")) {
                            openExternal(Uri.parse(u));
                            v.stopLoading();
                        }
                    }
                });
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(temp);
                resultMsg.sendToTarget();
                return true;
            }
        });

        web.loadUrl(url);
    }

    /* The web page calls these through window.Android */
    private class Bridge {
        @JavascriptInterface
        public int setReminders(String json) {
            return Reminders.schedule(getApplicationContext(), json);
        }

        @JavascriptInterface
        public void testReminder() {
            Reminders.test(getApplicationContext());
        }
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
