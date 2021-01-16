package com.loffler.scanServ;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;

public class MainActivity extends BaseActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private final String LOG_TAG = "MainActivity";
    private EditText smtpServAddressView;
    private EditText smtpUserAccView;
    private EditText smtpUserPwView;
    private EditText ezPassPwView;
    private EditText deviceIdView;
    private CompoundButton sendNormalTempCheck;
    private CompoundButton sendHighTempCheck;
    private CompoundButton sendPicCheck;
    private CompoundButton sendTempReadingCheck;
    private CompoundButton sendNameCheck;
    private CompoundButton scanDelayCheck;
    private EditText scanDelayEditText;
    private TextView serverStatus;
    private EditText smtpPortView;
    private EditText receivingEmailAddressView;
    private Spinner authSpinner;
    private SharedPreferences sharedPreferences;
    private EditText smtpFromAddressView;
    private CompoundButton autoDeleteCheck;
    private CompoundButton sendRecordsEODView;
    private ConstraintLayout scanDelayLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPreferences = getSharedPreferences(Constants.PreferenceName, Context.MODE_PRIVATE);

        smtpServAddressView = findViewById(R.id.SMTPServerAddress);
        smtpUserAccView = findViewById(R.id.SMTPToEmailAddress);
        smtpUserPwView = findViewById(R.id.SMTPUserPW);
        Button saveButton = findViewById(R.id.SaveBtn);
        sendNormalTempCheck = findViewById(R.id.SendNormalTempCheck);
        sendHighTempCheck = findViewById(R.id.SendHighTempCheck);
        sendPicCheck = findViewById(R.id.SendPicCheck);
        sendTempReadingCheck = findViewById(R.id.IncludeTempeReadingCheck);
        sendNameCheck = findViewById(R.id.sendNameCheck);
        scanDelayCheck = findViewById(R.id.scanDelayCheck);
        serverStatus = findViewById(R.id.ServerStatus);
        ezPassPwView = findViewById(R.id.EZPw);
        smtpPortView = findViewById(R.id.SMTPPort);
        receivingEmailAddressView = findViewById(R.id.ReceivingEmailAddress);
        authSpinner = findViewById(R.id.AuthSpinner);
        deviceIdView = findViewById(R.id.DeviceId);
        smtpFromAddressView = findViewById(R.id.SMTPFromAddress);
        autoDeleteCheck = findViewById(R.id.autoDeleteCheck);
        sendRecordsEODView = findViewById(R.id.sendCsvCheck);
        scanDelayEditText = findViewById(R.id.scanDelayEditText);
        scanDelayLayout = findViewById(R.id.scanDelayLayout);

        // Check if we have settings already, if so put them into the text and checkbox fields. Then start the server.
        String smptServAddres = sharedPreferences.getString(Constants.SMTPServAddressKey, null);
        String smtpUsername = sharedPreferences.getString(Constants.SMTPUsernameKey, null);
        String smtpPw = sharedPreferences.getString(Constants.SMTPPWKey, null);
        final String ezPassPw = sharedPreferences.getString(Constants.EZPassPwKey, null);
        int port = sharedPreferences.getInt(Constants.SmtpSSLPortKey, -1);
        String receivingEmailAddress = sharedPreferences.getString(Constants.ReceivingEmailKey, null);
        String authType = sharedPreferences.getString(Constants.AuthTypeKey, null);
        String deviceId = sharedPreferences.getString(Constants.DeviceIdKey, null);
        String fromEmailAddress = sharedPreferences.getString(Constants.SMTPFromAddressKey, null);
        int scanDelay = sharedPreferences.getInt(Constants.ScanDelay, 5);

        int spinnerDefaultPos;

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.AuthTypes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        authSpinner.setAdapter(adapter);
        saveButton.setOnClickListener(this);

        if (authType == null || authType.isEmpty()) {
            // default to SSL
            spinnerDefaultPos = adapter.getPosition(Authentication.SSL.toString());
        } else {
            // populate what they had
            spinnerDefaultPos = adapter.getPosition(authType);
        }
        authSpinner.setSelection(spinnerDefaultPos);

        boolean sendNormalTemp = sharedPreferences.getBoolean(Constants.SendNormalTempKey, false);
        boolean sendHighTemp = sharedPreferences.getBoolean(Constants.SendHighTempKey, false);
        boolean sendPic = sharedPreferences.getBoolean(Constants.SendPicKey, false);
        boolean sendTempReading = sharedPreferences.getBoolean(Constants.SendTempReadingKey, false);
        boolean sendName = sharedPreferences.getBoolean(Constants.SendNameKey, false);
        boolean autoDeleteRecord = sharedPreferences.getBoolean(Constants.AutoDeleteRecordKey, false);
        boolean sendRecordEOD = sharedPreferences.getBoolean(Constants.SendRecordEODKey, false);
        boolean scanDelayEnabled = sharedPreferences.getBoolean(Constants.ScanDelayEnabled,false);


        smtpServAddressView.setText(smptServAddres);
        smtpUserAccView.setText(smtpUsername);
        smtpUserPwView.setText(smtpPw);
        ezPassPwView.setText(ezPassPw);
        deviceIdView.setText(deviceId);
        smtpFromAddressView.setText(fromEmailAddress);
        receivingEmailAddressView.setText(receivingEmailAddress);


        // don't set a value for port when no value is there
        if (port != -1) smtpPortView.setText(String.valueOf(port));

        // Set boxes to checked, if needed
        sendNormalTempCheck.setChecked(sendNormalTemp);
        sendHighTempCheck.setChecked(sendHighTemp);
        sendPicCheck.setChecked(sendPic);
        sendTempReadingCheck.setChecked(sendTempReading);
        sendNameCheck.setChecked(sendName);
        autoDeleteCheck.setChecked(autoDeleteRecord);
        sendRecordsEODView.setChecked(sendRecordEOD);
        // Show/hide scan delay edit text if the setting is enabled/disabled
        if (!scanDelayEnabled) {
            scanDelayCheck.setChecked(false);
            scanDelayLayout.setVisibility(View.GONE);
        } else {
            scanDelayCheck.setChecked(true);
            scanDelayLayout.setVisibility(View.VISIBLE);
            scanDelayEditText.setText(String.valueOf(scanDelay));
        }


        // Listen for changes in toggle status for settings
        sendNormalTempCheck.setOnCheckedChangeListener(this);
        sendHighTempCheck.setOnCheckedChangeListener(this);
        sendPicCheck.setOnCheckedChangeListener(this);
        sendTempReadingCheck.setOnCheckedChangeListener(this);
        sendNameCheck.setOnCheckedChangeListener(this);
        autoDeleteCheck.setOnCheckedChangeListener(this);
        sendRecordsEODView.setOnCheckedChangeListener(this);
        scanDelayCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean enabled) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                // Default scan delay is 5 seconds
                int delay = sharedPreferences.getInt(Constants.ScanDelay, 5);
                if (enabled) {
                    scanDelayLayout.setVisibility(View.VISIBLE);
                    scanDelayEditText.setText(String.valueOf(delay));
                } else {
                    scanDelayLayout.setVisibility(View.GONE);
                }
                editor.putInt(Constants.ScanDelay, delay);
                editor.putBoolean(Constants.ScanDelayEnabled, enabled);
                editor.commit();
            }
        });

        findViewById(R.id.testMailButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // save what settings are there first then start server and proceed to grab settings
                // and sending test email
                SaveSettingsStartServ();

                // send the test mail
                String smptServAddress = sharedPreferences.getString(Constants.SMTPServAddressKey, "");
                String smtpUsername = sharedPreferences.getString(Constants.SMTPUsernameKey, "");
                String smtpPw = sharedPreferences.getString(Constants.SMTPPWKey, "");
                String ezPassPw = sharedPreferences.getString(Constants.EZPassPwKey, "");
                String deviceId = sharedPreferences.getString(Constants.DeviceIdKey, "");
                int sslPort = sharedPreferences.getInt(Constants.SmtpSSLPortKey, 0);
                String receivingEmail = sharedPreferences.getString(Constants.ReceivingEmailKey, "");
                Authentication authType = Authentication.valueOf(sharedPreferences.getString(Constants.AuthTypeKey, "SSL"));
                String fromAddress = sharedPreferences.getString(Constants.SMTPFromAddressKey, "");

                boolean sendNormalTemp = sharedPreferences.getBoolean(Constants.SendNormalTempKey, true);
                boolean sendHighTemp = sharedPreferences.getBoolean(Constants.SendHighTempKey, true);
                boolean sendPic = sharedPreferences.getBoolean(Constants.SendPicKey, false);
                boolean sendTempReading = sharedPreferences.getBoolean(Constants.SendTempReadingKey, false);
                boolean sendName = sharedPreferences.getBoolean(Constants.SendNameKey, false);
                boolean autoDeleteRecord = sharedPreferences.getBoolean(Constants.AutoDeleteRecordKey, false);
                boolean sendRecordEOD = sharedPreferences.getBoolean(Constants.SendRecordEODKey, false);

                MailerConfig mailServConfig = new MailerConfig(0, sendHighTemp, sendNormalTemp,
                        sendPic, sendTempReading, sendName, smptServAddress, smtpUsername, smtpPw, ezPassPw, sslPort,
                        receivingEmail, authType, deviceId, fromAddress, true, autoDeleteRecord, sendRecordEOD);

                MailSender ms = new MailSender();
                ms.finishDelegate = new MailSender.AsyncResponseDelegate() {
                    @Override
                    public void finish(MailSenderResponse resp) {
                        Toast.makeText(MainActivity.this, "Succeeded: " + resp.Success + "\nError message: " + resp.ErrorMessage,
                                Toast.LENGTH_LONG).show();
                    }
                };

                ms.execute("", mailServConfig);
            }
        });
    }

    @Override
    public void onClick(View v) {
        SaveSettingsStartServ();
        Toast.makeText(
                MainActivity.this,
                "Settings Saved",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void SaveSettingsStartServ() {
        // save to shared preferences and tell service to start
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(Constants.SMTPServAddressKey, smtpServAddressView.getText().toString());
        editor.putString(Constants.SMTPUsernameKey, smtpUserAccView.getText().toString());
        editor.putString(Constants.SMTPPWKey, smtpUserPwView.getText().toString());
        editor.putString(Constants.EZPassPwKey, ezPassPwView.getText().toString());
        editor.putString(Constants.ReceivingEmailKey, receivingEmailAddressView.getText().toString());
        editor.putString(Constants.DeviceIdKey, deviceIdView.getText().toString());
        editor.putString(Constants.SMTPFromAddressKey, smtpFromAddressView.getText().toString());
        String stringPort = smtpPortView.getText().toString();

        if (stringPort.isEmpty()) {
            stringPort = "-1";
        }

        editor.putInt(Constants.SmtpSSLPortKey, Integer.parseInt(stringPort));
        editor.putBoolean(Constants.SendNormalTempKey, sendNormalTempCheck.isChecked());
        editor.putBoolean(Constants.SendHighTempKey, sendHighTempCheck.isChecked());
        editor.putBoolean(Constants.SendPicKey, sendPicCheck.isChecked());
        editor.putBoolean(Constants.SendTempReadingKey, sendTempReadingCheck.isChecked());
        editor.putBoolean(Constants.SendNameKey, sendNameCheck.isChecked());
        editor.putString(Constants.AuthTypeKey, authSpinner.getSelectedItem().toString());
        editor.putBoolean(Constants.AutoDeleteRecordKey, autoDeleteCheck.isChecked());
        editor.putBoolean(Constants.SendRecordEODKey, sendRecordsEODView.isChecked());
        if (scanDelayCheck.isChecked()) {
            editor.putInt(Constants.ScanDelay, Integer.parseInt(scanDelayEditText.getText().toString()));
        }
        editor.putBoolean(Constants.ScanDelayEnabled, scanDelayCheck.isChecked());

        // commit immediately, do not apply for later
        editor.commit();

        StartServer(true);
    }

    private void StartServer(boolean reset) {
        String smtpServerAddress = smtpServAddressView.getText().toString();
        String ezPassPw = ezPassPwView.getText().toString();
        String recvEmail = receivingEmailAddressView.getText().toString();
        String stringPort = smtpPortView.getText().toString();
        String fromAddress = smtpFromAddressView.getText().toString();

        if (stringPort.isEmpty()) {
            stringPort = "-1";
        }

        int port = Integer.parseInt(stringPort);

        if (smtpServerAddress.isEmpty() || ezPassPw.isEmpty() || recvEmail.isEmpty() || port == -1 || fromAddress.isEmpty()) {

            // Notification should be printed to user?
            serverStatus.setText("Email Server is not running, Please enter all info above and save");
        } else {
            // set text that server is running
            serverStatus.setText("Server is Running...");
        }

        Intent servIntent = new Intent(getApplicationContext(), ScanService.class);
        servIntent.putExtra("reset", reset);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(servIntent);
        } else {
            startService(servIntent);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        String prefName;
        switch (compoundButton.getId()) {
            case R.id.SendNormalTempCheck:
                prefName = Constants.SendNormalTempKey;
                break;
            case R.id.SendHighTempCheck:
                prefName = Constants.SendHighTempKey;
                break;
            case R.id.SendPicCheck:
                prefName = Constants.SendPicKey;
                break;
            case R.id.IncludeTempeReadingCheck:
                prefName = Constants.SendTempReadingKey;
                break;
            case R.id.sendNameCheck:
                prefName = Constants.SendNameKey;
                break;
            case R.id.autoDeleteCheck:
                prefName = Constants.AutoDeleteRecordKey;
                break;
            case R.id.sendCsvCheck:
                prefName = Constants.SendRecordEODKey;
                break;
            default:
                Log.e(LOG_TAG, "Unknown switch ID");
                return;
        }
        updatePref(prefName, isChecked);
    }

    private void updatePref(String prefName, boolean newVal) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(prefName, newVal);
        editor.commit();
        StartServer(true);
    }
}


