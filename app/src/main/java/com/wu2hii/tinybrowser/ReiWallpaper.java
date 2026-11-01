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
import java.util.Random;

public final class ReiWallpaper {
    private static final String[] SOURCES = {
        "https://images.hdqwalls.com/download/rei-ayanami-neon-genesis-evangelion-5k-ir-2160x3840.jpg",
        "https://kabekin.com/iphone/uploads/converted/24/09/20/47564514-building_ayanamirei_0240920_iphone_wallpaper_kabekin-rp5V-750x1334-MM-100.jpg",
        "https://divnil.com/wallpaper/iphone5s/img/app/1/3/1392_744_eva_88_d02b82638b9ea6d8e4bf5ddd0e8123f8_raw.jpg",
        "https://getwallpapers.com/wallpaper/full/a/e/6/17599.jpg"
    };
    private static final String PREFS = "rei_wallpaper";
    private ReiWallpaper() {}

    public static void rotateForLaunch(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int old = p.getInt("index", -1);
        int next = old < 0 ? new Random().nextInt(SOURCES.length) : (old + 1) % SOURCES.length;
        p.edit().putInt("index", next).apply();
    }

    public static void apply(Context context, View target) {
        int index = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("index", 0);
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
        File file = new File(context.getCacheDir(), "rei-wallpaper-" + index + ".jpg");
        if (!file.isFile() || file.length() < 10000) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(SOURCES[index]).openConnection();
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
