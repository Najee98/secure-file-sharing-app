package com.cerebra.secure_file_sharing_app.Exceptions.CustomExceptions;

public class FileNotFoundException extends RuntimeException {
    public FileNotFoundException(String message) {
        super(message);
    }
}