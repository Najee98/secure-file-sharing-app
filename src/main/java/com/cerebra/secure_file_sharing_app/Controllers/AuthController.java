package com.cerebra.secure_file_sharing_app.Controllers;

import com.cerebra.secure_file_sharing_app.Security.DTO.AuthResponse;
import com.cerebra.secure_file_sharing_app.Security.DTO.OTPRequest;
import com.cerebra.secure_file_sharing_app.Security.DTO.OTPResponse;
import com.cerebra.secure_file_sharing_app.Security.DTO.OTPVerificationRequest;
import com.cerebra.secure_file_sharing_app.Services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "SMS-based OTP authentication endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/request-otp")
    @Operation(
        summary = "Request OTP",
        description = "Send a 6-digit OTP to the specified phone number. OTP is valid for 5 minutes."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "OTP sent successfully",
            content = @Content(schema = @Schema(implementation = OTPResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid phone number format"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "SMS service unavailable"
        )
    })
    public ResponseEntity<OTPResponse> requestOTP(@Valid @RequestBody OTPRequest request) {
        OTPResponse response = authService.requestOTP(request.getPhoneNumber());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    @Operation(
        summary = "Verify OTP",
        description = "Verify the OTP and receive a JWT token for authentication"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Authentication successful",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid OTP or phone number"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "OTP expired or invalid"
        )
    })
    public ResponseEntity<AuthResponse> verifyOTP(@Valid @RequestBody OTPVerificationRequest request) {
        AuthResponse response = authService.verifyOTP(request.getPhoneNumber(), request.getOtp());
        return ResponseEntity.ok(response);
    }
}
