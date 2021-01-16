package com.loffler.scanServ;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.text.format.DateFormat;
import android.util.Log;

import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;
import com.zebra.sdk.printer.discovery.DiscoveredPrinterUsb;
import com.zebra.sdk.printer.discovery.DiscoveryHandler;
import com.zebra.sdk.printer.discovery.UsbDiscoverer;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class ZebraPrinterImpl implements BasePrinter {
    private final String LOG_TAG = "ZebraPrinterImpl";
    private PendingIntent mPermissionIntent;
    private DiscoveredPrinterUsb discoveredPrinterUsb;
    private UsbManager mUsbManager;
    private SharedPreferences sharedPreferences;

    @Override
    public void printBadge(Context context, JSONObject json, String companyName, boolean failedTempBadge) {
        sharedPreferences = context.getSharedPreferences(Constants.PreferenceName, Context.MODE_PRIVATE);
        // Find connected printers
        UsbDiscoveryHandler handler = new UsbDiscoveryHandler();
        UsbDiscoverer.findPrinters(context, handler);

        try {
            while (!handler.discoveryComplete) {
                Thread.sleep(100);
            }

            if (handler.printers != null && handler.printers.size() > 0) {
                discoveredPrinterUsb = handler.printers.get(0);
            } else {
                // No printers found, return
                Log.e(LOG_TAG, "No Zebra printers detected");
                return;
            }

            Connection conn = null;
            try {
                if (!mUsbManager.hasPermission(discoveredPrinterUsb.device)) {
                    // OS seems to give permission automatically when we request
                    mUsbManager.requestPermission(discoveredPrinterUsb.device, mPermissionIntent);
                }

                conn = discoveredPrinterUsb.getConnection();
                conn.open();
                // Store the format on the printer
                conn.write(generateZplToPrint(json, failedTempBadge).getBytes());

                com.zebra.sdk.printer.ZebraPrinter printer = ZebraPrinterFactory.getInstance(conn);

                HashMap<Integer, String> vars = generateHashmap(json, companyName, failedTempBadge);

                String storedFormatName = (failedTempBadge ? "E:badge-high-temp.ZPL" : "E:badge-small.ZPL");

                printer.printStoredFormat(storedFormatName, vars);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error printing data: " + e.getLocalizedMessage());
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (ConnectionException e) {
                        e.printStackTrace();
                    }
                }
            }

        } catch (Exception e) {
            Log.e(LOG_TAG, "Error discovering printers: " + e.getLocalizedMessage());
        }
    }

    public boolean isConnected(final Context context) {
        boolean returnVal = false;
        // Find connected printers
        UsbDiscoveryHandler handler = new UsbDiscoveryHandler();
        UsbDiscoverer.findPrinters(context, handler);
        mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);

        try {
            while (!handler.discoveryComplete) {
                Thread.sleep(100);
            }

            if (handler.printers != null && handler.printers.size() > 0) {
                discoveredPrinterUsb = handler.printers.get(0);

                if (!mUsbManager.hasPermission(discoveredPrinterUsb.device)) {
                    mUsbManager.requestPermission(discoveredPrinterUsb.device, mPermissionIntent);
                    // Seems like the OS might give us permission automatically if we request it?
                    // so check again here after requesting.
                    if (mUsbManager.hasPermission(discoveredPrinterUsb.device)) {
                        returnVal = true;
                    }
                } else {
                    returnVal = true;
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error discovering printers when checking connection: " + e.getLocalizedMessage());
        }

        return returnVal;
    }

    private HashMap<Integer, String> generateHashmap(JSONObject json, String companyName, boolean failedTempBadge) throws JSONException {
        HashMap<Integer, String> vars = new HashMap<>();
        if (failedTempBadge) {
            vars.put(1, sharedPreferences.getString(Constants.FailedBadgeLine1, ""));
            vars.put(2, sharedPreferences.getString(Constants.FailedBadgeLine2, ""));
            vars.put(3, sharedPreferences.getString(Constants.FailedBadgeLine3, ""));
            vars.put(4, sharedPreferences.getString(Constants.FailedBadgeLine4, ""));
        } else {
            double temp = json.getDouble("temperature");
            temp = (temp * 9 / 5) + 32;

            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a");
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(json.getLong("checkTime"));


            vars.put(1, String.format("%.2f", temp));
            vars.put(2, dateFormat.format(calendar.getTime()));
            vars.put(3, timeFormat.format(calendar.getTime()));
            vars.put(4, json.getString("name"));
            vars.put(5, companyName);
            switch (ZPLValues.CheckmarkType.valueOf(sharedPreferences.getInt(Constants.CheckmarkPrintType, -1))) {
                case CircleDayOfWeek:
                    String dayOfTheWeek = (String) DateFormat.format("EEEE", calendar);
                    switch (dayOfTheWeek.toLowerCase()) {
                        case "thursday":
                        case "saturday":
                        case "sunday":
                            dayOfTheWeek = dayOfTheWeek.substring(0,2);
                            break;
                        default:
                            dayOfTheWeek = dayOfTheWeek.substring(0,1);
                    }
                    vars.put(6, dayOfTheWeek);
                    break;
                case CircleDayOfMonth:
                    vars.put(6, String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)));
                    break;
                default:
                    // No action needed
                    break;
            }
        }

        return vars;
    }

    private String generateZplToPrint(JSONObject json, boolean failedTempBadge) throws JSONException {
        if (failedTempBadge) {
            return ZPLValues.FAILED_TEMP_BADGE;
        }

        StringBuilder zpl = new StringBuilder();
        zpl.append(ZPLValues.SMALL_BADGE_BASE);

        if (sharedPreferences.getBoolean(Constants.PrintTempKey, false)) {
            zpl.append(ZPLValues.ZPL_TEMP_HEADER);
            zpl.append(ZPLValues.ZPL_TEMP_VALUE);
        }

        if (sharedPreferences.getBoolean(Constants.PrintDateKey, false)) {
            zpl.append(ZPLValues.ZPL_DATE_HEADER);
            zpl.append(ZPLValues.ZPL_DATE_VALUE);
        }

        if (sharedPreferences.getBoolean(Constants.PrintTimeKey, false)) {
            zpl.append(ZPLValues.ZPL_TIME_HEADER);
            zpl.append(ZPLValues.ZPL_TIME_VALUE);
        }

        String name = json.getString("name");
        if (sharedPreferences.getBoolean(Constants.PrintNameKey, false) && !name.equals("")) {
            zpl.append(ZPLValues.ZPL_NAME_HEADER);
            zpl.append(ZPLValues.ZPL_NAME_VALUE);
        }

        if (sharedPreferences.getBoolean(Constants.PrintCompanyNameKey, false)) {
            zpl.append(ZPLValues.ZPL_COMPANY_NAME);
        }

        if (sharedPreferences.getBoolean(Constants.PrintSignatureLine, false)) {
            zpl.append(ZPLValues.ZPL_SIGNATURE_LINE);
        }

        zpl.append(ZPLValues.ZPL_TEMP_OK_HEADER);
        switch(ZPLValues.CheckmarkType.valueOf(sharedPreferences.getInt(Constants.CheckmarkPrintType, -1))) {
            case CircleDayOfWeek:
            case CircleDayOfMonth:
                zpl.append(ZPLValues.ZPL_CIRCLE);
                zpl.append(ZPLValues.ZPL_CIRCLE_TEXT_VALUE);
                break;
            default:
                zpl.append(ZPLValues.ZPL_CHECKMARK);
        }

        zpl.append(ZPLValues.ZPL_END_OF_LABEL);

        return zpl.toString();
    }

    // Handles USB device discovery
    private static class UsbDiscoveryHandler implements DiscoveryHandler {
        public List<DiscoveredPrinterUsb> printers;
        public boolean discoveryComplete = false;

        public UsbDiscoveryHandler() {
            printers = new LinkedList<>();
        }

        public void foundPrinter(final DiscoveredPrinter printer) {
            printers.add((DiscoveredPrinterUsb) printer);
        }

        public void discoveryFinished() {
            discoveryComplete = true;
        }

        public void discoveryError(String message) {
            discoveryComplete = true;
        }
    }
}
