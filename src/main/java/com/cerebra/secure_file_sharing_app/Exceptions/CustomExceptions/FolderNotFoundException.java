package com.cerebra.secure_file_sharing_app.Exceptions.CustomExceptions;

public class FolderNotFoundException extends RuntimeException {
    public FolderNotFoundException(String message) {
        super(message);
    }
}