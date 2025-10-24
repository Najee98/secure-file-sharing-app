//package com.cerebra.secure_file_sharing_app.IntegrationTests.AuthenticationFlows;
//
//import com.cerebra.secure_file_sharing_app.Security.DTO.AuthResponse;
//import com.cerebra.secure_file_sharing_app.Security.DTO.OTPRequest;
//import com.cerebra.secure_file_sharing_app.Security.DTO.OTPResponse;
//import com.cerebra.secure_file_sharing_app.Security.DTO.OTPVerificationRequest;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.web.client.TestRestTemplate;
//import org.springframework.http.*;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.transaction.annotation.Transactional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@ActiveProfiles("test")
//@Transactional
//@DisplayName("Basic Authentication Flow Integration Tests")
//class BasicAuthenticationFlowIntegrationTest {
//
//    @Autowired
//    private TestRestTemplate restTemplate;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Test
//    @DisplayName("User Journey: Request OTP and verify for new user")
//    void userJourney_requestOTPAndVerifyForNewUser() throws Exception {
//        String phoneNumber = "+1234567890";
//
//        // Step 1: Request OTP
//        OTPRequest otpRequest = OTPRequest.builder()
//                .phoneNumber(phoneNumber)
//                .build();
//
//        ResponseEntity<OTPResponse> otpResponse = restTemplate.postForEntity(
//                "/api/auth/request-otp",
//                otpRequest,
//                OTPResponse.class
//        );
//
//        System.out.println("OTP Response Status: " + otpResponse.getStatusCode());
//        System.out.println("OTP Response Body: " + otpResponse.getBody());
//
//        assertThat(otpResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(otpResponse.getBody()).isNotNull();
//
//        // Step 2: Verify OTP
//        OTPVerificationRequest verifyRequest = OTPVerificationRequest.builder()
//                .phoneNumber(phoneNumber)
//                .otp("123456")  // This might be the problem
//                .build();
//
//        ResponseEntity<String> authResponseRaw = restTemplate.postForEntity(
//                "/api/auth/verify-otp",
//                verifyRequest,
//                String.class  // Get as String first to see what's returned
//        );
//
//        System.out.println("Auth Response Status: " + authResponseRaw.getStatusCode());
//        System.out.println("Auth Response Body: " + authResponseRaw.getBody());
//        System.out.println("Auth Response Headers: " + authResponseRaw.getHeaders());
//
//        // Now try with proper type
//        ResponseEntity<AuthResponse> authResponse = restTemplate.postForEntity(
//                "/api/auth/verify-otp",
//                verifyRequest,
//                AuthResponse.class
//        );
//
//        assertThat(authResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(authResponse.getBody()).isNotNull();
//    }
//
//    @Test
//    @DisplayName("User Journey: Login logout and relogin")
//    void userJourney_loginLogoutAndRelogin() throws Exception {
//        String phoneNumber = "+1987654321";
//
//        // Step 1: First login - Request OTP
//        OTPRequest otpRequest = OTPRequest.builder()
//                .phoneNumber(phoneNumber)
//                .build();
//
//        ResponseEntity<OTPResponse> firstOtpResponse = restTemplate.postForEntity(
//                "/api/auth/request-otp",
//                otpRequest,
//                OTPResponse.class
//        );
//
//        assertThat(firstOtpResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
//
//        // Step 2: Verify OTP and get token
//        OTPVerificationRequest verifyRequest = OTPVerificationRequest.builder()
//                .phoneNumber(phoneNumber)
//                .otp("123456")
//                .build();
//
//        ResponseEntity<AuthResponse> firstAuthResponse = restTemplate.postForEntity(
//                "/api/auth/verify-otp",
//                verifyRequest,
//                AuthResponse.class
//        );
//
//        assertThat(firstAuthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
//        String firstToken = firstAuthResponse.getBody().getToken();
//
//        // Step 3: Use token to access protected endpoint
//        HttpHeaders headers = new HttpHeaders();
//        headers.setBearerAuth(firstToken);
//        HttpEntity<String> entity = new HttpEntity<>(headers);
//
//        ResponseEntity<String> protectedResponse = restTemplate.exchange(
//                "/api/files/my-files",
//                HttpMethod.GET,
//                entity,
//                String.class
//        );
//
//        assertThat(protectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
//
//        // Step 4: Relogin (request new OTP)
//        ResponseEntity<OTPResponse> secondOtpResponse = restTemplate.postForEntity(
//                "/api/auth/request-otp",
//                otpRequest,
//                OTPResponse.class
//        );
//
//        assertThat(secondOtpResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
//
//        // Step 5: Verify new OTP and get new token
//        ResponseEntity<AuthResponse> secondAuthResponse = restTemplate.postForEntity(
//                "/api/auth/verify-otp",
//                verifyRequest,
//                AuthResponse.class
//        );
//
//        assertThat(secondAuthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
//        String secondToken = secondAuthResponse.getBody().getToken();
//
//        // Step 6: Verify new token works
//        HttpHeaders newHeaders = new HttpHeaders();
//        newHeaders.setBearerAuth(secondToken);
//        HttpEntity<String> newEntity = new HttpEntity<>(newHeaders);
//
//        ResponseEntity<String> newProtectedResponse = restTemplate.exchange(
//                "/api/files/my-files",
//                HttpMethod.GET,
//                newEntity,
//                String.class
//        );
//
//        assertThat(newProtectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
//    }
//}