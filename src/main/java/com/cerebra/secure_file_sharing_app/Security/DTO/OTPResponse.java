package com.cerebra.secure_file_sharing_app.Security.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "OTP request response")
public class OTPResponse {
    
    @Schema(description = "Success status", example = "true")
    private boolean success;
    
    @Schema(description = "Response message", example = "OTP sent successfully")
    private String message;
    
    @Schema(description = "Phone number where OTP was sent", example = "+1234567890")
    private String phoneNumber;
    
    @Schema(description = "OTP expiration time")
    private LocalDateTime expiresAt;
    
    public static OTPResponse success(String phoneNumber, LocalDateTime expiresAt) {
        return OTPResponse.builder()
                .success(true)
                .message("OTP sent successfully")
                .phoneNumber(phoneNumber)
                .expiresAt(expiresAt)
                .build();
    }
    
    public static OTPResponse failure(String message) {
        return OTPResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}