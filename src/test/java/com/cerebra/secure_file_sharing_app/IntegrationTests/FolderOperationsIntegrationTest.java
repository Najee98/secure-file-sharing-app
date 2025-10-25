package com.cerebra.secure_file_sharing_app.IntegrationTests;

import com.cerebra.secure_file_sharing_app.Shared.*;
import com.cerebra.secure_file_sharing_app.Security.DTO.*;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@DisplayName("Folder Operations Integration Tests")
class FolderOperationsIntegrationTest extends BaseIntegrationTest {

    @MockitoBean
    private HttpSMSService smsService;

    @Test
    @DisplayName("3-Level Nested Folder Creation: Verify Parent-Child Relationships")
    void threeLeveNestedFolderHierarchy() {
        // Setup: Authenticate user
        String jwtToken = authenticateUser("+6666666666");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Step 1: Create Grandparent Folder (Level 1)
        CreateFolderRequest grandparentRequest = new CreateFolderRequest();
        grandparentRequest.setName("Grandparent");
        grandparentRequest.setParentFolderId(null);

        HttpEntity<CreateFolderRequest> grandparentEntity = new HttpEntity<>(grandparentRequest, headers);

        ResponseEntity<Map> grandparentResponse = restTemplate.exchange(
                baseUrl() + "/api/folders",
                HttpMethod.POST,
                grandparentEntity,
                Map.class
        );

        assertThat(grandparentResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(grandparentResponse.getBody()).isNotNull();
        Integer grandparentIdInt = (Integer) grandparentResponse.getBody().get("id");
        Long grandparentId = grandparentIdInt.longValue();
        assertThat(grandparentResponse.getBody().get("name")).isEqualTo("Grandparent");
        assertThat(grandparentResponse.getBody().get("parentFolderId")).isNull();

        // Step 2: Create Parent Folder (Level 2) under Grandparent
        CreateFolderRequest parentRequest = new CreateFolderRequest();
        parentRequest.setName("Parent");
        parentRequest.setParentFolderId(grandparentId);

        HttpEntity<CreateFolderRequest> parentEntity = new HttpEntity<>(parentRequest, headers);

        ResponseEntity<Map> parentResponse = restTemplate.exchange(
                baseUrl() + "/api/folders",
                HttpMethod.POST,
                parentEntity,
                Map.class
        );

        assertThat(parentResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(parentResponse.getBody()).isNotNull();
        Integer parentIdInt = (Integer) parentResponse.getBody().get("id");
        Long parentId = parentIdInt.longValue();
        assertThat(parentResponse.getBody().get("name")).isEqualTo("Parent");
        assertThat(((Number) parentResponse.getBody().get("parentFolderId")).longValue()).isEqualTo(grandparentId);

        // Step 3: Create Child Folder (Level 3) under Parent
        CreateFolderRequest childRequest = new CreateFolderRequest();
        childRequest.setName("Child");
        childRequest.setParentFolderId(parentId);

        HttpEntity<CreateFolderRequest> childEntity = new HttpEntity<>(childRequest, headers);

        ResponseEntity<Map> childResponse = restTemplate.exchange(
                baseUrl() + "/api/folders",
                HttpMethod.POST,
                childEntity,
                Map.class
        );

        assertThat(childResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(childResponse.getBody()).isNotNull();
        Integer childIdInt = (Integer) childResponse.getBody().get("id");
        Long childId = childIdInt.longValue();
        assertThat(childResponse.getBody().get("name")).isEqualTo("Child");
        assertThat(((Number) childResponse.getBody().get("parentFolderId")).longValue()).isEqualTo(parentId);

        // Step 4: Verify Database Relationships
        Folder grandparentFolder = folderRepository.findById(grandparentId).orElseThrow();
        assertThat(grandparentFolder.getName()).isEqualTo("Grandparent");
        assertThat(grandparentFolder.getParentFolder()).isNull();

        Folder parentFolder = folderRepository.findById(parentId).orElseThrow();
        assertThat(parentFolder.getName()).isEqualTo("Parent");
        assertThat(parentFolder.getParentFolder()).isNotNull();
        assertThat(parentFolder.getParentFolder().getId()).isEqualTo(grandparentId);

        Folder childFolder = folderRepository.findById(childId).orElseThrow();
        assertThat(childFolder.getName()).isEqualTo("Child");
        assertThat(childFolder.getParentFolder()).isNotNull();
        assertThat(childFolder.getParentFolder().getId()).isEqualTo(parentId);

        // Step 5: Query Subfolders at Each Level
        HttpEntity<Void> getRequest = new HttpEntity<>(headers);

        // Grandparent's subfolders (should have 1: Parent)
        ResponseEntity<List> grandparentSubfolders = restTemplate.exchange(
                baseUrl() + "/api/folders/" + grandparentId + "/subfolders",
                HttpMethod.GET,
                getRequest,
                List.class
        );

        assertThat(grandparentSubfolders.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(grandparentSubfolders.getBody()).hasSize(1);

        // Parent's subfolders (should have 1: Child)
        ResponseEntity<List> parentSubfolders = restTemplate.exchange(
                baseUrl() + "/api/folders/" + parentId + "/subfolders",
                HttpMethod.GET,
                getRequest,
                List.class
        );

        assertThat(parentSubfolders.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(parentSubfolders.getBody()).hasSize(1);

        // Child's subfolders (should have 0)
        ResponseEntity<List> childSubfolders = restTemplate.exchange(
                baseUrl() + "/api/folders/" + childId + "/subfolders",
                HttpMethod.GET,
                getRequest,
                List.class
        );

        assertThat(childSubfolders.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(childSubfolders.getBody()).hasSize(0);
    }

    @Test
    @DisplayName("Folder Deletion Prevention: Cannot Delete Folder with Files")
    void cannotDeleteFolderWithFiles() {
        // Setup: Authenticate user
        String jwtToken = authenticateUser("+7777777777");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Step 1: Create Folder
        CreateFolderRequest folderRequest = new CreateFolderRequest();
        folderRequest.setName("Documents");
        folderRequest.setParentFolderId(null);

        HttpEntity<CreateFolderRequest> folderEntity = new HttpEntity<>(folderRequest, headers);

        ResponseEntity<Map> folderResponse = restTemplate.exchange(
                baseUrl() + "/api/folders",
                HttpMethod.POST,
                folderEntity,
                Map.class
        );

        assertThat(folderResponse.getBody()).isNotNull();
        Integer folderIdInt = (Integer) folderResponse.getBody().get("id");
        Long folderId = folderIdInt.longValue();

        // Step 2: Upload File to Folder
        uploadFileToFolder(jwtToken, "document.txt", "Content", folderId);

        // Step 3: Try to Delete Folder (should fail)
        HttpEntity<Void> deleteRequest = new HttpEntity<>(headers);

        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                baseUrl() + "/api/folders/" + folderId,
                HttpMethod.DELETE,
                deleteRequest,
                String.class
        );

        // Should return 403 Forbidden (FolderAccessDeniedException)
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // Step 4: Verify Folder Still Exists in Database
        assertThat(folderRepository.findById(folderId)).isPresent();
    }

    @Test
    @DisplayName("Folder Deletion Prevention: Cannot Delete Folder with Subfolders")
    void cannotDeleteFolderWithSubfolders() {
        // Setup: Authenticate user
        String jwtToken = authenticateUser("+8888888888");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Step 1: Create Parent Folder
        CreateFolderRequest parentRequest = new CreateFolderRequest();
        parentRequest.setName("Parent");
        parentRequest.setParentFolderId(null);

        HttpEntity<CreateFolderRequest> parentEntity = new HttpEntity<>(parentRequest, headers);

        ResponseEntity<Map> parentResponse = restTemplate.exchange(
                baseUrl() + "/api/folders",
                HttpMethod.POST,
                parentEntity,
                Map.class
        );

        assertThat(parentResponse.getBody()).isNotNull();
        Integer parentIdInt = (Integer) parentResponse.getBody().get("id");
        Long parentId = parentIdInt.longValue();

        // Step 2: Create Child Folder
        CreateFolderRequest childRequest = new CreateFolderRequest();
        childRequest.setName("Child");
        childRequest.setParentFolderId(parentId);

        HttpEntity<CreateFolderRequest> childEntity = new HttpEntity<>(childRequest, headers);

        ResponseEntity<Map> childResponse = restTemplate.exchange(
                baseUrl() + "/api/folders",
                HttpMethod.POST,
                childEntity,
                Map.class
        );

        assertThat(childResponse.getBody()).isNotNull();
        Integer childIdInt = (Integer) childResponse.getBody().get("id");
        Long childId = childIdInt.longValue();

        // Step 3: Try to Delete Parent Folder (should fail)
        HttpEntity<Void> deleteRequest = new HttpEntity<>(headers);

        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                baseUrl() + "/api/folders/" + parentId,
                HttpMethod.DELETE,
                deleteRequest,
                String.class
        );

        // Should return 403 Forbidden (FolderAccessDeniedException)
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // Step 4: Verify Both Folders Still Exist in Database
        assertThat(folderRepository.findById(parentId)).isPresent();
        assertThat(folderRepository.findById(childId)).isPresent();

        // Step 5: Verify Delete Works After Removing Child
        restTemplate.exchange(
                baseUrl() + "/api/folders/" + childId,
                HttpMethod.DELETE,
                deleteRequest,
                String.class
        );

        // Now parent can be deleted
        ResponseEntity<String> deleteParentResponse = restTemplate.exchange(
                baseUrl() + "/api/folders/" + parentId,
                HttpMethod.DELETE,
                deleteRequest,
                String.class
        );

        assertThat(deleteParentResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(folderRepository.findById(parentId)).isEmpty();
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

    private void uploadFileToFolder(String jwtToken, String filename, String content, Long folderId) {
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

        String url = baseUrl() + "/api/files/upload?folderId=" + folderId;

        restTemplate.exchange(url, HttpMethod.POST, uploadRequest, Map.class);
    }
}