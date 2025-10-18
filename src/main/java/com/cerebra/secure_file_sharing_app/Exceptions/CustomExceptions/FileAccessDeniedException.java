package com.cerebra.secure_file_sharing_app.Exceptions.CustomExceptions;

public class FileAccessDeniedException extends RuntimeException {
    public FileAccessDeniedException(String message) {
        super(message);
    }
}
