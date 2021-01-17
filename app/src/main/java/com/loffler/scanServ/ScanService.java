package com.loffler.scanServ;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.error.VolleyError;
import com.android.volley.request.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.loffler.scanServ.cdcgesture.DetectorActivity;
import com.loffler.scanServ.cdcgesture.MipsData;
import com.loffler.scanServ.cdcsetting.CDCSettingModel;
import com.loffler.scanServ.cdcsetting.Keys;
import com.loffler.scanServ.cdcsetting.SharedPreferencesController;
import com.loffler.scanServ.service.sql.dao.DaoResult;
import com.loffler.scanServ.service.sql.dao.OutputDao;
import com.loffler.scanServ.service.sql.dao.OutputDaoImpl;
import com.loffler.scanServ.utils.AppLauncher;
import com.loffler.scanServ.utils.AppLauncherImpl;
import com.loffler.scanServ.utils.ViewUtilsKt;
import com.loffler.scanServ.welcomescreen.WelcomeDetectorActivity;
import com.opencsv.CSVWriter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.loffler.scanServ.Constants.CURRENT_SCAN_ID;
import static com.loffler.scanServ.Constants.SQLTableName;
import static com.loffler.scanServ.Constants.swSQLWriteLogs;

public class ScanService extends Service {
    private NotificationChannel notificationChannel;
    private AsyncHttpServer serv;
    private final int callbackServerPort = 5000;
    private MailerConfig mailServConfig;
    private final String LOG_TAG = "ScanService";
    private static long lastScanTime = 0;
    private SharedPreferences prefs;
    private BadgePrinter badgePrinter = new BadgePrinter();
    private AppLauncher appLauncher;
    private OutputDao outputDao;
    private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();


