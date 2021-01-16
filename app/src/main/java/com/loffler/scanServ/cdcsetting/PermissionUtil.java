package com.loffler.scanServ.cdcsetting;

import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Process;

import com.topjohnwu.superuser.Shell;

import java.util.List;

import kotlin.TypeCastException;
import kotlin.jvm.internal.Intrinsics;

public final class PermissionUtil {
    public static final String ACTION_USB_PERMISSION = "com.lamasatech.visipoint.USB_PERMISSION";


    public static final boolean checkAndGrantUsbDevicePermission(Context paramContext, UsbManager paramUsbManager, UsbDevice paramUsbDevice) {
        Intrinsics.checkParameterIsNotNull(paramContext, "context");
        Intrinsics.checkParameterIsNotNull(paramUsbManager, "usbManager");
        if (!paramUsbManager.hasPermission(paramUsbDevice)) {
            paramContext = paramContext.getApplicationContext();
            Intrinsics.checkExpressionValueIsNotNull(paramContext, "context.applicationContext");
            try {
                ApplicationInfo applicationInfo = paramContext.getPackageManager().getApplicationInfo(paramContext.getPackageName(), 0);
                if (applicationInfo != null) {
                    UsbManager mUsbManager = (UsbManager) paramContext.getSystemService(Context.USB_SERVICE);
                    paramUsbManager.getDeviceList().values().iterator();
                    PendingIntent permissionIntent = PendingIntent.getBroadcast(paramContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    mUsbManager.requestPermission(paramUsbDevice, permissionIntent);
                }
            } catch (Exception exception) {

            } finally {
            }
            return paramUsbManager.hasPermission(paramUsbDevice);
        }
        return true;
    }

    public static final boolean hasAppOpsPermission(Context paramContext, String paramString) {
        Intrinsics.checkParameterIsNotNull(paramContext, "context");
        Intrinsics.checkParameterIsNotNull(paramString, "op");
        Object object = paramContext.getSystemService(Context.APP_OPS_SERVICE);
        if (object != null)
            return (((AppOpsManager) object).checkOpNoThrow(paramString, Process.myUid(), paramContext.getPackageName()) == 0);
        throw new TypeCastException("null cannot be cast to non-null type android.app.AppOpsManager");
    }

    public static boolean setMipsFilePermission(String mipsSharedPrefFilePath) {
        Shell.Result result;
        // Execute commands synchronously
        result = Shell.su("chmod 755 " + mipsSharedPrefFilePath).exec();
        // Aside from commands, you can also load scripts from InputStream
        //        result = Shell.su(getResources().openRawResource(R.raw.script)).exec();

        List<String> out = result.getOut();  // stdout
        int code = result.getCode();         // return code of the last command
        return result.isSuccess();     // return code == 0?

    }

    public static boolean setPoaFolderPermission() {
        Shell.Result result;
        // Execute commands synchronously
        result = Shell.su("chmod -R 777 /data/data/com.poa.tempscanner").exec();
        // Aside from commands, you can also load scripts from InputStream
        //        result = Shell.su(getResources().openRawResource(R.raw.script)).exec();

        List<String> out = result.getOut();  // stdout
        int code = result.getCode();         // return code of the last command
        return result.isSuccess();     // return code == 0?
    }
}

