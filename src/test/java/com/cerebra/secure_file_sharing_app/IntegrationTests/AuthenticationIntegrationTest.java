package com.cerebra.secure_file_sharing_app.IntegrationTests;

import com.cerebra.secure_file_sharing_app.Entities.*;
import com.cerebra.secure_file_sharing_app.Security.DTO.*;
import com.cerebra.secure_file_sharing_app.Services.*;
import com.cerebra.secure_file_sharing_app.Shared.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("Authentication Integration Tests")
class AuthenticationIntegrationTest extends BaseIntegrationTest {

    @MockitoBean
    private HttpSMSService smsService;

    @Test
    @DisplayName("Complete New User Journey: OTP → Verify → User/StoragePath/Folder Created → JWT → Access Protected Endpoint")
    void completeNewUserAuthenticationFlow() {
        String phoneNumber = "+1234567890";

        // Mock SMS service
        when(smsService.sendSMS(anyString(), anyString()))
                .thenReturn(SMSResponse.success("OTP sent"));

        // Step 1: Request OTP
        OTPRequest otpRequest = new OTPRequest(phoneNumber);
        ResponseEntity<OTPResponse> otpResponseEntity = restTemplate.postForEntity(
                baseUrl() + "/api/auth/request-otp",
                otpRequest,
                OTPResponse.class
        );

        assertThat(otpResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(otpResponseEntity.getBody()).isNotNull();

        OTPResponse otpResponse = otpResponseEntity.getBody();
        assertThat(otpResponse.isSuccess()).isTrue();
        assertThat(otpResponse.getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(otpResponse.getOtp()).isNotNull();
        assertThat(otpResponse.getExpiresAt()).isNotNull();

        String otp = otpResponse.getOtp();

        // Verify SMS was called with OTP
        verify(smsService, times(1)).sendSMS(eq(phoneNumber), contains(otp));

        // Step 2: Verify OTP and receive JWT
        OTPVerificationRequest verifyRequest = new OTPVerificationRequest(phoneNumber, otp);
        ResponseEntity<AuthResponse> authResponseEntity = restTemplate.postForEntity(
                baseUrl() + "/api/auth/verify-otp",
                verifyRequest,
                AuthResponse.class
        );

        assertThat(authResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(authResponseEntity.getBody()).isNotNull();

        AuthResponse authResponse = authResponseEntity.getBody();
        assertThat(authResponse.getToken()).isNotBlank();
        assertThat(authResponse.getTokenType()).isEqualTo("Bearer");
        assertThat(authResponse.getPhoneNumber()).isEqualTo(phoneNumber);

        String jwtToken = authResponse.getToken();

        // Step 3: Verify User Created in Database
        Optional<AppUser> userOpt = appUserRepository.findByPhoneNumber(phoneNumber);
        assertThat(userOpt).isPresent();
        AppUser user = userOpt.get();
        assertThat(user.getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(user.getId()).isNotNull();

        // Step 4: Verify StoragePath Created
        Optional<StoragePath> storagePathOpt = storagePathRepository.findByAppUserId(user.getId());
        assertThat(storagePathOpt).isPresent();
        StoragePath storagePath = storagePathOpt.get();
        assertThat(storagePath.getBasePath()).contains("user" + user.getId());

        // Step 5: Verify Default "My Files" Folder Created
        List<Folder> folders = folderRepository.findByStoragePathId(storagePath.getId());
        assertThat(folders).hasSize(1);
        Folder defaultFolder = folders.get(0);
        assertThat(defaultFolder.getName()).isEqualTo("My Files");
        assertThat(defaultFolder.getParentFolder()).isNull(); // Root level

        // Step 6: Use JWT to Access Protected Endpoint
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<List> foldersResponse = restTemplate.exchange(
                baseUrl() + "/api/folders/my-folders",
                HttpMethod.GET,
                requestEntity,
                List.class
        );

        assertThat(foldersResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(foldersResponse.getBody()).isNotNull();
        assertThat(foldersResponse.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("Existing User Re-authentication: No Duplicate User/StoragePath/Folder Created")
    void existingUserReauthentication() {
        String phoneNumber = "+9876543210";

        // Mock SMS service
        when(smsService.sendSMS(anyString(), anyString()))
                .thenReturn(SMSResponse.success("OTP sent"));

        // ========== First Authentication ==========
        // Request OTP
        OTPRequest otpRequest1 = new OTPRequest(phoneNumber);
        ResponseEntity<OTPResponse> otpResponse1 = restTemplate.postForEntity(
                baseUrl() + "/api/auth/request-otp",
                otpRequest1,
                OTPResponse.class
        );

        assertThat(otpResponse1.getStatusCode()).isEqualTo(HttpStatus.OK);
        String otp1 = otpResponse1.getBody().getOtp();

        // Verify OTP
        OTPVerificationRequest verifyRequest1 = new OTPVerificationRequest(phoneNumber, otp1);
        ResponseEntity<AuthResponse> authResponse1 = restTemplate.postForEntity(
                baseUrl() + "/api/auth/verify-otp",
                verifyRequest1,
                AuthResponse.class
        );

        assertThat(authResponse1.getStatusCode()).isEqualTo(HttpStatus.OK);
        String firstJwt = authResponse1.getBody().getToken();

        // Verify initial DB state
        long userCountAfterFirst = appUserRepository.count();
        long storagePathCountAfterFirst = storagePathRepository.count();
        long folderCountAfterFirst = folderRepository.count();

        assertThat(userCountAfterFirst).isEqualTo(1);
        assertThat(storagePathCountAfterFirst).isEqualTo(1);
        assertThat(folderCountAfterFirst).isEqualTo(1);

        // ========== Second Authentication (Same User) ==========
        // Request OTP again
        OTPRequest otpRequest2 = new OTPRequest(phoneNumber);
        ResponseEntity<OTPResponse> otpResponse2 = restTemplate.postForEntity(
                baseUrl() + "/api/auth/request-otp",
                otpRequest2,
                OTPResponse.class
        );

        assertThat(otpResponse2.getStatusCode()).isEqualTo(HttpStatus.OK);
        String otp2 = otpResponse2.getBody().getOtp();

        // Verify OTP again
        OTPVerificationRequest verifyRequest2 = new OTPVerificationRequest(phoneNumber, otp2);
        ResponseEntity<AuthResponse> authResponse2 = restTemplate.postForEntity(
                baseUrl() + "/api/auth/verify-otp",
                verifyRequest2,
                AuthResponse.class
        );

        assertThat(authResponse2.getStatusCode()).isEqualTo(HttpStatus.OK);
        String secondJwt = authResponse2.getBody().getToken();

        // Verify NO duplicate records created
        long userCountAfterSecond = appUserRepository.count();
        long storagePathCountAfterSecond = storagePathRepository.count();
        long folderCountAfterSecond = folderRepository.count();

        assertThat(userCountAfterSecond).isEqualTo(1); // Still 1 user
        assertThat(storagePathCountAfterSecond).isEqualTo(1); // Still 1 storage path
        assertThat(folderCountAfterSecond).isEqualTo(1); // Still 1 folder

        // Both JWTs should work for accessing protected endpoints
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(secondJwt);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<List> foldersResponse = restTemplate.exchange(
                baseUrl() + "/api/folders/my-folders",
                HttpMethod.GET,
                requestEntity,
                List.class
        );

        assertThat(foldersResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Public Shared Link Endpoint Accessible Without JWT")
    void publicEndpointAccessibleWithoutAuthentication() {
        // This test verifies SecurityConfig allows public access to /public/shared/*

        // Try to access a public endpoint without JWT
        // (will fail with 404/500 since link doesn't exist, but NOT 401/403)
        ResponseEntity<String> publicResponse = restTemplate.getForEntity(
                baseUrl() + "/public/shared/non-existent-token-123",
                String.class
        );

        // Should NOT be 401 (Unauthorized) or 403 (Forbidden) - those would mean auth is required
        assertThat(publicResponse.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(publicResponse.getStatusCode()).isNotEqualTo(HttpStatus.FORBIDDEN);

        // Verify other endpoints still require authentication
        ResponseEntity<String> protectedResponse = restTemplate.getForEntity(
                baseUrl() + "/api/folders/my-folders",
                String.class
        );

        assertThat(protectedResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Protected Endpoints Reject Missing or Invalid JWT")
    void protectedEndpointsRequireValidJWT() {
        // Test 1: No Authorization header
        ResponseEntity<String> noAuthResponse = restTemplate.getForEntity(
                baseUrl() + "/api/folders/my-folders",
                String.class
        );
        assertThat(noAuthResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // Test 2: Invalid JWT format
        HttpHeaders invalidHeaders = new HttpHeaders();
        invalidHeaders.set("Authorization", "Bearer invalid.jwt.token");
        HttpEntity<Void> invalidRequest = new HttpEntity<>(invalidHeaders);

        ResponseEntity<String> invalidResponse = restTemplate.exchange(
                baseUrl() + "/api/folders/my-folders",
                HttpMethod.GET,
                invalidRequest,
                String.class
        );
        assertThat(invalidResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // Test 3: Empty Bearer token
        HttpHeaders emptyHeaders = new HttpHeaders();
        emptyHeaders.set("Authorization", "Bearer ");
        HttpEntity<Void> emptyRequest = new HttpEntity<>(emptyHeaders);

        ResponseEntity<String> emptyResponse = restTemplate.exchange(
                baseUrl() + "/api/folders/my-folders",
                HttpMethod.GET,
                emptyRequest,
                String.class
        );
        assertThat(emptyResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}