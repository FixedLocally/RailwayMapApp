package me.lkp111138.railwaymap2.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Cache management.
 */
public class CacheManager {
    /**
     * @param ctx The context you are calling from, usually an Activity
     * @param remote The URL you are downloading
     * @param callback Functions called when something happens, see Callback
     */
    public static void getCachedFile(@NonNull final Context ctx, @NonNull String remote, final CacheCallback callback) {
        // we first try to get the local filename from the db
        if (!localResolve(ctx, remote, callback)) {
            // so if we didnt get a valid file above, now its time to fetch from the internet
            remoteResolve(ctx, remote, callback);
        }
    }

    /**
     * @param ctx the context you are calling from, usually an Activity
     * @param local_file the local file to invalidate
     */
    public static void invalidate(Context ctx, String local_file) {
        SQLiteDatabase db = SQLiteOpener.getDatabase(ctx);
        int affected = db.delete("cached_files", "local=?", new String[]{local_file});
        if (affected > 0) {
            // we indeed deleted a row
            File file = new File(local_file);
            file.delete();
        }
    }

    /**
     * @param ctx the context you are calling from, usually an Activity
     * @param remote the remote url to be resolved
     * @param callback Functions called when something happens, see Callback
     */
    private static void remoteResolve(Context ctx, String remote, CacheCallback callback) {
        OkHttpClient client = new OkHttpClient();
        // GET the file
        Request req = new Request.Builder().url(remote).build();
        // we tell the user we are in progress
        callback.onStartDownload();
        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                // time to cache
                String cache_filename = ctx.getCacheDir().getAbsolutePath() + File.separator + System.currentTimeMillis();
                ResponseBody body = response.body();
                if (body != null) {
                    File file = new File(cache_filename);
                    long length = body.contentLength();
                    if (file.createNewFile()) {
                        // we have the new file, now write to it
                        SQLiteDatabase db = SQLiteOpener.getDatabase(ctx);
                        FileOutputStream fos = new FileOutputStream(file);
                        // if the file isnt found even after we create it thats gonna be epic
                        byte[] buffer = new byte[4096];
                        int bytes_read;
                        long total_read = 0;
                        while ((bytes_read = body.byteStream().read(buffer)) != -1) {
                            fos.write(buffer, 0, bytes_read);
                            total_read += bytes_read;
                            callback.onProgress(total_read, length);
                        }
                        fos.close();
                        // how long should we cache?
                        int expires = -1;
                        // Cache-Control header
                        CacheControl cacheControl = response.cacheControl();
                        int max_age = cacheControl.maxAgeSeconds();
                        if (max_age > 0) {
                            // we got it
                            expires = (int) (System.currentTimeMillis() / 1000) + max_age;
                        } else {
                            // check Expires header
                            Date date = response.headers().getDate("Expires");
                            if (date != null) {
                                expires = (int) (date.getTime() / 1000);
                            }
                        }
                        response.close();
                        // we have the file, finally
                        // insert it into the db
                        ContentValues values = new ContentValues();
                        values.put("local", cache_filename);
                        values.put("remote", remote);
                        if (expires > 0) {
                            values.put("expires", expires);
                        } else {
                            // in our use case, loading from server every time isnt desirable, so we
                            // force it to not reload for a day instead
                            values.put("expires", (int) (System.currentTimeMillis() / 1000) + 86400);
                        }
                        db.replace("cached_files", null, values);
                        callback.onResponse(cache_filename);
                    } else {
                        callback.onError(new IOException("Cannot create file " + cache_filename));
                    }
                }
            }
        });
    }

    /**
     * @param ctx The context you are calling from, usually an Activity
     * @param remote The URL you are downloading
     * @param callback Functions called when something happens, see Callback
     * @return Whether we can resolve locally or not
     */
    private static boolean localResolve(Context ctx, String remote, CacheCallback callback) {
        SQLiteDatabase db = SQLiteOpener.getDatabase(ctx);
        Cursor c = db.query("cached_files", new String[]{"local", "expires"}, "remote=?",
                new String[]{remote}, null, null, null);
        if (c.moveToFirst()) {
            // theres a row
            String filename = c.getString(0);
            int expires = c.getInt(1);
            File file = new File(filename);
            // check for file existence
            if (file.exists()) {
                // k we got the file
                if (expires * 1000L >= System.currentTimeMillis()) {
                    // not expired yet
                    callback.onResponse(filename);
                    return true;
                } else {
                    // we got an expired file, cleanup
                    boolean delete = file.delete();
                    if (!delete) {
                        Log.w("CacheManager", "Cannot delete file " + filename);
                    }
                }
            }
        }
        c.close();
        return false;
    }

    public interface CacheCallback {
        /**
         * Called only once per `getCachedFile` call when the file is resolved.
         * @param filename The filename that the remote file resolved to
         */
        void onResponse(String filename);

        /**
         * Called at most once per `getCachedFile` call when an error occurred while resolving.
         * @param e The exception caused the operation to fail
         */
        void onError(IOException e);

        /**
         * May be called more than once per `getCachedFile` call while downloading.
         * @param bytes_read Number of bytes downloaded
         * @param bytes_total Number of bytes in total
         */
        void onProgress(long bytes_read, long bytes_total);

        /**
         * Called at most once per `getCachedFile` call when local resolving failed and we need to
         * download it from the internet
         */
        void onStartDownload();
    }
}
