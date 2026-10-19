/*
 * DiPlay — Zeekr adaptation. GPLv3 (see LICENSE).
 */
package com.shilapi.xcertplay.zeekr.mediabridge;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;

/**
 * Renders DiPlay's own launcher icon to a 192x192 PNG and exposes it as a
 * content:// Uri (served by {@link ZeekrAppIconProvider}) so the Zeekr media
 * centre can show it as the source-app logo. Mirrors the engine reference
 * ZeekrAppIcon; the bitmap is whatever getApplicationIcon() resolves to, so the
 * app's existing icon is used — nothing to configure.
 */
public final class ZeekrAppIcon {
    private static final String TAG = "DhuAdapter";
    static final String DIRECTORY = "zeekr-app-icon";
    static final String FILE_NAME = "app-icon.png";
    private static final int SIZE = 192;

    private ZeekrAppIcon() {}

    /** Authority is "<packageName>.app-icon". */
    public static String authority(Context ctx) {
        return ctx.getPackageName() + ".app-icon";
    }

    /** Render + cache the icon; returns its content:// Uri, or null on failure. */
    public static Uri publish(Context context) {
        try {
            Drawable icon = context.getPackageManager().getApplicationIcon(context.getPackageName());
            Bitmap bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
            icon.setBounds(0, 0, SIZE, SIZE);
            icon.draw(new Canvas(bitmap));
            File dir = new File(context.getCacheDir(), DIRECTORY);
            dir.mkdirs();
            File out = new File(dir, FILE_NAME);
            try (FileOutputStream fos = new FileOutputStream(out)) {
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)) {
                    return null;
                }
            }
            bitmap.recycle();
            return Uri.parse("content://" + authority(context) + "/" + FILE_NAME);
        } catch (Exception e) {
            Log.w(TAG, "Could not publish the app icon", e);
            return null;
        }
    }
}
