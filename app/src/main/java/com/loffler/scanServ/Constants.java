package com.loffler.scanServ;

import android.os.Environment;
import java.util.HashMap;
import java.util.Map;

public final class Constants {
    public static final String LEFT = "left";
    public static final String UP = "up";
    public static final String DOWN = "down";
    public static final String RIGHT = "right";
    public static final String PreferenceName = "LofflerPreferences";
    public static final String SMTPServAddressKey = "SMTPServer";
    public static final String SMTPUsernameKey = "SMTPUsername";
    public static final String SMTPPWKey = "SMTPPw";
    public static final String SendNormalTempKey = "SendNormTemp";
    public static final String SendHighTempKey = "SendHighTemp";
    public static final String SendPicKey = "SendPicKey";
    public static final String SendTempReadingKey = "SendTempReading";
    public static final String SendNameKey = "SendNameKey";
    public static final String EZPassPwKey = "EZPw";
    public static final String SmtpSSLPortKey = "SMTPPort";
    public static final String ReceivingEmailKey = "ReceiverKey";
    public static final String AuthTypeKey = "AuthType";
    public static final String DeviceIdKey = "DeviceID";
    public static final String SMTPFromAddressKey = "SMTPFromEmail";
    public static final String ProductKeyActivationCompleted = "Activated";
    public static final String FeatureSetKey = "FeatureSetKey";
    public static final String AutoDeleteRecordKey = "AutoDeleteRecord";
    public static final String SendRecordEODKey = "SendRecordsEOD";
    public static final String PrintTempKey = "PrintTemp";
    public static final String PrintDateKey = "PrintDate";
    public static final String PrintTimeKey = "PrintTime";
    public static final String PrintNameKey = "PrintName";
    public static final String PrintCompanyNameKey = "PrintCompanyName";
    public static final String PrintSignatureLine = "PrintSignatureLine";
    public static final String PrintFailedTempBadge = "PrintFailedTempBadge";
    public static final String PrintImageKey = "PrintImage";
    public static final String CheckmarkPrintType = "CheckmarkPrintType";
    public static final String FailedBadgeLine1 = "FailedBadgeLine1";
    public static final String FailedBadgeLine2 = "FailedBadgeLine2";
    public static final String FailedBadgeLine3 = "FailedBadgeLine3";
    public static final String FailedBadgeLine4 = "FailedBadgeLine4";
    public static final String BadgePrintingDisabled = "BadgePrintingDisabled";
    public static final String ScanDelay = "ScanDelay";
    public static final String ScanDelayEnabled = "ScanDelayEnabled";
    public static final String CompanyName = "CompanyName";
    public static final String SQLServerName = "SQLServerName";
    public static final String SQLDatabaseName = "SQLDatabaseName";
    public static final String SQLTableName = "SQLTableName";
    public static final String SQLUsername = "SQLUsername";
    public static final String SQLPassword = "SQLPassword";
    public static final String SQLConnected = "SQLConnected";
    public static final String swSQLWriteLogs = "swSQLWriteLogs";
    public static final String UPDATES_FOLDER_LOCATION = Environment.getExternalStorageDirectory().toString() + "/updates/";
    public static final String LOG_DUMP_LOCATION = Environment.getExternalStorageDirectory().toString() + "/logdump.txt";
    public static final String SCANSERV_FOLDER = Environment.getExternalStorageDirectory().toString() + "/scanserv/";
    public static final String SCANSERV_CONFIG_FILE = SCANSERV_FOLDER + "config.json";
    public static final String MANIFEST_LOCATION = UPDATES_FOLDER_LOCATION + "update_manifest.json";
    public static final String PROGRESS_FILE_LOCATION = UPDATES_FOLDER_LOCATION + "install_progress.txt";
    public static final String TRIAL_DIR = Environment.getExternalStorageDirectory().toString() + "/sysinfo/";
    public static final String TRAIL_FILE = TRIAL_DIR + "/config.dat";
    public static final String REBOOT_ACTION = "com.loffler.scanServ.reboot";
    public static final String GET_CONFIG_URL = "http://127.0.0.1:8080/getConfig?";
    public static final String SET_CALLBACK_URL = "http://127.0.0.1:8080/setIdentifyCallback?";
    public static final String CALLBACK_URL = "http://127.0.0.1:5000";
    public static final String CLOSE_ACTIVATION_PAGE = "com.loffler.scanServ.closeKeyPage";
    public static final String SupportCompanyKey = "SupportCompany";
    public static final String SupportEmailKey = "SupportEmail";
    public static final String SupportPhoneKey = "SupportPhone";
    public static final String CompanyLogoKey = "CompanyLogo";
    public static final String UpdateUrlKey = "UpdateUrl";
    public static final String SendLogsUrlKey = "SendLogsUrl";

    public static final String DashboardSettingsEnableFeatureKey = "DashboardSettingsEnableFeatureKey";
    public static final String DashboardSettingsLogoImagePathKey = "DashboardSettingsLogoImagePathKey";
    public static final String DashboardSettingsInstructionsTextKey = "DashboardSettingsInstructionsTextKey";
    public static final String DashboardSettingsQrCodeContentKey = "DashboardSettingsQrCodeContentKey";
    public static final String DashboardSettingsValidationTableNameKey = "SqlValidationTableNameKey";
    public static final String DashboardSettingsValidationTableEnableFeatureKey = "ValidationEnabledKey";
    public static final String DashboardSettingsValidationFailedMessageKey = "DashboardSettingsValidationFailedKey";
    public static final String DashboardSettingsReturnToForegroundTimeoutKey = "DashboardSettingsForceReturnDelay";
    public static final String WELCOME_ENABLE = "welcomeLogoPath";
    public static final String WELCOME_MESSAGE = "welcomeMessage";
    public static final String CURRENT_SCAN_ID = "currentScanID";
    public static final long FORCE_RETURN_TO_FOREGROUND_TIMEOUT_DEFAULT_MS = 15 * 1000;

