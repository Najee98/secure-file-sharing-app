package com.cerebra.secure_file_sharing_app.Services;

import com.cerebra.secure_file_sharing_app.Security.DTO.*;

public interface AuthService {
    OTPResponse requestOTP(String phoneNumber);
    AuthResponse verifyOTP(String phoneNumber, String otp);
}