package com.cerebra.secure_file_sharing_app.Shared;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SMSResponse {
    
    private boolean success;
    private String message;
    private LocalDateTime timestamp;
    
    public static SMSResponse success(String message) {
        return SMSResponse.builder()
                .success(true)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static SMSResponse failure(String message) {
        return SMSResponse.builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}