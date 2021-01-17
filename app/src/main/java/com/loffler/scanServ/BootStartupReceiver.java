package com.loffler.scanServ;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BootStartupReceiver extends BroadcastReceiver {
    private final String LOG_TAG = "BootStartupReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String action = intent.getAction();
            if (action != null && (action.equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED) || action.equalsIgnoreCase(Constants.REBOOT_ACTION))) {
                Intent startSerIntent = new Intent(context, ScanService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(startSerIntent);
                } else {
                    context.startService(startSerIntent);
                }
            }
        } catch (NullPointerException e) {
            Log.e(LOG_TAG, "BR: NPTE in receive");
            e.printStackTrace();
        }

    }
}
