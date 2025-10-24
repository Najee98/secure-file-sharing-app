//package com.cerebra.secure_file_sharing_app.IntegrationTests.FileManagementFlows;
//
//import com.cerebra.secure_file_sharing_app.Security.DTO.AuthResponse;
//import com.cerebra.secure_file_sharing_app.Security.DTO.OTPRequest;
//import com.cerebra.secure_file_sharing_app.Security.DTO.OTPResponse;
//import com.cerebra.secure_file_sharing_app.Security.DTO.OTPVerificationRequest;
//import com.cerebra.secure_file_sharing_app.Shared.CreateFolderRequest;
//import com.cerebra.secure_file_sharing_app.Shared.FileUploadResponse;
//import com.cerebra.secure_file_sharing_app.Shared.FolderResponse;
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
//@DisplayName("Basic File Management Flow Integration Tests")
//class BasicFileManagementFlowIntegrationTest {
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
//    @DisplayName("User Journey: Upload file to root directory")
//    void userJourney_uploadFileToRootDirectory() throws Exception {
//        // Step 1: Create file upload request
//        byte[] fileContent = "This is a test document content".getBytes();
//        ByteArrayResource fileResource = new ByteArrayResource(fileContent) {
//            @Override
//            public String getFilename() {
//                return "test-document.txt";
//            }
//        };
//
//        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
//        parts.add("file", fileResource);
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setBearerAuth(authToken);
//        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
//
//        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(parts, headers);
//
//        // Step 2: Upload file
//        ResponseEntity<FileUploadResponse> uploadResponse = restTemplate.postForEntity(
//                "/api/files/upload",
//                requestEntity,
//                FileUploadResponse.class
//        );
//
//        assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(uploadResponse.getBody()).isNotNull();
//        assertThat(uploadResponse.getBody().getFileName()).isEqualTo("test-document.txt");
//        assertThat(uploadResponse.getBody().getFileSize()).isEqualTo(fileContent.length);
//        assertThat(uploadResponse.getBody().getMimeType()).isEqualTo("text/plain");
//        assertThat(uploadResponse.getBody().getMessage()).isEqualTo("File uploaded successfully");
//
//        // Step 3: Verify file appears in user's files
//        HttpHeaders getHeaders = new HttpHeaders();
//        getHeaders.setBearerAuth(authToken);
//        HttpEntity<String> getEntity = new HttpEntity<>(getHeaders);
//
//        ResponseEntity<String> myFilesResponse = restTemplate.exchange(
//                "/api/files/my-files",
//                HttpMethod.GET,
//                getEntity,
//                String.class
//        );
//
//        assertThat(myFilesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(myFilesResponse.getBody()).contains("test-document.txt");
//
//        // Step 4: Verify file appears in root files
//        ResponseEntity<String> rootFilesResponse = restTemplate.exchange(
//                "/api/files/root",
//                HttpMethod.GET,
//                getEntity,
//                String.class
//        );
//
//        assertThat(rootFilesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(rootFilesResponse.getBody()).contains("test-document.txt");
//    }
//
//    @Test
//    @DisplayName("User Journey: Create folder and upload file to it")
//    void userJourney_createFolderAndUploadFile() throws Exception {
//        // Step 1: Create a folder
//        CreateFolderRequest createFolderRequest = CreateFolderRequest.builder()
//                .name("Documents")
//                .parentFolderId(null)
//                .build();
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setBearerAuth(authToken);
//        headers.setContentType(MediaType.APPLICATION_JSON);
//
//        HttpEntity<CreateFolderRequest> folderEntity = new HttpEntity<>(createFolderRequest, headers);
//
//        ResponseEntity<FolderResponse> folderResponse = restTemplate.postForEntity(
//                "/api/folders",
//                folderEntity,
//                FolderResponse.class
//        );
//
//        assertThat(folderResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(folderResponse.getBody()).isNotNull();
//        assertThat(folderResponse.getBody().getName()).isEqualTo("Documents");
//
//        Long folderId = folderResponse.getBody().getId();
//
//        // Step 2: Upload file to the created folder
//        byte[] fileContent = "PDF content here".getBytes();
//        ByteArrayResource fileResource = new ByteArrayResource(fileContent) {
//            @Override
//            public String getFilename() {
//                return "folder-document.pdf";
//            }
//        };
//
//        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
//        parts.add("file", fileResource);
//        parts.add("folderId", folderId.toString());
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
//        assertThat(uploadResponse.getBody().getFileName()).isEqualTo("folder-document.pdf");
//        assertThat(uploadResponse.getBody().getMimeType()).isEqualTo("application/pdf");
//
//        // Step 3: Verify file appears in folder
//        HttpHeaders getHeaders = new HttpHeaders();
//        getHeaders.setBearerAuth(authToken);
//        HttpEntity<String> getEntity = new HttpEntity<>(getHeaders);
//
//        ResponseEntity<String> folderFilesResponse = restTemplate.exchange(
//                "/api/files/folder/" + folderId,
//                HttpMethod.GET,
//                getEntity,
//                String.class
//        );
//
//        assertThat(folderFilesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(folderFilesResponse.getBody()).contains("folder-document.pdf");
//
//        // Step 4: Verify folder appears in user's folders
//        ResponseEntity<String> myFoldersResponse = restTemplate.exchange(
//                "/api/folders/my-folders",
//                HttpMethod.GET,
//                getEntity,
//                String.class
//        );
//
//        assertThat(myFoldersResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(myFoldersResponse.getBody()).contains("Documents");
//    }
//}