package com.rhythmcat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * 节奏猫 —— 一个极薄的 WebView 壳。
 *
 * 游戏本体是 assets/index.html（构建时从仓库根目录拷进来）。
 * 这里只做三件事：
 *   1. 全屏、保持竖屏/横屏自适应
 *   2. 允许 WebAudio 在没有用户手势时也能播
 *   3. 把 <input type="file"> 接上系统的文件选择器，
 *      并把选中的文件复制到应用缓存里（保留原始文件名和扩展名）
 */
public class MainActivity extends Activity {

    private static final int REQ_FILE = 1001;

    private WebView web;
    private ValueCallback<Uri[]> pending;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        web = new WebView(this);
        FrameLayout root = new FrameLayout(this);
        root.addView(web, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setTextZoom(100);

        web.setWebViewClient(new WebViewClient());

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view,
                                             ValueCallback<Uri[]> cb,
                                             FileChooserParams params) {
                if (pending != null) {
                    pending.onReceiveValue(null);
                }
                pending = cb;

                Intent intent = null;
                try {
                    intent = params.createIntent();
                } catch (Exception ignored) {
                }
                if (intent == null) {
                    intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("*/*");
                }
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                try {
                    startActivityForResult(intent, REQ_FILE);
                    return true;
                } catch (Exception e) {
                    pending = null;
                    return false;
                }
            }
        });

        web.loadUrl("file:///android_asset/index.html");
        goFullscreen();
    }

    private void goFullscreen() {
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) goFullscreen();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQ_FILE) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        if (pending == null) return;

        Uri[] result = null;
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri src = data.getData();
            File cached = copyToCache(src);
            result = new Uri[]{cached != null ? Uri.fromFile(cached) : src};
        }

        pending.onReceiveValue(result);
        pending = null;
    }

    /**
     * 把选中的 content:// 文件复制到应用缓存。
     * 保留原始文件名 —— 游戏里要用扩展名判断是不是 ncm/qmc 这类加密格式。
     */
    private File copyToCache(Uri src) {
        InputStream in = null;
        FileOutputStream out = null;
        try {
            String name = queryDisplayName(src);
            File dir = new File(getCacheDir(), "picks");
            if (!dir.exists() && !dir.mkdirs()) return null;

            File dst = new File(dir, name);
            in = getContentResolver().openInputStream(src);
            if (in == null) return null;

            out = new FileOutputStream(dst);
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            out.flush();
            return dst;
        } catch (Exception e) {
            return null;
        } finally {
            try {
                if (in != null) in.close();
            } catch (Exception ignored) {
            }
            try {
                if (out != null) out.close();
            } catch (Exception ignored) {
            }
        }
    }

    private String queryDisplayName(Uri uri) {
        String name = null;
        Cursor c = null;
        try {
            c = getContentResolver().query(uri, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = c.getString(idx);
            }
        } catch (Exception ignored) {
        } finally {
            try {
                if (c != null) c.close();
            } catch (Exception ignored) {
            }
        }

        if (name == null || name.trim().isEmpty()) {
            name = "music_" + System.currentTimeMillis() + ".mp3";
        }

        // 只保留安全字符，防止路径穿越和非法文件名
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            boolean ok = Character.isLetterOrDigit(ch)
                    || ch == '.' || ch == '-' || ch == '_' || ch == ' ';
            sb.append(ok ? ch : '_');
        }
        String safe = sb.toString().replace("..", "_").trim();
        if (safe.isEmpty()) safe = "music_" + System.currentTimeMillis();
        return safe;
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (web != null) web.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) web.onResume();
        goFullscreen();
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.loadUrl("about:blank");
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
