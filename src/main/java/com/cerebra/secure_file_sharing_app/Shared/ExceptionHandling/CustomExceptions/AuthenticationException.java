package com.cerebra.secure_file_sharing_app.Shared.ExceptionHandling.CustomExceptions;

public class AuthenticationException extends RuntimeException{

    public AuthenticationException(String exceptionMessage) {
        super(exceptionMessage);
    }

    public AuthenticationException(String exceptionMessage, Throwable exceptionCause) {
        super(exceptionMessage, exceptionCause);
    }

}