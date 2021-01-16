package com.loffler.scanServ;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.loffler.scanServ.dashboard.DashboardActivity;
import com.loffler.scanServ.welcomescreen.WelcomeDetectorActivity;

public class ProductKeyActivity extends AppCompatActivity implements View.OnClickListener{

    private TextView productKeyView;
    private SharedPreferences prefs;
    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_key);
        Button trialButton = findViewById(R.id.startTrialButton);
        TextView contactInfo = findViewById(R.id.keyContactInfo);

        // Always start config server so we can update license key
        Intent configIntent = new Intent(getApplicationContext(), ConfigService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(configIntent);
        } else {
            startService(configIntent);
        }

        if (configFileExists()) {
            // check if sharedPrefs has already got activationkey
            prefs = getApplicationContext().getSharedPreferences(Constants.PreferenceName, MODE_PRIVATE);
            contactInfo.setText("To obtain a key, please contact " + prefs.getString(Constants.SupportCompanyKey, "your dealer") + "\nEmail: " +
                    prefs.getString(Constants.SupportEmailKey, "") + "\nPhone: " + prefs.getString(Constants.SupportPhoneKey, ""));
            boolean isActivated = prefs.getBoolean(Constants.ProductKeyActivationCompleted, false);
            boolean isOnTrial = Utils.checkTrialFileExists() && Utils.checkTrialValidity(this);

            if (isOnTrial || isActivated) {


//                if (isDashboardFeatureEnabled) {
//                    startActivity(new Intent(getApplicationContext(), DashboardActivity.class));
//                } else  {
//                    startActivity(new Intent(getApplicationContext(), NavigationActivity.class));
//                }
                if (prefs.getBoolean(Constants.WELCOME_ENABLE, false)) {
                    startActivity(new Intent(getApplicationContext(), WelcomeDetectorActivity.class));
                } else if (prefs.getBoolean(Constants.DashboardSettingsEnableFeatureKey, false)){
                    startActivity(new Intent(getApplicationContext(), DashboardActivity.class));
                } else {
                    startActivity(new Intent(getApplicationContext(), NavigationActivity.class));
                }

                finish();
            } else {
                // If the trial file does not exist
                if (!Utils.checkTrialFileExists()) {
                    trialButton.setEnabled(true);
                    trialButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case DialogInterface.BUTTON_POSITIVE:
                                            if (Utils.createTrialFile(ProductKeyActivity.this)) {
                                                Toast.makeText(
                                                        ProductKeyActivity.this,
                                                        "Trial period started! All features have been enabled for 15 days.",
                                                        Toast.LENGTH_LONG
                                                ).show();
                                                startActivity(new Intent(getApplicationContext(), NavigationActivity.class).putExtra("resetserver", true));
                                                finish();
                                            } else {
                                                Toast.makeText(
                                                        ProductKeyActivity.this,
                                                        "Failed to start trial, please contact support.",
                                                        Toast.LENGTH_LONG
                                                ).show();
                                            }
                                            break;

                                        case DialogInterface.BUTTON_NEGATIVE:
                                            // Do nothing
                                            break;
                                    }
                                }
                            };
                            AlertDialog.Builder builder = new AlertDialog.Builder(ProductKeyActivity.this);
                            builder.setMessage(getResources().getString(R.string.trialInfo))
                                    .setPositiveButton("Yes", dialogClickListener)
                                    .setNegativeButton("No", dialogClickListener)
                                    .show();
                        }
                    });
                }

                findViewById(R.id.ActivateButton).setOnClickListener(this);
                productKeyView = findViewById(R.id.productKey);
            }
        }
    }
    @Override
    public void onClick(View v) {
        boolean validKey = Utils.validateKey(productKeyView.getText().toString(), prefs);
        if (validKey) {
            // key is valid, continue to the main navigation page
            startActivity(new Intent(getApplicationContext(), NavigationActivity.class).putExtra("resetserver", true));
            finish();
        }
        else
        {
            // key invalid prompt user with dialog
            new AlertDialog.Builder(this)
                    .setMessage("Invalid key, try again.")
                    .setCancelable(true)
                    .setNegativeButton("Try Again", null)
                    .show();

        }
    }

    @Override
    protected void onResume() {
        if (prefs == null) prefs = getApplicationContext().getSharedPreferences(Constants.PreferenceName, MODE_PRIVATE);
        boolean isActivated = prefs.getBoolean(Constants.ProductKeyActivationCompleted, false);
        boolean isOnTrial = Utils.checkTrialFileExists() && Utils.checkTrialValidity(this);

        // Only continue if we have a config file
        if (configFileExists()) {
            if (isOnTrial || isActivated) {
                startActivity(new Intent(getApplicationContext(), NavigationActivity.class).putExtra("resetserver", true));
                finish();
            }

            if (receiver == null) {
                receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        startActivity(new Intent(getApplicationContext(), NavigationActivity.class).putExtra("resetserver", true));
                        finish();
                    }
                };

                registerReceiver(receiver, new IntentFilter(Constants.CLOSE_ACTIVATION_PAGE));
            }
        }

        super.onResume();
    }

    @Override
    protected void onPause() {
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
        super.onPause();
    }

    private boolean configFileExists() {
        if (!Utils.readConfigFile(getApplicationContext())) {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        System.exit(-1);
                    }
                }
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(ProductKeyActivity.this);
            builder.setMessage("Cannot start scanserv without the config file located at " + Constants.SCANSERV_CONFIG_FILE)
                    .setPositiveButton("OK", dialogClickListener)
                    .show();
            return false;
        }

        return true;
    }
}
