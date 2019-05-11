package me.lkp111138.railwaymap2.helpers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;
import android.util.Log;

public class SQLiteOpener extends SQLiteOpenHelper {
    private static final String DB_FILENAME = "data.db";
    private static final int VERSION = 1;
    private static SQLiteDatabase db;

    /**
     * @param ctx The context you are calling from, usually an Activity
     */
    private SQLiteOpener(@Nullable Context ctx) {
        super(ctx, DB_FILENAME, null, VERSION);
    }

    /**
     * @param ctx The context you are calling from, usually an Activity
     */
    static SQLiteDatabase getDatabase(Context ctx) {
        if (db == null || !db.isOpen()) {
            // db invalid, get a new one
            db = new SQLiteOpener(ctx).getWritableDatabase();
        }
        return db;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // create our tables
        Log.d("Database", "creating database");
        sqLiteDatabase.execSQL("CREATE TABLE cached_files (remote text primary key, local text, expires int);");
        Log.d("Database", "create database ok");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int old, int now) {
//        if (old < 2 && now >= 2) {
//            // upgrade to 2
//        }
    }
}
