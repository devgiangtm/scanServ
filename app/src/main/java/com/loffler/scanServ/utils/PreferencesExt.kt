package com.loffler.scanServ.utils

import android.content.SharedPreferences

fun SharedPreferences.write(key: String, value: String?) {
    edit().putString(key, value).apply()
}

fun SharedPreferences.write(key: String, value: Boolean) {
    edit().putBoolean(key, value).apply()
}

