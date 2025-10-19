package com.cerebra.secure_file_sharing_app.Exceptions.CustomExceptions;

public class FolderAccessDeniedException extends RuntimeException {
    public FolderAccessDeniedException(String message) {
        super(message);
    }
}