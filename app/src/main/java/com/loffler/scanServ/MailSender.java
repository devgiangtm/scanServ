package com.loffler.scanServ;

import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;

public class MailSender extends AsyncTask<Object, Void, MailSenderResponse> {
    private final String LOG_TAG = "MailSender";

    public interface AsyncResponseDelegate {
        void finish(MailSenderResponse resp);
    }

    public AsyncResponseDelegate finishDelegate = null;

    @Override
    protected MailSenderResponse doInBackground(Object... params) {
        // retrieve relevant json out of the body
        final MailerConfig mailConfig = (MailerConfig) params[1];

        boolean result = true;
        String errorMessage = "No error";

        Properties props = new Properties();
        props.put("mail.smtp.host", mailConfig.SmtpServerAddress);
        props.put("mail.smtp.timeout", 10000);
        props.put("mail.smtp.connectiontimeout", 10000);

        if (mailConfig.AuthType == Authentication.SSL || mailConfig.AuthType == Authentication.TLS) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.socketFactory.port", mailConfig.SmtpPort);
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        }
        // else no auth

        props.put("mail.smtp.port", mailConfig.SmtpPort);

        Session session;

        if (mailConfig.SmtpPw.isEmpty()) {
            props.put("mail.smtp.auth", "false");
            session = Session.getInstance(props);
        } else {
            props.put("mail.smtp.auth", "true");
            session = Session.getInstance(props, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(mailConfig.SmtpUsername, mailConfig.SmtpPw);
                }
            });
        }

        // TODO fix this dirty hack.. no time. needs to go out now.
        // TODO: This needs a total refactor for methods to do of each type of email to be sent.. as of now its all jumbled in this
        // TODO: mess of a method that has bloated too far
        if (params.length >= 3){
            return SendCSV(params[2].toString(), session, mailConfig);
        }

        JSONObject reqJson = new JSONObject();
        if (!mailConfig.IsTestMail) reqJson = (JSONObject) params[0];

        try {
            MimeMessage mm = new MimeMessage(session);
            mm.setFrom(new InternetAddress(mailConfig.SmtpFromAddress));
            mm.addRecipient(Message.RecipientType.TO, new InternetAddress(mailConfig.ReceivingEmailAddress));
            String subjectLine;
            double tempReading = 0;

            if (!mailConfig.IsTestMail) tempReading = reqJson.getDouble("temperature");

            // high temp is considered to be at the limit or above >=
            if (mailConfig.IsTestMail) {
                subjectLine = "Temperature Test Email";
            } else if (mailConfig.SendHighTemp && tempReading >= mailConfig.TempLimit) {
                subjectLine = "ALERT: New High Temperature Recorded";
            } else if (mailConfig.SendNormalTemp && tempReading < mailConfig.TempLimit) {
                subjectLine = "New Normal Temperature Recorded";
            } else // we don't sending of high or normal temp.. so send nothing... hack of returning null as well
                return null;

            mm.setSubject(subjectLine);

            String initString = "Test email!";
            if (!mailConfig.IsTestMail) {
                Long timeTaken = reqJson.getLong("checkTime");
                final Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(timeTaken);
                DateFormat dateFormatter = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.LONG, SimpleDateFormat.MEDIUM);
                String stringTime = dateFormatter.format(cal.getTime());
                initString = "Time Taken: " + stringTime;
            }

            StringBuilder sb = new StringBuilder(initString);
            sb.append("\n");

            if (mailConfig.SendTempReading && !mailConfig.IsTestMail) {
                // convert to F from celsius
                tempReading = (tempReading * 9 / 5) + 32;

                sb.append("Temperature Recorded: ").append(tempReading).append(" F");
                sb.append("\n");
            }

            if (mailConfig.SendName && !mailConfig.IsTestMail) {
                String name = reqJson.getString("name");
                if (name.equals("")) name = "Stranger";
                sb.append("Name: ").append(name);
                sb.append("\n");
            }

            if (!mailConfig.IsTestMail) {
                int maskWorn = reqJson.getInt("mask");
                if (maskWorn == 0 || maskWorn == 1)
                    sb.append("Mask was worn: ").append(maskWorn != 0).append("\n");
            }

            sb.append("Device ID: ").append(mailConfig.DeviceId).append("\n\n");

            BodyPart bodyPart = new MimeBodyPart();
            Multipart bodyMultiPart = new MimeMultipart();
            bodyPart.setText(sb.toString());
            bodyMultiPart.addBodyPart(bodyPart);

            if (mailConfig.SendPicture && !mailConfig.IsTestMail) {
                bodyPart = new MimeBodyPart();
                String base64Pic = reqJson.getString("checkPic");
                final byte[] decodedPic = Base64.decode(base64Pic, Base64.DEFAULT);
                ByteArrayDataSource byteDataSource = new ByteArrayDataSource(decodedPic, "image/jpg");
                byteDataSource.setName("ScreenCapture.jpg");
                bodyPart.setDataHandler(new DataHandler((byteDataSource)));
                bodyPart.setFileName((byteDataSource.getName()));
                bodyMultiPart.addBodyPart(bodyPart);
            }

            mm.setContent(bodyMultiPart);

            Transport.send(mm);

        } catch (MessagingException e) {
            e.printStackTrace();
            result = false;
            errorMessage = e.getMessage();
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Exception transforming json: ");
            e.printStackTrace();
            result = false;
            errorMessage = e.getMessage();
        }

        return new MailSenderResponse(mailConfig.IsTestMail, result, errorMessage);
    }

    private MailSenderResponse SendCSV(String csvString, Session session, MailerConfig mailConfig) {

        try{
            MimeMessage mm = new MimeMessage(session);
            mm.setFrom(new InternetAddress(mailConfig.SmtpFromAddress));
            mm.addRecipient(Message.RecipientType.TO, new InternetAddress(mailConfig.ReceivingEmailAddress));
            mm.setSubject("Temperature Scan Record");
            MimeBodyPart bodyPart = new MimeBodyPart();
            bodyPart.setText("Recorded Scans are attached as a CSV");
            MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(bodyPart);
            bodyPart = new MimeBodyPart();
            bodyPart.setDataHandler(new DataHandler(new ByteArrayDataSource(csvString.getBytes(), "text/csv")));
            bodyPart.setFileName("records.csv");
            multipart.addBodyPart(bodyPart);
            mm.setContent(multipart);
            Transport.send(mm);
        }
        catch(Exception e){
            Log.e(LOG_TAG, "Error sending CSV Mail in mail sender: " + e.getMessage());
        }

        // TODO: hack of returning null, no time
        return null;
    }

    @Override
    protected void onPostExecute(MailSenderResponse mailSenderResponse) {

        if (mailSenderResponse != null && mailSenderResponse.IsTestEmail && finishDelegate != null)
            finishDelegate.finish(mailSenderResponse);
    }
}
