package com.loffler.scanServ;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.loffler.scanServ.cdcsetting.SharedPreferencesController;
import com.loffler.scanServ.service.sql.dao.DaoResult;
import com.loffler.scanServ.service.sql.dao.OutputDao;
import com.loffler.scanServ.service.sql.dao.OutputDaoImpl;
import com.loffler.scanServ.utils.AppLauncherImpl;
import com.loffler.scanServ.utils.ViewUtilsKt;

import java.sql.Connection;

import static com.loffler.scanServ.Constants.SQLTableName;

public class SQLSettingsActivity extends BaseActivity {
    private SharedPreferences sharedPreferences;
    private EditText sqlServerName;
    private EditText sqlDatabaseName;
    private EditText sqlTable;
    private EditText sqlUsername;
    private EditText sqlPassword;
    private TextView serverStatus;
    private Switch swSQLWriteLogs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sql_info);
        sqlServerName = findViewById(R.id.sqlServerName);
        sqlDatabaseName = findViewById(R.id.databaseName);
        sqlTable = findViewById(R.id.tableName);
        sqlUsername = findViewById(R.id.sqlUsername);
        sqlPassword = findViewById(R.id.sqlPassword);
        Button saveButton = findViewById(R.id.sqlSaveButton);
        serverStatus = findViewById(R.id.sqlServerStatus);
        swSQLWriteLogs = findViewById(R.id.swSQLWriteLogs);
        sharedPreferences = getSharedPreferences(Constants.PreferenceName, Context.MODE_PRIVATE);
        swSQLWriteLogs.setChecked(sharedPreferences.getBoolean(Constants.swSQLWriteLogs, false));
        sqlServerName.setText(sharedPreferences.getString(Constants.SQLServerName, null));
        sqlDatabaseName.setText(sharedPreferences.getString(Constants.SQLDatabaseName, null));
        sqlUsername.setText(sharedPreferences.getString(Constants.SQLUsername, null));
        sqlPassword.setText(sharedPreferences.getString(Constants.SQLPassword, null));
        sqlTable.setText(sharedPreferences.getString(Constants.SQLTableName, null));

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String server = sqlServerName.getText().toString();
                String database = sqlDatabaseName.getText().toString();
                String table = sqlTable.getText().toString();
                String user = sqlUsername.getText().toString();
                String pass = sqlPassword.getText().toString();

                SharedPreferences.Editor editor = sharedPreferences.edit();

                editor.putString(Constants.SQLServerName, server);
                editor.putString(Constants.SQLDatabaseName, database);
                editor.putString(Constants.SQLTableName, table);
                editor.putString(Constants.SQLUsername, user);
                editor.putString(Constants.SQLPassword, pass);
                editor.putBoolean(Constants.swSQLWriteLogs, swSQLWriteLogs.isChecked());
                // commit immediately, do not apply for later
                editor.commit();

                connectToSqlServer();

                Toast.makeText(
                        SQLSettingsActivity.this,
                        "Settings Saved",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        connectToSqlServer();
    }

    private void connectToSqlServer() {
        String server = sqlServerName.getText().toString();
        String database = sqlDatabaseName.getText().toString();
        String table = sqlTable.getText().toString();
        String user = sqlUsername.getText().toString();
        String pass = sqlPassword.getText().toString();
        String validationTable = sqlPassword.getText().toString();

        if (server.isEmpty() || database.isEmpty() || table.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            serverStatus.setText("Not connected to SQL server, Please enter all info above and save");
            return;
        }

        // Check to see if we can connect to the database
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (SQLHelper.connectToDatabase(server, database, table, user, pass)) {
            serverStatus.setText("Connected to SQL server...");
            if(!SharedPreferencesController.with(getApplicationContext()).getString(SQLTableName).equals("")){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        SharedPreferences prefs = getApplicationContext().getSharedPreferences(Constants.PreferenceName, MODE_PRIVATE);
                        OutputDao outputDao = new OutputDaoImpl(prefs);
                        //Create Table logs
                        String tableName = SharedPreferencesController.with(getApplicationContext()).getString(SQLTableName);
                        Boolean table = outputDao.createTable(tableName);
                    }
                }).start();
            }

            editor.putBoolean(Constants.SQLConnected, true);
        } else {
            serverStatus.setText("Failed to connect to SQL server; please check the information and try again.");
            editor.putBoolean(Constants.SQLConnected, false);
        }
        editor.commit();
    }
}
