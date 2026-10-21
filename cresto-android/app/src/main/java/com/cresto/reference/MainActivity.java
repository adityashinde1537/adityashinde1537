package com.cresto.reference;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.view.View;
import android.widget.Toast;
import androidx.webkit.WebViewAssetLoader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int EXPORT_FILE = 842;
    private WebView web;
    private String pendingFileText;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(Color.rgb(10,35,49));
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
          .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
        web = new WebView(this);
        web.setBackgroundColor(Color.rgb(244,247,246));
        web.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView w, WebResourceRequest r) {
                return loader.shouldInterceptRequest(r.getUrl());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView w, WebResourceRequest r) {
                return !r.getUrl().toString().startsWith("https://appassets.androidplatform.net/assets/");
            }
        });
        web.setWebChromeClient(new WebChromeClient());
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        web.addJavascriptInterface(new FileExport(), "CrestoAndroid");
        setContentView(web);
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }

    public class FileExport {
        @JavascriptInterface public void exportText(String filename, String content) {
            runOnUiThread(() -> {
                pendingFileText = content;
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType(filename.endsWith(".csv") ? "text/csv" : "text/plain");
                intent.putExtra(Intent.EXTRA_TITLE, filename.replaceAll("[^a-zA-Z0-9_.-]", "_"));
                try { startActivityForResult(intent, EXPORT_FILE); }
                catch (ActivityNotFoundException e) { Toast.makeText(MainActivity.this, "No file manager found", Toast.LENGTH_SHORT).show(); }
            });
        }
    }
    @Override protected void onActivityResult(int req, int result, Intent data) {
        super.onActivityResult(req, result, data);
        if (req != EXPORT_FILE || result != RESULT_OK || data == null || data.getData() == null || pendingFileText == null) return;
        try (OutputStream os = getContentResolver().openOutputStream(data.getData())) {
            if (os == null) throw new Exception("Unable to write");
            os.write(pendingFileText.getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this, "CRESTO export saved", Toast.LENGTH_SHORT).show();
        } catch (Exception e) { Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show(); }
        finally { pendingFileText = null; }
    }
    @Override public void onBackPressed() {
        new AlertDialog.Builder(this).setTitle("Exit CRESTO?")
          .setMessage("Your reference changes are saved on this phone.")
          .setNegativeButton("Stay", (d,i) -> {})
          .setPositiveButton("Exit", (d,i) -> finish()).show();
    }
    @Override protected void onDestroy() {
        if (web != null) { web.destroy(); web = null; }
        super.onDestroy();
    }
}