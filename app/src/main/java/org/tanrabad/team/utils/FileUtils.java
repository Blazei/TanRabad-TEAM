/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.tanrabad.team.utils;


import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class FileUtils {

    private static final String PATH = "path";
    private static final String MIME = "mime";
    private static final String SIZE = "size";
    private static final String TAG = "FileUtils";

    private FileUtils() {
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static Map<String, Object> getFileInfo(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;


        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) { // DocumentProvider
            // ExternalStorageProvider
            return getFileInfoDocuments(context, uri);
        } else if ("content".equalsIgnoreCase(uri.getScheme())) { // MediaStore (and general)
            if (isNewGooglePhotosUri(uri)) {
                return getFileInfoNewGooglePhoto(context, uri);
            } else {
                return getDataColumn(context, uri, null, null);
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) { // File
            Map<String, Object> value = new ConcurrentHashMap<>();
            value.put(PATH, uri.getPath());
            value.put(SIZE, (int) new File((String) value.get(PATH)).length());
            value.put(MIME, "application/octet-stream");

            return value;
        }

        return null;
    }

    private static Map<String, Object> getFileInfoNewGooglePhoto(Context context, Uri uri) {
        Map<String, Object> value = getDataColumn(context, uri, null, null);
        Bitmap bitmap;
        try {
            InputStream input = context.getContentResolver().openInputStream(uri);
            bitmap = BitmapFactory.decodeStream(input);
            File file = File.createTempFile("sendbird", ".jpg");
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, new BufferedOutputStream(new FileOutputStream(file)));
            value.put(PATH, file.getAbsolutePath());
            value.put(SIZE, (int) file.length());
        } catch (IOException io) {
            Log.e(TAG, "io exception", io);
        }
        return value;
    }

    private static Map<String, Object> getFileInfoDocuments(Context context, Uri uri) {
        if (isExternalStorageDocument(uri)) {
            final String docId = DocumentsContract.getDocumentId(uri);
            final String[] split = docId.split(":");
            final String type = split[0];

            if ("primary".equalsIgnoreCase(type)) {
                Map<String, Object> value = new ConcurrentHashMap<>();
                value.put(PATH, Environment.getExternalStorageDirectory() + "/" + split[1]);
                value.put(SIZE, (int) new File((String) value.get(PATH)).length());
                value.put(MIME, "application/octet-stream");

                return value;
            }
        } else if (isDownloadsDocument(uri)) { // DownloadsProvider
            final String id = DocumentsContract.getDocumentId(uri);
            final Uri contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));

            return getDataColumn(context, contentUri, null, null);
        } else if (isMediaDocument(uri)) { // MediaProvider
            final String docId = DocumentsContract.getDocumentId(uri);
            final String[] split = docId.split(":");
            final String type = split[0];

            Uri contentUri = null;
            if ("image".equals(type)) {
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            } else if ("video".equals(type)) {
                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            } else if ("audio".equals(type)) {
                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            }

            String selection = "_id=?";
            String[] selectionArgs = new String[]{
                    split[1]
            };

            return getDataColumn(context, contentUri, selection, selectionArgs);
        }
        return null;
    }

    private static Map<String, Object> getDataColumn(Context context, Uri uri, String selection,
                                                     String... selectionArgs) {
        String[] projection = {
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.SIZE,
        };

        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                String path = cursor.getString(columnIndex);

                columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE);
                String mime = cursor.getString(columnIndex);

                columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);

                Map<String, Object> value = new ConcurrentHashMap<>();
                if (path == null) path = "";
                if (mime == null) mime = "application/octet-stream";

                value.put(PATH, path);
                value.put(MIME, mime);
                value.put(SIZE, cursor.getInt(columnIndex));

                return value;
            }
        }
        return null;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static boolean isNewGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.contentprovider".equals(uri.getAuthority());
    }
}
