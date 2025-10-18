package com.cerebra.secure_file_sharing_app.Shared.ExceptionHandling;

import lombok.Data;

@Data
public class ErrorResponse {
    private String message;
    private int status;
    private long timestamp;
    private String path;

    public ErrorResponse(String message, int status, String path) {
        this.message = message;
        this.status = status;
        this.timestamp = System.currentTimeMillis();
        this.path = path;
    }

}
