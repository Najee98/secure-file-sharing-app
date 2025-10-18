package com.cerebra.secure_file_sharing_app.Shared.ExceptionHandling.CustomExceptions;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String exceptionMessage) {
        super(exceptionMessage);
    }

    public ResourceNotFoundException(String exceptionMessage, Throwable exceptionCause) {
        super(exceptionMessage, exceptionCause);
    }
}