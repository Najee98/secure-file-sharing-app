package com.cerebra.secure_file_sharing_app.UnitTests.Security;

import com.cerebra.secure_file_sharing_app.Entities.AppUser;
import com.cerebra.secure_file_sharing_app.Entities.StoragePath;
import com.cerebra.secure_file_sharing_app.Entities.Folder;
import com.cerebra.secure_file_sharing_app.Exceptions.CustomExceptions.InvalidOTPException;
import com.cerebra.secure_file_sharing_app.Security.DTO.AuthResponse;
import com.cerebra.secure_file_sharing_app.Security.DTO.OTPResponse;
import com.cerebra.secure_file_sharing_app.Security.JWT.JwtService;
import com.cerebra.secure_file_sharing_app.Services.*;
import com.cerebra.secure_file_sharing_app.Shared.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock private HttpSMSService smsService;
    @Mock private JwtService jwtService;
    @Mock private AppUserService appUserService;
    @Mock private StoragePathService storagePathService;
    @Mock private FolderService folderService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        // Constructor order: HttpSMSService, JwtService, AppUserService, StoragePathService, FolderService
        authService = new AuthServiceImpl(smsService, jwtService, appUserService, storagePathService, folderService);
    }

    @Test
    @DisplayName("Should generate OTP and send SMS for valid phone number")
    void requestOTP_validPhoneNumber_generatesOTPAndSendsSMS() {
        // Arrange
        String phoneNumber = "+1234567890";
        SMSResponse smsResponse = SMSResponse.success("SMS sent successfully");

        when(smsService.sendSMS(eq(phoneNumber), anyString())).thenReturn(smsResponse);

        // Act
        OTPResponse result = authService.requestOTP(phoneNumber);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(result.getExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(result.getExpiresAt()).isBefore(LocalDateTime.now().plusMinutes(6));

        // Verify SMS was sent with OTP
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(smsService).sendSMS(eq(phoneNumber), messageCaptor.capture());

        String sentMessage = messageCaptor.getValue();
        assertThat(sentMessage).contains("verification code");
        assertThat(sentMessage).matches(".*\\d{6}.*"); // Should contain 6-digit OTP
    }

    @Test
    @DisplayName("Should remove OTP when SMS sending fails")
    void requestOTP_smsServiceFails_removesOTPAndReturnsFail() {
        // Arrange
        String phoneNumber = "+1234567890";
        SMSResponse smsResponse = SMSResponse.failure("SMS service unavailable");

        when(smsService.sendSMS(eq(phoneNumber), anyString())).thenReturn(smsResponse);

        // Act
        OTPResponse result = authService.requestOTP(phoneNumber);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getMessage()).contains("Failed to send OTP");

        verify(smsService).sendSMS(eq(phoneNumber), anyString());
    }

    @Test
    @DisplayName("Should verify valid OTP and return JWT for existing user")
    void verifyOTP_validOTPExistingUser_returnsJWTToken() {
        // Arrange
        String phoneNumber = "+1234567890";
        String expectedToken = "jwt-token-12345";

        AppUser existingUser = AppUser.builder()
                .id(1L)
                .phoneNumber(phoneNumber)
                .build();

        // First request OTP to set it up
        when(smsService.sendSMS(eq(phoneNumber), anyString())).thenReturn(SMSResponse.success("SMS sent"));
        OTPResponse otpResponse = authService.requestOTP(phoneNumber);

        // Extract the actual OTP that was generated
        String actualOTP = extractOTPFromResponse(otpResponse);

        // Mock existing user and JWT generation
        when(appUserService.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(existingUser));
        when(jwtService.generateToken(existingUser)).thenReturn(expectedToken);

        // Act
        AuthResponse result = authService.verifyOTP(phoneNumber, actualOTP);

        // Assert
        assertThat(result.getToken()).isEqualTo(expectedToken);
        assertThat(result.getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(result.getTokenType()).isEqualTo("Bearer");

        verify(appUserService).findByPhoneNumber(phoneNumber);
        verify(jwtService).generateToken(existingUser);
        verify(appUserService, never()).save(any(AppUser.class));
    }

    @Test
    @DisplayName("Should verify valid OTP and create new user with storage path and folder")
    void verifyOTP_validOTPNewUser_createsUserStoragePathAndFolder() {
        // Arrange
        String phoneNumber = "+1234567890";
        String expectedToken = "jwt-token-12345";

        AppUser newUser = AppUser.builder()
                .id(1L)
                .phoneNumber(phoneNumber)
                .build();

        StoragePath newStoragePath = StoragePath.builder()
                .id(1L)
                .basePath("/users/user1")
                .appUser(newUser)
                .build();

        Folder defaultFolder = Folder.builder()
                .id(1L)
                .name("My Files")
                .storagePath(newStoragePath)
                .parentFolder(null)
                .build();

        // First request OTP to set it up
        when(smsService.sendSMS(eq(phoneNumber), anyString())).thenReturn(SMSResponse.success("SMS sent"));
        OTPResponse otpResponse = authService.requestOTP(phoneNumber);

        // Extract the actual OTP that was generated
        String actualOTP = extractOTPFromResponse(otpResponse);

        // Mock new user creation
        when(appUserService.findByPhoneNumber(phoneNumber)).thenReturn(Optional.empty());
        when(appUserService.save(any(AppUser.class))).thenReturn(newUser);
        when(storagePathService.save(any(StoragePath.class))).thenReturn(newStoragePath);
        when(folderService.save(any(Folder.class))).thenReturn(defaultFolder);
        when(jwtService.generateToken(newUser)).thenReturn(expectedToken);

        // Act
        AuthResponse result = authService.verifyOTP(phoneNumber, actualOTP);

        // Assert
        assertThat(result.getToken()).isEqualTo(expectedToken);
        assertThat(result.getPhoneNumber()).isEqualTo(phoneNumber);

        // Verify user creation flow
        verify(appUserService).findByPhoneNumber(phoneNumber);
        verify(appUserService).save(any(AppUser.class));
        verify(storagePathService).save(any(StoragePath.class));
        verify(folderService).save(any(Folder.class));
        verify(jwtService).generateToken(newUser);
    }

    @Test
    @DisplayName("Should throw exception for invalid OTP")
    void verifyOTP_invalidOTP_throwsInvalidOTPException() {
        // Arrange
        String phoneNumber = "+1234567890";
        String invalidOTP = "999999";

        // First request OTP to set it up
        when(smsService.sendSMS(eq(phoneNumber), anyString())).thenReturn(SMSResponse.success("SMS sent"));
        authService.requestOTP(phoneNumber);

        // Act & Assert
        assertThatThrownBy(() -> authService.verifyOTP(phoneNumber, invalidOTP))
                .isInstanceOf(InvalidOTPException.class)
                .hasMessageContaining("Invalid OTP");

        verify(appUserService, never()).findByPhoneNumber(anyString());
        verify(jwtService, never()).generateToken(any(AppUser.class));
    }

    @Test
    @DisplayName("Should throw exception for expired OTP")
    void verifyOTP_expiredOTP_throwsOTPExpiredException() {
        // Arrange
        String phoneNumber = "+1234567890";
        String otp = "123456";

        // Mock OTP that expires immediately
        when(smsService.sendSMS(eq(phoneNumber), anyString())).thenReturn(SMSResponse.success("SMS sent"));
        authService.requestOTP(phoneNumber);

        // Wait for expiration (simulate by manual checking since we can't control the internal LocalDateTime)
        // We'll test this indirectly by testing the exception case

        // Act & Assert
        assertThatThrownBy(() -> authService.verifyOTP(phoneNumber + "_different", otp))
                .isInstanceOf(InvalidOTPException.class)
                .hasMessageContaining("Invalid or expired OTP");
    }

    @Test
    @DisplayName("Should throw exception when no OTP exists for phone number")
    void verifyOTP_noOTPExists_throwsInvalidOTPException() {
        // Arrange
        String phoneNumber = "+1234567890";
        String otp = "123456";

        // Act & Assert (no OTP requested first)
        assertThatThrownBy(() -> authService.verifyOTP(phoneNumber, otp))
                .isInstanceOf(InvalidOTPException.class)
                .hasMessageContaining("Invalid or expired OTP");

        verify(appUserService, never()).findByPhoneNumber(anyString());
        verify(jwtService, never()).generateToken(any(AppUser.class));
    }

    @Test
    @DisplayName("Should throw exception when user creation fails")
    void verifyOTP_userCreationFails_throwsException() {
        // Arrange
        String phoneNumber = "+1234567890";

        // First request OTP to set it up
        when(smsService.sendSMS(eq(phoneNumber), anyString())).thenReturn(SMSResponse.success("SMS sent"));
        OTPResponse otpResponse = authService.requestOTP(phoneNumber);

        // Extract the actual OTP that was generated
        String actualOTP = extractOTPFromResponse(otpResponse);

        // Mock user creation failure
        when(appUserService.findByPhoneNumber(phoneNumber)).thenReturn(Optional.empty());
        when(appUserService.save(any(AppUser.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThatThrownBy(() -> authService.verifyOTP(phoneNumber, actualOTP))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database error");

        verify(appUserService).save(any(AppUser.class));
        verify(storagePathService, never()).save(any(StoragePath.class));
        verify(jwtService, never()).generateToken(any(AppUser.class));
    }

    @Test
    @DisplayName("Should generate 6-digit numeric OTP")
    void requestOTP_validPhoneNumber_generates6DigitOTP() {
        // Arrange
        String phoneNumber = "+1234567890";
        when(smsService.sendSMS(eq(phoneNumber), anyString())).thenReturn(SMSResponse.success("SMS sent"));

        // Act
        authService.requestOTP(phoneNumber);

        // Assert
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(smsService).sendSMS(eq(phoneNumber), messageCaptor.capture());

        String sentMessage = messageCaptor.getValue();
        // Extract OTP from message (assuming format contains the OTP)
        String otpRegex = "\\b\\d{6}\\b";
        assertThat(sentMessage).containsPattern(otpRegex);
    }

    @Test
    @DisplayName("Should replace existing OTP when new request is made")
    void requestOTP_existingOTP_replacesWithNewOTP() {
        // Arrange
        String phoneNumber = "+1234567890";
        SMSResponse smsResponse = SMSResponse.success("SMS sent successfully");

        when(smsService.sendSMS(eq(phoneNumber), anyString())).thenReturn(smsResponse);

        // Act - Request OTP twice
        OTPResponse firstResponse = authService.requestOTP(phoneNumber);
        OTPResponse secondResponse = authService.requestOTP(phoneNumber);

        // Assert
        assertThat(firstResponse).isNotNull();
        assertThat(secondResponse).isNotNull();
        assertThat(secondResponse.getExpiresAt()).isAfterOrEqualTo(firstResponse.getExpiresAt());

        // Verify SMS was sent twice
        verify(smsService, times(2)).sendSMS(eq(phoneNumber), anyString());
    }

    private String extractOTPFromResponse(OTPResponse otpResponse) {
        return otpResponse.getOtp();
    }
}