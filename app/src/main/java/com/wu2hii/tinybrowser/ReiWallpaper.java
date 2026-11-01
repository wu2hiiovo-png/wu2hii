package com.wu2hii.cleanx;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public final class ReiWallpaper {
    // Keep only the cool-toned mecha-style Rei wallpaper.
    private static final String WALLPAPER =
        "https://images.hdqwalls.com/download/rei-ayanami-neon-genesis-evangelion-5k-ir-2160x3840.jpg";
    private static final String PREFS = "rei_wallpaper";
    private ReiWallpaper() {}

    public static void rotateForLaunch(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().putInt("index", 0).apply();
    }

    public static void apply(Context context, View target) {
        int index = 0;
        target.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{0xff0b1428, 0xff172b49, 0xff0c1830}));
        new Thread(() -> {
            Bitmap bitmap = load(context.getApplicationContext(), index);
            if (bitmap == null) return;
            int width = Math.max(1, target.getWidth());
            int height = Math.max(1, target.getHeight());
            Bitmap cropped = cropCenter(bitmap, (float) width / height);
            BitmapDrawable image = new BitmapDrawable(context.getResources(), cropped);
            image.setGravity(android.view.Gravity.FILL);
            GradientDrawable shade = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x6e071327, 0x650b1930, 0x99071120});
            LayerDrawable layers = new LayerDrawable(new Drawable[]{image, shade});
            target.post(() -> {
                if (target.getWindowToken() != null) target.setBackground(layers);
            });
        }).start();
    }

    // Center-crop to the view aspect ratio; do not stretch the source artwork.
    private static Bitmap cropCenter(Bitmap source, float targetAspect) {
        if (source == null || targetAspect <= 0f) return source;
        int width = source.getWidth();
        int height = source.getHeight();
        float sourceAspect = (float) width / height;
        if (Math.abs(sourceAspect - targetAspect) < 0.01f) return source;
        if (sourceAspect > targetAspect) {
            int cropWidth = Math.max(1, Math.min(width, Math.round(height * targetAspect)));
            return Bitmap.createBitmap(source, (width - cropWidth) / 2, 0, cropWidth, height);
        }
        int cropHeight = Math.max(1, Math.min(height, Math.round(width / targetAspect)));
        return Bitmap.createBitmap(source, 0, (height - cropHeight) / 2, width, cropHeight);
    }

    private static synchronized Bitmap load(Context context, int index) {
        File file = new File(context.getCacheDir(), "rei-wallpaper-cool-mecha.jpg");
        if (!file.isFile() || file.length() < 10000) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(WALLPAPER).openConnection();
                connection.setConnectTimeout(9000);
                connection.setReadTimeout(12000);
                connection.setRequestProperty("User-Agent", "wu2hii-Toolbox/3.4 Android");
                connection.setInstanceFollowRedirects(true);
                if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300)
                    return null;
                try (InputStream in = connection.getInputStream();
                     FileOutputStream out = new FileOutputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int n;
                    while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                }
            } catch (Exception e) {
                if (file.exists()) file.delete();
                return null;
            } finally {
                if (connection != null) connection.disconnect();
            }
        }
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = Math.max(1, Math.min(bounds.outWidth, bounds.outHeight) / 1080);
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        } catch (Exception e) {
            return null;
        }
    }
}
