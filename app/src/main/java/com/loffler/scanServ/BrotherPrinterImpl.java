package com.loffler.scanServ;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.text.format.DateFormat;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.brother.ptouch.sdk.Printer;
import com.brother.ptouch.sdk.PrinterInfo;
import com.brother.ptouch.sdk.PrinterStatus;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Objects;

public class BrotherPrinterImpl implements BasePrinter {
    private final String LOG_TAG = "BrotherPrinterImpl";
    private Printer brotherPrinter = new Printer();
    private SharedPreferences sharedPreferences;

    @Override
    public void printBadge(Context context, JSONObject json, String companyName, boolean failedTempBadge) {
        sharedPreferences = context.getSharedPreferences(Constants.PreferenceName, Context.MODE_PRIVATE);

        if (!isConnected(context) || !initPrinter(context)) {
            Log.e(LOG_TAG, "Failed to connect to Brother printer");
            return;
        }

        View badge;
        try {
            if (failedTempBadge) {
                badge = generateFailedBrotherBadge(context);
            } else {
                badge = generateBrotherBadge(context, json, companyName);
            }
        } catch (Exception e) {
            Log.d(LOG_TAG, "Failed to generate badge for printing: " + e);
            return;
        }

        Bitmap badgeBitmap = getScreenViewBitmap(badge);

        brotherPrinter.startCommunication();
        final PrinterStatus printImage = brotherPrinter.printImage(badgeBitmap);
        brotherPrinter.endCommunication();

        if (printImage.errorCode != PrinterInfo.ErrorCode.ERROR_NONE) {
            Log.e(LOG_TAG, "Error printing Brother badge: " + printImage.errorCode.toString());
        }
    }

    @Override
    public boolean isConnected(Context context) {
        UsbManager usbManager = (UsbManager)context.getSystemService(Context.USB_SERVICE);
        UsbDevice usbDevice = brotherPrinter.getUsbDevice(usbManager);

        if (usbDevice == null) {
            return false;
        }

        boolean isBrother = ("Brother".equalsIgnoreCase(usbDevice.getManufacturerName()) && Objects.requireNonNull(usbDevice.getProductName()).toLowerCase().contains("ql"));
        return isBrother && checkAndGrantUsbDevicePermission(context, usbManager, usbDevice);
    }

    private boolean checkAndGrantUsbDevicePermission(Context context, UsbManager usbManager, UsbDevice usbDevice) {
        if (!usbManager.hasPermission(usbDevice)) {
            context = context.getApplicationContext();
            try {
                ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
                if (applicationInfo != null) {
                    UsbManager mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
                    usbManager.getDeviceList().values().iterator();
                    PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    assert mUsbManager != null;
                    mUsbManager.requestPermission(usbDevice, permissionIntent);
                }
            } catch (Exception exception) {
                Log.e("GrantUSBPermissions", "Error granting USB permissions: " + exception.toString());
            }
            return usbManager.hasPermission(usbDevice);
        }
        return true;
    }

    private boolean initPrinter(Context context) {
        // Initialize brother printer settings
        try {
            final PrinterInfo settings = brotherPrinter.getPrinterInfo();
            settings.printerModel = getModel(context, brotherPrinter);
            settings.port = PrinterInfo.Port.USB;
            settings.enabledTethering = true;
            settings.printMode = PrinterInfo.PrintMode.FIT_TO_PAGE;
            settings.isAutoCut = true;
            settings.isSpecialTape = false;
            settings.orientation = PrinterInfo.Orientation.LANDSCAPE;
            settings.workPath = context.getCacheDir().getPath();
            settings.isCutAtEnd = true;
            settings.isHalfCut = false;
            settings.labelNameIndex = 0;
            brotherPrinter.setPrinterInfo(settings);
            settings.labelNameIndex = (brotherPrinter.getLabelInfo()).labelNameIndex;
            brotherPrinter.setPrinterInfo(settings);
        } catch (Exception e) {
            Log.d(LOG_TAG, "Failed to initialize Brother printer: " + e);
            return false;
        }

        return true;
    }

