package com.loffler.scanServ;

import android.app.smdt.SmdtManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.util.Log;

import java.io.File;

import static android.hardware.usb.UsbConstants.USB_CLASS_MASS_STORAGE;

public class USBEventReceiver extends BroadcastReceiver {
    private final String LOG_TAG = "USBEventReceiver";

    private UpdateHandler updateHandler = new UpdateHandler();
    @Override
    public void onReceive(final Context context, final Intent intent) {
        final Handler handler = new Handler();
        // TODO: Can be smarter about checking if the drive is mounted. For instance, check /proc/mount for "usbport0"
        // or figure out how to get MEDIA_MOUNTED events to fire
        // Give the drive 5 seconds to get mounted
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateIfMediaDevice(context, intent);
            }
        }, 5000);
    }

    private void updateIfMediaDevice(Context context, Intent intent) {
        SmdtManager smdtManager = new SmdtManager(context);
        Log.d(LOG_TAG, "USB device plugged in");

        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

        if (device == null) {
            Log.e(LOG_TAG, "Failed to get USB device from intent");
            return;
        }

        UsbInterface intf = device.getInterface(0);
        if (intf.getInterfaceClass() != USB_CLASS_MASS_STORAGE ||
                intf.getInterfaceSubclass() != 6 ||
                intf.getInterfaceProtocol() != 80) {
            Log.d(LOG_TAG, "Not a Media Device");
            // Not a Media device do nothing
            return;
        }

        Utils.deleteSubFiles(new File(Constants.UPDATES_FOLDER_LOCATION), 0);
        smdtManager.execSuCmd("cp -r /storage/usbport0/updates/. " + Constants.UPDATES_FOLDER_LOCATION);
        updateHandler.checkForUpdates(context);
    }
}
