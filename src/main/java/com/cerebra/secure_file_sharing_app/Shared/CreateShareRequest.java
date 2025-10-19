package com.cerebra.secure_file_sharing_app.Shared;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to create a shareable link")
public class CreateShareRequest {
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$|^$", message = "Invalid phone number format")
    @Schema(description = "Recipient phone number (optional)", example = "+1234567890")
    private String recipientPhone;
    
    @Schema(description = "Optional message to include in SMS", example = "Check out this file!")
    private String message;
}