    private static Bitmap textToBitmap(String str){
        byte[] decodedString = Base64.decode(str, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }

    private static PrinterInfo.Model getModel(Context context, Printer printer) {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        UsbDevice usbDevice = printer.getUsbDevice(usbManager);
        if (usbDevice != null)
            try {
                String str = usbDevice.getProductName();
                return PrinterInfo.Model.valueOf(str.replace("-", "_"));
            } catch (Exception exception) {
                return null;
            }
        return null;
    }

    private Bitmap getScreenViewBitmap(final View v) {
        v.setDrawingCacheEnabled(true);
        int width = 350;
        int height = 905;
        v.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST));
        v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
        v.requestLayout();
        v.buildDrawingCache(true);
        Bitmap b = Bitmap.createBitmap(v.getDrawingCache());
        v.setDrawingCacheEnabled(false); // clear drawing cache
        return b;
    }

    private View generateFailedBrotherBadge(Context context) {
        final View badge = LayoutInflater.from(context.getApplicationContext()).inflate(R.layout.brother_badge_failed_temp, (ViewGroup) null);
        TextView line1 = badge.findViewById(R.id.instructionsLine1);
        TextView line2 = badge.findViewById(R.id.instructionsLine2);
        TextView line3 = badge.findViewById(R.id.instructionsLine3);
        TextView line4 = badge.findViewById(R.id.instructionsLine4);

        line1.setText(sharedPreferences.getString(Constants.FailedBadgeLine1, ""));
        line2.setText(sharedPreferences.getString(Constants.FailedBadgeLine2, ""));
        line3.setText(sharedPreferences.getString(Constants.FailedBadgeLine3, ""));
        line4.setText(sharedPreferences.getString(Constants.FailedBadgeLine4, ""));

        return badge;
    }

    private View generateBrotherBadge(Context context, JSONObject json, String companyName) throws JSONException {
        final View badge = LayoutInflater.from(context.getApplicationContext()).inflate(R.layout.brother_badge, (ViewGroup) null);
        TextView companyNametv = badge.findViewById(R.id.brotherCompanyName);
        ImageView userPicture = badge.findViewById(R.id.imgUser);
        TextView userName = badge.findViewById(R.id.userName);
        TextView scanDate = badge.findViewById(R.id.scanDate);
        TextView scanTime = badge.findViewById(R.id.scanTime);
        TextView scanTemp = badge.findViewById(R.id.scanTemp);
        ImageView checkmark = badge.findViewById(R.id.checkmark);
        TextView checkmarkHeader = badge.findViewById(R.id.checkmarkHeader);
        ImageView circle = badge.findViewById(R.id.circle);
        TextView circleText = badge.findViewById(R.id.circleText);
        View signatureLine = badge.findViewById(R.id.signatureLine);

        double temp = json.getDouble("temperature");
        temp = (temp * 9 / 5) + 32;

        userPicture.setImageBitmap(textToBitmap(json.getString("checkPic")));

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(json.getLong("checkTime"));

        if (sharedPreferences.getBoolean(Constants.PrintImageKey, false)) {
            userPicture.setVisibility(View.VISIBLE);
            userPicture.setImageBitmap(textToBitmap(json.getString("checkPic")));
        }

        if (sharedPreferences.getBoolean(Constants.PrintTempKey, false)) {
            scanTemp.setVisibility(View.VISIBLE);
            scanTemp.setText(String.format("Temp: %.2f F", temp));
        }

        if (sharedPreferences.getBoolean(Constants.PrintDateKey, false)) {
            scanDate.setVisibility(View.VISIBLE);
            scanDate.setText(String.format("Date: %s", dateFormat.format(calendar.getTime())));
        }

        if (sharedPreferences.getBoolean(Constants.PrintTimeKey, false)) {
            scanTime.setVisibility(View.VISIBLE);
            scanTime.setText(String.format("Time: %s", timeFormat.format(calendar.getTime())));
        }

        String name = json.getString("name");
        if (sharedPreferences.getBoolean(Constants.PrintNameKey, false) && !name.equals("")) {
            userName.setVisibility(View.VISIBLE);
            userName.setText(String.format("Name: %s", json.getString("name")));
        }

        if (sharedPreferences.getBoolean(Constants.PrintCompanyNameKey, false)) {
            companyNametv.setVisibility(View.VISIBLE);
            companyNametv.setText(companyName);
        }

        if (sharedPreferences.getBoolean(Constants.PrintSignatureLine, false)) {
            signatureLine.setVisibility(View.VISIBLE);
        }

        switch(ZPLValues.CheckmarkType.valueOf(sharedPreferences.getInt(Constants.CheckmarkPrintType, -1))) {
            case CircleDayOfWeek:
                circle.setVisibility(View.VISIBLE);
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
                circleText.setVisibility(View.VISIBLE);
                circleText.setText(dayOfTheWeek);
                break;
            case CircleDayOfMonth:
                circle.setVisibility(View.VISIBLE);
                circleText.setVisibility(View.VISIBLE);
                circleText.setText(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)));
                break;
            default:
                checkmark.setVisibility(View.VISIBLE);
        }
        return badge;
    }
}
