package com.loffler.scanServ.cdcsetting;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

//import com.lamasatech.visipoint.VPApp;

public class FileUtil {
    public static boolean appendStringToFile(String paramString) {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "logs.text");
        boolean bool = false;
        try {
            file.createNewFile();
            if (file.canWrite()) {
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, true), 1024);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getToday());
                stringBuilder.append("    ");
                stringBuilder.append(paramString);
                stringBuilder.append("\n");
                bufferedWriter.write(stringBuilder.toString());
                bufferedWriter.close();
                bool = true;
            }
            return bool;
        } catch (IOException iOException) {
            return false;
        }
    }

    public static void copyFile(File source, File dest) throws IOException {
        PermissionUtil.setMipsFilePermission(source.getAbsolutePath());
        PermissionUtil.setPoaFolderPermission();
//        PermissionUtil.setMipsFilePermission(dest.getAbsolutePath());
        FileChannel sourceChannel = null;
        FileChannel destChannel = null;
        try {
            sourceChannel = new FileInputStream(source).getChannel();
            destChannel = new FileOutputStream(dest).getChannel();
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        }
        catch (Exception e){
            e.printStackTrace();
        }
        finally{
            sourceChannel.close();
            destChannel.close();
        }
    }
//    public static void copyFile(File paramFile1, File paramFile2) throws IOException {
//        Exception exception1;
//        Exception exception2;
//        if (!paramFile2.exists())
//            paramFile2.createNewFile();
//        Exception exception3 = null;
//        File file = null;
//        try {
//            FileChannel fileChannel2 = (new FileInputStream(paramFile1)).getChannel();
//        } finally {
//            exception2 = null;
//            paramFile2 = null;
//        }
//        if (exception1 != null)
//            exception1.close();
//        if (paramFile2 != null)
//            paramFile2.close();
//        throw exception2;
//    }

    public static String getFileContents(File paramFile) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(paramFile);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
        StringBuilder stringBuilder = new StringBuilder();
        boolean bool = false;
        while (!bool) {
            String str = bufferedReader.readLine();
            if (str == null) {
                bool = true;
            } else {
                bool = false;
            }
            if (str != null)
                stringBuilder.append(str);
        }
        bufferedReader.close();
        fileInputStream.close();
        return stringBuilder.toString();
    }

    public static File getMipsSharedPreferencesFile() {
        File file2 = new File("/data/data/com.neldtv.mips/shared_prefs/shared_data.xml");
        File file1 = file2;
        if (!file2.exists())
            file1 = new File("/data/data/com.gate.mips/shared_prefs/shared_data.xml");
        return file1;
    }

    public static String getToday() {
        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return simpleDateFormat.format(date);
    }

//    public static void loadMissingMipsConfigFromFile(Context paramContext) {
//        File file1 = getMipsSharedPreferencesFile();
//        StringBuilder stringBuilder = new StringBuilder();
//        stringBuilder.append("/data/data/");
//        stringBuilder.append(paramContext.getPackageName());
//        stringBuilder.append("/shared_prefs/shared_data.xml");
//        File file2 = new File(stringBuilder.toString());
////        setPermissionFile(file1.getAbsolutePath());
//        try {
////            file2.delete();
//            copyFile(file1, file2);
//        } catch (IOException iOException) {}
//        SharedPreferences sharedPreferences = paramContext.getSharedPreferences("shared_data", Context.MODE_MULTI_PROCESS);
//        if (sharedPreferences != null) {
//            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences((Context) App.getAppContext()).edit();
//            String str = sharedPreferences.getString("exit_app_password_setting", null);
//            if (!TextUtils.isEmpty(str))
//                editor.putString("password", str);
//            editor.putString("deviceName", sharedPreferences.getString("deviceName", null));
//            editor.putString("tempShowMode", String.valueOf(sharedPreferences.getInt("temp_show_mode", 0)));
//            editor.putInt("strangerMode", sharedPreferences.getInt("strange_mode", 0));
//            editor.apply();
//        }
//    }

    public static String getAppPassword(Context paramContext){
        SharedPreferences sharedPreferences = paramContext.getSharedPreferences("shared_data", Context.MODE_MULTI_PROCESS);
        return sharedPreferences.getString("exit_app_password_setting", "123456");
    }

//    public static Single<Void> loadMissingMipsConfigFromFileRx(Context paramContext) {
//        return Single.fromCallable(new -$$Lambda$FileUtil$4aFv7OV9EjXV9aslxYW5gUNcDz4(paramContext));
//    }

//    public static boolean setPermissionFile(String paramString) {
//        StringBuilder stringBuilder1;
//        Process process;
//        Exception exception = null;
//        StringBuilder stringBuilder2 = null;
//        try {
//
//        } catch (Exception exception1) {
//            process = null;
//            exception1 = exception;
//            return false;
//        } finally {
//            paramString = null;
//            process = null;
//        }
//        if (stringBuilder1 != null)
//            try {
//                stringBuilder1.close();
//                process.destroy();
//                return false;
//            } catch (Exception exception1) {
//                return false;
//            }
//        process.destroy();
//        return false;
//    }
}
