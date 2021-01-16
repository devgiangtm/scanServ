package com.loffler.scanServ.cdcsetting;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.os.Bundle;

import com.loffler.scanServ.R;

public class CDCSettingActivity extends AppCompatActivity {
    protected FragmentManager mFragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_c_d_c_setting);
        mFragmentManager = getSupportFragmentManager();
        mFragmentManager.beginTransaction()
                .add(R.id.container, new CDCSettingFragment())
                .commit();
    }
}