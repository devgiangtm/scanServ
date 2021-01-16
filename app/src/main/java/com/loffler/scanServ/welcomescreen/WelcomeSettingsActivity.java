package com.loffler.scanServ.welcomescreen;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;

import com.google.android.material.textfield.TextInputLayout;
import com.loffler.scanServ.R;
import com.loffler.scanServ.cdcsetting.SharedPreferencesController;

import static com.loffler.scanServ.Constants.DashboardSettingsLogoImagePathKey;
import static com.loffler.scanServ.Constants.DashboardSettingsQrCodeContentKey;
import static com.loffler.scanServ.Constants.DashboardSettingsReturnToForegroundTimeoutKey;
import static com.loffler.scanServ.Constants.WELCOME_ENABLE;
import static com.loffler.scanServ.Constants.WELCOME_MESSAGE;

public class WelcomeSettingsActivity extends AppCompatActivity implements Button.OnClickListener {
    private Uri imageLogoUri;
    private ImageView ivLogoPreview;
    private TextInputLayout tilTempTimeout;
    private TextInputLayout tilQrCode;
    private TextInputLayout tilWelcomeMessage;
    private Switch swEnableWelcomeScreen;

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
        String stringUri = imageLogoUri != null ? imageLogoUri.toString() : "";
        String tempTimeout = tilTempTimeout.getEditText().getText().toString();
        String stringQr = tilQrCode.getEditText().getText().toString();
        String stringMessage = tilWelcomeMessage.getEditText().getText().toString();
        SharedPreferencesController.with(getBaseContext()).saveLong(
                DashboardSettingsReturnToForegroundTimeoutKey,
                Long.valueOf(tempTimeout.equals("") ? "-1" : tempTimeout));
        SharedPreferencesController.with(getBaseContext()).saveString(DashboardSettingsLogoImagePathKey, stringUri);
        SharedPreferencesController.with(getBaseContext()).saveString(DashboardSettingsQrCodeContentKey, stringQr);
        SharedPreferencesController.with(getBaseContext()).saveString(WELCOME_MESSAGE, stringMessage);
        SharedPreferencesController.with(getBaseContext()).saveBoolean(WELCOME_ENABLE, swEnableWelcomeScreen.isChecked());
        finish();

    }

    private void loadData() {
        Long timeOut = SharedPreferencesController.with(getBaseContext()).getLong(DashboardSettingsReturnToForegroundTimeoutKey);
        String uriLogo = SharedPreferencesController.with(getBaseContext()).getString(DashboardSettingsLogoImagePathKey);
        String qRcode = SharedPreferencesController.with(getBaseContext()).getString(DashboardSettingsQrCodeContentKey);
        String message = SharedPreferencesController.with(getBaseContext()).getString(WELCOME_MESSAGE);
        boolean isWSEnable = SharedPreferencesController.with(getBaseContext()).getBoolean(WELCOME_ENABLE);
        tilTempTimeout.getEditText().setText(timeOut != -1 ? String.valueOf(timeOut) : "");
        if (uriLogo.equals("")) {
            ivLogoPreview.setImageResource(R.drawable.img_logo);
        } else {
            imageLogoUri = Uri.parse(uriLogo);
            ivLogoPreview.setImageURI(imageLogoUri);
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
                imageLogoUri = data.getData();
                ivLogoPreview.setImageURI(imageLogoUri);
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
                ivLogoPreview.setImageURI(null);
                break;
            case R.id.btnSave:
                saveData();
                break;
        }
    }
}