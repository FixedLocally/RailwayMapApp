package me.lkp111138.railwaymap2.helpers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;

public class SQLiteOpener extends SQLiteOpenHelper {
    private static final String DB_FILENAME = "data.db";
    private static final int VERSION = 1;
    private static SQLiteDatabase db;

    private SQLiteOpener(@Nullable Context context) {
        super(context, DB_FILENAME, null, VERSION);
    }

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
        sqLiteDatabase.execSQL("CREATE TABLE caches (remote varchar(255) primary key, local varchar(255), expires int)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int old, int now) {
        switch (old) {
            case 1:
                // upgrade to 2
            case 2:
                if (now < 2) {
                    break;
                }
                // upgrade to 3
        }
    }
}
