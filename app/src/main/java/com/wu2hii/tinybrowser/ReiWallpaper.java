package com.wu2hii.cleanx;

import android.content.Context;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public final class ReiWallpaper {
    private static final String PREFS = "wallpaper_settings";
    private static final String KEY_URI = "custom_wallpaper_uri";
    private static final String WALLPAPER =
        "https://images.hdqwalls.com/download/rei-ayanami-neon-genesis-evangelion-5k-ir-2160x3840.jpg";
    private ReiWallpaper() {}

    public static void rotateForLaunch(Context context) { /* One fixed cool-toned wallpaper. */ }

    public static void apply(Context context, View target) {
        GradientDrawable fallback = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{0xff0b1428, 0xff172b49, 0xff0c1830});
        target.setBackground(fallback);
        Runnable startLoad = () -> new Thread(() -> {
            Bitmap bitmap = loadSelected(context.getApplicationContext());
            if (bitmap == null) return;
            int w = Math.max(1, target.getWidth());
            int h = Math.max(1, target.getHeight());
            Bitmap cropped = cropCenter(bitmap, (float) w / h);
            BitmapDrawable image = new BitmapDrawable(context.getResources(), cropped);
            image.setGravity(Gravity.FILL);
            GradientDrawable shade = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x4d071327, 0x420b1930, 0x77071120});
            LayerDrawable layers = new LayerDrawable(new Drawable[]{image, shade});
            target.post(() -> {
                if (target.isAttachedToWindow()) target.setBackground(layers);
            });
        }, "wallpaper-loader").start();

        View.OnAttachStateChangeListener listener = new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View v) {
                v.removeOnAttachStateChangeListener(this);
                ViewTreeObserver observer = v.getViewTreeObserver();
                observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override public void onGlobalLayout() {
                        if (v.getWidth() <= 0 || v.getHeight() <= 0) return;
                        v.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        startLoad.run();
                    }
                });
            }
            @Override public void onViewDetachedFromWindow(View v) {}
        };
        if (target.isAttachedToWindow()) listener.onViewAttachedToWindow(target);
        else target.addOnAttachStateChangeListener(listener);
    }



    public static Uri selectedUri(Context context) {
        String value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_URI, null);
        return value == null ? null : Uri.parse(value);
    }
    public static void setSelectedUri(Context context, Uri uri) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_URI, uri == null ? null : uri.toString()).apply();
    }
    private static Bitmap loadSelected(Context context) {
        Uri uri = selectedUri(context);
        if (uri == null) return load(context);
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
            try (InputStream in = context.getContentResolver().openInputStream(uri)) { BitmapFactory.decodeStream(in, null, bounds); }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return load(context);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = Math.max(1, Math.min(bounds.outWidth, bounds.outHeight) / 1200);
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                Bitmap bitmap = BitmapFactory.decodeStream(in, null, options);
                return bitmap != null ? bitmap : load(context);
            }
        } catch (Exception e) { return load(context); }
    }

    private static Bitmap cropCenter(Bitmap source, float targetAspect) {
        if (source == null || targetAspect <= 0f) return source;
        int width = source.getWidth(), height = source.getHeight();
        float sourceAspect = (float) width / height;
        if (sourceAspect > targetAspect) {
            int cropWidth = Math.max(1, Math.min(width, Math.round(height * targetAspect)));
            return Bitmap.createBitmap(source, (width - cropWidth) / 2, 0, cropWidth, height);
        }
        int cropHeight = Math.max(1, Math.min(height, Math.round(width / targetAspect)));
        return Bitmap.createBitmap(source, 0, (height - cropHeight) / 2, width, cropHeight);
    }

    private static synchronized Bitmap load(Context context) {
        File file = new File(context.getCacheDir(), "rei-wallpaper-cool-mecha.jpg");
        if (!file.isFile() || file.length() < 10000) {
            HttpURLConnection connection = null;
            File temp = new File(context.getCacheDir(), "rei-wallpaper-cool-mecha.tmp");
            try {
                connection = (HttpURLConnection) new URL(WALLPAPER).openConnection();
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(20000);
                connection.setRequestProperty("User-Agent", "wu2hii-Toolbox/3.5 Android");
                connection.setInstanceFollowRedirects(true);
                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) return null;
                try (InputStream in = connection.getInputStream();
                     FileOutputStream out = new FileOutputStream(temp)) {
                    byte[] buffer = new byte[8192]; int n;
                    while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                }
                if (!temp.renameTo(file)) {
                    try (InputStream in = new FileInputStream(temp);
                         FileOutputStream out = new FileOutputStream(file)) {
                        byte[] buffer = new byte[8192]; int n;
                        while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                    }
                    temp.delete();
                }
            } catch (Exception e) {
                temp.delete();
                return null;
            } finally {
                if (connection != null) connection.disconnect();
            }
        }
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = Math.max(1, Math.min(bounds.outWidth, bounds.outHeight) / 1080);
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        } catch (Exception e) { return null; }
    }
}