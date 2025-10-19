package com.cerebra.secure_file_sharing_app.Exceptions.CustomExceptions;

public class InvalidFolderNameException extends RuntimeException {
    public InvalidFolderNameException(String message) {
        super(message);
    }
}