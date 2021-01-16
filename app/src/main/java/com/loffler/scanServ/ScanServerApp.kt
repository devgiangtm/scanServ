package com.loffler.scanServ

import android.app.Application
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.loffler.scanServ.cdcsetting.FileUtil

class ScanServerApp : Application(), LifecycleObserver {
    var isAppInBackground: Boolean = false
        private set


    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        Companion.integrateWithMIPSApp(applicationContext)


    }


    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        // app moved to foreground
        isAppInBackground = false
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onMoveToBackground() {
        // app moved to background
        isAppInBackground = true
    }

    class Companion private constructor() {
        companion object {
            fun integrateWithMIPSApp(param1Context: Context?) {
//            Intrinsics.checkParameterIsNotNull(param1Context, "context");
                FileUtil.appendStringToFile("WebServer: integrateWithMIPSApp")
                //            if (Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(param1Context))
//                param1Context.startService(new Intent(param1Context, FloatingWidgetService.class));
//            param1Context.startService(new Intent(param1Context, WebService.class));
                //todo: sync mips worker
//            SyncMipsConfigWorker.Companion.scheduleSyncMipsConfigJob(param1Context, 0L);
//            PrinterManager.getInstance(param1Context).scanUsbDevices();
            }
        }
    }
}