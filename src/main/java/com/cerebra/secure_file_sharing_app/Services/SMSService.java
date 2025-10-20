package com.cerebra.secure_file_sharing_app.Services;

import com.cerebra.secure_file_sharing_app.Shared.SMSResponse;

public interface SMSService {
    SMSResponse sendSMS(String phoneNumber, String message);
}