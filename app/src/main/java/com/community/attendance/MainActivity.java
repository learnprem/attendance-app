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
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import java.util.Collections;

public class MainActivity extends Activity {

    static final String PREFS = "attendance";
    private WebView web;
    private LinearLayout offline;
    private String homeUrl = "";

    /* Runs inside every page (and frame): turns a "download" of a PDF or Excel file into a call to the app */
    private static final String SHIM =
            "(function(){"
            + "if(window.__appShim)return;window.__appShim=true;"
            + "function send(b,n,m){try{Android.saveFile(b,n,m);}catch(e){}}"
            + "function fromBlob(bl,n){var r=new FileReader();"
            + "r.onloadend=function(){send(String(r.result).split(',')[1]||'',n,bl.type||'application/octet-stream');};"
            + "r.readAsDataURL(bl);}"
            + "var orig=HTMLAnchorElement.prototype.click;"
            + "HTMLAnchorElement.prototype.click=function(){"
            + "var a=this,h=a.href||'';"
            + "if(a.hasAttribute('download')&&window.Android&&window.Android.saveFile){"
            + "var n=a.getAttribute('download')||'file';"
            + "if(h.indexOf('blob:')===0){fetch(h).then(function(r){return r.blob();})"
            + ".then(function(b){fromBlob(b,n);});return;}"
            + "var m=/^data:([^;,]*)(;base64)?,(.*)$/.exec(h);"
            + "if(m){if(m[2]){send(m[3],n,m[1]||'application/octet-stream');}"
            + "else{send(btoa(decodeURIComponent(m[3])),n,m[1]||'text/plain');}return;}"
            + "}"
            + "return orig.apply(this,arguments);};"
            + "})();";

    /* Hides the grey "created by a Google Apps Script user" bar, without touching the page itself */
    private static final String HIDE_BANNER =
            "(function(){try{"
            + "if(window.__bannerDone)return;"
            + "window.__bannerTry=(window.__bannerTry||0)+1;"
            + "var all=document.querySelectorAll('div,span,p,td,a'),hid=0;"
            + "for(var i=0;i<all.length;i++){var e=all[i];var t=e.textContent||'';"
            + "if(t.length<300&&/this application was created by|not by google/i.test(t)){"
            + "var x=e;"
            + "while(x.parentElement&&x.parentElement!==document.body&&!x.parentElement.querySelector('iframe')"
            + "&&(x.parentElement.textContent||'').length<300){x=x.parentElement;}"
            + "if(!x.querySelector('iframe')){x.style.display='none';hid++;}}}"
            + "if(hid){window.__bannerDone=true;return;}"
            + "if(window.__bannerTry>=3){var f=document.querySelector('iframe');var n=f;"
            + "while(n&&n!==document.body){var q=n.previousElementSibling;"
            + "while(q){if(q.tagName!=='SCRIPT'&&q.tagName!=='STYLE'&&!q.querySelector('iframe')&&(q.textContent||'').length<300){q.style.display='none';}"
            + "q=q.previousElementSibling;}n=n.parentElement;}window.__bannerDone=true;}"
            + "}catch(e){}})();";

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

    /* The "no internet" screen with a Retry button, shown on top of the page */
    private LinearLayout buildOffline() {
        float d = getResources().getDisplayMetrics().density;
        int pad = (int) (32 * d);
        final LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(pad, pad, pad, pad);
        box.setBackgroundColor(0xFFFFFFFF);
        box.setClickable(true);

        TextView t = new TextView(this);
        t.setText("No internet connection.\nPlease check your connection and try again.");
        t.setTextSize(18);
        t.setGravity(Gravity.CENTER);

        Button retry = new Button(this);
        retry.setText("Retry");
        retry.setOnClickListener(v -> {
            box.setVisibility(View.GONE);
            if (web != null) web.loadUrl(homeUrl);
        });

        box.addView(t);
        box.addView(retry);
        box.setVisibility(View.GONE);
        return box;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void showWeb(String url) {
        homeUrl = url;
        web = new WebView(this);
        offline = buildOffline();

        FrameLayout root = new FrameLayout(this);
        root.addView(web, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        root.addView(offline, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setSupportMultipleWindows(true);

        web.addJavascriptInterface(new Bridge(), "Android");

        // lets PDF and Excel downloads from the pages reach the app
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(web, SHIM, Collections.singleton("*"));
        }

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                Uri u = request.getUrl();
                if (isGoogle(u)) return false;
                openExternal(u);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String u, Bitmap icon) {
                offline.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) offline.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String u) {
                // the bar is added a moment after the page loads, so try a few times
                final WebView w = view;
                int[] waits = {300, 1200, 3000};
                for (int ms : waits) {
                    w.postDelayed(() -> w.evaluateJavascript(HIDE_BANNER, null), ms);
                }
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
        Updater.check(this, url);
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

        @JavascriptInterface
        public void saveFile(String base64, String name, String mime) {
            FileSaver.save(MainActivity.this, base64, name, mime);
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
