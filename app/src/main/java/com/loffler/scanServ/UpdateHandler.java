package com.loffler.scanServ;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.smdt.SmdtManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.error.VolleyError;
import com.android.volley.request.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Vector;

import static android.content.Context.MODE_PRIVATE;

public class UpdateHandler extends BroadcastReceiver {
    private final String LOG_TAG = "UpdateHandler";

    private Context context = null;
    SmdtManager smdtManager = null;
    DownloadFiles fileDownloader = new DownloadFiles();

    private static String currentPackage = "";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null || !action.equalsIgnoreCase(Intent.ACTION_PACKAGE_ADDED)) {
            return;
        }
        String pkgName = intent.getDataString().replace("package:", "");
        if (!pkgName.equals(currentPackage)) {
            // Not the package we just installed, ignore
            return;
        }

        // Update succeeded, continue updates
        currentPackage = "";
        checkForUpdates(context);
    }

    public void checkForUpdates(Context ctx) {
        Log.i(LOG_TAG, "Checking for updates in manifest: " + Constants.MANIFEST_LOCATION);
        context = ctx;
        smdtManager = new SmdtManager(context);

        String manifestJson = readManifest();
        if (manifestJson != null) {
            parseManifest(manifestJson);
        }
    }

    private String readManifest() {
        File file = new File(Constants.MANIFEST_LOCATION);
        StringBuilder json = new StringBuilder();
        if (file.exists() && file.canRead()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;

                while ((line = br.readLine()) != null) {
                    json.append(line);
                    json.append('\n');
                }
                br.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Failed to read manifest: " + e.getMessage());
                return null;
            }
        }
        return json.toString();
    }

    private void parseManifest(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            Iterator<String> keys = json.keys();
            int startIndex = readProgressFile();
            int i = 0;
            while (keys.hasNext()) {
                if (i < startIndex) {
                    i++;
                    keys.next();
                    continue;
                }
                String key = keys.next();
                JSONObject subKeys = json.getJSONObject(key);

                // Keep track of our progress through the manifest so we can resume after installs complete
                createProgressFile(++i);

                String filePath = subKeys.getString("path");
                String pkgName = "";
                if (subKeys.has("packageName")) pkgName = subKeys.getString("packageName");
                String version = subKeys.getString("version");
                String updateType = subKeys.getString("updateType");

                Log.i(LOG_TAG, "Update type: " + updateType + "; filepath: " + filePath + "; version: " + version + "; package name: " + pkgName);
                switch (updateType.toUpperCase()) {
                    case "APK":
                        if (installPackage(filePath, version, pkgName) == 0) {
                            // Return here so we wait for install to finish
                            return;
                        }
                        break;
                    case "ZIP":
                        try {
                            installZip(filePath, version);
                        } catch (IOException e) {
                            Log.e(LOG_TAG, "Failed to update smdt: " + e.getMessage());
                        }
                        break;
                    case "SELF":
                        if (installSelf(filePath, version) == 0) {
                            // Return here so we wait for install to finish
                            return;
                        }
                        break;

                    default:
                        Log.w(LOG_TAG, "Unknown update type " + updateType + ", skipping" + subKeys);
                }
            }

            // Cleanup files
            removeFiles();
            Log.i(LOG_TAG, "Finished updates.");
            Toast.makeText(context, "Updates finished.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to parse JSON manifest: " + e.getMessage());
        }
    }

    private void removeFiles() {
        // Removes everything in updates folder
        Utils.deleteSubFiles(new File(Constants.UPDATES_FOLDER_LOCATION), 0);
    }

    private void createProgressFile(int progressIndex) {
        try {
            File progressFile = new File(Constants.PROGRESS_FILE_LOCATION);
            FileOutputStream stream = new FileOutputStream(progressFile);
            stream.write(String.valueOf(progressIndex).getBytes());
            stream.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to create progress file: " + e.getMessage());
        }
    }

    // Read the value from the progress file, if any, to know where to continue
    private int readProgressFile() {
        File progressFile = new File(Constants.PROGRESS_FILE_LOCATION);

        // If the file doesn't exist, start at beginning of update manifest
        if (!progressFile.exists()) {
            return 0;
        }

        int progressIndex = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader(progressFile));
            String line;

            while ((line = br.readLine()) != null) {
                progressIndex = Integer.parseInt(line);
            }
            br.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to read install_progress.txt file: " + e.getMessage());
            return 0;
        }

        return progressIndex;
    }

    public void checkForOTAUpdates(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(Constants.PreferenceName, MODE_PRIVATE);
        int featureSet = prefs.getInt(Constants.FeatureSetKey, 0);
        if ((featureSet & Utils.FEATURE_SET.EXTENDED.getNumericType()) == 0) {
            // Key does not support extended feature set, do not perform OTA update
            return;
        }
        context = ctx;
        Utils.deleteSubFiles(new File(Constants.UPDATES_FOLDER_LOCATION), 0);
        File downloadDir = new File(Constants.UPDATES_FOLDER_LOCATION + "files/");
        if (!downloadDir.exists() && !downloadDir.mkdirs()) {
            Log.e(LOG_TAG, "Failed to create " + Constants.UPDATES_FOLDER_LOCATION + "files/" + " dir for downloads");
            return;
        }

        String otaUrl = prefs.getString(Constants.UpdateUrlKey, "");
        getOTAManifest(otaUrl);
    }

    private void getOTAManifest(String url) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    Log.i(LOG_TAG, "getOTAManifest onResponse: OTA manifest file received" + response);
                    JSONObject reply = new JSONObject(response);
                    String sasToken = reply.getString("sastoken");
                    String manifest = reply.getString("manifest");
                    String containerURI = reply.getString("containeruri");

                    if (createManifestFile(manifest)) {
                        // Download all files before updating
                        downloadFilesFromManifest(manifest, sasToken, containerURI);
                    }
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Failed to parse OTA manifest: " + e.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "Error receiving OTA manifest information azure function: " + error.getMessage());
            }
        });
        RequestQueue queue = Volley.newRequestQueue(context);
        queue.add(stringRequest);
    }

    private void downloadFilesFromManifest(String manifest, String sasToken, String containerURI) {
        Vector<DownloadData> filesToDownload = new Vector<>();
        try {
            JSONObject json = new JSONObject(manifest);
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject subKeys = json.getJSONObject(key);
                String filePath = subKeys.getString("path");

                DownloadData temp = new DownloadData(filePath, new URL(containerURI + "/" + filePath + sasToken));

                filesToDownload.add(temp);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        fileDownloader.execute(filesToDownload);
    }

    private boolean createManifestFile(String jsonString){
        try {
            FileOutputStream fos = new FileOutputStream(new File(Constants.UPDATES_FOLDER_LOCATION + "update_manifest.json"));
            if (jsonString != null) {
                fos.write(jsonString.getBytes());
            }
            fos.close();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to create manifest file: " + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Background Async Task to download files
     * */
    class DownloadFiles extends AsyncTask<Vector<DownloadData>, String, Boolean> {
        private final String LOG_TAG = "DownloadFiles";
        @SafeVarargs
        @Override
        protected final Boolean doInBackground(Vector<DownloadData>... downloads) {
            int count;
            try {
                for (DownloadData downloadData: downloads[0]) {
                    URLConnection connection = downloadData.url.openConnection();
                    connection.connect();

                    // Download the file
                    InputStream input = new BufferedInputStream(downloadData.url.openStream(),
                            8192);

                    // Output stream
                    OutputStream output = new FileOutputStream(Constants.UPDATES_FOLDER_LOCATION + downloadData.path);

                    byte[] data = new byte[1024];

                    while ((count = input.read(data)) != -1) {
                        // Writing data to file
                        output.write(data, 0, count);
                    }


                    output.flush();
                    output.close();
                    input.close();
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error downloading files: " + e.getMessage());
                return false;
            }

            return true;
        }

        /**
         * After completing background task
         * **/
        @Override
        protected void onPostExecute(Boolean success) {
            if (!success) {
                Log.e(LOG_TAG, "Failed to download update files.");
                return;
            }
            new UpdateHandler().checkForUpdates(context);
        }
    }

    private int installPackage(String apkLocation, String version, String pkgName) {
        Version current = new Version(getCurrentVersion(pkgName));
        Version manifestVersion = new Version(version);

        if (current.compareTo(manifestVersion) >= 0) {
            Log.i(LOG_TAG, "Skipping install: Version installed for " + pkgName + " matches or exceeds version in manifest");
            return -1;
        }
        if (!new File(Constants.UPDATES_FOLDER_LOCATION + apkLocation).exists()) {
            Log.e(LOG_TAG, "APK not found: " + (Constants.UPDATES_FOLDER_LOCATION + apkLocation));
            return -1;
        }
        Log.i(LOG_TAG, "Installing apk: " + (Constants.UPDATES_FOLDER_LOCATION + apkLocation));
        currentPackage = pkgName;
        smdtManager.smdtSilentInstall((Constants.UPDATES_FOLDER_LOCATION + apkLocation), context);

        return 0;
    }

    private void installZip(String zipLocation, String version) throws IOException {
        Version current = new Version(smdtManager.smdtVersion());
        Version manifestVersion = new Version(version);
        if (current.compareTo(manifestVersion) >= 0) {
            Log.i(LOG_TAG, "Skipping smdt install: Version installed matches or exceeds version in manifest");
            return;
        }

        File zip = new File(Constants.UPDATES_FOLDER_LOCATION + zipLocation);
        if (!zip.exists()) {
            Log.e(LOG_TAG, "Zip not found: " + (Constants.UPDATES_FOLDER_LOCATION + zipLocation));
            return;
        }
        smdtManager.smdtInstallPackage(context, zip);
    }

    private int installSelf(String apkLocation, String version) {
        String currentVersion = getCurrentVersion(context.getPackageName());
        Version current = new Version(currentVersion);
        Version manifestVersion = new Version(version);
        if (current.compareTo(manifestVersion) >= 0) {
            Log.i(LOG_TAG, "Skipping selfScan install: Version installed matches or exceeds version in manifest");
            return -1;
        }

        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, BootStartupReceiver.class);
        intent.setAction(Constants.REBOOT_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 50, intent, 0);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.SECOND, 30);
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);

        smdtManager.execSuCmd("pm install -r " + (Constants.UPDATES_FOLDER_LOCATION + apkLocation));

        return 0;
    }

    private String getCurrentVersion(String pkgName) {
        PackageManager pm = context.getPackageManager();
        String currentVersion = "NONE";
        try {
            currentVersion = pm.getPackageInfo(pkgName, 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {}

        return currentVersion;
    }
}
