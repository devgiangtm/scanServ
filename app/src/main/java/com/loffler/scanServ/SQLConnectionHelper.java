package com.loffler.scanServ;

import android.os.StrictMode;
import android.util.Log;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLConnectionHelper {
    private static final String LOG_TAG = "SQLConnectionHelper";
    public static Connection CONN(String server, String database, String user, String pass) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);
        Connection conn = null;
        String ConnURL;
        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            ConnURL = "jdbc:jtds:sqlserver://" + server + ";"
                    + "databaseName=" + database + ";user=" + user + ";password="
                    + pass + ";";
            conn = DriverManager.getConnection(ConnURL);
            Log.i(LOG_TAG, "Connected to SQL Server");
        } catch (SQLException se) {
            Log.e(LOG_TAG, se.getMessage());
        } catch (ClassNotFoundException e) {
            Log.e(LOG_TAG, e.getMessage());
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
        return conn;
    }
}
