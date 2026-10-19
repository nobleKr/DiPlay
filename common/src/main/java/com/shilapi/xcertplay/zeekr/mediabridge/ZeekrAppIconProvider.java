/*
 * DiPlay — Zeekr adaptation. GPLv3 (see LICENSE).
 */
package com.shilapi.xcertplay.zeekr.mediabridge;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;

/**
 * Read-only ContentProvider that serves the PNG written by {@link ZeekrAppIcon}
 * so the Zeekr media centre (a system app) can read the source-app logo via
 * ContentResolver.openFileDescriptor. Declared (authority "<pkg>.app-icon") in
 * the manifest — unlike the engine reference, which left the provider undeclared.
 */
public final class ZeekrAppIconProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (getContext() == null) throw new FileNotFoundException(String.valueOf(uri));
        if (!"r".equals(mode) || !Collections.singletonList(ZeekrAppIcon.FILE_NAME).equals(uri.getPathSegments())) {
            throw new FileNotFoundException(String.valueOf(uri));
        }
        File file = new File(new File(getContext().getCacheDir(), ZeekrAppIcon.DIRECTORY), ZeekrAppIcon.FILE_NAME);
        if (!file.isFile()) throw new FileNotFoundException(String.valueOf(uri));
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override public String getType(Uri uri) { return "image/png"; }
    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { return null; }
    @Override public int delete(Uri u, String s, String[] a) { return 0; }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
}
