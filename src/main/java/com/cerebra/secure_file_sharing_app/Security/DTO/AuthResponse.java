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
@Schema(description = "Authentication response with JWT token")
public class AuthResponse {
    
    @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;
    
    @Schema(description = "Token type", example = "Bearer")
    private String tokenType = "Bearer";
    
    @Schema(description = "User's phone number", example = "+1234567890")
    private String phoneNumber;
    
    @Schema(description = "Token generation timestamp")
    private LocalDateTime issuedAt;
    
    public static AuthResponse success(String token, String phoneNumber) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .phoneNumber(phoneNumber)
                .issuedAt(LocalDateTime.now())
                .build();
    }
}