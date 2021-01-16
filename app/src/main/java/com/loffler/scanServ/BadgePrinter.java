package com.loffler.scanServ;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.error.VolleyError;
import com.android.volley.request.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;

public class BadgePrinter {
    private final String LOG_TAG = "BadgePrinter";
    private SharedPreferences sharedPreferences;
    private BrotherPrinterImpl brotherPrinter = new BrotherPrinterImpl();
    private ZebraPrinterImpl zebraPrinter = new ZebraPrinterImpl();

    public void printBadge(final Context context, JSONObject json, double highTempLimit) {
        sharedPreferences = context.getSharedPreferences(Constants.PreferenceName, Context.MODE_PRIVATE);

        // Badge printing disabled, do nothing
        if (sharedPreferences.getBoolean(Constants.BadgePrintingDisabled, true)) {
            return;
        }

        try {
            double scannedTemp = (json.getDouble("temperature") * 9 / 5) + 32;
            highTempLimit = (highTempLimit * 9 / 5) + 32;
            // If scannedTemp is higher than temp limit, print failed temp badge (if enabled)
            boolean failedTempCheck = scannedTemp >= highTempLimit;

            if (failedTempCheck && !sharedPreferences.getBoolean(Constants.PrintFailedTempBadge, false)) {
                // No failed temperature badge to be printed, exit here
                return;
            }

            // Skip grabbing of company name from EZ-pass if we have a company name stored
            String sharedPrefCompanyName = sharedPreferences.getString(Constants.CompanyName, "");
            if (failedTempCheck) {
                // Company name is not used for failed temp check
                _printBadge(context, json, "", true);
            } else if (!sharedPrefCompanyName.equals("")) {
                _printBadge(context, json, sharedPrefCompanyName, false);
            } else {
                getCompanyName(sharedPreferences.getString(Constants.EZPassPwKey, ""), context, json);
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Failed to parse JSON for badge printing: " + e.getMessage());
        }
    }

    private void getCompanyName(String ezPassPw, final Context context, final JSONObject jsonToPrint) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, Constants.GET_CONFIG_URL + "pass=" + ezPassPw, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    Log.i(LOG_TAG, "onResponse: EZ-Pass Config information received: " + response);
                    JSONObject settingsObject = new JSONObject(new JSONObject(response).getString("data"));
                    _printBadge(context, jsonToPrint, settingsObject.getString("companyName"), false);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Failed to parse EZ-Pass config JSON: ");
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "Error receiving config information from EZ-Pass http server");
                error.printStackTrace();
            }
        });
        RequestQueue queue = Volley.newRequestQueue(context);
        queue.add(stringRequest);
    }

    public boolean isZebraPrinterConnected(final Context context) {
        return zebraPrinter.isConnected(context);
    }

    public boolean isBrotherPrinterConnected(final Context context) {
        return brotherPrinter.isConnected(context);
    }

    private void _printBadge(final Context context, final JSONObject json, final String companyName, final boolean failedTempBadge) {
        new Thread(new Runnable() {
            public void run() {
                BasePrinter printer = null;
                if (zebraPrinter.isConnected(context)) {
                    printer = zebraPrinter;
                } else if (brotherPrinter.isConnected(context)) {
                    printer = brotherPrinter;
                }
                if (printer != null) {
                    printer.printBadge(context, json, companyName, failedTempBadge);
                }
            }
        }).start();
    }
}
