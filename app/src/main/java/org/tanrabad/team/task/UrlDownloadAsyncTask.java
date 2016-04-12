/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.tanrabad.team.task;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;
import org.tanrabad.team.R;

import java.io.*;
import java.net.URL;

public class UrlDownloadAsyncTask extends AsyncTask<Void, Void, Object> {

    private static final String TAG = "UrlDownloadAsyncTask";
    private static final int TARGET_HEIGHT = 256;
    private static final int TARGET_WIDTH = 256;
    /**
     * 1/16th of the maximum memory
     */
    private static final LruCache CACHE = new LruCache((int) (Runtime.getRuntime().maxMemory() / 16));

    private final UrlDownloadAsyncTaskHandler handler;
    private final String url;

    public UrlDownloadAsyncTask(String url, UrlDownloadAsyncTaskHandler handler) {
        super();
        this.handler = handler;
        this.url = url;
    }

    public static void download(String url, final File downloadFile, final Context context) {
        UrlDownloadAsyncTask task = new UrlDownloadAsyncTask(url, new UrlDownloadAsyncTaskHandler() {
            @Override
            public void onPreExecute() {
                Toast.makeText(context, "Start downloading", Toast.LENGTH_SHORT).show();
            }

            @Override
            public Object doInBackground(File file) {
                if (file == null) {
                    return null;
                }

                try {
                    BufferedInputStream in = null;
                    BufferedOutputStream out = null;

                    //create output directory if it doesn't exist
                    File dir = downloadFile.getParentFile();
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }

                    in = new BufferedInputStream(new FileInputStream(file));
                    out = new BufferedOutputStream(new FileOutputStream(downloadFile));

                    byte[] buffer = new byte[1024 * 100];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    in.close();
                    out.flush();
                    out.close();

                    return downloadFile;
                } catch (IOException e) {
                    Log.e(TAG, "doInBackground IO Exception", e);
                }

                return null;
            }

            @Override
            public void onPostExecute(Object object, UrlDownloadAsyncTask task) {
                if (object instanceof File) {
                    Toast.makeText(context, "Finish downloading: "
                            + ((File) object).getAbsolutePath(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, "Error downloading", Toast.LENGTH_SHORT).show();
                }
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            task.execute();
        }
    }

    public static void display(String url, final ImageView imageView) {
        UrlDownloadAsyncTask task = null;

        if (imageView.getTag() != null && imageView.getTag() instanceof UrlDownloadAsyncTask) {
            task = (UrlDownloadAsyncTask) imageView.getTag();
            task.cancel(true);
            imageView.setTag(null);
        }

        task = new UrlDownloadAsyncTask(url, new UrlDownloadAsyncTaskHandler() {
            @Override
            public void onPreExecute() {
                //do nothing here
            }

            @Override
            public Object doInBackground(File file) {
                if (file == null) {
                    return null;
                }

                Bitmap bm = null;
                try {

                    BufferedInputStream bin = new BufferedInputStream(new FileInputStream(file));
                    bin.mark(bin.available());

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(bin, null, options);

                    Boolean scaleByHeight = Math.abs(options.outHeight - TARGET_HEIGHT)
                            >= Math.abs(options.outWidth - TARGET_WIDTH);

                    if (options.outHeight * options.outWidth >= TARGET_HEIGHT * TARGET_WIDTH) {
                        double sampleSize = scaleByHeight
                                ? options.outHeight / TARGET_HEIGHT
                                : options.outWidth / TARGET_WIDTH;
                        options.inSampleSize = (int) Math.pow(2d, Math.floor(Math.log(sampleSize) / Math.log(2d)));
                    }

                    try {
                        bin.reset();
                    } catch (IOException e) {
                        bin = new BufferedInputStream(new FileInputStream(file));
                    }

                    // Do the actual decoding
                    options.inJustDecodeBounds = false;
                    bm = BitmapFactory.decodeStream(bin, null, options);
                } catch (IOException e) {
                    Log.e(TAG, "doInBackground IOException", e);
                }

                return bm;
            }

            @Override
            public void onPostExecute(Object object, UrlDownloadAsyncTask task) {
                if (object != null && object instanceof Bitmap && imageView.getTag() == task) {
                    imageView.setImageBitmap((Bitmap) object);
                } else {
                    imageView.setImageResource(R.drawable.sendbird_img_placeholder);
                }
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            task.execute();
        }

        imageView.setTag(task);
    }

    protected Object doInBackground(Void... args) {
        File outFile = null;
        try {
            if (CACHE.get(url) != null && new File(CACHE.get(url)).exists()) { // Cache Hit
                outFile = new File(CACHE.get(url));
            } else { // Cache Miss, Downloading a file from the url.
                outFile = File.createTempFile("sendbird-download", ".tmp");
                OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outFile));

                InputStream input = new BufferedInputStream(new URL(url).openStream());
                byte[] buf = new byte[1024 * 100];
                int read = 0;
                while ((read = input.read(buf, 0, buf.length)) >= 0) {
                    outputStream.write(buf, 0, read);
                }

                outputStream.flush();
                outputStream.close();
                CACHE.put(url, outFile.getAbsolutePath());
            }
        } catch (IOException e) {
            Log.e(TAG, "doInBackground io Exception", e);
            if (outFile != null) {
                outFile.delete();
            }
            outFile = null;
        }


        if (handler != null) {
            return handler.doInBackground(outFile);
        }

        return outFile;
    }

    @Override
    protected void onPreExecute() {
        if (handler != null) {
            handler.onPreExecute();
        }
    }

    protected void onPostExecute(Object result) {
        if (handler != null) {
            handler.onPostExecute(result, this);
        }
    }

    public interface UrlDownloadAsyncTaskHandler {
        void onPreExecute();

        Object doInBackground(File file);

        void onPostExecute(Object object, UrlDownloadAsyncTask task);
    }
}
