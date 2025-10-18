package com.cerebra.secure_file_sharing_app.Exceptions.CustomExceptions;

public class OTPExpiredException extends RuntimeException {
    public OTPExpiredException(String message) {
        super(message);
    }
}