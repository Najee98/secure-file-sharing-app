package com.cerebra.secure_file_sharing_app.IntegrationTests;

import com.cerebra.secure_file_sharing_app.Shared.*;
import com.cerebra.secure_file_sharing_app.Security.DTO.*;
import com.cerebra.secure_file_sharing_app.Entities.SharedLink;
import com.cerebra.secure_file_sharing_app.Services.HttpSMSService;
import com.cerebra.secure_file_sharing_app.Shared.SMSResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("File Sharing Integration Tests")
class FileSharingIntegrationTest extends BaseIntegrationTest {

    @MockitoBean
    private HttpSMSService smsService;

    @Test
    @DisplayName("Complete File Sharing Flow: Upload → Share → SMS → Public Download Without JWT")
    void completeFileSharingFlow() {
        // Setup: Authenticate User A
        String jwtToken = authenticateUser("+1111111111");

        // Mock SMS service for sharing
        when(smsService.sendSMS(anyString(), anyString()))
                .thenReturn(SMSResponse.success("Share link sent"));

        // Step 1: User A uploads file
        String fileContent = "Shared file content";
        Long fileId = uploadFile(jwtToken, "shared-document.txt", fileContent, null);

        // Step 2: User A creates share link for the file
        CreateShareRequest shareRequest = new CreateShareRequest();
        shareRequest.setRecipientPhone("+2222222222");
        shareRequest.setMessage("Check out this document!");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateShareRequest> shareEntity = new HttpEntity<>(shareRequest, headers);

        // ✅ CORRECTED: fileId in URL path, not body
        ResponseEntity<Map> shareResponse = restTemplate.exchange(
                baseUrl() + "/api/files/" + fileId + "/share",
                HttpMethod.POST,
                shareEntity,
                Map.class
        );

        assertThat(shareResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(shareResponse.getBody()).isNotNull();

        String linkToken = (String) shareResponse.getBody().get("linkToken");
        assertThat(linkToken).isNotBlank();

        assertThat(shareResponse.getBody().get("itemType")).isEqualTo("file");
        assertThat(shareResponse.getBody().get("itemName")).isEqualTo("shared-document.txt");
        assertThat(((Number) shareResponse.getBody().get("itemId")).longValue()).isEqualTo(fileId);

        // Step 3: Verify SharedLink in Database
        SharedLink sharedLink = sharedLinkRepository.findByLinkToken(linkToken).orElseThrow();
        assertThat(sharedLink.getFile().getId()).isEqualTo(fileId);
        assertThat(sharedLink.getExpiresAt()).isAfter(LocalDateTime.now());

        // Step 4: Verify SMS was sent with share link
        verify(smsService, atLeastOnce()).sendSMS(eq("+2222222222"), contains(linkToken));

        // Step 5: Unauthenticated user (no JWT) downloads via public link
        ResponseEntity<byte[]> downloadResponse = restTemplate.getForEntity(
                baseUrl() + "/public/shared/" + linkToken,
                byte[].class
        );

        assertThat(downloadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(downloadResponse.getBody()).isNotNull();

        String downloadedContent = new String(downloadResponse.getBody());
        assertThat(downloadedContent).isEqualTo(fileContent);

        // Verify Content-Disposition header
        assertThat(downloadResponse.getHeaders().getContentDisposition().getFilename())
                .isEqualTo("shared-document.txt");
    }

    @Test
    @DisplayName("Folder Share Downloads as ZIP: Multiple Files")
    void folderShareDownloadsAsZip() throws IOException {
        // Setup: Authenticate user
        String jwtToken = authenticateUser("+3333333333");

        // Mock SMS service
        when(smsService.sendSMS(anyString(), anyString()))
                .thenReturn(SMSResponse.success("Share link sent"));

        // Step 1: Create folder with 3 files
        Long folderId = createFolder(jwtToken, "Shared Folder", null);

        uploadFile(jwtToken, "file1.txt", "Content 1", folderId);
        uploadFile(jwtToken, "file2.txt", "Content 2", folderId);
        uploadFile(jwtToken, "file3.txt", "Content 3", folderId);

        // Step 2: Share folder
        CreateShareRequest shareRequest = new CreateShareRequest();
        shareRequest.setRecipientPhone("+4444444444");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateShareRequest> shareEntity = new HttpEntity<>(shareRequest, headers);

        // ✅ CORRECTED: folderId in URL path
        ResponseEntity<Map> shareResponse = restTemplate.exchange(
                baseUrl() + "/api/folders/" + folderId + "/share",
                HttpMethod.POST,
                shareEntity,
                Map.class
        );

        assertThat(shareResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String linkToken = (String) shareResponse.getBody().get("linkToken");

        assertThat(shareResponse.getBody().get("itemType")).isEqualTo("folder");

        // Step 3: Verify SharedLink references folder
        SharedLink sharedLink = sharedLinkRepository.findByLinkToken(linkToken).orElseThrow();
        assertThat(sharedLink.getFolder()).isNotNull();
        assertThat(sharedLink.getFolder().getId()).isEqualTo(folderId);

        // Step 4: Public download (should be ZIP)
        ResponseEntity<byte[]> downloadResponse = restTemplate.getForEntity(
                baseUrl() + "/public/shared/" + linkToken,
                byte[].class
        );

        assertThat(downloadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(downloadResponse.getBody()).isNotNull();

        // Verify Content-Type is ZIP
        assertThat(downloadResponse.getHeaders().getContentType())
                .isEqualTo(MediaType.parseMediaType("application/zip"));

        // Step 5: Extract ZIP and verify all 3 files present
        byte[] zipBytes = downloadResponse.getBody();
        try (ZipInputStream zipIn = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            int fileCount = 0;

            while ((entry = zipIn.getNextEntry()) != null) {
                fileCount++;
                String fileName = entry.getName();
                assertThat(fileName).matches("file[1-3]\\.txt");

                // Read content
                byte[] buffer = new byte[1024];
                int len = zipIn.read(buffer);
                String content = new String(buffer, 0, len);
                assertThat(content).matches("Content [1-3]");

                zipIn.closeEntry();
            }

            assertThat(fileCount).isEqualTo(3);
        }
    }

    @Test
    @DisplayName("Expired Share Link Rejection")
    void expiredShareLinkRejection() {
        // Setup: Authenticate user
        String jwtToken = authenticateUser("+5555555555");

        when(smsService.sendSMS(anyString(), anyString()))
                .thenReturn(SMSResponse.success("Share link sent"));

        // Step 1: Upload file and create share
        Long fileId = uploadFile(jwtToken, "expires-soon.txt", "Will expire", null);

        CreateShareRequest shareRequest = new CreateShareRequest();
        shareRequest.setRecipientPhone("+6666666666");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateShareRequest> shareEntity = new HttpEntity<>(shareRequest, headers);

        ResponseEntity<Map> shareResponse = restTemplate.exchange(
                baseUrl() + "/api/files/" + fileId + "/share",
                HttpMethod.POST,
                shareEntity,
                Map.class
        );

        String linkToken = (String) shareResponse.getBody().get("linkToken");

        // Step 2: Manually expire the link in database
        SharedLink sharedLink = sharedLinkRepository.findByLinkToken(linkToken).orElseThrow();
        sharedLink.setExpiresAt(LocalDateTime.now().minusHours(1)); // Expired 1 hour ago
        sharedLinkRepository.save(sharedLink);

        // Step 3: Try to download expired link
        ResponseEntity<String> downloadResponse = restTemplate.getForEntity(
                baseUrl() + "/public/shared/" + linkToken,
                String.class
        );

        // Should return 404 or 410 (Gone)
        assertThat(downloadResponse.getStatusCode()).isIn(HttpStatus.NOT_FOUND, HttpStatus.GONE);
    }

    @Test
    @DisplayName("Revoked Share Link")
    void revokedShareLink() {
        // Setup: Authenticate user
        String jwtToken = authenticateUser("+7777777777");

        when(smsService.sendSMS(anyString(), anyString()))
                .thenReturn(SMSResponse.success("Share link sent"));

        // Step 1: Upload file and create share
        Long fileId = uploadFile(jwtToken, "revoke-me.txt", "Will be revoked", null);

        CreateShareRequest shareRequest = new CreateShareRequest();
        shareRequest.setRecipientPhone("+8888888888");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateShareRequest> shareEntity = new HttpEntity<>(shareRequest, headers);

        ResponseEntity<Map> shareResponse = restTemplate.exchange(
                baseUrl() + "/api/files/" + fileId + "/share",
                HttpMethod.POST,
                shareEntity,
                Map.class
        );

        String linkToken = (String) shareResponse.getBody().get("linkToken");
        Integer shareIdInt = (Integer) shareResponse.getBody().get("shareId");
        Long shareId = shareIdInt.longValue();

        // Step 2: Verify download works before revocation
        ResponseEntity<byte[]> beforeRevoke = restTemplate.getForEntity(
                baseUrl() + "/public/shared/" + linkToken,
                byte[].class
        );
        assertThat(beforeRevoke.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Step 3: Revoke the share
        HttpEntity<Void> deleteEntity = new HttpEntity<>(headers);
        ResponseEntity<String> revokeResponse = restTemplate.exchange(
                baseUrl() + "/api/shared/" + shareId,
                HttpMethod.DELETE,
                deleteEntity,
                String.class
        );

        assertThat(revokeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Step 4: Verify removed from database
        assertThat(sharedLinkRepository.findById(shareId)).isEmpty();

        // Step 5: Try to download after revocation
        ResponseEntity<String> afterRevoke = restTemplate.getForEntity(
                baseUrl() + "/public/shared/" + linkToken,
                String.class
        );

        // Should return 404
        assertThat(afterRevoke.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Empty Folder Share: Valid Empty ZIP")
    void emptyFolderShare() throws IOException {
        // Setup: Authenticate user
        String jwtToken = authenticateUser("+9999999999");

        when(smsService.sendSMS(anyString(), anyString()))
                .thenReturn(SMSResponse.success("Share link sent"));

        // Step 1: Create empty folder
        Long folderId = createFolder(jwtToken, "Empty Folder", null);

        // Step 2: Share empty folder
        CreateShareRequest shareRequest = new CreateShareRequest();
        shareRequest.setRecipientPhone("+1010101010");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateShareRequest> shareEntity = new HttpEntity<>(shareRequest, headers);

        ResponseEntity<Map> shareResponse = restTemplate.exchange(
                baseUrl() + "/api/folders/" + folderId + "/share",
                HttpMethod.POST,
                shareEntity,
                Map.class
        );

        String linkToken = (String) shareResponse.getBody().get("linkToken");

        // Step 3: Download should return valid empty ZIP
        ResponseEntity<byte[]> downloadResponse = restTemplate.getForEntity(
                baseUrl() + "/public/shared/" + linkToken,
                byte[].class
        );

        assertThat(downloadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(downloadResponse.getBody()).isNotNull();

        // Verify it's a valid ZIP (even if empty)
        byte[] zipBytes = downloadResponse.getBody();
        try (ZipInputStream zipIn = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry = zipIn.getNextEntry();
            assertThat(entry).isNull(); // No entries in empty ZIP
        }
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

    private Long uploadFile(String jwtToken, String filename, String content, Long folderId) {
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

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, uploadRequest, Map.class);
        Integer fileIdInt = (Integer) response.getBody().get("fileId");
        return fileIdInt.longValue();
    }

    private Long createFolder(String jwtToken, String folderName, Long parentId) {
        CreateFolderRequest folderRequest = new CreateFolderRequest();
        folderRequest.setName(folderName);
        folderRequest.setParentFolderId(parentId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateFolderRequest> folderEntity = new HttpEntity<>(folderRequest, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/folders",
                HttpMethod.POST,
                folderEntity,
                Map.class
        );

        Integer folderIdInt = (Integer) response.getBody().get("id");
        return folderIdInt.longValue();
    }
}