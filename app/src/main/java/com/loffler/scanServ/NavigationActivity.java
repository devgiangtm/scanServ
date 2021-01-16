package com.loffler.scanServ;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;

import com.loffler.scanServ.cdcsetting.CDCSettingActivity;
import com.loffler.scanServ.dashboard.DashboardActivity;
import com.loffler.scanServ.dashboard.DashboardSettingsActivity;
import com.loffler.scanServ.welcomescreen.WelcomeDetectorActivity;
import com.loffler.scanServ.welcomescreen.WelcomeSettingsActivity;


public class NavigationActivity extends BaseActivity implements Button.OnClickListener {
    private final String LOG_TAG = "NavigationActivity";
    private SharedPreferences prefs;
    private Button sqlButton;
    private Button printButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        sqlButton = findViewById(R.id.sqlSettingsButton);
        Button emailButton = findViewById(R.id.emailSettingsButton);
        printButton = findViewById(R.id.printSettingsButton);
        Button aboutButton = findViewById(R.id.aboutButton);
        Button supportButton = findViewById(R.id.supportButton);
        Button dashboardSettingsButton = findViewById(R.id.dashboardSettingsButton);
        Button cdcSettingsButton = findViewById(R.id.cdcSettingsButton);
        Button welcomeSettingsButton = findViewById(R.id.welcomeSettingsButton);

        prefs = getApplicationContext().getSharedPreferences(Constants.PreferenceName, MODE_PRIVATE);


        // Enable features if the key supports them
        setupButtons(prefs.getInt(Constants.FeatureSetKey, 0));

        emailButton.setOnClickListener(this);
        aboutButton.setOnClickListener(this);
        supportButton.setOnClickListener(this);
        dashboardSettingsButton.setOnClickListener(this);
        cdcSettingsButton.setOnClickListener(this);
        welcomeSettingsButton.setOnClickListener(this);

        Intent servIntent = new Intent(getApplicationContext(), ScanService.class);
        // reset the server if we are coming from the product key page
        if (getIntent().hasExtra("resetserver")) {
            servIntent.putExtra("reset", true);
        } else {
            servIntent.putExtra("reset", false);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(servIntent);
        } else {
            startService(servIntent);
        }
    }

    private void setupButtons(int featureSet) {
        // Enable sql button if it was disabled and our key now includes SQL feature
        if ((featureSet & Utils.FEATURE_SET.SQL.getNumericType()) != 0) {
            sqlButton.setEnabled(true);
            sqlButton.setOnClickListener(this);
        } else {
            sqlButton.setEnabled(false);
        }
        // Enable badge print button if it was disabled and our key now includes badge print feature
        // Also, grandfather old keys in to have badge printing
        if (featureSet == 0 || (featureSet & Utils.FEATURE_SET.BADGE_PRINTING.getNumericType()) != 0) {
            printButton.setEnabled(true);
            printButton.setOnClickListener(this);
        } else {
            printButton.setEnabled(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupButtons(prefs.getInt(Constants.FeatureSetKey, 0));
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.sqlSettingsButton:
                startActivity(new Intent(getApplicationContext(), SQLSettingsActivity.class));
                break;
            case R.id.emailSettingsButton:
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                break;
            case R.id.printSettingsButton:
                startActivity(new Intent(getApplicationContext(), BadgePrintSettingsActivity.class));
                break;
            case R.id.aboutButton:
                startActivity(new Intent(getApplicationContext(), AboutPageActivity.class));
                break;
            case R.id.supportButton:
                startActivity(new Intent(getApplicationContext(), SupportPageActivity.class));
                break;
            case R.id.dashboardSettingsButton:
                startActivity(new Intent(getApplicationContext(), DashboardSettingsActivity.class));
                break;
            case R.id.welcomeSettingsButton:
                startActivity(new Intent(getApplicationContext(), WelcomeSettingsActivity.class));
                break;
            case R.id.cdcSettingsButton:
                startActivity(new Intent(getApplicationContext(), CDCSettingActivity.class));
                break;
            default:
                Log.d(LOG_TAG, "Unrecognized button clicked, ignoring");
        }
    }

    @Override
    public void finish() {
        super.finish();
//        if (!prefs.getBoolean(Constants.DashboardSettingsEnableFeatureKey, false)) {
//            finishAffinity();
//        } else {
//            startActivity(new Intent(getApplicationContext(), DashboardActivity.class));
//        }
        if (prefs.getBoolean(Constants.WELCOME_ENABLE, false)) {
            startActivity(new Intent(getApplicationContext(), WelcomeDetectorActivity.class));
        } else if (prefs.getBoolean(Constants.DashboardSettingsEnableFeatureKey, false)){
            startActivity(new Intent(getApplicationContext(), DashboardActivity.class));
        }

    }
}
