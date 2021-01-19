package com.loffler.scanServ.welcomescreen;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;

import com.google.android.material.textfield.TextInputLayout;
import com.loffler.scanServ.Constants;
import com.loffler.scanServ.ImageFilePath;
import com.loffler.scanServ.R;
import com.loffler.scanServ.cdcsetting.SharedPreferencesController;

import java.io.ByteArrayOutputStream;

import static com.loffler.scanServ.Constants.DashboardSettingsLogoImage;
import static com.loffler.scanServ.Constants.DashboardSettingsLogoImagePathKey;
import static com.loffler.scanServ.Constants.DashboardSettingsQrCodeContentKey;
import static com.loffler.scanServ.Constants.DashboardSettingsReturnToForegroundTimeoutKey;
import static com.loffler.scanServ.Constants.WELCOME_ENABLE;
import static com.loffler.scanServ.Constants.WELCOME_MESSAGE;

public class WelcomeSettingsActivity extends AppCompatActivity implements Button.OnClickListener {
    private ImageView ivLogoPreview;
    private TextInputLayout tilTempTimeout;
    private TextInputLayout tilQrCode;
    private TextInputLayout tilWelcomeMessage;
    private Switch swEnableWelcomeScreen;
    private Bitmap myBitmap;

    private static final int PICK_IMAGE_CODE = 100;
    private static final String TYPE_IMAGE = "image/*";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_settings);
        Button imagePickerButtons = findViewById(R.id.btnImagePicker);
        Button deleteLogoButtons = findViewById(R.id.btnDeleteLogo);
        Button btnSave = findViewById(R.id.btnSave);
        ivLogoPreview = findViewById(R.id.ivLogoPreview);
        tilTempTimeout = findViewById(R.id.tilTempTimeout);
        tilQrCode = findViewById(R.id.tilQrCode);
        tilWelcomeMessage = findViewById(R.id.tilWelcomeMessage);
        swEnableWelcomeScreen = findViewById(R.id.swEnableWelcomeScreen);
        imagePickerButtons.setOnClickListener(this);
        deleteLogoButtons.setOnClickListener(this);
        btnSave.setOnClickListener(this);
        loadData();
    }


    private void saveData() {
        String tempTimeout = tilTempTimeout.getEditText().getText().toString();
        String stringQr = tilQrCode.getEditText().getText().toString();
        String stringMessage = tilWelcomeMessage.getEditText().getText().toString();
        SharedPreferencesController.with(getBaseContext()).saveLong(
                DashboardSettingsReturnToForegroundTimeoutKey,
                Long.valueOf(tempTimeout.equals("") ? "15" : tempTimeout));
        if(myBitmap != null){
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            myBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream .toByteArray();
            String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
            SharedPreferencesController.with(getBaseContext()).saveString(DashboardSettingsLogoImage, encoded);
        } else {
            SharedPreferencesController.with(getBaseContext()).saveString(DashboardSettingsLogoImage, "");
        }

        SharedPreferencesController.with(getBaseContext()).saveString(DashboardSettingsQrCodeContentKey, stringQr);
        SharedPreferencesController.with(getBaseContext()).saveString(WELCOME_MESSAGE, stringMessage);
        SharedPreferencesController.with(getBaseContext()).saveBoolean(WELCOME_ENABLE, swEnableWelcomeScreen.isChecked());
        finish();

    }

    private void loadData() {
        Long timeOut = SharedPreferencesController.with(getBaseContext()).getLong(DashboardSettingsReturnToForegroundTimeoutKey);
        String logo = getApplicationContext().getSharedPreferences(Constants.PreferenceName, MODE_PRIVATE).getString(DashboardSettingsLogoImage, "");
        String qRcode = SharedPreferencesController.with(getBaseContext()).getString(DashboardSettingsQrCodeContentKey);
        String message = SharedPreferencesController.with(getBaseContext()).getString(WELCOME_MESSAGE);
        boolean isWSEnable = SharedPreferencesController.with(getBaseContext()).getBoolean(WELCOME_ENABLE);
        tilTempTimeout.getEditText().setText(timeOut != -1 ? String.valueOf(timeOut) : "15");
        if (!logo.equals("")) {
            byte[] decodedString = Base64.decode(logo, Base64.DEFAULT);
            myBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            ivLogoPreview.setImageBitmap(myBitmap);
        }
        tilQrCode.getEditText().setText(qRcode);
        tilWelcomeMessage.getEditText().setText(message);
        swEnableWelcomeScreen.setChecked(isWSEnable);


    }

    private void openPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(TYPE_IMAGE);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_CODE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                myBitmap = BitmapFactory.decodeFile(ImageFilePath.getPath(getBaseContext(), data.getData()));
                ivLogoPreview.setImageBitmap(myBitmap);

            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnImagePicker:
                openPicker();
                break;
            case R.id.btnDeleteLogo:
                myBitmap = null;
                ivLogoPreview.setImageBitmap(myBitmap);
                break;
            case R.id.btnSave:
                saveData();
                break;
        }
    }
}