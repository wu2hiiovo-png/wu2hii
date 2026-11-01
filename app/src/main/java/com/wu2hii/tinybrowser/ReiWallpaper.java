package com.wu2hii.cleanx;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.Gravity;
import android.view.View;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public final class ReiWallpaper {
    private static final String WALLPAPER =
        "https://images.hdqwalls.com/download/rei-ayanami-neon-genesis-evangelion-5k-ir-2160x3840.jpg";
    private ReiWallpaper() {}

    public static void rotateForLaunch(Context context) { /* One fixed cool-toned wallpaper. */ }

    public static void apply(Context context, View target) {
        GradientDrawable fallback = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{0xff0b1428, 0xff172b49, 0xff0c1830});
        target.setBackground(fallback);
        Runnable startLoad = () -> new Thread(() -> {
            Bitmap bitmap = load(context.getApplicationContext());
            if (bitmap == null) return;
            BitmapDrawable image = new BitmapDrawable(context.getResources(), bitmap);
            image.setGravity(Gravity.CENTER_CROP);
            GradientDrawable shade = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x4d071327, 0x420b1930, 0x77071120});
            LayerDrawable layers = new LayerDrawable(new Drawable[]{image, shade});
            target.post(() -> {
                if (target.isAttachedToWindow()) target.setBackground(layers);
            });
        }, "wallpaper-loader").start();

        if (target.isAttachedToWindow()) {
            startLoad.run();
        } else {
            View.OnAttachStateChangeListener listener = new View.OnAttachStateChangeListener() {
                @Override public void onViewAttachedToWindow(View v) {
                    v.removeOnAttachStateChangeListener(this);
                    startLoad.run();
                }
                @Override public void onViewDetachedFromWindow(View v) {}
            };
            target.addOnAttachStateChangeListener(listener);
        }
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