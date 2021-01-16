package com.loffler.scanServ;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;

public class ConfigService extends Service {
    private AsyncHttpServer configServer;
    private final int configServerPort = 3000;
    private SharedPreferences prefs;

    private void setupConfigServer() {
        prefs = getApplicationContext().getSharedPreferences(Constants.PreferenceName, MODE_PRIVATE);

        // Server already running
        if (configServer != null) return;

        setupNotificationSettings();

        configServer = new AsyncHttpServer();

        configServer.post("/setconfigfile", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                try {
                    File dir = new File(Constants.SCANSERV_FOLDER);
                    if (!dir.exists()) dir.mkdirs();
                    File file = new File(Constants.SCANSERV_CONFIG_FILE);
                    FileOutputStream outputStream = new FileOutputStream(file);
                    outputStream.write(request.getBody().get().toString().getBytes());
                    outputStream.flush();
                    outputStream.close();
                    if (Utils.readConfigFile(getApplicationContext())) {
                        response.send("The config file has been updated");
                    } else {
                        response.send("The config file was updated, but appears to be malformed; verify the contents and try again.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    response.send("Failed to update config file: " + e);
                }
            }
        });


        configServer.get("/forceupdatecheck", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                response.send("The device will now check for updates");
                new UpdateHandler().checkForOTAUpdates(ConfigService.this);
            }
        });

        configServer.post("/updatelicensekey", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                try {
                    JSONObject reqJson = new JSONObject(request.getBody().get().toString());
                    Iterator<String> keys = reqJson.keys();
                    String key = keys.next();
                    if (!key.toLowerCase().equals("licensekey")) {
                        response.send("Error: Expected jsonkey 'licensekey' in body");
                        return;
                    }

                    String licKey = reqJson.getString(key);
                    if (!Utils.validateKey(licKey, prefs)) {
                        response.send("Invalid license key supplied; please check your key and try again");
                    } else {
                        response.send("Successfully updated license key");
                        // Valid key, close activation page (if it is in the foreground) and then start ScanService
                        sendBroadcast(new Intent(Constants.CLOSE_ACTIVATION_PAGE));

                        Intent servIntent = new Intent(getApplicationContext(), ScanService.class);
                        servIntent.putExtra("reset", true);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(servIntent);
                        } else {
                            startService(servIntent);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    response.send("Failed to update license key: " + e.getMessage());
                }
            }
        });

        configServer.post("/updateconfig", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                try {
                    JSONObject reqJson = new JSONObject(request.getBody().get().toString());
                    SharedPreferences.Editor editor = prefs.edit();
                    Iterator<String> keys = reqJson.keys();
                    StringBuilder failedKeys = new StringBuilder();

                    loop: while (keys.hasNext()) {
                        String key = keys.next();
                        String lowercaseKey = key.toLowerCase();
                        // If the key is not "licensekey" or any of the keys in the mapping, add it to a list
                        // of keys that were invalid to notify the user
                        if(!Constants.jsonKeyMapping.containsKey(lowercaseKey) && !lowercaseKey.equals("licensekey")) {
                            failedKeys.append(key).append(", ");
                            continue;
                        }
                        int featureSet = prefs.getInt(Constants.FeatureSetKey, 0);
                        if (featureSet == 0) {
                            response.send("Settings configuration is unavailable until a license key is supplied.");
                            return;
                        }

                        if (lowercaseKey.contains("print") && (featureSet & Utils.FEATURE_SET.BADGE_PRINTING.getNumericType()) == 0) {
                            // Do not allow a user to update badge settings if they do not have that feature
                            failedKeys.append(key).append(", ");
                            continue;
                        } else if (lowercaseKey.contains("sql") && (featureSet & Utils.FEATURE_SET.SQL.getNumericType()) == 0) {
                            // Do not allow a user to update sql settings if they do not have that feature
                            failedKeys.append(key).append(", ");
                            continue;
                        }

                        // Make sure supplied data is valid
                        if (!validateKeyData(key, reqJson)) {
                            failedKeys.append(key).append(", ");
                            continue;
                        }

                        Constants.PrefInfo prefInfo = Constants.jsonKeyMapping.get(lowercaseKey);

                        switch (prefInfo.preferenceType) {
                            case INT:
                                editor.putInt(prefInfo.preferenceKey, reqJson.getInt(key));
                                break;
                            case STRING:
                                editor.putString(prefInfo.preferenceKey, reqJson.getString(key));
                                break;
                            case BOOLEAN:
                                editor.putBoolean(prefInfo.preferenceKey, reqJson.getBoolean(key));
                                break;
                            default:
                                break;
                        }
                    }
                    editor.apply();
                    if (!failedKeys.toString().isEmpty()) {
                        response.send("Some settings updated, but these keys were unrecognized, their data was malformed, or your license key does not support them: " + failedKeys.toString());
                    } else {
                        response.send("Settings updated!");
                    }
                    Intent servIntent = new Intent(getApplicationContext(), ScanService.class);
                    servIntent.putExtra("reset", true);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(servIntent);
                    } else {
                        startService(servIntent);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    response.send("Failed to update configuration: " + e.getMessage());
                }
            }
        });

        configServer.listen(configServerPort);
    }

    // If the key corresponds to a preference that has a select few valid values, make sure the value
    // provided is one that is recognized
    private boolean validateKeyData(String key, JSONObject json) {
        try {
            switch (key.toLowerCase()) {
                case "authtype":
                    switch (json.getString(key)) {
                        case "SSL":
                        case "TLS":
                        case "None":
                            return true;
                        default:
                            return false;
                    }
                case "checkmarkprinttype":
                    return ZPLValues.CheckmarkType.valueOf(json.getInt(key)) != null;
                default:
                    return true;
            }
        } catch(Exception e){
            return false;
        }
    }

    private void setupNotificationSettings() {
        String notificationChannelId = "95";
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(this, notificationChannelId)
                .setAutoCancel(false)
                .setPriority(Notification.FLAG_HIGH_PRIORITY)
                .setOngoing(true)
                .setContentTitle("ScanServ config service is running...")
                .setTicker("ScanServ config service is running...")
                .setSmallIcon(R.drawable.ic_scan_serv);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(notificationChannelId, "Config Channel", NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("ScanServ Config Service Notification Channel");

            notificationManager.createNotificationChannel(notificationChannel);
            notifBuilder.setChannelId(notificationChannelId);
            startForeground(2, notifBuilder.build());
        } else {
            notifBuilder.setChannelId(notificationChannelId);
            notificationManager.notify(2, notifBuilder.build());
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        setupConfigServer();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // no bind
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartServiceIntent);
        } else {
            startService(restartServiceIntent);
        }

        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onCreate() {
        setupConfigServer();

        super.onCreate();
    }
}
