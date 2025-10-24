//package com.cerebra.secure_file_sharing_app.IntegrationTests.SharingFlows;
//
//import com.cerebra.secure_file_sharing_app.Security.DTO.AuthResponse;
//import com.cerebra.secure_file_sharing_app.Security.DTO.OTPRequest;
//import com.cerebra.secure_file_sharing_app.Security.DTO.OTPResponse;
//import com.cerebra.secure_file_sharing_app.Security.DTO.OTPVerificationRequest;
//import com.cerebra.secure_file_sharing_app.Shared.CreateShareRequest;
//import com.cerebra.secure_file_sharing_app.Shared.FileUploadResponse;
//import com.cerebra.secure_file_sharing_app.Shared.ShareResponse;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.web.client.TestRestTemplate;
//import org.springframework.core.io.ByteArrayResource;
//import org.springframework.http.*;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.util.LinkedMultiValueMap;
//import org.springframework.util.MultiValueMap;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@ActiveProfiles("test")
//@Transactional
//@DisplayName("Basic Sharing Flow Integration Tests")
//class BasicSharingFlowIntegrationTest {
//
//    @Autowired
//    private TestRestTemplate restTemplate;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    private String authToken;
//    private String phoneNumber = "+1234567890";
//
//    @BeforeEach
//    void authenticateUser() throws Exception {
//        // Authenticate user before each test
//        OTPRequest otpRequest = OTPRequest.builder()
//                .phoneNumber(phoneNumber)
//                .build();
//
//        restTemplate.postForEntity("/api/auth/request-otp", otpRequest, OTPResponse.class);
//
//        OTPVerificationRequest verifyRequest = OTPVerificationRequest.builder()
//                .phoneNumber(phoneNumber)
//                .otp("123456")
//                .build();
//
//        ResponseEntity<AuthResponse> authResponse = restTemplate.postForEntity(
//                "/api/auth/verify-otp",
//                verifyRequest,
//                AuthResponse.class
//        );
//
//        this.authToken = authResponse.getBody().getToken();
//    }
//
//    @Test
//    @DisplayName("User Journey: Upload file and share without SMS notification")
//    void userJourney_uploadFileAndShareWithoutSMS() throws Exception {
//        // Step 1: Upload a file
//        byte[] fileContent = "Content to be shared".getBytes();
//        ByteArrayResource fileResource = new ByteArrayResource(fileContent) {
//            @Override
//            public String getFilename() {
//                return "shared-document.txt";
//            }
//        };
//
//        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
//        parts.add("file", fileResource);
//
//        HttpHeaders uploadHeaders = new HttpHeaders();
//        uploadHeaders.setBearerAuth(authToken);
//        uploadHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
//
//        HttpEntity<MultiValueMap<String, Object>> uploadEntity = new HttpEntity<>(parts, uploadHeaders);
//
//        ResponseEntity<FileUploadResponse> uploadResponse = restTemplate.postForEntity(
//                "/api/files/upload",
//                uploadEntity,
//                FileUploadResponse.class
//        );
//
//        assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
//        Long fileId = uploadResponse.getBody().getFileId();
//
//        // Step 2: Share the file without SMS
//        CreateShareRequest shareRequest = CreateShareRequest.builder()
//                .recipientPhone("")
//                .build();
//
//        HttpHeaders shareHeaders = new HttpHeaders();
//        shareHeaders.setBearerAuth(authToken);
//        shareHeaders.setContentType(MediaType.APPLICATION_JSON);
//
//        HttpEntity<CreateShareRequest> shareEntity = new HttpEntity<>(shareRequest, shareHeaders);
//
//        ResponseEntity<ShareResponse> shareResponse = restTemplate.postForEntity(
//                "/api/files/" + fileId + "/share",
//                shareEntity,
//                ShareResponse.class
//        );
//
//        assertThat(shareResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(shareResponse.getBody()).isNotNull();
//        assertThat(shareResponse.getBody().getShareId()).isNotNull();
//        assertThat(shareResponse.getBody().getLinkToken()).isNotNull();
//        assertThat(shareResponse.getBody().getShareUrl()).isNotNull();
//        assertThat(shareResponse.getBody().getItemType()).isEqualTo("file");
//        assertThat(shareResponse.getBody().getItemName()).isEqualTo("shared-document.txt");
//        assertThat(shareResponse.getBody().getExpiresAt()).isNotNull();
//
//        String linkToken = shareResponse.getBody().getLinkToken();
//
//        // Step 3: Access file via public share link (no authentication needed)
//        ResponseEntity<byte[]> downloadResponse = restTemplate.getForEntity(
//                "/public/shared/" + linkToken,
//                byte[].class
//        );
//
//        assertThat(downloadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(downloadResponse.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
//        assertThat(downloadResponse.getHeaders().getFirst("Content-Disposition"))
//                .contains("attachment; filename=\"shared-document.txt\"");
//        assertThat(downloadResponse.getBody()).isEqualTo(fileContent);
//
//        // Step 4: Verify share appears in user's shares
//        HttpHeaders getHeaders = new HttpHeaders();
//        getHeaders.setBearerAuth(authToken);
//        HttpEntity<String> getEntity = new HttpEntity<>(getHeaders);
//
//        ResponseEntity<String> mySharesResponse = restTemplate.exchange(
//                "/api/shared/my-shares",
//                HttpMethod.GET,
//                getEntity,
//                String.class
//        );
//
//        assertThat(mySharesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(mySharesResponse.getBody()).contains("shared-document.txt");
//        assertThat(mySharesResponse.getBody()).contains("file");
//    }
//
//    @Test
//    @DisplayName("User Journey: Upload file and share with SMS notification")
//    void userJourney_uploadFileAndShareWithSMS() throws Exception {
//        // Step 1: Upload a file
//        byte[] fileContent = "Content to be shared with SMS".getBytes();
//        ByteArrayResource fileResource = new ByteArrayResource(fileContent) {
//            @Override
//            public String getFilename() {
//                return "sms-shared-document.txt";
//            }
//        };
//
//        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
//        parts.add("file", fileResource);
//
//        HttpHeaders uploadHeaders = new HttpHeaders();
//        uploadHeaders.setBearerAuth(authToken);
//        uploadHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
//
//        HttpEntity<MultiValueMap<String, Object>> uploadEntity = new HttpEntity<>(parts, uploadHeaders);
//
//        ResponseEntity<FileUploadResponse> uploadResponse = restTemplate.postForEntity(
//                "/api/files/upload",
//                uploadEntity,
//                FileUploadResponse.class
//        );
//
//        assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
//        Long fileId = uploadResponse.getBody().getFileId();
//
//        // Step 2: Share the file with SMS notification
//        CreateShareRequest shareRequest = CreateShareRequest.builder()
//                .recipientPhone("+1987654321")
//                .message("Check out this shared file!")
//                .build();
//
//        HttpHeaders shareHeaders = new HttpHeaders();
//        shareHeaders.setBearerAuth(authToken);
//        shareHeaders.setContentType(MediaType.APPLICATION_JSON);
//
//        HttpEntity<CreateShareRequest> shareEntity = new HttpEntity<>(shareRequest, shareHeaders);
//
//        ResponseEntity<ShareResponse> shareResponse = restTemplate.postForEntity(
//                "/api/files/" + fileId + "/share",
//                shareEntity,
//                ShareResponse.class
//        );
//
//        assertThat(shareResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(shareResponse.getBody().getItemName()).isEqualTo("sms-shared-document.txt");
//
//        String linkToken = shareResponse.getBody().getLinkToken();
//
//        // Step 3: Verify public access works
//        ResponseEntity<byte[]> downloadResponse = restTemplate.getForEntity(
//                "/public/shared/" + linkToken,
//                byte[].class
//        );
//
//        assertThat(downloadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(downloadResponse.getBody()).isEqualTo(fileContent);
//    }
//}