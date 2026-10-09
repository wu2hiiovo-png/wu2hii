package com.wu2hii.tinybrowser;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;

public class MainActivity extends Activity {
    private WebView web;
    private EditText address;
    private ProgressBar progress;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.WHITE);
        LinearLayout bar = new LinearLayout(this); bar.setOrientation(LinearLayout.HORIZONTAL); bar.setPadding(dp(8), dp(8), dp(8), dp(6)); bar.setGravity(android.view.Gravity.CENTER_VERTICAL);
        address = new EditText(this); address.setSingleLine(true); address.setTextSize(14); address.setHint("搜索或输入网址"); address.setImeOptions(EditorInfo.IME_ACTION_GO); address.setSelectAllOnFocus(false);
        address.setPadding(dp(12), 0, dp(8), 0); address.setBackgroundColor(Color.rgb(243,245,247));
        bar.addView(address, new LinearLayout.LayoutParams(0, dp(44), 1));
        Button go = button("前往"); bar.addView(go, new LinearLayout.LayoutParams(dp(52), dp(44)));
        root.addView(bar);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal); progress.setMax(100); root.addView(progress, new LinearLayout.LayoutParams(-1, dp(2)));
        web = new WebView(this); web.getSettings().setJavaScriptEnabled(true); web.getSettings().setDomStorageEnabled(true); web.getSettings().setLoadsImagesAutomatically(true);
        web.setWebViewClient(new WebViewClient() { @Override public void onPageFinished(WebView v, String url) { address.setText(url); } });
        web.setWebChromeClient(new WebChromeClient() { @Override public void onProgressChanged(WebView v, int p) { progress.setProgress(p); progress.setVisibility(p >= 100 ? View.GONE : View.VISIBLE); } });
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout controls = new LinearLayout(this); controls.setOrientation(LinearLayout.HORIZONTAL); controls.setGravity(android.view.Gravity.CENTER); controls.setPadding(dp(4), dp(4), dp(4), dp(4));
        Button back = button("‹"); Button forward = button("›"); Button refresh = button("↻"); Button home = button("主页"); Button translate = button("翻译");
        controls.addView(back, weightButton()); controls.addView(forward, weightButton()); controls.addView(refresh, weightButton()); controls.addView(home, weightButton()); controls.addView(translate, weightButton()); root.addView(controls);
        setContentView(root);
        go.setOnClickListener(v -> loadAddress());
        address.setOnEditorActionListener((v, action, event) -> { if (action == EditorInfo.IME_ACTION_GO || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) { loadAddress(); return true; } return false; });
        back.setOnClickListener(v -> { if (web.canGoBack()) web.goBack(); });
        forward.setOnClickListener(v -> { if (web.canGoForward()) web.goForward(); });
        refresh.setOnClickListener(v -> web.reload());
        home.setOnClickListener(v -> web.loadUrl("https://www.google.com"));
        translate.setOnClickListener(v -> { String url = "https://translate.google.com/translate?sl=auto&tl=zh-CN&u=" + Uri.encode(web.getUrl() == null ? "https://www.google.com" : web.getUrl()); web.loadUrl(url); });
        if (state != null) web.restoreState(state); else web.loadUrl("https://www.google.com");
    }
    private void loadAddress() {
        String s = address.getText().toString().trim(); if (s.isEmpty()) return;
        String url;
        if (s.matches("(?i)^[a-z][a-z0-9+.-]*://.*")) url = s;
        else if (s.contains(" ") || !s.contains(".")) url = "https://www.google.com/search?q=" + Uri.encode(s);
        else url = "https://" + s;
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(address.getWindowToken(), 0);
        web.loadUrl(url);
    }
    private Button button(String label) { Button b = new Button(this); b.setText(label); b.setTextSize(13); b.setAllCaps(false); b.setPadding(dp(2), 0, dp(2), 0); return b; }
    private LinearLayout.LayoutParams weightButton() { return new LinearLayout.LayoutParams(0, dp(44), 1); }
    private int dp(int x) { return (int)(x * getResources().getDisplayMetrics().density + 0.5f); }
    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); if (web != null) web.saveState(out); }
    @Override public void onBackPressed() { if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed(); }
}
