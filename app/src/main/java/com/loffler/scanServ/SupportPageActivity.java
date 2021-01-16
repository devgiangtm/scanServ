package com.loffler.scanServ;

import android.app.smdt.SmdtManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.error.VolleyError;
import com.android.volley.request.SimpleMultiPartRequest;
import com.android.volley.request.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;

public class SupportPageActivity extends BaseActivity {
    private static final String LOG_TAG = "SupportPageActivity";
    private SharedPreferences sharedPreferences;
    private EditText newFeatureSetValueEditText;
    private Button sendLogsButton;
    private Button checkUpdatesButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support_page);
        sharedPreferences = getSharedPreferences(Constants.PreferenceName, Context.MODE_PRIVATE);
        sendLogsButton = findViewById(R.id.sendLogsButton);
        checkUpdatesButton = findViewById(R.id.checkForUpdates);
        Button updateFeatureSetButton = findViewById(R.id.updateFeatureSetButton);
        newFeatureSetValueEditText = findViewById(R.id.featureSetEditText);
        TextView supportInfo = findViewById(R.id.supportTextView);

        supportInfo.setText("For support, contact " + sharedPreferences.getString(Constants.SupportCompanyKey, "your dealer")
                + " at: " + sharedPreferences.getString(Constants.SupportEmailKey, ""));

        setupButtons();

        if (Utils.checkTrialFileExists() && Utils.checkTrialValidity(this)) {
            // Disable the ability to change feature set key when on trial (they need to get the whole key)
            updateFeatureSetButton.setEnabled(false);
            updateFeatureSetButton.setText("Not available during trial");
        } else {
            updateFeatureSetButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    checkFeatureSetKey();
                }
            });
        }
    }

    private void setupButtons() {
        final String ezPassPw = sharedPreferences.getString(Constants.EZPassPwKey, "");
        int featureSet = sharedPreferences.getInt(Constants.FeatureSetKey, 0);
        if ((featureSet & Utils.FEATURE_SET.EXTENDED.getNumericType()) == 0) {
            return;
        }
        sendLogsButton.setEnabled(true);
        checkUpdatesButton.setEnabled(true);
        sendLogsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SmdtManager manager = SmdtManager.create(getApplicationContext());
                // Grab the last 30 minutes of logs from logcat
                String time = new SimpleDateFormat("MM-dd kk:mm:ss.SSS").format(System.currentTimeMillis() - (1000 * 60 * 30));

                manager.execSuCmd("rm " + Constants.LOG_DUMP_LOCATION + "; logcat -t '" + time + "' -f " + Constants.LOG_DUMP_LOCATION);
                // Copy files to root of flash drive, if any
                manager.execSuCmd("cp " + Constants.LOG_DUMP_LOCATION + " /storage/usbport0/");

                // Send logs to Azure to be emailed to support
                sendLogs(ezPassPw);
            }
        });

        checkUpdatesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(
                        SupportPageActivity.this,
                        "Checking for updates...",
                        Toast.LENGTH_LONG
                ).show();
                new UpdateHandler().checkForOTAUpdates(getApplicationContext());
            }
        });
    }

    private void checkFeatureSetKey() {
        String featureSetKey = newFeatureSetValueEditText.getText().toString();
        String macKey = Utils.getWifiMacAddressKeyCheck();
        if (Utils.validateKey(macKey + featureSetKey, sharedPreferences)) {
            // New feature set key is legit
            Toast.makeText(
                    SupportPageActivity.this,
                    "Feature set updated",
                    Toast.LENGTH_LONG
            ).show();
            setupButtons();
        } else {
            // New feature set key is bad
            Toast.makeText(
                    SupportPageActivity.this,
                    "Feature set key is not valid, try again.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void sendLogs(String ezPassPw) {
        // Get company name from EZ-pass, once that is received send logs from the Response Listener
        getCompanyName(ezPassPw);
    }

    private void getCompanyName(String ezPassPw) {
        final String[] companyName = {""};
        StringRequest stringRequest = new StringRequest(Request.Method.POST, Constants.GET_CONFIG_URL + "pass=" + ezPassPw, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    Log.i(LOG_TAG, "onResponse: EZ-Pass Config information received: " + response);
                    JSONObject settingsObject = new JSONObject(new JSONObject(response).getString("data"));
                    companyName[0] = settingsObject.getString("companyName");
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Failed to parse EZ-Pass config JSON: ");
                    e.printStackTrace();
                } finally {
                    // Attempt to send logs even if we failed to get the company name
                    sendLogsToAzure(companyName[0]);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "Error receiving config information from EZ-Pass http server");
                error.printStackTrace();
                // Attempt to send logs even if we failed to get the company name
                sendLogsToAzure(companyName[0]);
            }
        });
        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
        queue.add(stringRequest);
    }

    private void sendLogsToAzure(String companyName) {
        String sendLogsUrl = sharedPreferences.getString(Constants.SendLogsUrlKey, "");
        SimpleMultiPartRequest request = new SimpleMultiPartRequest(Request.Method.POST, sendLogsUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.i(LOG_TAG, "Sent logs to azure, response: " + response);
                        Toast.makeText(
                                SupportPageActivity.this,
                                "A log file of this Kiosk has been sent to your service provider, please contact them for additional support.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "Error uploading file to azure: " + error.toString());
                Toast.makeText(
                        SupportPageActivity.this,
                        "Failed to send log file.",
                        Toast.LENGTH_LONG
                ).show();
            }
        });
        request.addStringParam("mac", Utils.getEthMacAddress());
        request.addStringParam("deviceId", sharedPreferences.getString(Constants.DeviceIdKey, ""));
        request.addStringParam("companyName", companyName);
        request.addFile("logfile", Constants.LOG_DUMP_LOCATION);
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(request);
    }
}