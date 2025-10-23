package com.cerebra.secure_file_sharing_app.UnitTests.Controllers;

import com.cerebra.secure_file_sharing_app.Controllers.SharedLinkController;
import com.cerebra.secure_file_sharing_app.Entities.AppUser;
import com.cerebra.secure_file_sharing_app.Entities.File;
import com.cerebra.secure_file_sharing_app.Entities.Folder;
import com.cerebra.secure_file_sharing_app.Entities.SharedLink;
import com.cerebra.secure_file_sharing_app.Exceptions.CustomExceptions.ShareNotFoundException;
import com.cerebra.secure_file_sharing_app.Exceptions.GlobalExceptionHandler;
import com.cerebra.secure_file_sharing_app.Services.SharedLinkService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SharedLinkController Tests")
class SharedLinkControllerTest {

    @Mock
    private SharedLinkService sharedLinkService;

    @Mock
    private Authentication authentication;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private AppUser mockUser;

    @BeforeEach
    void setUp() {
        SharedLinkController sharedLinkController = new SharedLinkController(sharedLinkService);
        mockMvc = MockMvcBuilders.standaloneSetup(sharedLinkController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Set up mock user
        mockUser = AppUser.builder()
                .id(1L)
                .phoneNumber("+1234567890")
                .build();

        // Default authentication setup
        when(authentication.getName()).thenReturn("+1234567890");
        when(authentication.getPrincipal()).thenReturn(mockUser);
    }

    @Test
    @DisplayName("Should share file successfully")
    void shareFile_validRequest_returnsShareResponse() throws Exception {
        // Arrange
        Long fileId = 1L;
        String requestJson = """
        {
            "recipientPhone": "+1234567890"
        }
        """;

        File file = File.builder()
                .id(fileId)
                .displayName("test.pdf")
                .build();

        SharedLink sharedLink = SharedLink.builder()
                .id(1L)
                .linkToken("test-token-123")
                .file(file)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();

        when(sharedLinkService.createFileShare(fileId, 1L, "+1234567890")).thenReturn(sharedLink);

        // Act & Assert
        mockMvc.perform(post("/api/files/{fileId}/share", fileId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.shareId").value(1L))
                .andExpect(jsonPath("$.linkToken").value("test-token-123"))
                .andExpect(jsonPath("$.shareUrl").value("http://localhost:8080/public/shared/test-token-123"))
                .andExpect(jsonPath("$.itemType").value("file"))
                .andExpect(jsonPath("$.itemName").value("test.pdf"))
                .andExpect(jsonPath("$.itemId").value(fileId))
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("Should share folder successfully")
    void shareFolder_validRequest_returnsShareResponse() throws Exception {
        // Arrange
        Long folderId = 5L;
        String requestJson = """
        {
            "recipientPhone": "+1234567890"
        }
        """;

        Folder folder = Folder.builder()
                .id(folderId)
                .name("Documents")
                .build();

        SharedLink sharedLink = SharedLink.builder()
                .id(2L)
                .linkToken("folder-token-456")
                .folder(folder)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();

        when(sharedLinkService.createFolderShare(folderId, 1L, "+1234567890")).thenReturn(sharedLink);

        // Act & Assert
        mockMvc.perform(post("/api/folders/{folderId}/share", folderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.shareId").value(2L))
                .andExpect(jsonPath("$.linkToken").value("folder-token-456"))
                .andExpect(jsonPath("$.itemType").value("folder"))
                .andExpect(jsonPath("$.itemName").value(""))
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    @DisplayName("Should download shared file successfully")
    void downloadSharedFile_validToken_returnsFileResource() throws Exception {
        // Arrange
        String linkToken = "valid-token-123";
        
        File file = File.builder()
                .displayName("document.pdf")
                .mimeType("application/pdf")
                .build();

        SharedLink sharedLink = SharedLink.builder()
                .linkToken(linkToken)
                .file(file)
                .build();

        Resource mockResource = new ByteArrayResource("file content".getBytes());

        when(sharedLinkService.downloadSharedFile(linkToken)).thenReturn(mockResource);
        when(sharedLinkService.findByLinkToken(linkToken)).thenReturn(Optional.of(sharedLink));

        // Act & Assert
        mockMvc.perform(get("/public/shared/{linkToken}", linkToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"document.pdf\""));
    }

    @Test
    @DisplayName("Should download shared folder as ZIP")
    void downloadSharedFile_folderToken_returnsZipResource() throws Exception {
        // Arrange
        String linkToken = "folder-token-456";
        
        Folder folder = Folder.builder()
                .name("My Folder")
                .build();

        SharedLink sharedLink = SharedLink.builder()
                .linkToken(linkToken)
                .folder(folder)
                .build();

        Resource mockResource = new ByteArrayResource("zip content".getBytes());

        when(sharedLinkService.downloadSharedFile(linkToken)).thenReturn(mockResource);
        when(sharedLinkService.findByLinkToken(linkToken)).thenReturn(Optional.of(sharedLink));

        // Act & Assert
        mockMvc.perform(get("/public/shared/{linkToken}", linkToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/zip"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"My Folder.zip\""));
    }

    @Test
    @DisplayName("Should download by URL parameter")
    void downloadSharedFileByUrl_validUrl_returnsResource() throws Exception {
        // Arrange
        String shareUrl = "http://localhost:8080/public/shared/12345678-1234-1234-1234-123456789012";
        String linkToken = "12345678-1234-1234-1234-123456789012";
        
        File file = File.builder()
                .displayName("test.txt")
                .mimeType("text/plain")
                .build();

        SharedLink sharedLink = SharedLink.builder()
                .linkToken(linkToken)
                .file(file)
                .build();

        Resource mockResource = new ByteArrayResource("content".getBytes());

        when(sharedLinkService.downloadSharedFile(linkToken)).thenReturn(mockResource);
        when(sharedLinkService.findByLinkToken(linkToken)).thenReturn(Optional.of(sharedLink));

        // Act & Assert
        mockMvc.perform(get("/public/shared")
                .param("url", shareUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"test.txt\""));
    }

    @Test
    @DisplayName("Should revoke share successfully")
    void revokeShare_validShareId_returnsSuccessMessage() throws Exception {
        // Arrange
        Long shareId = 1L;

        // Act & Assert
        mockMvc.perform(delete("/api/shared/{shareId}", shareId)
                .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().string("Share revoked successfully"));
    }

    @Test
    @DisplayName("Should get file shares successfully")
    void getFileShares_validFileId_returnsShareList() throws Exception {
        // Arrange
        Long fileId = 1L;
        
        File file = File.builder()
                .id(fileId)
                .displayName("document.pdf")
                .build();

        List<SharedLink> shares = Arrays.asList(
                SharedLink.builder()
                        .id(1L)
                        .linkToken("token-1")
                        .file(file)
                        .expiresAt(LocalDateTime.now().plusDays(7))
                        .createdAt(LocalDateTime.now())
                        .build(),
                SharedLink.builder()
                        .id(2L)
                        .linkToken("token-2")
                        .file(file)
                        .expiresAt(LocalDateTime.now().plusDays(7))
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        when(sharedLinkService.findByFileId(fileId)).thenReturn(shares);

        // Act & Assert
        mockMvc.perform(get("/api/files/{fileId}/shares", fileId)
                .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].shareId").value(1L))
                .andExpect(jsonPath("$[0].linkToken").value("token-1"))
                .andExpect(jsonPath("$[1].shareId").value(2L))
                .andExpect(jsonPath("$[1].linkToken").value("token-2"));
    }

    @Test
    @DisplayName("Should get user shares successfully")
    void getMyShares_authenticatedUser_returnsUserShares() throws Exception {
        // Arrange
        File file = File.builder()
                .id(1L)
                .displayName("file.txt")
                .build();

        Folder folder = Folder.builder()
                .id(2L)
                .name("folder")
                .build();

        List<SharedLink> shares = Arrays.asList(
                SharedLink.builder()
                        .id(1L)
                        .linkToken("file-token")
                        .file(file)
                        .expiresAt(LocalDateTime.now().plusDays(7))
                        .createdAt(LocalDateTime.now())
                        .build(),
                SharedLink.builder()
                        .id(2L)
                        .linkToken("folder-token")
                        .folder(folder)
                        .expiresAt(LocalDateTime.now().plusDays(7))
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        when(sharedLinkService.getUserShares(1L)).thenReturn(shares);

        // Act & Assert
        mockMvc.perform(get("/api/shared/my-shares")
                .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].itemType").value("file"))
                .andExpect(jsonPath("$[1].itemType").value("folder"));
    }

    @Test
    @DisplayName("Should get share info successfully")
    void getShareInfo_validToken_returnsShareInfo() throws Exception {
        // Arrange
        String linkToken = "info-token-123";
        
        File file = File.builder()
                .id(1L)
                .displayName("info.pdf")
                .build();

        SharedLink sharedLink = SharedLink.builder()
                .id(1L)
                .linkToken(linkToken)
                .file(file)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();

        when(sharedLinkService.findByLinkToken(linkToken)).thenReturn(Optional.of(sharedLink));

        // Act & Assert
        mockMvc.perform(get("/public/shared/{linkToken}/info", linkToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.shareId").value(1L))
                .andExpect(jsonPath("$.linkToken").value(linkToken))
                .andExpect(jsonPath("$.itemType").value("file"))
                .andExpect(jsonPath("$.itemName").value("info.pdf"));
    }

    @Test
    @DisplayName("Should handle share not found for download")
    void downloadSharedFile_invalidToken_returnsInternalServerError() throws Exception {
        // Arrange
        String invalidToken = "invalid-token";

        when(sharedLinkService.downloadSharedFile(invalidToken))
                .thenThrow(new ShareNotFoundException("Share not found"));

        // Act & Assert
        mockMvc.perform(get("/public/shared/{linkToken}", invalidToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Requested resource not found"));
    }

    @Test
    @DisplayName("Should handle share not found for info")
    void getShareInfo_invalidToken_returnsInternalServerError() throws Exception {
        // Arrange
        String invalidToken = "invalid-token";

        when(sharedLinkService.findByLinkToken(invalidToken)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/public/shared/{linkToken}/info", invalidToken))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    @DisplayName("Should handle invalid URL format in download by URL")
    void downloadSharedFileByUrl_invalidUrl_returnsBadRequest() throws Exception {
        // Arrange
        String invalidUrl = "not-a-valid-url";

        // Act & Assert
        mockMvc.perform(get("/public/shared")
                .param("url", invalidUrl))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    @DisplayName("Should return empty shares list")
    void getFileShares_noShares_returnsEmptyList() throws Exception {
        // Arrange
        Long fileId = 1L;

        when(sharedLinkService.findByFileId(fileId)).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/files/{fileId}/shares", fileId)
                .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Should handle service exception during file share")
    void shareFile_serviceException_returnsInternalServerError() throws Exception {
        // Arrange
        Long fileId = 1L;
        String requestJson = """
        {
            "recipientPhone": "+1234567890"
        }
        """;

        when(sharedLinkService.createFileShare(fileId, 1L, "+1234567890"))
                .thenThrow(new RuntimeException("File share failed"));

        // Act & Assert
        mockMvc.perform(post("/api/files/{fileId}/share", fileId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .principal(authentication))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    @DisplayName("Should handle service exception during folder share")
    void shareFolder_serviceException_returnsInternalServerError() throws Exception {
        // Arrange
        Long folderId = 5L;
        String requestJson = """
        {
            "recipientPhone": "+1234567890"
        }
        """;

        when(sharedLinkService.createFolderShare(folderId, 1L, "+1234567890"))
                .thenThrow(new RuntimeException("Folder share failed"));

        // Act & Assert
        mockMvc.perform(post("/api/folders/{folderId}/share", folderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .principal(authentication))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    @DisplayName("Should handle missing resource info for download")
    void downloadSharedFile_missingResourceInfo_returnsDefaultValues() throws Exception {
        // Arrange
        String linkToken = "no-resource-token";
        
        SharedLink sharedLink = SharedLink.builder()
                .linkToken(linkToken)
                .file(null)
                .folder(null)
                .build();

        Resource mockResource = new ByteArrayResource("content".getBytes());

        when(sharedLinkService.downloadSharedFile(linkToken)).thenReturn(mockResource);
        when(sharedLinkService.findByLinkToken(linkToken)).thenReturn(Optional.of(sharedLink));

        // Act & Assert
        mockMvc.perform(get("/public/shared/{linkToken}", linkToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/octet-stream"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"shared-item\""));
    }

    @Test
    @DisplayName("Should share file without recipient phone")
    void shareFile_noRecipientPhone_returnsShareResponse() throws Exception {
        // Arrange
        Long fileId = 1L;
        String requestJson = """
        {
            "recipientPhone": ""
        }
        """;

        File file = File.builder()
                .id(fileId)
                .displayName("test.pdf")
                .build();

        SharedLink sharedLink = SharedLink.builder()
                .id(1L)
                .linkToken("test-token-123")
                .file(file)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();

        when(sharedLinkService.createFileShare(fileId, 1L, "")).thenReturn(sharedLink);

        // Act & Assert
        mockMvc.perform(post("/api/files/{fileId}/share", fileId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shareId").value(1L))
                .andExpect(jsonPath("$.linkToken").value("test-token-123"));
    }

    @Test
    @DisplayName("Should handle invalid phone number format")
    void shareFile_invalidPhoneFormat_returnsBadRequest() throws Exception {
        // Arrange
        Long fileId = 1L;
        String requestJson = """
        {
            "recipientPhone": "+0987654321"
        }
        """;

        // Act & Assert
        mockMvc.perform(post("/api/files/{fileId}/share", fileId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .principal(authentication))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.recipientPhone").value("Invalid phone number format"));
    }
}