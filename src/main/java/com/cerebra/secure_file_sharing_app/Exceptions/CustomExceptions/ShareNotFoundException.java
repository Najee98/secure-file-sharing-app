package com.cerebra.secure_file_sharing_app.Exceptions.CustomExceptions;

public class ShareNotFoundException extends RuntimeException {
    public ShareNotFoundException(String message) {
        super(message);
    }
}