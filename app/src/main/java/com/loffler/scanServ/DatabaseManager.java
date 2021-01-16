package com.loffler.scanServ;

import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

public class DatabaseManager {

    public static synchronized SQLiteDatabase getInstance() {
        return SQLiteDatabase.openDatabase(Environment.getExternalStorageDirectory() + "/mipsClientDb/renzhengbidui_db", null, SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING);
    }
}
