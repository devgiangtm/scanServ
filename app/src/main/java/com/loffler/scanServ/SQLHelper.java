package com.loffler.scanServ;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.loffler.scanServ.service.sql.dao.OutputDao;
import com.loffler.scanServ.service.sql.dao.OutputDaoImpl;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SQLHelper {
    private final static String LOG_TAG = "SQLHelper";
    private static Connection conn = null;
    private static String tableName = null;

    @Deprecated // "Use getConnection instead"
    public static boolean connectToDatabase(String server, String database, String table, String user, String pass) {
        disconnectFromDatabase();
        tableName = table;
        conn = getConnection(server, database, user, pass);
        return conn != null;
    }

    public static Connection getConnection(SharedPreferences sharedPreferences) {
        String server = sharedPreferences.getString(Constants.SQLServerName, "");
        String database = sharedPreferences.getString(Constants.SQLDatabaseName, "");
        String user = sharedPreferences.getString(Constants.SQLUsername, "");
        String pass = sharedPreferences.getString(Constants.SQLPassword, "");
        return getConnection(server, database, user, pass);
    }

    public static Connection getConnection(String server, String database, String user, String pass) {
        CompletableFuture<Connection> connectionCompletableFuture = CompletableFuture.supplyAsync(() -> SQLConnectionHelper.CONN(server, database, user, pass));
        try {
            return connectionCompletableFuture.get(3, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean isDatabaseConnected(SharedPreferences sharedPreferences) {
        boolean isConnected = false;

        try (Connection connection = getConnection(sharedPreferences)) {
            isConnected = connection != null;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return isConnected;
    }

    public static void disconnectFromDatabase() {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            Log.e(LOG_TAG, "Failed to close SQL connection: " + e.getMessage());
        }
    }
}