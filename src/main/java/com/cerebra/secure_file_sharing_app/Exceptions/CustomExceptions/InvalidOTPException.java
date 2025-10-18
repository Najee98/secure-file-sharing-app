package com.cerebra.secure_file_sharing_app.Exceptions.CustomExceptions;

public class InvalidOTPException extends RuntimeException {
    public InvalidOTPException(String message) {
        super(message);
    }
}