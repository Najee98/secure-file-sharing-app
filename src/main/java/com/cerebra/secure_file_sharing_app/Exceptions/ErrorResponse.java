package com.cerebra.secure_file_sharing_app.Exceptions;

import lombok.*;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private String message;
    private int status;
    private String path;
    private long timestamp;
    private Map<String, String> validationErrors;
}
