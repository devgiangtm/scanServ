package com.loffler.scanServ;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

public class BadgePrintSettingsActivity extends BaseActivity implements CompoundButton.OnCheckedChangeListener {
    private SharedPreferences sharedPreferences;
    private String LOG_TAG = "BadgePrintSettingsActivity";
    private EditText companyName;
    private TextView printerStatus;
    private ConstraintLayout badgePrintLayout;
    private EditText infoLine1;
    private EditText infoLine2;
    private EditText infoLine3;
    private EditText infoLine4;
    private ImageView exampleBadgeImage;
    private BadgePrinter badgePrinter = new BadgePrinter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print_settings);
        sharedPreferences = getSharedPreferences(Constants.PreferenceName, Context.MODE_PRIVATE);
        badgePrintLayout = findViewById(R.id.badgePrintLayout);
        CompoundButton badgePrintingSwitch = findViewById(R.id.badgePrintingSwitch);
        CompoundButton tempSwitch = findViewById(R.id.printTempSwitch);
        CompoundButton dateSwitch = findViewById(R.id.printDateSwitch);
        CompoundButton timeSwitch = findViewById(R.id.printTimeSwitch);
        CompoundButton nameSwitch = findViewById(R.id.printNameSwitch);
        CompoundButton companyNameSwitch = findViewById(R.id.printCompanyNameSwitch);
        CompoundButton signatureSwitch = findViewById(R.id.printSignatureSwitch);
        CompoundButton failedBadgeSwitch = findViewById(R.id.printFailedTempBadgeSwitch);
        CompoundButton printImageSwitch = findViewById(R.id.printImageSwitch);
        Button saveButton = findViewById(R.id.savePrintSettingsButton);
        Spinner checkmarkSpinner = findViewById(R.id.checkmarkTypeSpinner);
        exampleBadgeImage = findViewById(R.id.exampleBadgeImage);
        infoLine1 = findViewById(R.id.infoLine1EditText);
        infoLine2 = findViewById(R.id.infoLine2EditText);
        infoLine3 = findViewById(R.id.infoLine3EditText);
        infoLine4 = findViewById(R.id.infoLine4EditText);
        final ConstraintLayout failedBadgeInfoLayout = findViewById(R.id.failedBadgeInfoLayout);



        int checkmarkType = sharedPreferences.getInt(Constants.CheckmarkPrintType, -1);
        int spinnerDefaultPos;

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.ZPLCheckmarkTypes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        checkmarkSpinner.setAdapter(adapter);

        if (checkmarkType == -1) {
            // Default to checkmark with box
            spinnerDefaultPos = adapter.getPosition(ZPLValues.enumToString(this, ZPLValues.CheckmarkType.Regular));
        } else {
            // Populate what was stored in preferences
            spinnerDefaultPos = adapter.getPosition(ZPLValues.enumToString(this, ZPLValues.CheckmarkType.valueOf(checkmarkType)));
        }
        setPreviewImage(spinnerDefaultPos);
        checkmarkSpinner.setSelection(spinnerDefaultPos);
        checkmarkSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                sharedPreferences.edit().putInt(Constants.CheckmarkPrintType, i).commit();
                setPreviewImage(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        final Button testPrintButton = findViewById(R.id.testPrintButton);
        companyName = findViewById(R.id.companyName);
        printerStatus = findViewById(R.id.printerStatus);

        tempSwitch.setChecked(sharedPreferences.getBoolean(Constants.PrintTempKey, false));
        dateSwitch.setChecked(sharedPreferences.getBoolean(Constants.PrintDateKey, false));
        timeSwitch.setChecked(sharedPreferences.getBoolean(Constants.PrintTimeKey, false));
        nameSwitch.setChecked(sharedPreferences.getBoolean(Constants.PrintNameKey, false));
        printImageSwitch.setChecked(sharedPreferences.getBoolean(Constants.PrintImageKey, false));
        companyNameSwitch.setChecked(sharedPreferences.getBoolean(Constants.PrintCompanyNameKey, false));
        badgePrintingSwitch.setChecked(sharedPreferences.getBoolean(Constants.BadgePrintingDisabled, true));
        if (badgePrintingSwitch.isChecked()) {
            badgePrintLayout.setVisibility(View.GONE);
        }
        signatureSwitch.setChecked(sharedPreferences.getBoolean(Constants.PrintSignatureLine, false));
        if (sharedPreferences.getBoolean(Constants.PrintFailedTempBadge, false)) {
            String line1 = sharedPreferences.getString(Constants.FailedBadgeLine1, "");
            String line2 = sharedPreferences.getString(Constants.FailedBadgeLine2, "");
            String line3 = sharedPreferences.getString(Constants.FailedBadgeLine3, "");
            String line4 = sharedPreferences.getString(Constants.FailedBadgeLine4, "");
            infoLine1.setText(line1);
            infoLine2.setText(line2);
            infoLine3.setText(line3);
            infoLine4.setText(line4);
            failedBadgeSwitch.setChecked(true);
            failedBadgeInfoLayout.setVisibility(View.VISIBLE);
        } else {
            failedBadgeSwitch.setChecked(false);
            failedBadgeInfoLayout.setVisibility(View.GONE);
        }

        tempSwitch.setOnCheckedChangeListener(this);
        dateSwitch.setOnCheckedChangeListener(this);
        timeSwitch.setOnCheckedChangeListener(this);
        nameSwitch.setOnCheckedChangeListener(this);
        companyNameSwitch.setOnCheckedChangeListener(this);
        signatureSwitch.setOnCheckedChangeListener(this);
        printImageSwitch.setOnCheckedChangeListener(this);
        failedBadgeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean enabled) {
                SharedPreferences.Editor editor = sharedPreferences.edit();

                String line1 = sharedPreferences.getString(Constants.FailedBadgeLine1, "");
                String line2 = sharedPreferences.getString(Constants.FailedBadgeLine2, "");
                String line3 = sharedPreferences.getString(Constants.FailedBadgeLine3, "");
                String line4 = sharedPreferences.getString(Constants.FailedBadgeLine4, "");
                if (enabled) {
                    failedBadgeInfoLayout.setVisibility(View.VISIBLE);
                    infoLine1.setText(line1);
                    infoLine2.setText(line2);
                    infoLine3.setText(line3);
                    infoLine4.setText(line4);
                } else {
                    failedBadgeInfoLayout.setVisibility(View.GONE);
                }

                editor.putBoolean(Constants.PrintFailedTempBadge, enabled);
                editor.commit();
            }
        });

        // Handle disable/enable of badge printing
        badgePrintingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean status) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                if (status) {
                    badgePrintLayout.setVisibility(View.GONE);
                } else {
                    badgePrintLayout.setVisibility(View.VISIBLE);
                }
                editor.putBoolean(Constants.BadgePrintingDisabled, status);
                editor.commit();
            }
        });

        companyName.setText(sharedPreferences.getString(Constants.CompanyName, ""));

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveCompanyName();
                saveFailedTempBadgeInfo();
                Toast.makeText(
                        BadgePrintSettingsActivity.this,
                        "Settings Saved",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        testPrintButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(
                        BadgePrintSettingsActivity.this,
                        "Printing test label...",
                        Toast.LENGTH_SHORT
                ).show();
                Calendar calendar = Calendar.getInstance();
                String json = "{\"checkPic\":\"\", \"name\":\"Test\", \"temperature\":\"37\", \"checkTime\":\"" + calendar.getTimeInMillis() + "\"}";
                try {
                    badgePrinter.printBadge(getApplicationContext(), new JSONObject(json), 40);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Failed to perform test print: " + e.getMessage());
                    Toast.makeText(
                            BadgePrintSettingsActivity.this,
                            "Failed to print test label: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
        });

        updateStatusText();
    }

    private void setPreviewImage(int spinnerPos) {
        switch(ZPLValues.CheckmarkType.valueOf(spinnerPos)) {
            case Regular:
                exampleBadgeImage.setImageDrawable(getResources().getDrawable(R.drawable.badge_checkmark));
                break;
            case CircleDayOfWeek:
                exampleBadgeImage.setImageDrawable(getResources().getDrawable(R.drawable.badge_day_of_week));
                break;
            case CircleDayOfMonth:
                exampleBadgeImage.setImageDrawable(getResources().getDrawable(R.drawable.badge_day_of_month));
                break;
            default:
                Log.e(LOG_TAG, "Unknown spinner position, cannot set preview image");
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        String prefName;
        switch (compoundButton.getId()) {
            case R.id.printDateSwitch:
                prefName = Constants.PrintDateKey;
                break;
            case R.id.printTimeSwitch:
                prefName = Constants.PrintTimeKey;
                break;
            case R.id.printTempSwitch:
                prefName = Constants.PrintTempKey;
                break;
            case R.id.printNameSwitch:
                prefName = Constants.PrintNameKey;
                break;
            case R.id.printCompanyNameSwitch:
                prefName = Constants.PrintCompanyNameKey;
                break;
            case R.id.printSignatureSwitch:
                prefName = Constants.PrintSignatureLine;
                break;
            case R.id.printImageSwitch:
                prefName = Constants.PrintImageKey;
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
    }

    private void saveCompanyName() {
        String name = companyName.getText().toString();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.CompanyName, name);
        editor.commit();
    }

    private void saveFailedTempBadgeInfo() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.FailedBadgeLine1, infoLine1.getText().toString());
        editor.putString(Constants.FailedBadgeLine2, infoLine2.getText().toString());
        editor.putString(Constants.FailedBadgeLine3, infoLine3.getText().toString());
        editor.putString(Constants.FailedBadgeLine4, infoLine4.getText().toString());
        editor.commit();
    }

    private void updateStatusText() {
        if (badgePrinter.isZebraPrinterConnected(this)) {
            printerStatus.setText("Printer status - Connected to Zebra printer");
        } else if (badgePrinter.isBrotherPrinterConnected(this)) {
            printerStatus.setText("Printer status - Connected to Brother printer");
        } else {
            printerStatus.setText("Printer status - Offline");
        }
    }
}
