package com.cerebra.secure_file_sharing_app.UnitTests.Services;

import com.cerebra.secure_file_sharing_app.Entities.AppUser;
import com.cerebra.secure_file_sharing_app.Entities.File;
import com.cerebra.secure_file_sharing_app.Entities.Folder;
import com.cerebra.secure_file_sharing_app.Entities.StoragePath;
import com.cerebra.secure_file_sharing_app.Exceptions.CustomExceptions.FolderAccessDeniedException;
import com.cerebra.secure_file_sharing_app.Exceptions.CustomExceptions.FolderNotFoundException;
import com.cerebra.secure_file_sharing_app.Exceptions.CustomExceptions.InvalidFolderNameException;
import com.cerebra.secure_file_sharing_app.Repositories.FolderRepository;
import com.cerebra.secure_file_sharing_app.Services.FolderServiceImpl;
import com.cerebra.secure_file_sharing_app.Services.StoragePathService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FolderService Tests")
class FolderServiceTest {

    @Mock private FolderRepository folderRepository;
    @Mock private StoragePathService storagePathService;

    private FolderServiceImpl folderService;

    private AppUser testUser;
    private StoragePath testStoragePath;
    private Folder testParentFolder;
    private Folder testChildFolder;

    @BeforeEach
    void setUp() {
        folderService = new FolderServiceImpl(folderRepository, storagePathService);
        
        // Set up test entities
        testUser = AppUser.builder()
                .id(1L)
                .phoneNumber("+1234567890")
                .build();
        
        testStoragePath = StoragePath.builder()
                .id(1L)
                .basePath("/users/user1")
                .appUser(testUser)
                .build();
        
        testParentFolder = Folder.builder()
                .id(1L)
                .name("Parent Folder")
                .storagePath(testStoragePath)
                .parentFolder(null)
                .build();
        
        testChildFolder = Folder.builder()
                .id(2L)
                .name("Child Folder")
                .storagePath(testStoragePath)
                .parentFolder(testParentFolder)
                .build();
    }

