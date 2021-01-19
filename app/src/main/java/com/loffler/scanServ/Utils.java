package com.loffler.scanServ;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.loffler.scanServ.utils.AppLauncher;
import com.loffler.scanServ.utils.AppLauncherImpl;
import com.loffler.scanServ.utils.ViewUtilsKt;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Utils {
    private static final String LOG_TAG = "Utils";
    // Doesn't delete the top folder, just everything underneath

    public static boolean isMyServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void deleteSubFiles(File dirOrFile, int level) {
        if (dirOrFile.isDirectory()) {
            for (File child : dirOrFile.listFiles()) {
                deleteSubFiles(child, level + 1);
            }
        }

        if (level != 0)
            dirOrFile.delete();
    }

    public static void notificationArrived(Context context, String title, String myMsg, long timeout, Handler handler) {

        final boolean overlayEnabled = Settings.canDrawOverlays(context);
        if (!overlayEnabled) return;
        final AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle(title);
        dialog.setMessage(myMsg);
        dialog.setCancelable(true);
        AlertDialog alertDialog = dialog.create();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        } else {
            alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        }
        handler.postDelayed(new Runnable() {
            public void run() {
                if (ViewUtilsKt.isAppInBackground(context)) {
                    alertDialog.show();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            new AppLauncherImpl(context).launchScanServ();
                            alertDialog.dismiss();
                        }
                    }, 5000L);
                }
            }
        }, (timeout * 1000) - 5000L);
    }

    public static String getEthMacAddress() {
        try {
            String interfaceName = "eth0";
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (!intf.getName().equalsIgnoreCase(interfaceName)) {
                    continue;
                }

                byte[] mac = intf.getHardwareAddress();
                if (mac == null) {
                    return "";
                }

                StringBuilder buf = new StringBuilder();
                for (byte aMac : mac) {
                    buf.append(String.format("%02X", aMac));
                }

                return buf.toString();
            }
        } catch (Exception ex) {
            Log.e(LOG_TAG, "Error when getting Mac address: " + ex.getMessage());
        }
        return "";
    }

    public static boolean validateKey(String key, SharedPreferences prefs) {
        // If the first 24 characters don't match the mac address key check, it's a bad key
        if (key.length() < 24 || !key.substring(0, 24).equals(getWifiMacAddressKeyCheck())) {
            Log.e(LOG_TAG, "Bad license key");
            return false;
        }
        try {
            String last6 = key.substring(key.length() - 6);
            String first6 = key.substring(0, 6);
            String last6MacKey = key.substring((key.length() - 12), (key.length() - 6));
            int last6Int = Integer.parseInt(last6);
            int first6Int = Integer.parseInt(first6);
            int last6MacKeyInt = Integer.parseInt(last6MacKey);
            int xor = (last6Int ^ first6Int);
            int featureSet = xor - last6MacKeyInt;

            // If the feature set value does not contain one of the enum values, or it is not
            // divisible by the base value, it is bad key
            if ((featureSet & FEATURE_SET.FEATURE_SET_MASK.getNumericType()) == 0 ||
                    featureSet % FEATURE_SET.BASE.getNumericType() != 0) {
                Log.e(LOG_TAG, "Bad feature set key");
                return false;
            }

            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(Constants.FeatureSetKey, featureSet);
            editor.putBoolean(Constants.ProductKeyActivationCompleted, true);
            editor.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getWifiMacAddressKeyCheck() {
        try {
            String address = getEthMacAddress();

            // reverse the bytes in the string
            byte[] byteArray = address.getBytes();
            for (int pos = 0; pos < (byteArray.length) / 2; pos++) {
                byte temp = byteArray[pos];
                int endPos = byteArray.length - pos - 1;
                byteArray[pos] = byteArray[endPos];
                byteArray[endPos] = temp;
            }

            StringBuilder checkVal = new StringBuilder();
            for (byte b : byteArray) {
                checkVal.append(b);
            }

            return checkVal.toString();
        } catch (Exception ex) {
            Log.e(LOG_TAG, "Error when performing key check: " + ex.getMessage());
        }
        return "";
    }

    enum FEATURE_SET {
        // The base functionality is Email, support page, USB updates.
        BASE(1024),
        //Extended functionality is OTA updates, check updates button, send logs button, plus all features in base functionality.
        EXTENDED(2048),
        BADGE_PRINTING(4096),
        SQL(8192),
        // Value of all other enums added together
        FEATURE_SET_MASK(15360);

        FEATURE_SET(int i) {
            this.type = i;
        }

        public int getNumericType() {
            return type;
        }

        private int type;
    }

    public static void SetupPeriodicUpdateAlarm(Context context) {
        Intent i = new Intent(context, PeriodicUpdateAlarmReceiver.class);
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 99, i, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent cancelIntent = PendingIntent.getBroadcast(context, 99, i, PendingIntent.FLAG_NO_CREATE);
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 1);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (cancelIntent != null) {
            alarm.cancel(cancelIntent);
        }

        Log.i(LOG_TAG, "New Periodic Alarm Set: " + calendar.toString());

        alarm.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis() + (1000 * 60 * 60 * 24), AlarmManager.INTERVAL_DAY, pendingIntent);
    }

    public static boolean createTrialFile(Context context) {
        File dir = new File(Constants.TRIAL_DIR);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            long currentDate = Calendar.getInstance().getTimeInMillis();
            // 15 days
            long trialPeriod = 1000 * 60 * 60 * 24 * 15;
            File progressFile = new File(Constants.TRAIL_FILE);
            FileOutputStream stream = new FileOutputStream(progressFile);
            stream.write(String.valueOf((currentDate + trialPeriod)).getBytes());
            stream.close();

            // Trial has started, set feature set key to contain all features
            SharedPreferences prefs = context.getSharedPreferences(Constants.PreferenceName, Context.MODE_PRIVATE);
            prefs.edit().putInt(Constants.FeatureSetKey, FEATURE_SET.FEATURE_SET_MASK.type).commit();

            // Start alarm for checking trial
            setTrialCheckAlarm(context);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error registering trial");
            return false;
        }

        return true;
    }

    public static boolean checkTrialFileExists() {
        File inputFile = new File(Environment.getExternalStorageDirectory().toString() + "/sysinfo/config.dat");

        return inputFile.exists();
    }

    public static boolean checkTrialValidity(Context context) {
        File inputFile = new File(Environment.getExternalStorageDirectory().toString() + "/sysinfo/config.dat");
        SharedPreferences prefs = context.getSharedPreferences(Constants.PreferenceName, Context.MODE_PRIVATE);

        if (!inputFile.exists()) {
            // Trial not started
            return false;
        } else {
            long trialDate = 0;
            try {
                BufferedReader br = new BufferedReader(new FileReader(inputFile));
                String line;

                while ((line = br.readLine()) != null) {
                    trialDate = Long.parseLong(line);
                }
                br.close();

                if (Calendar.getInstance().getTimeInMillis() >= trialDate) {
                    // Trial date has run out, trial unavailable
                    if (!prefs.getBoolean(Constants.ProductKeyActivationCompleted, false)) {
                        // If the customer has not activated a key, set feature set back to 0
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putInt(Constants.FeatureSetKey, 0);
                        // Disable Badge printing and SQL, in case the customer does not buy a key with
                        // those features.
                        editor.putBoolean(Constants.BadgePrintingDisabled, true);
                        editor.putBoolean(Constants.SQLConnected, false);
                        editor.commit();
                    }
                    return false;
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error checking trial validity");
                if (!prefs.getBoolean(Constants.ProductKeyActivationCompleted, false)) {
                    // If the customer has not activated a key, set feature set back to 0
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt(Constants.FeatureSetKey, 0);
                    // Disable Badge printing and SQL, in case the customer does not buy a key with
                    // those features.
                    editor.putBoolean(Constants.BadgePrintingDisabled, true);
                    editor.putBoolean(Constants.SQLConnected, false);
                    editor.commit();
                }
                return false;
            }
        }

        return true;
    }

    public static void setTrialCheckAlarm(Context context) {
        Intent intent = new Intent(context, TrialValidityCheckAlarmReceiver.class);
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 151, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent cancelIntent = PendingIntent.getBroadcast(context, 151, intent, PendingIntent.FLAG_NO_CREATE);
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 15);
        calendar.set(Calendar.SECOND, 0);

        if (cancelIntent != null) {
            alarm.cancel(cancelIntent);
        }

        alarm.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis() + (1000 * 60 * 60 * 24), AlarmManager.INTERVAL_DAY, pendingIntent);
        Log.d(LOG_TAG, "New Trial Check Alarm set.");
    }

    public static boolean readConfigFile(final Context context) {
        try {
            File configDir = new File(Constants.SCANSERV_FOLDER);
            File configFile = new File(Constants.SCANSERV_CONFIG_FILE);

            // If the config file doesn't exist, write one with default Loffler values
            // This can be removed after devices are updated
            if (!configFile.exists()) {
                if (!configDir.exists() && !configDir.mkdirs()) {
                    Log.d(LOG_TAG, "Failed to create directory for config file");
                    return false;
                }

                byte[] buff = new byte[1024];
                int read = 0;
                try (InputStream in = context.getApplicationContext().getResources().openRawResource(R.raw.default_loffler_config); FileOutputStream out = new FileOutputStream(Constants.SCANSERV_CONFIG_FILE)) {
                    while ((read = in.read(buff)) > 0) {
                        out.write(buff, 0, read);
                    }
                }
            }

            FileInputStream is = new FileInputStream(configFile);

            int size = is.available();

            byte[] buffer = new byte[size];

            is.read(buffer);

            is.close();

            String jsonString = new String(buffer, StandardCharsets.UTF_8);

            JSONObject config = new JSONObject(jsonString);

            SharedPreferences preferences = context.getSharedPreferences(Constants.PreferenceName, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            Iterator<String> keys = config.keys();
            String key;
            while (keys.hasNext()) {
                key = keys.next();
                switch (key.toLowerCase()) {
                    case "company":
                        editor.putString(Constants.SupportCompanyKey, config.getString(key));
                        break;
                    case "email":
                        editor.putString(Constants.SupportEmailKey, config.getString(key));
                        break;
                    case "phone":
                        editor.putString(Constants.SupportPhoneKey, config.getString(key));
                        break;
                    case "sendlogsurl":
                        String sendlogsurl = config.getString(key);
                        if (!sendlogsurl.endsWith("?")) sendlogsurl += "?";
                        editor.putString(Constants.SendLogsUrlKey, sendlogsurl);
                        break;
                    case "updatesurl":
                        String updateurl = config.getString(key);
                        if (!updateurl.endsWith("?")) updateurl += "?";
                        editor.putString(Constants.UpdateUrlKey, updateurl);
                        break;
                    case "base64logo":
                        editor.putString(Constants.CompanyLogoKey, config.getString(key));
                        break;
                    case "licensekey":
                        if (!validateKey(config.getString(key), preferences)) {
                            Log.e(LOG_TAG, "Bad license key provided in scanserv config file");
                        }
                        break;
                    default:
                        // Unknown tag, ignore
                }
            }
            editor.apply();
        } catch (Exception ex) {
            Log.e(LOG_TAG, "Failed to read scanserv config file: " + ex);
            return false;
        }

        return true;
    }
}
