package com.cerebra.secure_file_sharing_app.SMS;

import com.cerebra.secure_file_sharing_app.SMS.DTO.SMSResponse;

public interface SMSService {
    SMSResponse sendSMS(String phoneNumber, String message);
}