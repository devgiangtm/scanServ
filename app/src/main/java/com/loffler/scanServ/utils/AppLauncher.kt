package com.loffler.scanServ.utils

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.loffler.scanServ.Constants


interface AppLauncher {
    fun launchEzPass()
    fun launchScanServ()
    fun finishCurrentActivity()
}

class AppLauncherImpl(val context: Context) : AppLauncher {
    companion object {
        private const val EZ_PASS_PACKAGE_NAME: String = "com.neldtv.mips"
        private const val SCAN_SERV_DASHBOARD_ACTION = "com.loffler.scanServ"
    }

    override fun launchEzPass() {
        if (openApp(EZ_PASS_PACKAGE_NAME)) {
            val preferences = context.getSharedPreferences(Constants.PreferenceName, Context.MODE_PRIVATE)
            val forceReturnDelay = preferences.getLong(Constants.DashboardSettingsReturnToForegroundTimeoutKey, Constants.FORCE_RETURN_TO_FOREGROUND_TIMEOUT_DEFAULT_MS)
            val handler = Handler(Looper.getMainLooper())

            handler.postDelayed({
                if (context.isAppInBackground()) {
                    launchScanServ()
                }
            }, forceReturnDelay*1000L)
        } else {
            MessageProvider.showToast(context, "Unable to open EZ Pass")
        }
    }

    override fun launchScanServ() {
        if (!openApp(SCAN_SERV_DASHBOARD_ACTION)) {
            MessageProvider.showToast(context, "Unable to open ScanServ")
        }
    }

    override fun finishCurrentActivity() {
        (context as Activity).finish()
    }

    private fun openApp(packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        context.startActivity(intent)
        return true
    }
}