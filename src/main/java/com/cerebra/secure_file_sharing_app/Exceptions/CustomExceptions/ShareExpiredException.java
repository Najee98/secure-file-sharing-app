package com.cerebra.secure_file_sharing_app.Exceptions.CustomExceptions;

public class ShareExpiredException extends RuntimeException {
    public ShareExpiredException(String message) {
        super(message);
    }
}