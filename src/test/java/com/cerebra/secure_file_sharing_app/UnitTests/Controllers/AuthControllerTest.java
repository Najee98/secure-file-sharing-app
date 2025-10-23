package com.cerebra.secure_file_sharing_app.UnitTests.Controllers;

import com.cerebra.secure_file_sharing_app.Controllers.AuthController;
import com.cerebra.secure_file_sharing_app.Exceptions.CustomExceptions.InvalidOTPException;
import com.cerebra.secure_file_sharing_app.Exceptions.CustomExceptions.OTPExpiredException;
import com.cerebra.secure_file_sharing_app.Exceptions.GlobalExceptionHandler;
import com.cerebra.secure_file_sharing_app.Security.DTO.AuthResponse;
import com.cerebra.secure_file_sharing_app.Security.DTO.OTPResponse;
import com.cerebra.secure_file_sharing_app.Services.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        AuthController authController = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Should request OTP successfully with valid phone number")
    void requestOTP_validPhoneNumber_returnsOTPResponse() throws Exception {
        // Arrange
        String phoneNumber = "+1234567890";
        String requestJson = """
            {
                "phoneNumber": "%s"
            }
            """.formatted(phoneNumber);

        OTPResponse otpResponse = OTPResponse.builder()
                .phoneNumber(phoneNumber)
                .message("OTP sent successfully")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(authService.requestOTP(phoneNumber)).thenReturn(otpResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.phoneNumber").value(phoneNumber))
                .andExpect(jsonPath("$.message").value("OTP sent successfully"))
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    @DisplayName("Should verify OTP successfully with valid credentials")
    void verifyOTP_validCredentials_returnsAuthResponse() throws Exception {
        // Arrange
        String phoneNumber = "+1234567890";
        String otp = "123456";
        String requestJson = """
            {
                "phoneNumber": "%s",
                "otp": "%s"
            }
            """.formatted(phoneNumber, otp);

        AuthResponse authResponse = AuthResponse.success("jwt-token-123", phoneNumber);

        when(authService.verifyOTP(phoneNumber, otp)).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.phoneNumber").value(phoneNumber))
                .andExpect(jsonPath("$.issuedAt").exists());
    }

    @Test
    @DisplayName("Should handle InvalidOTPException with UNAUTHORIZED status")
    void verifyOTP_invalidOTP_returnsUnauthorized() throws Exception {
        // Arrange
        String phoneNumber = "+1234567890";
        String otp = "999999";
        String requestJson = """
            {
                "phoneNumber": "%s",
                "otp": "%s"
            }
            """.formatted(phoneNumber, otp);

        when(authService.verifyOTP(phoneNumber, otp)).thenThrow(new InvalidOTPException("Invalid OTP"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Invalid or incorrect OTP"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Should handle OTPExpiredException with UNAUTHORIZED status")
    void verifyOTP_expiredOTP_returnsUnauthorized() throws Exception {
        // Arrange
        String phoneNumber = "+1234567890";
        String otp = "123456";
        String requestJson = """
            {
                "phoneNumber": "%s",
                "otp": "%s"
            }
            """.formatted(phoneNumber, otp);

        when(authService.verifyOTP(phoneNumber, otp)).thenThrow(new OTPExpiredException("OTP has expired"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("OTP has expired. Please request a new one"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Should handle service exceptions during OTP request")
    void requestOTP_serviceException_returnsInternalServerError() throws Exception {
        // Arrange
        String phoneNumber = "+1234567890";
        String requestJson = """
            {
                "phoneNumber": "%s"
            }
            """.formatted(phoneNumber);

        when(authService.requestOTP(phoneNumber)).thenThrow(new RuntimeException("SMS service unavailable"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.status").value(500));
    }
}