package com.loffler.scanServ;

import android.content.Context;

import org.json.JSONObject;

public interface BasePrinter {
    String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    void printBadge(final Context context, final JSONObject json, final String companyName, final boolean failedTempBadge);
    boolean isConnected(Context context);
}
