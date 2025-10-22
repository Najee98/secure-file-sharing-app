package com.cerebra.secure_file_sharing_app.UnitTests.Services;

import com.cerebra.secure_file_sharing_app.Entities.File;
import com.cerebra.secure_file_sharing_app.Entities.Folder;
import com.cerebra.secure_file_sharing_app.Entities.SharedLink;
import com.cerebra.secure_file_sharing_app.Entities.StoragePath;
import com.cerebra.secure_file_sharing_app.Exceptions.CustomExceptions.*;
import com.cerebra.secure_file_sharing_app.Repositories.SharedLinkRepository;
import com.cerebra.secure_file_sharing_app.Services.*;
import com.cerebra.secure_file_sharing_app.Shared.SMSResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SharedLinkService Tests")
class SharedLinkServiceTest {

    @Mock private SharedLinkRepository sharedLinkRepository;
    @Mock private FileService fileService;
    @Mock private FolderService folderService;
    @Mock private HttpSMSService smsService;
    @Mock private StoragePathService storagePathService;
    @Mock private Resource mockResource;

    private SharedLinkServiceImpl sharedLinkService;

    @TempDir
    Path tempDir;

    private File testFile;
    private Folder testFolder;
    private SharedLink testSharedLink;
    private StoragePath testStoragePath;

    @BeforeEach
    void setUp() {
        sharedLinkService = new SharedLinkServiceImpl(
            sharedLinkRepository, fileService, folderService, smsService, storagePathService
        );
        
        // Set configuration values
        ReflectionTestUtils.setField(sharedLinkService, "shareExpirationDays", 7);
        ReflectionTestUtils.setField(sharedLinkService, "baseUrl", "http://localhost:8080");
        
        // Set up test entities
        testStoragePath = StoragePath.builder()
                .id(1L)
                .basePath("/users/user1")
                .build();
        
        testFile = File.builder()
                .id(1L)
                .displayName("test.txt")
                .physicalPath("/path/to/test.txt")
                .storagePath(testStoragePath)
                .build();
        
        testFolder = Folder.builder()
                .id(1L)
                .name("Test Folder")
                .storagePath(testStoragePath)
                .build();
        
        testSharedLink = SharedLink.builder()
                .id(1L)
                .linkToken("test-token-123")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .file(testFile)
                .build();
    }

    @Test
    @DisplayName("Should save shared link successfully")
    void save_validSharedLink_returnsSharedLink() {
        // Arrange
        when(sharedLinkRepository.save(testSharedLink)).thenReturn(testSharedLink);
        
        // Act
        SharedLink result = sharedLinkService.save(testSharedLink);
        
        // Assert
        assertThat(result).isEqualTo(testSharedLink);
        verify(sharedLinkRepository).save(testSharedLink);
    }

