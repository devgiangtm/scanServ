package com.loffler.scanServ;

/**
 * Created by Eric on 7/13/2020.
 */
class MailSenderResponse {
    public Boolean IsTestEmail;
    public Boolean Success;
    public String ErrorMessage;
    MailSenderResponse(boolean isTestEmail, boolean success, String errorMessage){
        IsTestEmail = isTestEmail;
        Success = success;
        ErrorMessage = errorMessage;
    }
}