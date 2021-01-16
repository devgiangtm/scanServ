package com.loffler.scanServ;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class TrialValidityCheckAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Utils.checkTrialFileExists() && Utils.checkTrialValidity(context)) {
            Utils.setTrialCheckAlarm(context);
        } else {
            Intent servIntent = new Intent(context, ScanService.class);
            servIntent.putExtra("reset", true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(servIntent);
            } else {
                context.startService(servIntent);
            }
        }
    }
}