    @Test
    @DisplayName("Should create folder successfully with valid parameters")
    void createFolder_validParameters_createsFolder() {
        // Arrange
        String folderName = "New Folder";
        Long parentFolderId = 1L;
        Long userId = 1L;

        Folder savedFolder = Folder.builder()
                .id(3L)
                .name(folderName)
                .storagePath(testStoragePath)
                .parentFolder(testParentFolder)
                .build();

        when(storagePathService.findByAppUserId(userId)).thenReturn(Optional.of(testStoragePath));
        when(folderRepository.findById(parentFolderId)).thenReturn(Optional.of(testParentFolder));
        when(folderRepository.findByParentFolderId(parentFolderId)).thenReturn(Collections.emptyList());
        when(folderRepository.save(any(Folder.class))).thenReturn(savedFolder);

        // Act
        Folder result = folderService.createFolder(folderName, parentFolderId, userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(folderName);
        assertThat(result.getStoragePath()).isEqualTo(testStoragePath);
        assertThat(result.getParentFolder()).isEqualTo(testParentFolder);

        // Service calls findByAppUserId multiple times (for user storage path + parent folder validation)
        verify(storagePathService, atLeast(1)).findByAppUserId(userId);
        verify(folderRepository).findById(parentFolderId);
        verify(folderRepository).save(any(Folder.class));
    }

    @Test
    @DisplayName("Should create root folder when no parent specified")
    void createFolder_noParent_createsRootFolder() {
        // Arrange
        String folderName = "Root Folder";
        Long userId = 1L;
        
        Folder savedFolder = Folder.builder()
                .id(3L)
                .name(folderName)
                .storagePath(testStoragePath)
                .parentFolder(null)
                .build();
        
        when(storagePathService.findByAppUserId(userId)).thenReturn(Optional.of(testStoragePath));
        when(folderRepository.findByStoragePathIdAndParentFolderIsNull(testStoragePath.getId()))
                .thenReturn(Collections.emptyList());
        when(folderRepository.save(any(Folder.class))).thenReturn(savedFolder);
        
        // Act
        Folder result = folderService.createFolder(folderName, null, userId);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(folderName);
        assertThat(result.getParentFolder()).isNull();
        assertThat(result.getStoragePath()).isEqualTo(testStoragePath);
        
        verify(storagePathService).findByAppUserId(userId);
        verify(folderRepository).findByStoragePathIdAndParentFolderIsNull(testStoragePath.getId());
        verify(folderRepository).save(any(Folder.class));
    }

    @Test
    @DisplayName("Should throw exception when user has no storage path")
    void createFolder_userWithoutStoragePath_throwsException() {
        // Arrange
        String folderName = "Test Folder";
        Long userId = 1L;
        
        when(storagePathService.findByAppUserId(userId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> folderService.createFolder(folderName, null, userId))
                .isInstanceOf(FolderAccessDeniedException.class)
                .hasMessageContaining("User storage path not found");
        
        verify(storagePathService).findByAppUserId(userId);
        verify(folderRepository, never()).save(any(Folder.class));
    }

    @Test
    @DisplayName("Should throw exception when parent folder not found")
    void createFolder_parentFolderNotFound_throwsFolderNotFoundException() {
        // Arrange
        String folderName = "Test Folder";
        Long parentFolderId = 999L;
        Long userId = 1L;
        
        when(storagePathService.findByAppUserId(userId)).thenReturn(Optional.of(testStoragePath));
        when(folderRepository.findById(parentFolderId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> folderService.createFolder(folderName, parentFolderId, userId))
                .isInstanceOf(FolderNotFoundException.class)
                .hasMessageContaining("Folder not found");
        
        verify(folderRepository).findById(parentFolderId);
        verify(folderRepository, never()).save(any(Folder.class));
    }

    @Test
    @DisplayName("Should throw exception when user has no access to parent folder")
    void createFolder_noAccessToParentFolder_throwsFolderAccessDeniedException() {
        // Arrange
        String folderName = "Test Folder";
        Long parentFolderId = 1L;
        Long userId = 1L;
        
        // Different user's storage path
        StoragePath otherStoragePath = StoragePath.builder()
                .id(2L)
                .basePath("/users/user2")
                .build();
        
        Folder otherUserFolder = Folder.builder()
                .id(1L)
                .storagePath(otherStoragePath)
                .build();
        
        when(storagePathService.findByAppUserId(userId)).thenReturn(Optional.of(testStoragePath));
        when(folderRepository.findById(parentFolderId)).thenReturn(Optional.of(otherUserFolder));
        
        // Act & Assert
        assertThatThrownBy(() -> folderService.createFolder(folderName, parentFolderId, userId))
                .isInstanceOf(FolderAccessDeniedException.class)
                .hasMessageContaining("Access denied to folder");
        
        verify(folderRepository, never()).save(any(Folder.class));
    }

    @Test
    @DisplayName("Should throw exception for duplicate folder name in same location")
    void createFolder_duplicateName_throwsInvalidFolderNameException() {
        // Arrange
        String folderName = "Existing Folder";
        Long parentFolderId = 1L;
        Long userId = 1L;
        
        Folder existingFolder = Folder.builder()
                .id(5L)
                .name(folderName)
                .build();
        
        when(storagePathService.findByAppUserId(userId)).thenReturn(Optional.of(testStoragePath));
        when(folderRepository.findById(parentFolderId)).thenReturn(Optional.of(testParentFolder));
        when(folderRepository.findByParentFolderId(parentFolderId)).thenReturn(Arrays.asList(existingFolder));
        
        // Act & Assert
        assertThatThrownBy(() -> folderService.createFolder(folderName, parentFolderId, userId))
                .isInstanceOf(InvalidFolderNameException.class)
                .hasMessageContaining("already exists in this location");
        
        verify(folderRepository, never()).save(any(Folder.class));
    }

    @Test
    @DisplayName("Should delete empty folder successfully")
    void deleteFolder_emptyFolder_deletesSuccessfully() {
        // Arrange
        Long folderId = 2L;
        Long userId = 1L;
        
        when(folderRepository.findById(folderId)).thenReturn(Optional.of(testChildFolder));
        when(storagePathService.findByAppUserId(userId)).thenReturn(Optional.of(testStoragePath));
        when(folderRepository.findByParentFolderId(folderId)).thenReturn(Collections.emptyList());
        when(folderRepository.findFilesByFolderId(folderId)).thenReturn(Collections.emptyList());
        
        // Act
        folderService.deleteFolder(folderId, userId);
        
        // Assert
        verify(folderRepository).findById(folderId);
        verify(storagePathService).findByAppUserId(userId);
        verify(folderRepository).findByParentFolderId(folderId);
        verify(folderRepository).findFilesByFolderId(folderId);
        verify(folderRepository).deleteById(folderId);
    }

    @Test
    @DisplayName("Should throw exception when deleting folder with subfolders")
    void deleteFolder_folderWithSubfolders_throwsFolderAccessDeniedException() {
        // Arrange
        Long folderId = 1L;
        Long userId = 1L;
        
        List<Folder> childFolders = Arrays.asList(testChildFolder);
        
        when(folderRepository.findById(folderId)).thenReturn(Optional.of(testParentFolder));
        when(storagePathService.findByAppUserId(userId)).thenReturn(Optional.of(testStoragePath));
        when(folderRepository.findByParentFolderId(folderId)).thenReturn(childFolders);
        
        // Act & Assert
        assertThatThrownBy(() -> folderService.deleteFolder(folderId, userId))
                .isInstanceOf(FolderAccessDeniedException.class)
                .hasMessageContaining("contains 1 subfolder(s)");
        
        verify(folderRepository, never()).deleteById(folderId);
    }

    @Test
    @DisplayName("Should throw exception when deleting folder with files")
    void deleteFolder_folderWithFiles_throwsFolderAccessDeniedException() {
        // Arrange
        Long folderId = 2L;
        Long userId = 1L;
        
        File testFile = File.builder()
                .id(1L)
                .displayName("test.txt")
                .build();
        
        when(folderRepository.findById(folderId)).thenReturn(Optional.of(testChildFolder));
        when(storagePathService.findByAppUserId(userId)).thenReturn(Optional.of(testStoragePath));
        when(folderRepository.findByParentFolderId(folderId)).thenReturn(Collections.emptyList());
        when(folderRepository.findFilesByFolderId(folderId)).thenReturn(Arrays.asList(testFile));
        
        // Act & Assert
        assertThatThrownBy(() -> folderService.deleteFolder(folderId, userId))
                .isInstanceOf(FolderAccessDeniedException.class)
                .hasMessageContaining("contains 1 file(s)");
        
        verify(folderRepository, never()).deleteById(folderId);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent folder")
    void deleteFolder_folderNotFound_throwsFolderNotFoundException() {
        // Arrange
        Long folderId = 999L;
        Long userId = 1L;
        
        when(folderRepository.findById(folderId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> folderService.deleteFolder(folderId, userId))
                .isInstanceOf(FolderNotFoundException.class)
                .hasMessageContaining("Folder not found");
        
        verify(folderRepository, never()).deleteById(folderId);
    }

    @Test
    @DisplayName("Should return true for valid folder access")
    void hasAccess_validAccess_returnsTrue() {
        // Arrange
        Long folderId = 1L;
        Long userId = 1L;
        
        when(folderRepository.findById(folderId)).thenReturn(Optional.of(testParentFolder));
        when(storagePathService.findByAppUserId(userId)).thenReturn(Optional.of(testStoragePath));
        
        // Act
        boolean result = folderService.hasAccess(folderId, userId);
        
        // Assert
        assertThat(result).isTrue();
        
        verify(folderRepository).findById(folderId);
        verify(storagePathService).findByAppUserId(userId);
    }

    @Test
    @DisplayName("Should return false for invalid folder access")
    void hasAccess_invalidAccess_returnsFalse() {
        // Arrange
        Long folderId = 1L;
        Long userId = 1L;
        
        when(folderRepository.findById(folderId)).thenReturn(Optional.empty());
        
        // Act
        boolean result = folderService.hasAccess(folderId, userId);
        
        // Assert
        assertThat(result).isFalse();
        
        verify(folderRepository).findById(folderId);
    }

    @Test
    @DisplayName("Should validate folder names correctly")
    void isValidFolderName_variousNames_returnsCorrectValidation() {
        // Valid names
        assertThat(folderService.isValidFolderName("Valid Folder")).isTrue();
        assertThat(folderService.isValidFolderName("Test123")).isTrue();
        assertThat(folderService.isValidFolderName("Folder-Name_v2")).isTrue();
        
        // Invalid names
        assertThat(folderService.isValidFolderName("")).isFalse();
        assertThat(folderService.isValidFolderName(null)).isFalse();
        assertThat(folderService.isValidFolderName("   ")).isFalse();
        assertThat(folderService.isValidFolderName("Invalid/Name")).isFalse();
        assertThat(folderService.isValidFolderName("Invalid\\Name")).isFalse();
        assertThat(folderService.isValidFolderName("Invalid:Name")).isFalse();
        assertThat(folderService.isValidFolderName("Invalid*Name")).isFalse();
        assertThat(folderService.isValidFolderName("Invalid?Name")).isFalse();
        assertThat(folderService.isValidFolderName("Invalid\"Name")).isFalse();
        assertThat(folderService.isValidFolderName("Invalid<Name")).isFalse();
        assertThat(folderService.isValidFolderName("Invalid>Name")).isFalse();
        assertThat(folderService.isValidFolderName("Invalid|Name")).isFalse();
    }

    @Test
    @DisplayName("Should reject reserved folder names")
    void isValidFolderName_reservedNames_returnsFalse() {
        // Windows reserved names
        assertThat(folderService.isValidFolderName("CON")).isFalse();
        assertThat(folderService.isValidFolderName("PRN")).isFalse();
        assertThat(folderService.isValidFolderName("AUX")).isFalse();
        assertThat(folderService.isValidFolderName("NUL")).isFalse();
        assertThat(folderService.isValidFolderName("COM1")).isFalse();
        assertThat(folderService.isValidFolderName("LPT1")).isFalse();
        
        // Case insensitive
        assertThat(folderService.isValidFolderName("con")).isFalse();
        assertThat(folderService.isValidFolderName("Con")).isFalse();
    }

    @Test
    @DisplayName("Should reject folder names that are too long")
    void isValidFolderName_tooLong_returnsFalse() {
        // Create a name longer than 255 characters
        String longName = "a".repeat(256);
        
        assertThat(folderService.isValidFolderName(longName)).isFalse();
        
        // 255 characters should be valid
        String maxLengthName = "a".repeat(255);
        assertThat(folderService.isValidFolderName(maxLengthName)).isTrue();
    }

    @Test
    @DisplayName("Should throw exception for empty folder name during creation")
    void createFolder_emptyName_throwsInvalidFolderNameException() {
        // Arrange
        String emptyName = "";
        Long userId = 1L;

        // Don't stub storagePathService - validation happens before that call

        // Act & Assert
        assertThatThrownBy(() -> folderService.createFolder(emptyName, null, userId))
                .isInstanceOf(InvalidFolderNameException.class)
                .hasMessageContaining("Folder name cannot be empty");

        verify(folderRepository, never()).save(any(Folder.class));
        // No verification for storagePathService since validation fails before that call
    }

    @Test
    @DisplayName("Should throw exception for folder name with invalid characters during creation")
    void createFolder_invalidCharacters_throwsInvalidFolderNameException() {
        // Arrange
        String invalidName = "Invalid/Name";
        Long userId = 1L;

        // Don't stub storagePathService - validation happens before that call

        // Act & Assert
        assertThatThrownBy(() -> folderService.createFolder(invalidName, null, userId))
                .isInstanceOf(InvalidFolderNameException.class)
                .hasMessageContaining("invalid character");

        verify(folderRepository, never()).save(any(Folder.class));
        // No verification for storagePathService since validation fails before that call
    }

    @Test
    @DisplayName("Should find folders by storage path")
    void findByStoragePathId_validId_returnsFolders() {
        // Arrange
        Long storagePathId = 1L;
        List<Folder> folders = Arrays.asList(testParentFolder, testChildFolder);
        
        when(folderRepository.findByStoragePathId(storagePathId)).thenReturn(folders);
        
        // Act
        List<Folder> result = folderService.findByStoragePathId(storagePathId);
        
        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).contains(testParentFolder, testChildFolder);
        
        verify(folderRepository).findByStoragePathId(storagePathId);
    }

    @Test
    @DisplayName("Should find folders by parent folder")
    void findByParentFolderId_validId_returnsChildFolders() {
        // Arrange
        Long parentFolderId = 1L;
        List<Folder> childFolders = Arrays.asList(testChildFolder);
        
        when(folderRepository.findByParentFolderId(parentFolderId)).thenReturn(childFolders);
        
        // Act
        List<Folder> result = folderService.findByParentFolderId(parentFolderId);
        
        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).contains(testChildFolder);
        
        verify(folderRepository).findByParentFolderId(parentFolderId);
    }

    @Test
    @DisplayName("Should find root folders")
    void findRootFolders_validStoragePathId_returnsRootFolders() {
        // Arrange
        Long storagePathId = 1L;
        List<Folder> rootFolders = Arrays.asList(testParentFolder);
        
        when(folderRepository.findByStoragePathIdAndParentFolderIsNull(storagePathId)).thenReturn(rootFolders);
        
        // Act
        List<Folder> result = folderService.findRootFolders(storagePathId);
        
        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).contains(testParentFolder);
        
        verify(folderRepository).findByStoragePathIdAndParentFolderIsNull(storagePathId);
    }

    @Test
    @DisplayName("Should save folder successfully")
    void save_validFolder_returnsFolder() {
        // Arrange
        when(folderRepository.save(testParentFolder)).thenReturn(testParentFolder);
        
        // Act
        Folder result = folderService.save(testParentFolder);
        
        // Assert
        assertThat(result).isEqualTo(testParentFolder);
        
        verify(folderRepository).save(testParentFolder);
    }

    @Test
    @DisplayName("Should find folder by ID")
    void findById_validId_returnsFolder() {
        // Arrange
        Long folderId = 1L;
        
        when(folderRepository.findById(folderId)).thenReturn(Optional.of(testParentFolder));
        
        // Act
        Optional<Folder> result = folderService.findById(folderId);
        
        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testParentFolder);
        
        verify(folderRepository).findById(folderId);
    }
}