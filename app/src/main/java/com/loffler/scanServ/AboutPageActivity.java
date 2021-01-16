package com.loffler.scanServ;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class AboutPageActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_page);

        SharedPreferences prefs = getApplicationContext().getSharedPreferences(Constants.PreferenceName, MODE_PRIVATE);

        ImageView logo = findViewById(R.id.companyLogo);
        TextView eulaTextView = findViewById(R.id.eulaTextView);
        TextView scanServVersion = findViewById(R.id.scanServVersion);
        scanServVersion.setText("ScanServ " + BuildConfig.VERSION_NAME);

        byte[] decodedImg = Base64.decode(prefs.getString(Constants.CompanyLogoKey, ""), Base64.DEFAULT);
        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedImg, 0, decodedImg.length);
        logo.setImageBitmap(decodedByte);

        // Read EULA text file and set text view to its contents
        String string;
        StringBuilder stringBuilder = new StringBuilder();
        InputStream inputStream = getResources().openRawResource(R.raw.eula);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            while (true) {
                if ((string = reader.readLine()) == null) break;
                stringBuilder.append(string).append("\n");
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        eulaTextView.setText(stringBuilder.toString());
    }
}