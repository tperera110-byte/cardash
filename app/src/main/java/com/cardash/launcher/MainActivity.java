package com.cardash.launcher;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.webkit.WebViewAssetLoader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    private static final String URL = "https://appassets.androidplatform.net/assets/index.html";
    private WebView web;
    private MediaPlayer player;
    private String currentId = "";
    private boolean ended = false;
    private float volume = 0.8f;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideBars();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();

        web = new WebView(this);
        web.setBackgroundColor(0xFF05080E);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setGeolocationEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return loader.shouldInterceptRequest(r.getUrl());
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback cb) {
                cb.invoke(origin, true, false);
            }

            @Override
            public void onPermissionRequest(final PermissionRequest req) {
                runOnUiThread(new Runnable() {
                    public void run() { req.grant(req.getResources()); }
                });
            }
        });
        web.addJavascriptInterface(new Bridge(), "Android");
        requestPerms();
        web.loadUrl(URL);
    }

    private void hideBars() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    private void requestPerms() {
        List<String> need = new ArrayList<String>();
        String[] base = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA};
        for (String p : base) if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) need.add(p);
        String audio = Build.VERSION.SDK_INT >= 33 ? "android.permission.READ_MEDIA_AUDIO" : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (checkSelfPermission(audio) != PackageManager.PERMISSION_GRANTED) need.add(audio);
        if (!need.isEmpty() && Build.VERSION.SDK_INT >= 23) requestPermissions(need.toArray(new String[0]), 7);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] p, int[] r) {
        js("window.onPermissions&&window.onPermissions()");
    }

    private void js(final String code) {
        runOnUiThread(new Runnable() {
            public void run() { if (web != null) web.evaluateJavascript(code, null); }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean f) { super.onWindowFocusChanged(f); if (f) hideBars(); }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        if (Intent.ACTION_MAIN.equals(i.getAction())) js("window.onHome&&window.onHome()");
    }

    @Override
    public boolean onKeyDown(int k, KeyEvent e) {
        if (k == KeyEvent.KEYCODE_BACK) { js("window.onBack&&window.onBack()"); return true; }
        return super.onKeyDown(k, e);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) { player.release(); player = null; }
    }

    private class Bridge {
        @JavascriptInterface
        public String musicList() {
            JSONArray out = new JSONArray();
            try {
                Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                String[] cols = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DURATION};
                Cursor c = getContentResolver().query(uri, cols, MediaStore.Audio.Media.IS_MUSIC + "!=0", null, MediaStore.Audio.Media.TITLE + " COLLATE NOCASE");
                if (c != null) {
                    while (c.moveToNext()) {
                        JSONObject o = new JSONObject();
                        o.put("id", c.getLong(0));
                        o.put("title", c.getString(1));
                        String a = c.getString(2);
                        o.put("artist", "<unknown>".equals(a) ? "" : a);
                        o.put("dur", c.getLong(3) / 1000);
                        out.put(o);
                    }
                    c.close();
                }
            } catch (Exception ignored) { }
            return out.toString();
        }

        @JavascriptInterface
        public void musicPlay(String id) {
            try {
                if (player != null) { player.release(); }
                player = new MediaPlayer();
                player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                player.setDataSource(MainActivity.this, ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(id)));
                player.setVolume(volume, volume);
                player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    public void onCompletion(MediaPlayer mp) { ended = true; }
                });
                player.prepare();
                player.start();
                currentId = id;
                ended = false;
            } catch (Exception e) { currentId = ""; }
        }

        @JavascriptInterface
        public void musicToggle() {
            try {
                if (player == null) return;
                if (player.isPlaying()) player.pause(); else { player.start(); ended = false; }
            } catch (Exception ignored) { }
        }

        @JavascriptInterface
        public void musicSeek(int ms) { try { if (player != null) player.seekTo(ms); } catch (Exception ignored) { } }

        @JavascriptInterface
        public void musicVolume(float v) { volume = v; try { if (player != null) player.setVolume(v, v); } catch (Exception ignored) { } }

        @JavascriptInterface
        public String musicState() {
            try {
                JSONObject o = new JSONObject();
                boolean has = player != null;
                o.put("id", currentId);
                o.put("playing", has && !ended && player.isPlaying());
                o.put("pos", has ? player.getCurrentPosition() : 0);
                o.put("dur", has ? player.getDuration() : 0);
                o.put("ended", ended);
                return o.toString();
            } catch (Exception e) { return "{}"; }
        }

        @JavascriptInterface
        public String listApps() {
            JSONArray out = new JSONArray();
            try {
                PackageManager pm = getPackageManager();
                Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> l = pm.queryIntentActivities(i, 0);
                final PackageManager fpm = pm;
                Collections.sort(l, new Comparator<ResolveInfo>() {
                    public int compare(ResolveInfo a, ResolveInfo b) {
                        return a.loadLabel(fpm).toString().compareToIgnoreCase(b.loadLabel(fpm).toString());
                    }
                });
                for (ResolveInfo r : l) {
                    if (r.activityInfo.packageName.equals(getPackageName())) continue;
                    JSONObject o = new JSONObject();
                    o.put("pkg", r.activityInfo.packageName);
                    o.put("label", r.loadLabel(pm).toString());
                    out.put(o);
                }
            } catch (Exception ignored) { }
            return out.toString();
        }

        @JavascriptInterface
        public void openApp(String pkg) {
            try {
                Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
                if (i != null) startActivity(i);
            } catch (Exception ignored) { }
        }

        @JavascriptInterface
        public void openMaps() {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="));
                startActivity(i);
            } catch (Exception e) {
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps"))); } catch (Exception ignored) { }
            }
        }
    }
}
