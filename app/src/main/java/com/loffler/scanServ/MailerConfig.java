package com.loffler.scanServ;

enum Authentication {None, SSL, TLS}

public class MailerConfig {
    public double TempLimit;
    public boolean SendHighTemp;
    public boolean SendNormalTemp;
    public boolean SendTempReading;
    public boolean SendPicture;
    public boolean SendName;
    public String SmtpServerAddress;
    public String SmtpUsername;
    public String SmtpPw;
    public String EzPassPw;
    public int SmtpPort;
    public String ReceivingEmailAddress;
    public Authentication AuthType;
    public String DeviceId;
    public String SmtpFromAddress;
    public Boolean IsTestMail;
    public boolean AutoDeleteRecord;
    public boolean SendRecordsAtEndOfDay;

    public MailerConfig(double tempLimit, boolean sendHighTemp, boolean sendNormalTemp,
                        boolean sendPicture, boolean sendTempReading, boolean sendName, String smtpServAddress,
                        String smtpUsername, String smtpPw, String ezPassPw, int smtpPort,
                        String receivingEmailAddress, Authentication authType, String deviceId, String smtpFromAddress,
                        Boolean isTestMail, Boolean autoDeleteRecord, Boolean sendRecordAtEndOfDay) {
        TempLimit = tempLimit;
        SendHighTemp = sendHighTemp;
        SendNormalTemp = sendNormalTemp;
        SendPicture = sendPicture;
        SendTempReading = sendTempReading;
        SendName = sendName;
        DeviceId = deviceId;
        SmtpServerAddress = smtpServAddress;
        SmtpUsername = smtpUsername;
        SmtpPw = smtpPw;
        EzPassPw = ezPassPw;
        SmtpPort = smtpPort;
        AuthType = authType;
        ReceivingEmailAddress = receivingEmailAddress;
        SmtpFromAddress = smtpFromAddress;
        IsTestMail = isTestMail;
        AutoDeleteRecord = autoDeleteRecord;
        SendRecordsAtEndOfDay = sendRecordAtEndOfDay;
    }
}