    private void loadAndStartServer() {
        Log.i(LOG_TAG, "loadAndStartServer: entering");

        // Start config service, if it's not already running
        Intent configIntent = new Intent(getApplicationContext(), ConfigService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(configIntent);
        } else {
            startService(configIntent);
        }

        prefs = getApplicationContext().getSharedPreferences(Constants.PreferenceName, MODE_PRIVATE);
        appLauncher = new AppLauncherImpl(getApplicationContext());
        outputDao = new OutputDaoImpl(prefs);
        boolean isActivated = prefs.getBoolean(Constants.ProductKeyActivationCompleted, false);
        boolean isOnTrial = Utils.checkTrialFileExists() && Utils.checkTrialValidity(this);

        if (serv != null) serv.stop();

        if (!isActivated) {
            if (!isOnTrial) {
                // software wasn't activated. Do not start the service.
                Log.e(LOG_TAG, "loadAndStartServer: Software not activated. Returning to key activation page");
                startActivity(new Intent(this, ProductKeyActivity.class));
                return;
            } else {
                Utils.setTrialCheckAlarm(this);
            }
        }

        // TODO: put into a preference handler to load up these settings...
        String smptServAddress = prefs.getString(Constants.SMTPServAddressKey, "");
        String smtpUsername = prefs.getString(Constants.SMTPUsernameKey, "");
        String smtpPw = prefs.getString(Constants.SMTPPWKey, "");
        final String ezPassPw = prefs.getString(Constants.EZPassPwKey, "");
        String deviceId = prefs.getString(Constants.DeviceIdKey, "");
        int sslPort = prefs.getInt(Constants.SmtpSSLPortKey, 0);
        String receivingEmail = prefs.getString(Constants.ReceivingEmailKey, "");
        Authentication authType = Authentication.valueOf(prefs.getString(Constants.AuthTypeKey, "SSL"));
        String fromAddress = prefs.getString(Constants.SMTPFromAddressKey, "");


        boolean sendNormalTemp = prefs.getBoolean(Constants.SendNormalTempKey, true);
        boolean sendHighTemp = prefs.getBoolean(Constants.SendHighTempKey, true);
        boolean sendPic = prefs.getBoolean(Constants.SendPicKey, false);
        boolean sendTempReading = prefs.getBoolean(Constants.SendTempReadingKey, false);
        boolean sendName = prefs.getBoolean(Constants.SendNameKey, false);
        boolean autoDeleteRecord = prefs.getBoolean(Constants.AutoDeleteRecordKey, false);
        boolean sendRecordEOD = prefs.getBoolean(Constants.SendRecordEODKey, false);

        mailServConfig = new MailerConfig(0, sendHighTemp, sendNormalTemp, sendPic, sendTempReading, sendName,
                smptServAddress, smtpUsername, smtpPw, ezPassPw, sslPort, receivingEmail, authType, deviceId,
                fromAddress, false, autoDeleteRecord, sendRecordEOD);

        String notificationChannelId = "1";
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(this, notificationChannelId)
                .setAutoCancel(false)
                .setPriority(Notification.FLAG_HIGH_PRIORITY)
                .setOngoing(true)
                .setContentTitle("Scan Service Running...")
                .setTicker("Scan Service Running...")
                .setSmallIcon(R.drawable.ic_scan_serv);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(notificationChannelId, "Notif Channel", NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("Scan Service Notification Channel");

            notificationManager.createNotificationChannel(notificationChannel);
            notifBuilder.setChannelId(notificationChannelId);
            startForeground(1, notifBuilder.build());
        } else {
            notifBuilder.setChannelId(notificationChannelId);
            notificationManager.notify(1, notifBuilder.build());
        }

        // Populate ez-pass callback value
        if (!ezPassPw.isEmpty()) {
            setCallbackUrl(ezPassPw);
        }

        if (serv != null) serv.stop();

        serv = new AsyncHttpServer();

        serv.get("/", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                Log.i(LOG_TAG, "onRequest: Scan Server GET received. Why are you calling this?");
                response.send("Hi, I am running here.");
            }
        });

        serv.post("/", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                Log.i(LOG_TAG, "onRequest: Scan Server POST received. Temp/Face Scan passed off");
                response.send("thank you");

                if (isDashboardFeatureEnabled()) {
                    appLauncher.launchScanServ();
                }


                if (mailServConfig.AutoDeleteRecord && !mailServConfig.SendRecordsAtEndOfDay) {
                    DeleteLocalRecordAndPictures();
                }

                final MailSender ms = new MailSender();
                final AsyncHttpServerRequest req = request;
                RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

                // Check to see if scan delay is enabled; if it is, see if this scan came before
                // the delay threshold was met.
                final JSONObject reqJson;
                try {
                    boolean delayEnabled = prefs.getBoolean(Constants.ScanDelayEnabled, false);
                    int delay = prefs.getInt(Constants.ScanDelay, -1);
                    reqJson = new JSONObject(req.getBody().get().toString());

                    long checkTime = Long.parseLong(reqJson.getString("checkTime"));
                    if (delayEnabled && checkTime < lastScanTime + delay * 1000) {
                        Log.i(LOG_TAG, "Skipping scan, scan occurred before scan delay threshold");
                        return;
                    }
                    lastScanTime = checkTime;
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Exception Transforming Json: ");
                    e.printStackTrace();
                    return;
                }

                Log.i(LOG_TAG, "onRequest: Queue request to get temp and mask settings");
                // TODO: bad, hardcoded localhost
                StringRequest stringRequest = new StringRequest(Request.Method.POST, "http://127.0.0.1:8080/getTempAndMaskSetting?pass=" + mailServConfig.EzPassPw, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            Log.i(LOG_TAG, "onResponse: Temp and Mask settings received");
                            JSONObject settingsObject = new JSONObject(new JSONObject(response).getString("data"));

                            double tempAdjust = settingsObject.getDouble("tempCompensation");
                            mailServConfig.TempLimit = settingsObject.getDouble("standardBodyTemp") + tempAdjust;

                            // If we started the server without SMTP info, don't attempt to send the email but continue
                            if (!mailServConfig.SmtpFromAddress.equals("")) {
                                ms.execute(reqJson, mailServConfig);
                            }
                            int isMaskRequired = settingsObject.getInt("isWearingMask");
                            // Do not print a badge if the user was not wearing a mask but should be
                            if (isMaskRequired == 1 && reqJson.getInt("mask") != 1) {
                                Log.i(LOG_TAG, "User not wearing mask but masks are required; a badge will not be printed.");
                            } else {
                                // Prints out a badge, if enabled
                                badgePrinter.printBadge(ScanService.this, reqJson, mailServConfig.TempLimit);
                            }
                            CDCSettingModel cdcSettingModel = new Gson().fromJson(SharedPreferencesController.with(getBaseContext()).getString(Keys.CDC_SETTING_MODEL), CDCSettingModel.class);
                            if (cdcSettingModel == null) {
                                cdcSettingModel = new CDCSettingModel();
                                SharedPreferencesController.with(getBaseContext()).saveString(Keys.CDC_SETTING_MODEL, new Gson().toJson(cdcSettingModel));
                            }
                            boolean isRequire = false;
                            MipsData paramMipsData = new Gson().fromJson(req.getBody().get().toString(), MipsData.class);
                            if ((paramMipsData.getType() == -1) && cdcSettingModel.isCdcQuestionnaire() && cdcSettingModel.isCdcRequireRoleUnregister())
                                isRequire = true;
                            if ((paramMipsData.getType() == 1) && cdcSettingModel.isCdcQuestionnaire() && cdcSettingModel.isCdcRequireRoleVisitor())
                                isRequire = true;
                            if ((paramMipsData.getType() == 3) && cdcSettingModel.isCdcQuestionnaire() && cdcSettingModel.isCdcRequireRoleEmployee())
                                isRequire = true;
//                            if (isRequire && cdcSettingModel.isCdcMask()) {
//                                if (paramMipsData.getMask() == 1) {
//                                    if (prefs.getBoolean(Constants.SQLConnected, false) && prefs.getBoolean(swSQLWriteLogs, false)) {
//                                        insertScanRecord(reqJson);
//                                    }
//                                    Intent dialogIntent = new Intent(getBaseContext(), DetectorActivity.class);
//                                    dialogIntent.putExtra("mimpsdata", paramMipsData);
//                                    getBaseContext().startActivity(dialogIntent);
//                                }
//                            } else {
//                                if (isRequire) {
//                                    if (prefs.getBoolean(Constants.SQLConnected, false) && prefs.getBoolean(swSQLWriteLogs, false)) {
//                                        insertScanRecord(reqJson);
//                                    }
//                                    Intent dialogIntent = new Intent(getBaseContext(), DetectorActivity.class);
//                                    dialogIntent.putExtra("mimpsdata", paramMipsData);
//                                    getBaseContext().startActivity(dialogIntent);
//                                }
//                            }
                            boolean isDashboardFeatureEnabled = prefs.getBoolean(Constants.DashboardSettingsEnableFeatureKey, false);

                            if (prefs.getBoolean(Constants.SQLConnected, false) && prefs.getBoolean(swSQLWriteLogs, false) && !isDashboardFeatureEnabled ) {
                                insertScanRecord(reqJson);
                            }
                            if (isRequire) {
                                if (cdcSettingModel.isCdcMask() && paramMipsData.getMask() == 1) {
                                    Intent dialogIntent = new Intent(getBaseContext(), DetectorActivity.class);
                                    dialogIntent.putExtra("mimpsdata", paramMipsData);
                                    getBaseContext().startActivity(dialogIntent);
                                } else {
                                    Intent dialogIntent = new Intent(getBaseContext(), DetectorActivity.class);
                                    dialogIntent.putExtra("mimpsdata", paramMipsData);
                                    getBaseContext().startActivity(dialogIntent);
                                }
                            } else {
                                if(ViewUtilsKt.isAppInBackground(getApplicationContext())){
                                    if (prefs.getBoolean(Constants.SQLConnected, false) && prefs.getBoolean(swSQLWriteLogs, false)) {
                                        submitOutput(reqJson);
                                    }
                                    openLofflerApp();
                                }
                            }
                            // Insert into the SQL table if we have successfully connected to one

                        } catch (JSONException e) {
                            Log.e(LOG_TAG, "Exception Transforming Json: ");
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(LOG_TAG, "Error receiving settings from ez pass http serv");
                        error.printStackTrace();
                    }
                });

                queue.add(stringRequest);
            }
        });

        SetupRecurringDeleteAlarm();
        Utils.SetupPeriodicUpdateAlarm(getApplicationContext());

        // listen on port 5000
        serv.listen(callbackServerPort);

        // Continue updates if the progress file exists
        if (new File(Constants.PROGRESS_FILE_LOCATION).exists()) {
            new UpdateHandler().checkForUpdates(getApplicationContext());
        }
    }

    public void openLofflerApp() {
        PackageManager packageManager = getBaseContext().getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage("com.loffler.scanServ");
        startActivity(intent);
    }


    private boolean isDashboardFeatureEnabled() {
        return prefs.getBoolean(Constants.DashboardSettingsEnableFeatureKey, false);
    }

    private void SetupRecurringDeleteAlarm() {
        // setup if we need to delete at end of each day!
        if (mailServConfig.AutoDeleteRecord || mailServConfig.SendRecordsAtEndOfDay) {
            Intent i = new Intent(getApplicationContext(), DailyAlarmReceiver.class);
            i.putExtra("delete", true);
            final PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 99, i, PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent cancelIntent = PendingIntent.getService(this, 99, i, PendingIntent.FLAG_NO_CREATE);
            AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 1);
            calendar.set(Calendar.SECOND, 0);

            try {
                if (cancelIntent != null)
                    alarm.cancel(cancelIntent);
                alarm.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis() + (1000 * 60 * 60 * 24), AlarmManager.INTERVAL_DAY, pendingIntent);
            } catch (NullPointerException e) {
                Log.e(LOG_TAG, "nullptr when setting up alarm");
            }
        }
    }

    // End time is midnight of the current day
    private Long getEndTime() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DATE);
        calendar.set(year, month, day, 0, 0, 0);

        return calendar.getTime().getTime();
    }

    private void SendCsv() {

        Log.i(LOG_TAG, "SendCsv: entering to send CSV");
        SQLiteDatabase db = DatabaseManager.getInstance();

        // get the date range from start of yesterday to the start of today
        Long endTime = getEndTime();
        Long startTime = endTime - (1000 * 60 * 60 * 24);

        Cursor result = db.rawQuery("SELECT * FROM IDCardMsg WHERE currentTime BETWEEN " + startTime + " AND " + endTime, null);
        result.moveToFirst();
        String[] cols = new String[]{"name", "currentTime", "cardInfo", "idCardNum", "tempratrue"};

        try {
            StringWriter sw = new StringWriter();
            CSVWriter csvWriter = new CSVWriter(sw);

            // write header
            csvWriter.writeNext(new String[]{"device id", "Mac Address", "Name", "Current Time", "Card Info", "ID Card Number", "Temperature"});
            String macAddress = Utils.getEthMacAddress();
            while (!result.isAfterLast()) {
                ArrayList<String> data = new ArrayList<>();
                data.add(mailServConfig.DeviceId);
                data.add(macAddress);
                for (String col : cols) {
                    String value = result.getString(result.getColumnIndex(col));
                    if (col.equalsIgnoreCase("tempratrue")) {
                        value = String.valueOf(convertToFahrenheit(Double.parseDouble(value)));
                    } else if (col.equalsIgnoreCase("currentTime")) {
                        final Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(Long.parseLong(value));
                        DateFormat dateFormatter = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.LONG, SimpleDateFormat.MEDIUM);
                        value = dateFormatter.format(cal.getTime());
                    }

                    data.add(value);
                }

                csvWriter.writeNext(data.toArray(new String[data.size()]));
                result.moveToNext();
            }
            csvWriter.close();

            MailSender ms = new MailSender();
            ms.execute(null, mailServConfig, sw.toString());
            Log.i(LOG_TAG, "Successfully handed off send CSV to mailer");
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error sending daily CSV: " + e.getMessage());
        } finally {
            db.close();
        }

        result.close();
    }

    private void DeleteLocalRecordAndPictures() {
        Log.i(LOG_TAG, "DeleteLocalRecordAndPictures: delete local record and pictures");
        try (SQLiteDatabase db = DatabaseManager.getInstance()) {
            // get the date range from start of yesterday to the start of today
            Long endTime = getEndTime();
            Long startTime = endTime - (1000 * 60 * 60 * 24);
            db.delete("IDCardMsg", "currentTime BETWEEN " + startTime + " AND " + endTime, null);
            File dir = new File(Environment.getExternalStorageDirectory() + "/currentImg/");
            Utils.deleteSubFiles(dir, 0);
            Log.i(LOG_TAG, "DeleteLocalRecordAndPictures: records deleted");
        }
    }

    private void setCallbackUrl(String ezPassPw) {
        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                Constants.SET_CALLBACK_URL + "pass=" + ezPassPw + "&" + "callbackUrl=" + Constants.CALLBACK_URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                // Succeeded, no further action needed
                System.out.println(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "Error setting callback");
                error.printStackTrace();
            }
        });
        queue.add(stringRequest);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // no bind
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Config file does not exist, close the app
        if (!Utils.readConfigFile(getApplicationContext())) {
            System.exit(-1);
        }

        if (intent == null) {
            loadAndStartServer();
            return START_STICKY;
        }

        if (intent.getBooleanExtra("reset", false)) {
            //reset the settings.. reload
            loadAndStartServer();
        } else if (intent.getStringExtra("delete") != null && intent.getStringExtra("delete").equalsIgnoreCase("true")) {
            // need to check the settings now if we need to send up the records then delete or just delete
            if (mailServConfig.SendRecordsAtEndOfDay)
                SendCsv();
            if (mailServConfig.AutoDeleteRecord)
                DeleteLocalRecordAndPictures();
        }
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // Config file does not exist, do not try to restart
        if (!Utils.readConfigFile(getApplicationContext())) {
            return;
        }

        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartServiceIntent);
        } else {
            startService(restartServiceIntent);
        }

        super.onTaskRemoved(rootIntent);
    }

    private void submitOutput(JSONObject json) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String mac = json.getString("mac");
                    String name = json.getString("name");
                    String userId = json.getString("userId");
                    String type = json.getString("type");
                    String mask = json.getString("mask");
                    Double temperature = parseTemperature(json);
                    Date checkTime = parseCheckTime(json);
                    OutputDao.Record.Update record = new OutputDao.Record.Update(mac, name, checkTime, temperature, userId, type, mask, null, null, null, null, null);
                    if (SharedPreferencesController.with(getApplicationContext()).getInt(CURRENT_SCAN_ID) != -1){
                        outputDao.update(record,SharedPreferencesController.with(getApplicationContext()).getInt(CURRENT_SCAN_ID));
                        SharedPreferencesController.with(getApplicationContext()).saveInt(CURRENT_SCAN_ID,-1);
                    }

                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error getting data from JSON before inserting into SQL database: " + e.getMessage());
                }
            }
        }).start();
    }

    private void insertScanRecord(JSONObject json) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String userId = json.getString("userId");
                    OutputDao.Record.Insert record = new OutputDao.Record.Insert(userId);
                    int insert = outputDao.insert(record);
                    SharedPreferencesController.with(getApplicationContext()).saveInt(CURRENT_SCAN_ID,insert);
//                    Future<DaoResult<Boolean>> submit = singleThreadExecutor.submit(() -> outputDao.insert(record));
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error getting data from JSON before inserting into SQL database: " + e.getMessage());
                }
            }
        }).start();
    }

    private Double parseTemperature(JSONObject json) throws JSONException {
        double temperature = json.getDouble("temperature");
        return convertToFahrenheit(temperature);
    }

    private double convertToFahrenheit(double temperature) {
        return (temperature * 9 / 5) + 32;
    }

    private Date parseCheckTime(JSONObject json) throws JSONException {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(json.getLong("checkTime"));
        return cal.getTime();
    }

    @Override
    public void onCreate() {
        loadAndStartServer();
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        SQLHelper.disconnectFromDatabase();
        super.onDestroy();
    }
}
