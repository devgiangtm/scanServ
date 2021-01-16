package com.loffler.scanServ.cdcsetting;

import android.content.Context;
import android.content.SharedPreferences;

import static com.loffler.scanServ.Constants.PreferenceName;

public class SharedPreferencesController {

    public static final String mySharedPreferences = PreferenceName;
    public static SharedPreferences sharedPreferences;
    public static SharedPreferences.Editor editor;
    private static SharedPreferencesController instance;

    public static SharedPreferencesController with(Context context) {
        if (instance == null) {
            instance = new SharedPreferencesController(context);
        }
        return instance;
    }

    public SharedPreferencesController(Context context) {
        sharedPreferences = context.getSharedPreferences(mySharedPreferences, Context.MODE_PRIVATE);
    }

    public void saveString(String key, String value) {
        editor = sharedPreferences.edit();
        editor.putString(key, value);
//        editor.apply();
        editor.commit();
    }

    public String getString(String key) {
        return sharedPreferences.getString(key, "");
    }

    public void saveBoolean(String key, boolean value) {
        editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public boolean getBoolean(String key) {
        return sharedPreferences.getBoolean(key, false);
    }

    public void saveLong(String key, Long value) {
        editor = sharedPreferences.edit();
        editor.putLong(key, value);
//        editor.apply();
        editor.commit();
    }

    public Long getLong(String key) {
        return sharedPreferences.getLong(key, -1L);
    }

    public void clearAllData(Context context) {
        sharedPreferences = context.getSharedPreferences(mySharedPreferences, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear().apply();
    }
}
