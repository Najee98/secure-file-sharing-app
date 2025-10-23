package com.cerebra.secure_file_sharing_app.UnitTests.Controllers;

import com.cerebra.secure_file_sharing_app.Controllers.FileController;
import com.cerebra.secure_file_sharing_app.Entities.AppUser;
import com.cerebra.secure_file_sharing_app.Entities.File;
import com.cerebra.secure_file_sharing_app.Entities.StoragePath;
import com.cerebra.secure_file_sharing_app.Exceptions.CustomExceptions.FileNotFoundException;
import com.cerebra.secure_file_sharing_app.Exceptions.GlobalExceptionHandler;
import com.cerebra.secure_file_sharing_app.Services.FileService;
import com.cerebra.secure_file_sharing_app.Services.StoragePathService;
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
import org.springframework.mock.web.MockMultipartFile;
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
@DisplayName("FileController Tests")
class FileControllerTest {

    @Mock
    private FileService fileService;

    @Mock
    private StoragePathService storagePathService;

    @Mock
    private Authentication authentication;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private AppUser mockUser;

    @BeforeEach
    void setUp() {
        FileController fileController = new FileController(fileService, storagePathService);
        mockMvc = MockMvcBuilders.standaloneSetup(fileController)
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
    @DisplayName("Should upload file successfully")
    void uploadFile_validFile_returnsFileUploadResponse() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "test content".getBytes()
        );

        File uploadedFile = File.builder()
                .id(1L)
                .displayName("test.txt")
                .size(12L)
                .mimeType("text/plain")
                .createdAt(LocalDateTime.now())
                .build();

        when(fileService.uploadFile(any(), eq(null), eq(1L))).thenReturn(uploadedFile);

        // Act & Assert
        mockMvc.perform(multipart("/api/files/upload")
                        .file(file)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.fileId").value(1L))
                .andExpect(jsonPath("$.fileName").value("test.txt"))
                .andExpect(jsonPath("$.fileSize").value(12L))
                .andExpect(jsonPath("$.mimeType").value("text/plain"))
                .andExpect(jsonPath("$.message").value("File uploaded successfully"));
    }

    @Test
    @DisplayName("Should upload file to specific folder")
    void uploadFile_withFolderId_uploadsToFolder() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "pdf content".getBytes()
        );

        File uploadedFile = File.builder()
                .id(2L)
                .displayName("document.pdf")
                .size(11L)
                .mimeType("application/pdf")
                .createdAt(LocalDateTime.now())
                .build();

        when(fileService.uploadFile(any(), eq(5L), eq(1L))).thenReturn(uploadedFile);

        // Act & Assert
        mockMvc.perform(multipart("/api/files/upload")
                        .file(file)
                        .param("folderId", "5")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileId").value(2L))
                .andExpect(jsonPath("$.fileName").value("document.pdf"))
                .andExpect(jsonPath("$.mimeType").value("application/pdf"));
    }

    @Test
    @DisplayName("Should download file successfully")
    void downloadFile_validFileId_returnsFileResource() throws Exception {
        // Arrange
        Long fileId = 1L;

        File fileEntity = File.builder()
                .id(fileId)
                .displayName("test.txt")
                .mimeType("text/plain")
                .build();

        Resource mockResource = new ByteArrayResource("test content".getBytes());

        when(fileService.downloadFile(fileId, 1L)).thenReturn(mockResource);
        when(fileService.findById(fileId)).thenReturn(Optional.of(fileEntity));

        // Act & Assert
        mockMvc.perform(get("/api/files/{fileId}/download", fileId)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"test.txt\""));
    }

    @Test
    @DisplayName("Should delete file successfully")
    void deleteFile_validFileId_returnsSuccessMessage() throws Exception {
        // Arrange
        Long fileId = 1L;

        // Act & Assert
        mockMvc.perform(delete("/api/files/{fileId}", fileId)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().string("File deleted successfully"));
    }

    @Test
    @DisplayName("Should get user files successfully")
    void getMyFiles_authenticatedUser_returnsUserFiles() throws Exception {
        // Arrange
        List<File> userFiles = Arrays.asList(
                File.builder().id(1L).displayName("file1.txt").build(),
                File.builder().id(2L).displayName("file2.pdf").build()
        );

        when(fileService.getUserFiles(1L)).thenReturn(userFiles);

        // Act & Assert
        mockMvc.perform(get("/api/files/my-files")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].displayName").value("file1.txt"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].displayName").value("file2.pdf"));
    }

    @Test
    @DisplayName("Should get folder files successfully")
    void getFolderFiles_validFolderId_returnsFolderFiles() throws Exception {
        // Arrange
        Long folderId = 5L;

        List<File> folderFiles = Arrays.asList(
                File.builder().id(3L).displayName("folder-file1.txt").build(),
                File.builder().id(4L).displayName("folder-file2.jpg").build()
        );

        when(fileService.getFolderFiles(folderId, 1L)).thenReturn(folderFiles);

        // Act & Assert
        mockMvc.perform(get("/api/files/folder/{folderId}", folderId)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].displayName").value("folder-file1.txt"))
                .andExpect(jsonPath("$[1].displayName").value("folder-file2.jpg"));
    }

    @Test
    @DisplayName("Should get root files successfully")
    void getRootFiles_authenticatedUser_returnsRootFiles() throws Exception {
        // Arrange
        StoragePath storagePath = StoragePath.builder().id(10L).basePath("/user1").build();

        List<File> rootFiles = Arrays.asList(
                File.builder().id(5L).displayName("root-file1.txt").build()
        );

        when(storagePathService.findByAppUserId(1L)).thenReturn(Optional.of(storagePath));
        when(fileService.findRootFiles(10L)).thenReturn(rootFiles);

        // Act & Assert
        mockMvc.perform(get("/api/files/root")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].displayName").value("root-file1.txt"));
    }

    @Test
    @DisplayName("Should return empty list when user has no files")
    void getMyFiles_userWithNoFiles_returnsEmptyList() throws Exception {
        // Arrange
        when(fileService.getUserFiles(1L)).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/files/my-files")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Should handle file service exceptions during upload")
    void uploadFile_serviceException_returnsInternalServerError() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "test content".getBytes()
        );

        when(fileService.uploadFile(any(), eq(null), eq(1L)))
                .thenThrow(new RuntimeException("Storage error"));

        // Act & Assert
        mockMvc.perform(multipart("/api/files/upload")
                        .file(file)
                        .principal(authentication))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    @DisplayName("Should handle file not found during download")
    void downloadFile_fileNotFound_returnsNotFound() throws Exception {
        // Arrange
        Long fileId = 999L;

        when(fileService.downloadFile(fileId, 1L))
                .thenThrow(new FileNotFoundException("File not found"));

        // Act & Assert
        mockMvc.perform(get("/api/files/{fileId}/download", fileId)
                        .principal(authentication))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Requested resource not found"));
    }

    @Test
    @DisplayName("Should handle missing file parameter in upload")
    void uploadFile_missingFile_returnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(multipart("/api/files/upload")
                        .principal(authentication))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle storage path not found for root files")
    void getRootFiles_storagePathNotFound_returnsInternalServerError() throws Exception {
        // Arrange
        when(storagePathService.findByAppUserId(1L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/files/root")
                        .principal(authentication))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}