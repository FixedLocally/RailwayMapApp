package me.lkp111138.railwaymap2.helpers;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class CacheManager {
    // we manage our caches here

    public static void getCachedFile(@NonNull final Context ctx, @NonNull String remote, final CacheCallback callback) {
        // we first try to get the local filename from the db
        SQLiteDatabase db = SQLiteOpener.getDatabase(ctx);
        Cursor c = db.query("caches", new String[]{"local", "expires"}, "remote=?",
                new String[]{remote}, null, null, null);
        c.moveToFirst();
        if (c.moveToNext()) {
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
                    return;
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
        // so if we didnt get a valid file above, now its time to fetch from the internet
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
                String cache_filename = ctx.getCacheDir().getAbsolutePath() + System.currentTimeMillis();
                ResponseBody body = response.body();
                if (body != null) {
                    File file = new File(cache_filename);
                    long length = body.contentLength();
                    if (file.createNewFile()) {
                        // we have the new file, now write to it
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
                        // we have the file, finally

                        callback.onResponse(cache_filename);
                    } else {
                        callback.onError(new IOException("Cannot create file " + cache_filename));
                    }
                }
            }
        });
    }

    public interface CacheCallback {
        void onResponse(String filename);

        void onError(IOException e);

        // bytes_total < 0 if the length is unknown
        void onProgress(long bytes_read, long bytes_total);

        void onStartDownload();
    }
}