    public static class PrefInfo {
        public String preferenceKey;
        public PREFERENCE_TYPE preferenceType;

        public PrefInfo(String preferenceKey, PREFERENCE_TYPE preferenceType) {
            this.preferenceKey = preferenceKey;
            this.preferenceType = preferenceType;
        }
    }

    public static final Map<String, PrefInfo> jsonKeyMapping = new HashMap<String, PrefInfo>()
    {{
        put("smtpserver", new PrefInfo(SMTPServAddressKey, PREFERENCE_TYPE.STRING));
        put("smtpusername", new PrefInfo(SMTPUsernameKey, PREFERENCE_TYPE.STRING));
        put("smtppw", new PrefInfo(SMTPPWKey, PREFERENCE_TYPE.STRING));
        put("sendnormaltemp", new PrefInfo(SendNormalTempKey, PREFERENCE_TYPE.BOOLEAN));
        put("sendhightemp", new PrefInfo(SendHighTempKey, PREFERENCE_TYPE.BOOLEAN));
        put("sendscannedpicture", new PrefInfo(SendPicKey, PREFERENCE_TYPE.BOOLEAN));
        put("sendscannedtemp", new PrefInfo(SendTempReadingKey, PREFERENCE_TYPE.BOOLEAN));
        put("sendname", new PrefInfo(SendNameKey, PREFERENCE_TYPE.BOOLEAN));
        put("ezpasspwkey", new PrefInfo(EZPassPwKey, PREFERENCE_TYPE.STRING));
        put("smtpsslport", new PrefInfo(SmtpSSLPortKey, PREFERENCE_TYPE.INT));
        put("receivingemail", new PrefInfo(ReceivingEmailKey, PREFERENCE_TYPE.STRING));
        put("deviceid", new PrefInfo(DeviceIdKey, PREFERENCE_TYPE.STRING));
        put("authtype", new PrefInfo(AuthTypeKey, PREFERENCE_TYPE.STRING));
        put("smtpfromaddress", new PrefInfo(SMTPFromAddressKey, PREFERENCE_TYPE.STRING));
        put("autodeleterecords", new PrefInfo(AutoDeleteRecordKey, PREFERENCE_TYPE.BOOLEAN));
        put("sendrecordseod", new PrefInfo(SendRecordEODKey, PREFERENCE_TYPE.BOOLEAN));
        put("printscannedtemp", new PrefInfo(PrintTempKey, PREFERENCE_TYPE.BOOLEAN));
        put("printdateofscan", new PrefInfo(PrintDateKey, PREFERENCE_TYPE.BOOLEAN));
        put("printtimeofscan", new PrefInfo(PrintTimeKey, PREFERENCE_TYPE.BOOLEAN));
        put("printname", new PrefInfo(PrintNameKey, PREFERENCE_TYPE.BOOLEAN));
        put("printcompanyname", new PrefInfo(PrintCompanyNameKey, PREFERENCE_TYPE.BOOLEAN));
        put("printsignatureline", new PrefInfo(PrintSignatureLine, PREFERENCE_TYPE.BOOLEAN));
        put("printimage", new PrefInfo(PrintImageKey, PREFERENCE_TYPE.BOOLEAN));
        put("printhightempbadge", new PrefInfo(PrintFailedTempBadge, PREFERENCE_TYPE.BOOLEAN));
        put("checkmarkprinttype", new PrefInfo(CheckmarkPrintType, PREFERENCE_TYPE.INT));
        put("hightempbadgeline1", new PrefInfo(FailedBadgeLine1, PREFERENCE_TYPE.STRING));
        put("hightempbadgeline2", new PrefInfo(FailedBadgeLine2, PREFERENCE_TYPE.STRING));
        put("hightempbadgeline3", new PrefInfo(FailedBadgeLine3, PREFERENCE_TYPE.STRING));
        put("hightempbadgeline4", new PrefInfo(FailedBadgeLine4, PREFERENCE_TYPE.STRING));
        put("disablebadgeprinting", new PrefInfo(BadgePrintingDisabled, PREFERENCE_TYPE.BOOLEAN));
        put("scandelay", new PrefInfo(ScanDelay, PREFERENCE_TYPE.INT));
        put("enablescandelay", new PrefInfo(ScanDelayEnabled, PREFERENCE_TYPE.BOOLEAN));
        put("companyname", new PrefInfo(CompanyName, PREFERENCE_TYPE.STRING));
        put("sqlservername", new PrefInfo(SQLServerName, PREFERENCE_TYPE.STRING));
        put("sqldatabasename", new PrefInfo(SQLDatabaseName, PREFERENCE_TYPE.STRING));
        put("sqltablename", new PrefInfo(SQLTableName, PREFERENCE_TYPE.STRING));
        put("sqlusername", new PrefInfo(SQLUsername, PREFERENCE_TYPE.STRING));
        put("sqlpassword", new PrefInfo(SQLPassword, PREFERENCE_TYPE.STRING));
    }};

    public enum PREFERENCE_TYPE {
        INT,
        STRING,
        BOOLEAN
    }
}
