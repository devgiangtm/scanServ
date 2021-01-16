package com.loffler.scanServ;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

public class PeriodicUpdateAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Utils.SetupPeriodicUpdateAlarm(context);

        Calendar calendar = Calendar.getInstance();
        // If it's the second Wednesday of the month, check for updates
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY && calendar.get(Calendar.DAY_OF_WEEK_IN_MONTH) == 2) {
            new UpdateHandler().checkForOTAUpdates(context);
        }
    }
}
