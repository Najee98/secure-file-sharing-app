package com.cerebra.secure_file_sharing_app.IntegrationTests;

import com.cerebra.secure_file_sharing_app.Security.DTO.*;
import com.cerebra.secure_file_sharing_app.Shared.*;
import com.cerebra.secure_file_sharing_app.Entities.File;
import com.cerebra.secure_file_sharing_app.Entities.Folder;
import com.cerebra.secure_file_sharing_app.Services.HttpSMSService;
import com.cerebra.secure_file_sharing_app.Shared.SMSResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@DisplayName("File Operations Integration Tests")
class FileOperationsIntegrationTest extends BaseIntegrationTest {

    @MockitoBean
    private HttpSMSService smsService;

    @Test
    @DisplayName("Complete File Lifecycle: Upload → DB → Filesystem → Download → Delete → Cleanup")
    void completeFileLifecycle() throws IOException {
        // Setup: Authenticate user
        String jwtToken = authenticateUser("+1111111111");

        // Step 1: Upload File
        String fileContent = "This is test file content for integration testing!";
        byte[] fileBytes = fileContent.getBytes();

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return "test-document.txt";
            }
        });

        HttpHeaders uploadHeaders = new HttpHeaders();
        uploadHeaders.setBearerAuth(jwtToken);
        uploadHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> uploadRequest = new HttpEntity<>(body, uploadHeaders);

        ResponseEntity<Map> uploadResponse = restTemplate.exchange(
                baseUrl() + "/api/files/upload",
                HttpMethod.POST,
                uploadRequest,
                Map.class
        );

        assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(uploadResponse.getBody()).isNotNull();
        
        Integer fileIdInt = (Integer) uploadResponse.getBody().get("fileId");
        Long fileId = fileIdInt.longValue();
        assertThat(uploadResponse.getBody().get("fileName")).isEqualTo("test-document.txt");
        assertThat(uploadResponse.getBody().get("message")).isEqualTo("File uploaded successfully");

        // Step 2: Verify File in Database
        File fileEntity = fileRepository.findById(fileId).orElseThrow();
        assertThat(fileEntity.getDisplayName()).isEqualTo("test-document.txt");
        assertThat(fileEntity.getSize()).isEqualTo(fileContent.length());
        assertThat(fileEntity.getMimeType()).isEqualTo("text/plain");
        assertThat(fileEntity.getPhysicalPath()).isNotNull();

        // Step 3: Verify Physical File Exists on Filesystem
        Path physicalPath = Paths.get(fileEntity.getPhysicalPath());
        assertThat(Files.exists(physicalPath)).isTrue();
        assertThat(Files.isRegularFile(physicalPath)).isTrue();

        // Step 4: Verify File Content Matches
        String actualContent = Files.readString(physicalPath);
        assertThat(actualContent).isEqualTo(fileContent);

        // Step 5: Download File via API
        HttpHeaders downloadHeaders = new HttpHeaders();
        downloadHeaders.setBearerAuth(jwtToken);
        HttpEntity<Void> downloadRequest = new HttpEntity<>(downloadHeaders);

        ResponseEntity<byte[]> downloadResponse = restTemplate.exchange(
                baseUrl() + "/api/files/" + fileId + "/download",
                HttpMethod.GET,
                downloadRequest,
                byte[].class
        );

        assertThat(downloadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(downloadResponse.getBody()).isNotNull();
        
        String downloadedContent = new String(downloadResponse.getBody());
        assertThat(downloadedContent).isEqualTo(fileContent);

        // Step 6: Delete File
        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                baseUrl() + "/api/files/" + fileId,
                HttpMethod.DELETE,
                downloadRequest,
                String.class
        );

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deleteResponse.getBody()).contains("deleted successfully");

        // Step 7: Verify File Deleted from Database
        assertThat(fileRepository.findById(fileId)).isEmpty();

        // Step 8: Verify Physical File Deleted from Filesystem
        assertThat(Files.exists(physicalPath)).isFalse();
    }

    @Test
    @DisplayName("Upload File to Folder: Multi-Service Integration")
    void uploadFileToFolder() {
        // Setup: Authenticate user
        String jwtToken = authenticateUser("+2222222222");

        // Step 1: Create Folder
        CreateFolderRequest folderRequest = new CreateFolderRequest();
        folderRequest.setName("Documents");
        folderRequest.setParentFolderId(null);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateFolderRequest> folderRequestEntity = new HttpEntity<>(folderRequest, headers);

        ResponseEntity<Map> folderResponse = restTemplate.exchange(
                baseUrl() + "/api/folders",
                HttpMethod.POST,
                folderRequestEntity,
                Map.class
        );

        assertThat(folderResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Integer folderIdInt = (Integer) folderResponse.getBody().get("id");
        Long folderId = folderIdInt.longValue();

        // Step 2: Upload File to Folder
        String fileContent = "Document in folder";
        byte[] fileBytes = fileContent.getBytes();

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return "folder-document.txt";
            }
        });

        HttpHeaders uploadHeaders = new HttpHeaders();
        uploadHeaders.setBearerAuth(jwtToken);
        uploadHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> uploadRequest = new HttpEntity<>(body, uploadHeaders);

        ResponseEntity<Map> uploadResponse = restTemplate.exchange(
                baseUrl() + "/api/files/upload?folderId=" + folderId,
                HttpMethod.POST,
                uploadRequest,
                Map.class
        );

        assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Integer fileIdInt = (Integer) uploadResponse.getBody().get("fileId");
        Long fileId = fileIdInt.longValue();

        // Step 3: Verify File-Folder Relationship in Database
        File fileEntity = fileRepository.findById(fileId).orElseThrow();
        assertThat(fileEntity.getFolder()).isNotNull();
        assertThat(fileEntity.getFolder().getId()).isEqualTo(folderId);

        Folder folderEntity = folderRepository.findById(folderId).orElseThrow();
        assertThat(folderEntity.getName()).isEqualTo("Documents");

        // Step 4: Query Files by Folder
        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<List> filesInFolderResponse = restTemplate.exchange(
                baseUrl() + "/api/files/folder/" + folderId,
                HttpMethod.GET,
                getRequest,
                List.class
        );

        assertThat(filesInFolderResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(filesInFolderResponse.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("Multi-User Data Isolation: User B Cannot Access User A's File")
    void multiUserDataIsolation() {
        // Setup: Authenticate User A
        String jwtTokenA = authenticateUser("+3333333333");

        // User A uploads file
        String fileContent = "User A's private file";
        byte[] fileBytes = fileContent.getBytes();

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return "private.txt";
            }
        });

        HttpHeaders uploadHeaders = new HttpHeaders();
        uploadHeaders.setBearerAuth(jwtTokenA);
        uploadHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> uploadRequest = new HttpEntity<>(body, uploadHeaders);

        ResponseEntity<Map> uploadResponse = restTemplate.exchange(
                baseUrl() + "/api/files/upload",
                HttpMethod.POST,
                uploadRequest,
                Map.class
        );

        assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Integer fileIdInt = (Integer) uploadResponse.getBody().get("fileId");
        Long fileId = fileIdInt.longValue();

        // Setup: Authenticate User B
        String jwtTokenB = authenticateUser("+4444444444");

        // User B tries to download User A's file (should fail)
        HttpHeaders downloadHeaders = new HttpHeaders();
        downloadHeaders.setBearerAuth(jwtTokenB);
        HttpEntity<Void> downloadRequest = new HttpEntity<>(downloadHeaders);

        ResponseEntity<String> downloadResponse = restTemplate.exchange(
                baseUrl() + "/api/files/" + fileId + "/download",
                HttpMethod.GET,
                downloadRequest,
                String.class
        );

        // Should return 403 Forbidden (access denied)
        assertThat(downloadResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // Note: User B CAN access the file via shared link (tested in FileSharingIntegrationTest)
    }

    @Test
    @DisplayName("List Files by Hierarchy: Default Folder vs Subfolder Files")
    void listFilesByHierarchy() {
        // Setup: Authenticate user
        String jwtToken = authenticateUser("+5555555555");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);

        // Step 1: Upload 2 files without specifying folder (goes to "My Files")
        uploadFileWithName(jwtToken, "default-file-1.txt", "Default content 1", null);
        uploadFileWithName(jwtToken, "default-file-2.txt", "Default content 2", null);

        // Step 2: Create custom subfolder
        CreateFolderRequest folderRequest = new CreateFolderRequest();
        folderRequest.setName("Subfolder");
        folderRequest.setParentFolderId(null);

        HttpEntity<CreateFolderRequest> folderRequestEntity = new HttpEntity<>(folderRequest, headers);

        ResponseEntity<Map> folderResponse = restTemplate.exchange(
                baseUrl() + "/api/folders",
                HttpMethod.POST,
                folderRequestEntity,
                Map.class
        );

        Integer folderIdInt = (Integer) folderResponse.getBody().get("id");
        Long folderId = folderIdInt.longValue();

        // Step 3: Upload 3 files to custom subfolder
        uploadFileWithName(jwtToken, "folder-file-1.txt", "Folder content 1", folderId);
        uploadFileWithName(jwtToken, "folder-file-2.txt", "Folder content 2", folderId);
        uploadFileWithName(jwtToken, "folder-file-3.txt", "Folder content 3", folderId);

        // Step 4: List all user files (should return 5 total)
        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<List> allFilesResponse = restTemplate.exchange(
                baseUrl() + "/api/files/my-files",
                HttpMethod.GET,
                getRequest,
                List.class
        );

        assertThat(allFilesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(allFilesResponse.getBody()).hasSize(5);

        // Step 5: Get "My Files" folder ID (the default folder)
        ResponseEntity<List> foldersResponse = restTemplate.exchange(
                baseUrl() + "/api/folders/my-folders",
                HttpMethod.GET,
                getRequest,
                List.class
        );

        List<Map<String, Object>> folders = (List<Map<String, Object>>) foldersResponse.getBody();
        assertThat(folders).hasSizeGreaterThanOrEqualTo(2); // "My Files" + "Subfolder"

        // Find "My Files" folder
        Map<String, Object> myFilesFolder = folders.stream()
                .filter(f -> "My Files".equals(f.get("name")))
                .findFirst()
                .orElseThrow();

        Integer myFilesFolderIdInt = (Integer) myFilesFolder.get("id");
        Long myFilesFolderId = myFilesFolderIdInt.longValue();

        // Step 6: List files in "My Files" folder (should return 2)
        ResponseEntity<List> defaultFilesResponse = restTemplate.exchange(
                baseUrl() + "/api/files/folder/" + myFilesFolderId,
                HttpMethod.GET,
                getRequest,
                List.class
        );

        assertThat(defaultFilesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(defaultFilesResponse.getBody()).hasSize(2);

        // Step 7: List files in custom subfolder (should return 3)
        ResponseEntity<List> folderFilesResponse = restTemplate.exchange(
                baseUrl() + "/api/files/folder/" + folderId,
                HttpMethod.GET,
                getRequest,
                List.class
        );

        assertThat(folderFilesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(folderFilesResponse.getBody()).hasSize(3);
    }

    // ========== Helper Methods ==========

    private String authenticateUser(String phoneNumber) {
        when(smsService.sendSMS(anyString(), anyString()))
                .thenReturn(SMSResponse.success("OTP sent"));

        ResponseEntity<OTPResponse> otpResponse = restTemplate.postForEntity(
                baseUrl() + "/api/auth/request-otp",
                new OTPRequest(phoneNumber),
                OTPResponse.class
        );

        String otp = otpResponse.getBody().getOtp();

        ResponseEntity<AuthResponse> authResponse = restTemplate.postForEntity(
                baseUrl() + "/api/auth/verify-otp",
                new OTPVerificationRequest(phoneNumber, otp),
                AuthResponse.class
        );

        return authResponse.getBody().getToken();
    }

    private void uploadFileWithName(String jwtToken, String filename, String content, Long folderId) {
        byte[] fileBytes = content.getBytes();

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        });

        HttpHeaders uploadHeaders = new HttpHeaders();
        uploadHeaders.setBearerAuth(jwtToken);
        uploadHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> uploadRequest = new HttpEntity<>(body, uploadHeaders);

        String url = baseUrl() + "/api/files/upload";
        if (folderId != null) {
            url += "?folderId=" + folderId;
        }

        restTemplate.exchange(url, HttpMethod.POST, uploadRequest, Map.class);
    }
}