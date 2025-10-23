package com.cerebra.secure_file_sharing_app.UnitTests.Controllers;

import com.cerebra.secure_file_sharing_app.Controllers.FolderController;
import com.cerebra.secure_file_sharing_app.Entities.AppUser;
import com.cerebra.secure_file_sharing_app.Entities.Folder;
import com.cerebra.secure_file_sharing_app.Entities.StoragePath;
import com.cerebra.secure_file_sharing_app.Exceptions.GlobalExceptionHandler;
import com.cerebra.secure_file_sharing_app.Services.FolderService;
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
@DisplayName("FolderController Tests")
class FolderControllerTest {

    @Mock
    private FolderService folderService;

    @Mock
    private StoragePathService storagePathService;

    @Mock
    private Authentication authentication;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private AppUser mockUser;
    private StoragePath mockStoragePath;

    @BeforeEach
    void setUp() {
        FolderController folderController = new FolderController(folderService, storagePathService);
        mockMvc = MockMvcBuilders.standaloneSetup(folderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Set up mock user and storage path
        mockUser = AppUser.builder()
                .id(1L)
                .phoneNumber("+1234567890")
                .build();

        mockStoragePath = StoragePath.builder()
                .id(10L)
                .basePath("/user1")
                .build();

        // Default authentication setup
        when(authentication.getName()).thenReturn("+1234567890");
        when(authentication.getPrincipal()).thenReturn(mockUser);
        when(storagePathService.findByAppUserId(1L)).thenReturn(Optional.of(mockStoragePath));
    }

    @Test
    @DisplayName("Should create folder successfully")
    void createFolder_validRequest_returnsFolderResponse() throws Exception {
        // Arrange
        String requestJson = """
            {
                "name": "My Documents",
                "parentFolderId": null
            }
            """;

        Folder createdFolder = Folder.builder()
                .id(1L)
                .name("My Documents")
                .parentFolder(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(folderService.createFolder("My Documents", null, 1L)).thenReturn(createdFolder);

        // Act & Assert
        mockMvc.perform(post("/api/folders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
                .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("My Documents"))
                .andExpect(jsonPath("$.parentFolderId").isEmpty())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @DisplayName("Should create subfolder successfully")
    void createFolder_withParentFolder_returnsSubfolderResponse() throws Exception {
        // Arrange
        String requestJson = """
            {
                "name": "Subfolder",
                "parentFolderId": 5
            }
            """;

        Folder parentFolder = Folder.builder().id(5L).name("Parent").build();
        Folder createdFolder = Folder.builder()
                .id(2L)
                .name("Subfolder")
                .parentFolder(parentFolder)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(folderService.createFolder("Subfolder", 5L, 1L)).thenReturn(createdFolder);

        // Act & Assert
        mockMvc.perform(post("/api/folders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
                .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.name").value("Subfolder"))
                .andExpect(jsonPath("$.parentFolderId").value(5L));
    }

    @Test
    @DisplayName("Should delete folder successfully")
    void deleteFolder_validFolderId_returnsSuccessMessage() throws Exception {
        // Arrange
        Long folderId = 1L;

        // Act & Assert
        mockMvc.perform(delete("/api/folders/{folderId}", folderId)
                .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().string("Folder deleted successfully"));
    }

    @Test
    @DisplayName("Should get user folders successfully")
    void getMyFolders_authenticatedUser_returnsFolderList() throws Exception {
        // Arrange
        List<Folder> userFolders = Arrays.asList(
                Folder.builder().id(1L).name("Documents").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                Folder.builder().id(2L).name("Pictures").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build()
        );

        when(folderService.findByStoragePathId(10L)).thenReturn(userFolders);

        // Act & Assert
        mockMvc.perform(get("/api/folders/my-folders")
                .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Documents"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].name").value("Pictures"));
    }

    @Test
    @DisplayName("Should get root folders successfully")
    void getRootFolders_authenticatedUser_returnsRootFolders() throws Exception {
        // Arrange
        List<Folder> rootFolders = Arrays.asList(
                Folder.builder().id(1L).name("Root Folder 1").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                Folder.builder().id(2L).name("Root Folder 2").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build()
        );

        when(folderService.findRootFolders(10L)).thenReturn(rootFolders);

        // Act & Assert
        mockMvc.perform(get("/api/folders/root")
                .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Root Folder 1"))
                .andExpect(jsonPath("$[1].name").value("Root Folder 2"));
    }

    @Test
    @DisplayName("Should get subfolders successfully")
    void getSubfolders_validFolderId_returnsSubfolders() throws Exception {
        // Arrange
        Long folderId = 5L;
        List<Folder> subfolders = Arrays.asList(
                Folder.builder().id(6L).name("Subfolder 1").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                Folder.builder().id(7L).name("Subfolder 2").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build()
        );

        when(folderService.hasAccess(folderId, 1L)).thenReturn(true);
        when(folderService.findByParentFolderId(folderId)).thenReturn(subfolders);

        // Act & Assert
        mockMvc.perform(get("/api/folders/{folderId}/subfolders", folderId)
                .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Subfolder 1"))
                .andExpect(jsonPath("$[1].name").value("Subfolder 2"));
    }

    @Test
    @DisplayName("Should get folder details successfully")
    void getFolderDetails_validFolderId_returnsFolderDetails() throws Exception {
        // Arrange
        Long folderId = 1L;
        Folder folder = Folder.builder()
                .id(folderId)
                .name("Test Folder")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(folderService.hasAccess(folderId, 1L)).thenReturn(true);
        when(folderService.findById(folderId)).thenReturn(Optional.of(folder));

        // Act & Assert
        mockMvc.perform(get("/api/folders/{folderId}", folderId)
                .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Test Folder"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @DisplayName("Should handle access denied for subfolders")
    void getSubfolders_accessDenied_returnsInternalServerError() throws Exception {
        // Arrange
        Long folderId = 5L;

        when(folderService.hasAccess(folderId, 1L)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/folders/{folderId}/subfolders", folderId)
                .principal(authentication))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    @DisplayName("Should handle access denied for folder details")
    void getFolderDetails_accessDenied_returnsInternalServerError() throws Exception {
        // Arrange
        Long folderId = 1L;

        when(folderService.hasAccess(folderId, 1L)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/folders/{folderId}", folderId)
                .principal(authentication))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    @DisplayName("Should handle folder not found")
    void getFolderDetails_folderNotFound_returnsInternalServerError() throws Exception {
        // Arrange
        Long folderId = 999L;

        when(folderService.hasAccess(folderId, 1L)).thenReturn(true);
        when(folderService.findById(folderId)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/folders/{folderId}", folderId)
                .principal(authentication))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    @DisplayName("Should return empty list when user has no folders")
    void getMyFolders_userWithNoFolders_returnsEmptyList() throws Exception {
        // Arrange
        when(folderService.findByStoragePathId(10L)).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/folders/my-folders")
                .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Should handle service exceptions during folder creation")
    void createFolder_serviceException_returnsInternalServerError() throws Exception {
        // Arrange
        String requestJson = """
            {
                "name": "Test Folder",
                "parentFolderId": null
            }
            """;

        when(folderService.createFolder("Test Folder", null, 1L))
                .thenThrow(new RuntimeException("Folder creation failed"));

        // Act & Assert
        mockMvc.perform(post("/api/folders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
                .principal(authentication))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    @DisplayName("Should handle storage path not found")
    void getMyFolders_storagePathNotFound_returnsInternalServerError() throws Exception {
        // Arrange
        when(storagePathService.findByAppUserId(1L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/folders/my-folders")
                .principal(authentication))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    @DisplayName("Should return empty subfolders list")
    void getSubfolders_noSubfolders_returnsEmptyList() throws Exception {
        // Arrange
        Long folderId = 5L;

        when(folderService.hasAccess(folderId, 1L)).thenReturn(true);
        when(folderService.findByParentFolderId(folderId)).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/folders/{folderId}/subfolders", folderId)
                .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}