    @Test
    @DisplayName("Should find shared link by ID")
    void findById_existingLink_returnsSharedLink() {
        // Arrange
        Long linkId = 1L;
        when(sharedLinkRepository.findById(linkId)).thenReturn(Optional.of(testSharedLink));
        
        // Act
        Optional<SharedLink> result = sharedLinkService.findById(linkId);
        
        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testSharedLink);
        verify(sharedLinkRepository).findById(linkId);
    }

    @Test
    @DisplayName("Should find shared link by token")
    void findByLinkToken_existingToken_returnsSharedLink() {
        // Arrange
        String token = "test-token-123";
        when(sharedLinkRepository.findByLinkToken(token)).thenReturn(Optional.of(testSharedLink));
        
        // Act
        Optional<SharedLink> result = sharedLinkService.findByLinkToken(token);
        
        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testSharedLink);
        verify(sharedLinkRepository).findByLinkToken(token);
    }

    @Test
    @DisplayName("Should create file share successfully")
    void createFileShare_validFileAndUser_createsSharedLink() {
        // Arrange
        Long fileId = 1L;
        Long userId = 1L;
        String recipientPhone = "+1234567890";
        
        SharedLink savedLink = SharedLink.builder()
                .id(1L)
                .linkToken("generated-token")
                .file(testFile)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        
        when(fileService.findById(fileId)).thenReturn(Optional.of(testFile));
        when(sharedLinkRepository.save(any(SharedLink.class))).thenReturn(savedLink);
        when(smsService.sendSMS(eq(recipientPhone), anyString())).thenReturn(SMSResponse.success("SMS sent"));
        
        // Act
        SharedLink result = sharedLinkService.createFileShare(fileId, userId, recipientPhone);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFile()).isEqualTo(testFile);
        assertThat(result.getLinkToken()).isNotNull();
        assertThat(result.getExpiresAt()).isAfter(LocalDateTime.now());
        
        verify(fileService).findById(fileId);
        verify(sharedLinkRepository).save(any(SharedLink.class));
        verify(smsService).sendSMS(eq(recipientPhone), anyString());
    }

    @Test
    @DisplayName("Should create file share without SMS when no recipient phone")
    void createFileShare_noRecipientPhone_createsShareWithoutSMS() {
        // Arrange
        Long fileId = 1L;
        Long userId = 1L;
        
        SharedLink savedLink = SharedLink.builder()
                .id(1L)
                .linkToken("generated-token")
                .file(testFile)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        
        when(fileService.findById(fileId)).thenReturn(Optional.of(testFile));
        when(sharedLinkRepository.save(any(SharedLink.class))).thenReturn(savedLink);
        
        // Act
        SharedLink result = sharedLinkService.createFileShare(fileId, userId, null);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFile()).isEqualTo(testFile);
        
        verify(fileService).findById(fileId);
        verify(sharedLinkRepository).save(any(SharedLink.class));
        verify(smsService, never()).sendSMS(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw exception when creating share for non-existent file")
    void createFileShare_fileNotFound_throwsFileNotFoundException() {
        // Arrange
        Long fileId = 999L;
        Long userId = 1L;
        
        when(fileService.findById(fileId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> sharedLinkService.createFileShare(fileId, userId, null))
                .isInstanceOf(FileNotFoundException.class)
                .hasMessageContaining("File not found");
        
        verify(fileService).findById(fileId);
        verify(sharedLinkRepository, never()).save(any(SharedLink.class));
    }

    @Test
    @DisplayName("Should create folder share successfully")
    void createFolderShare_validFolderAndUser_createsSharedLink() {
        // Arrange
        Long folderId = 1L;
        Long userId = 1L;
        String recipientPhone = "+1234567890";
        
        SharedLink savedLink = SharedLink.builder()
                .id(1L)
                .linkToken("generated-token")
                .folder(testFolder)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        
        when(folderService.hasAccess(folderId, userId)).thenReturn(true);
        when(folderService.findById(folderId)).thenReturn(Optional.of(testFolder));
        when(sharedLinkRepository.save(any(SharedLink.class))).thenReturn(savedLink);
        when(smsService.sendSMS(eq(recipientPhone), anyString())).thenReturn(SMSResponse.success("SMS sent"));
        
        // Act
        SharedLink result = sharedLinkService.createFolderShare(folderId, userId, recipientPhone);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFolder()).isEqualTo(testFolder);
        assertThat(result.getFile()).isNull();
        assertThat(result.getLinkToken()).isNotNull();
        
        verify(folderService).hasAccess(folderId, userId);
        verify(folderService).findById(folderId);
        verify(sharedLinkRepository).save(any(SharedLink.class));
        verify(smsService).sendSMS(eq(recipientPhone), anyString());
    }

    @Test
    @DisplayName("Should throw exception when creating folder share without access")
    void createFolderShare_noAccess_throwsFolderNotFoundException() {
        // Arrange
        Long folderId = 1L;
        Long userId = 1L;
        
        when(folderService.hasAccess(folderId, userId)).thenReturn(false);
        
        // Act & Assert
        assertThatThrownBy(() -> sharedLinkService.createFolderShare(folderId, userId, null))
                .isInstanceOf(FolderNotFoundException.class)
                .hasMessageContaining("Folder not found or access denied");
        
        verify(folderService).hasAccess(folderId, userId);
        verify(sharedLinkRepository, never()).save(any(SharedLink.class));
    }

    @Test
    @DisplayName("Should download shared file successfully")
    void downloadSharedFile_validToken_returnsFileResource() {
        // Arrange
        String linkToken = "valid-token";
        
        when(sharedLinkRepository.findByLinkToken(linkToken)).thenReturn(Optional.of(testSharedLink));
        when(fileService.downloadSharedFile(testFile.getId())).thenReturn(mockResource);
        
        // Act
        Resource result = sharedLinkService.downloadSharedFile(linkToken);
        
        // Assert
        assertThat(result).isEqualTo(mockResource);
        
        verify(sharedLinkRepository).findByLinkToken(linkToken);
        verify(fileService).downloadSharedFile(testFile.getId());
    }

    @Test
    @DisplayName("Should download shared folder as ZIP")
    void downloadSharedFile_folderToken_returnsZipResource() throws IOException {
        // Arrange
        String linkToken = "folder-token";
        SharedLink folderShare = SharedLink.builder()
                .linkToken(linkToken)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .folder(testFolder)
                .build();
        
        List<File> folderFiles = Arrays.asList(testFile);
        
        when(sharedLinkRepository.findByLinkToken(linkToken)).thenReturn(Optional.of(folderShare));
        when(fileService.getFolderFiles(testFolder.getId(), null)).thenReturn(folderFiles);
        
        // Create a mock file for the test
        Path testFilePath = tempDir.resolve("test.txt");
        Files.write(testFilePath, "test content".getBytes());
        testFile = File.builder()
                .id(1L)
                .displayName("test.txt")
                .physicalPath(testFilePath.toString())
                .build();
        folderFiles = Arrays.asList(testFile);
        when(fileService.getFolderFiles(testFolder.getId(), null)).thenReturn(folderFiles);
        
        // Act
        Resource result = sharedLinkService.downloadSharedFile(linkToken);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFilename()).contains(".zip");
        
        verify(sharedLinkRepository).findByLinkToken(linkToken);
        verify(fileService).getFolderFiles(testFolder.getId(), null);
    }

    @Test
    @DisplayName("Should throw exception for expired share link")
    void downloadSharedFile_expiredToken_throwsShareExpiredException() {
        // Arrange
        String linkToken = "expired-token";
        SharedLink expiredLink = SharedLink.builder()
                .linkToken(linkToken)
                .expiresAt(LocalDateTime.now().minusDays(1)) // Expired
                .file(testFile)
                .build();
        
        when(sharedLinkRepository.findByLinkToken(linkToken)).thenReturn(Optional.of(expiredLink));
        
        // Act & Assert
        assertThatThrownBy(() -> sharedLinkService.downloadSharedFile(linkToken))
                .isInstanceOf(ShareExpiredException.class)
                .hasMessageContaining("Share link has expired");
        
        verify(sharedLinkRepository).findByLinkToken(linkToken);
        verify(fileService, never()).downloadSharedFile(anyLong());
    }

    @Test
    @DisplayName("Should throw exception for invalid share token")
    void downloadSharedFile_invalidToken_throwsShareNotFoundException() {
        // Arrange
        String linkToken = "invalid-token";
        
        when(sharedLinkRepository.findByLinkToken(linkToken)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> sharedLinkService.downloadSharedFile(linkToken))
                .isInstanceOf(ShareNotFoundException.class)
                .hasMessageContaining("Invalid or expired share link");
        
        verify(sharedLinkRepository).findByLinkToken(linkToken);
    }

    @Test
    @DisplayName("Should revoke share successfully")
    void revokeShare_validShareAndUser_deletesShare() {
        // Arrange
        Long shareId = 1L;
        Long userId = 1L;
        
        when(sharedLinkRepository.findById(shareId)).thenReturn(Optional.of(testSharedLink));
        
        // Act
        sharedLinkService.revokeShare(shareId, userId);
        
        // Assert
        verify(sharedLinkRepository).findById(shareId);
        verify(sharedLinkRepository).deleteById(shareId);
    }

    @Test
    @DisplayName("Should throw exception when revoking non-existent share")
    void revokeShare_shareNotFound_throwsShareNotFoundException() {
        // Arrange
        Long shareId = 999L;
        Long userId = 1L;
        
        when(sharedLinkRepository.findById(shareId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> sharedLinkService.revokeShare(shareId, userId))
                .isInstanceOf(ShareNotFoundException.class)
                .hasMessageContaining("Share not found");
        
        verify(sharedLinkRepository).findById(shareId);
        verify(sharedLinkRepository, never()).deleteById(shareId);
    }

    @Test
    @DisplayName("Should get user shares successfully")
    void getUserShares_validUser_returnsUserShares() {
        // Arrange
        Long userId = 1L;
        List<SharedLink> allShares = Arrays.asList(testSharedLink);
        
        when(storagePathService.findByAppUserId(userId)).thenReturn(Optional.of(testStoragePath));
        when(sharedLinkRepository.findAll()).thenReturn(allShares);
        
        // Act
        List<SharedLink> result = sharedLinkService.getUserShares(userId);
        
        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).contains(testSharedLink);
        
        verify(storagePathService).findByAppUserId(userId);
        verify(sharedLinkRepository).findAll();
    }

    @Test
    @DisplayName("Should validate share token correctly")
    void isValidShareToken_validToken_returnsTrue() {
        // Arrange
        String validToken = "valid-token";
        
        when(sharedLinkRepository.findByLinkToken(validToken)).thenReturn(Optional.of(testSharedLink));
        
        // Act
        boolean result = sharedLinkService.isValidShareToken(validToken);
        
        // Assert
        assertThat(result).isTrue();
        verify(sharedLinkRepository).findByLinkToken(validToken);
    }

    @Test
    @DisplayName("Should validate share token as invalid for expired link")
    void isValidShareToken_expiredToken_returnsFalse() {
        // Arrange
        String expiredToken = "expired-token";
        SharedLink expiredLink = SharedLink.builder()
                .linkToken(expiredToken)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();
        
        when(sharedLinkRepository.findByLinkToken(expiredToken)).thenReturn(Optional.of(expiredLink));
        
        // Act
        boolean result = sharedLinkService.isValidShareToken(expiredToken);
        
        // Assert
        assertThat(result).isFalse();
        verify(sharedLinkRepository).findByLinkToken(expiredToken);
    }

    @Test
    @DisplayName("Should delete expired links successfully")
    void deleteExpiredLinks_expiredLinksExist_deletesExpiredLinks() {
        // Arrange
        List<SharedLink> expiredLinks = Arrays.asList(testSharedLink);
        
        when(sharedLinkRepository.findByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(expiredLinks);
        
        // Act
        sharedLinkService.deleteExpiredLinks();
        
        // Assert
        verify(sharedLinkRepository).findByExpiresAtBefore(any(LocalDateTime.class));
        verify(sharedLinkRepository).deleteAll(expiredLinks);
    }

    @Test
    @DisplayName("Should handle empty folder ZIP creation")
    void downloadSharedFile_emptyFolder_returnsEmptyZip() throws IOException {
        // Arrange
        String linkToken = "empty-folder-token";
        SharedLink folderShare = SharedLink.builder()
                .linkToken(linkToken)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .folder(testFolder)
                .build();
        
        when(sharedLinkRepository.findByLinkToken(linkToken)).thenReturn(Optional.of(folderShare));
        when(fileService.getFolderFiles(testFolder.getId(), null)).thenReturn(Collections.emptyList());
        
        // Act
        Resource result = sharedLinkService.downloadSharedFile(linkToken);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFilename()).contains("empty");
        
        verify(fileService).getFolderFiles(testFolder.getId(), null);
    }

    @Test
    @DisplayName("Should handle SMS sending failure gracefully")
    void createFileShare_smsFailure_stillCreatesShare() {
        // Arrange
        Long fileId = 1L;
        Long userId = 1L;
        String recipientPhone = "+1234567890";
        
        SharedLink savedLink = SharedLink.builder()
                .id(1L)
                .linkToken("generated-token")
                .file(testFile)
                .build();
        
        when(fileService.findById(fileId)).thenReturn(Optional.of(testFile));
        when(sharedLinkRepository.save(any(SharedLink.class))).thenReturn(savedLink);
        when(smsService.sendSMS(eq(recipientPhone), anyString())).thenReturn(SMSResponse.failure("SMS failed"));
        
        // Act
        SharedLink result = sharedLinkService.createFileShare(fileId, userId, recipientPhone);
        
        // Assert
        assertThat(result).isNotNull();
        verify(smsService).sendSMS(eq(recipientPhone), anyString());
    }
}