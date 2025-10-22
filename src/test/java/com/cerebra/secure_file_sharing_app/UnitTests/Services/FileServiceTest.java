package com.cerebra.secure_file_sharing_app.UnitTests.Services;

import com.cerebra.secure_file_sharing_app.Entities.AppUser;
import com.cerebra.secure_file_sharing_app.Entities.File;
import com.cerebra.secure_file_sharing_app.Entities.Folder;
import com.cerebra.secure_file_sharing_app.Entities.StoragePath;
import com.cerebra.secure_file_sharing_app.Exceptions.CustomExceptions.*;
import com.cerebra.secure_file_sharing_app.Repositories.FileRepository;
import com.cerebra.secure_file_sharing_app.Services.FileServiceImpl;
import com.cerebra.secure_file_sharing_app.Services.FolderService;
import com.cerebra.secure_file_sharing_app.Services.StoragePathService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileService Tests")
class FileServiceTest {

    @Mock private FileRepository fileRepository;
    @Mock private StoragePathService storagePathService;
    @Mock private FolderService folderService;

    private FileServiceImpl fileService;

    @TempDir
    Path tempDir;

    private AppUser testUser;
    private StoragePath testStoragePath;
    private Folder testFolder;

    @BeforeEach
    void setUp() throws IOException {
        // Correct constructor order: FileRepository, StoragePathService, FolderService
        fileService = new FileServiceImpl(fileRepository, storagePathService, folderService);

        // Set up test storage directory
        Path userStorageDir = tempDir.resolve("user1");
        Files.createDirectories(userStorageDir);

        ReflectionTestUtils.setField(fileService, "rootStoragePath", tempDir.toString());

        // Set up test entities
        testUser = AppUser.builder()
                .id(1L)
                .phoneNumber("+1234567890")
                .build();

        testStoragePath = StoragePath.builder()
                .id(1L)
                .basePath("user1") // Relative path from root
                .appUser(testUser)
                .build();

        testFolder = Folder.builder()
                .id(1L)
                .name("Test Folder")
                .storagePath(testStoragePath)
                .parentFolder(null)
                .build();
    }

    @Test
    @DisplayName("Should upload file successfully with valid parameters")
    void uploadFile_validFile_savesFileAndReturnsEntity() throws IOException {
        // Arrange
        MultipartFile mockFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Test file content".getBytes()
        );
        Long folderId = 1L;
        Long userId = 1L;

        File savedFile = File.builder()
                .id(1L)
                .displayName("test.txt")
                .physicalName("generated-uuid.txt")
                .size(17L)
                .mimeType("text/plain")
                .storagePath(testStoragePath)
                .folder(testFolder)
                .build();

        when(storagePathService.findByAppUserId(userId)).thenReturn(Optional.of(testStoragePath));
        when(folderService.findById(folderId)).thenReturn(Optional.of(testFolder));
        when(fileRepository.save(any(File.class))).thenReturn(savedFile);

        // Act
        File result = fileService.uploadFile(mockFile, folderId, userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDisplayName()).isEqualTo("test.txt");
        assertThat(result.getMimeType()).isEqualTo("text/plain");
        assertThat(result.getSize()).isEqualTo(17L);
        assertThat(result.getFolder()).isEqualTo(testFolder);

        // Allow multiple calls since service calls it multiple times for validation
        verify(storagePathService, atLeast(1)).findByAppUserId(userId);
        verify(folderService).findById(folderId);
        verify(fileRepository).save(any(File.class));
    }

    @Test
    @DisplayName("Should upload file to default folder when no folder specified")
    void uploadFile_noFolderSpecified_uploadsToDefaultFolder() throws IOException {
        // Arrange
        MultipartFile mockFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Test file content".getBytes()
        );
        Long userId = 1L;

        Folder defaultFolder = Folder.builder()
                .id(2L)
                .name("My Files")
                .storagePath(testStoragePath)
                .parentFolder(null)
                .build();

        File savedFile = File.builder()
                .id(1L)
                .displayName("test.txt")
                .folder(defaultFolder)
                .storagePath(testStoragePath)
                .build();

        when(storagePathService.findByAppUserId(userId)).thenReturn(Optional.of(testStoragePath));
        when(folderService.findByStoragePathIdAndParentFolderIsNull(testStoragePath.getId()))
                .thenReturn(Arrays.asList(defaultFolder));
        when(fileRepository.save(any(File.class))).thenReturn(savedFile);

        // Act
        File result = fileService.uploadFile(mockFile, null, userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFolder()).isEqualTo(defaultFolder);

        verify(folderService).findByStoragePathIdAndParentFolderIsNull(testStoragePath.getId());
        verify(fileRepository).save(any(File.class));
    }

    @Test
    @DisplayName("Should throw exception when user has no storage path")
    void uploadFile_userWithoutStoragePath_throwsException() {
        // Arrange
        MultipartFile mockFile = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
        Long userId = 1L;

        when(storagePathService.findByAppUserId(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> fileService.uploadFile(mockFile, null, userId))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("User storage path not found");

        verify(storagePathService).findByAppUserId(userId);
        verify(folderService, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Should throw exception when folder not found")
    void uploadFile_folderNotFound_throwsFileNotFoundException() {
        // Arrange
        MultipartFile mockFile = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
        Long userId = 1L;
        Long folderId = 999L;

        when(storagePathService.findByAppUserId(userId)).thenReturn(Optional.of(testStoragePath));
        when(folderService.findById(folderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> fileService.uploadFile(mockFile, folderId, userId))
                .isInstanceOf(FileNotFoundException.class)
                .hasMessageContaining("Folder not found");

        verify(folderService).findById(folderId);
    }

    @Test
    @DisplayName("Should throw exception when user has no access to folder")
    void uploadFile_noFolderAccess_throwsFileAccessDeniedException() {
        // Arrange
        MultipartFile mockFile = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
        Long userId = 1L;
        Long folderId = 1L;

        // Different user's storage path
        StoragePath otherStoragePath = StoragePath.builder()
                .id(2L)
                .basePath("user2")
                .build();

        Folder otherFolder = Folder.builder()
                .id(1L)
                .storagePath(otherStoragePath)
                .build();

        when(storagePathService.findByAppUserId(userId)).thenReturn(Optional.of(testStoragePath));
        when(folderService.findById(folderId)).thenReturn(Optional.of(otherFolder));

        // Act & Assert
        assertThatThrownBy(() -> fileService.uploadFile(mockFile, folderId, userId))
                .isInstanceOf(FileAccessDeniedException.class)
                .hasMessageContaining("Access denied to folder");

        verify(folderService).findById(folderId);
    }

    @Test
    @DisplayName("Should throw exception for empty file")
    void uploadFile_emptyFile_throwsFileStorageException() {
        // Arrange
        MultipartFile mockFile = new MockMultipartFile("file", "test.txt", "text/plain", new byte[0]);
        Long userId = 1L;

        // Act & Assert
        assertThatThrownBy(() -> fileService.uploadFile(mockFile, null, userId))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("Cannot store empty file");

        // Validation happens before storage path lookup, so no verification needed
    }

    @Test
    @DisplayName("Should throw exception for file with dangerous filename")
    void uploadFile_dangerousFilename_throwsFileStorageException() {
        // Arrange
        MultipartFile mockFile = new MockMultipartFile(
                "file",
                "../../../etc/passwd",
                "text/plain",
                "malicious content".getBytes()
        );
        Long userId = 1L;

        // Act & Assert
        assertThatThrownBy(() -> fileService.uploadFile(mockFile, null, userId))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("Invalid file path");

        // Validation happens before storage path lookup, so no verification needed
    }

    @Test
    @DisplayName("Should download file successfully for authorized user")
    void downloadFile_validFileAndUser_returnsResource() throws IOException {
        // Arrange
        Long fileId = 1L;
        Long userId = 1L;

        // Create actual test file
        Path testFilePath = tempDir.resolve("user1").resolve("test.txt");
        Files.createDirectories(testFilePath.getParent());
        Files.write(testFilePath, "Test file content".getBytes());

        File testFile = File.builder()
                .id(fileId)
                .displayName("test.txt")
                .physicalPath(testFilePath.toString())
                .storagePath(testStoragePath)
                .build();

        when(fileRepository.findById(fileId)).thenReturn(Optional.of(testFile));
        when(storagePathService.findByAppUserId(userId)).thenReturn(Optional.of(testStoragePath));

        // Act
        Resource result = fileService.downloadFile(fileId, userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.exists()).isTrue();
        assertThat(result.isReadable()).isTrue();

        verify(fileRepository).findById(fileId);
        verify(storagePathService).findByAppUserId(userId);
    }

    @Test
    @DisplayName("Should throw exception when downloading non-existent file")
    void downloadFile_fileNotFound_throwsFileNotFoundException() {
        // Arrange
        Long fileId = 999L;
        Long userId = 1L;

        when(fileRepository.findById(fileId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> fileService.downloadFile(fileId, userId))
                .isInstanceOf(FileNotFoundException.class)
                .hasMessageContaining("File not found");

        verify(fileRepository).findById(fileId);
    }

    @Test
    @DisplayName("Should throw exception when user has no access to file")
    void downloadFile_noFileAccess_throwsFileAccessDeniedException() {
        // Arrange
        Long fileId = 1L;
        Long userId = 1L;

        // Different user's storage path
        StoragePath otherStoragePath = StoragePath.builder()
                .id(2L)
                .basePath("user2")
                .build();

        File otherFile = File.builder()
                .id(fileId)
                .displayName("test.txt")
                .storagePath(otherStoragePath)
                .build();

        when(fileRepository.findById(fileId)).thenReturn(Optional.of(otherFile));
        when(storagePathService.findByAppUserId(userId)).thenReturn(Optional.of(testStoragePath));

        // Act & Assert
        assertThatThrownBy(() -> fileService.downloadFile(fileId, userId))
                .isInstanceOf(FileAccessDeniedException.class)
                .hasMessageContaining("Access denied to file");

        verify(fileRepository).findById(fileId);
        verify(storagePathService).findByAppUserId(userId);
    }

    @Test
    @DisplayName("Should download shared file without user validation")
    void downloadSharedFile_validFile_returnsResourceWithoutUserValidation() throws IOException {
        // Arrange
        Long fileId = 1L;

        // Create actual test file
        Path testFilePath = tempDir.resolve("user1").resolve("shared.txt");
        Files.createDirectories(testFilePath.getParent());
        Files.write(testFilePath, "Shared file content".getBytes());

        File testFile = File.builder()
                .id(fileId)
                .displayName("shared.txt")
                .physicalPath(testFilePath.toString())
                .build();

        when(fileRepository.findById(fileId)).thenReturn(Optional.of(testFile));

        // Act
        Resource result = fileService.downloadSharedFile(fileId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.exists()).isTrue();
        assertThat(result.isReadable()).isTrue();

        verify(fileRepository).findById(fileId);
        verify(storagePathService, never()).findByAppUserId(anyLong()); // No user validation
    }

    @Test
    @DisplayName("Should delete file successfully for authorized user")
    void deleteFile_validFileAndUser_deletesFileAndRecord() throws IOException {
        // Arrange
        Long fileId = 1L;
        Long userId = 1L;

        // Create actual test file
        Path testFilePath = tempDir.resolve("user1").resolve("delete-me.txt");
        Files.createDirectories(testFilePath.getParent());
        Files.write(testFilePath, "File to delete".getBytes());

        File testFile = File.builder()
                .id(fileId)
                .displayName("delete-me.txt")
                .physicalPath(testFilePath.toString())
                .storagePath(testStoragePath)
                .build();

        when(fileRepository.findById(fileId)).thenReturn(Optional.of(testFile));
        when(storagePathService.findByAppUserId(userId)).thenReturn(Optional.of(testStoragePath));

        // Act
        fileService.deleteFile(fileId, userId);

        // Assert
        assertThat(Files.exists(testFilePath)).isFalse(); // Physical file deleted

        verify(fileRepository).findById(fileId);
        verify(storagePathService).findByAppUserId(userId);
        verify(fileRepository).deleteById(fileId); // Database record deleted
    }

    @Test
    @DisplayName("Should get user files successfully")
    void getUserFiles_validUser_returnsUserFiles() {
        // Arrange
        Long userId = 1L;

        List<File> userFiles = Arrays.asList(
                File.builder().id(1L).displayName("file1.txt").build(),
                File.builder().id(2L).displayName("file2.txt").build()
        );

        when(storagePathService.findByAppUserId(userId)).thenReturn(Optional.of(testStoragePath));
        when(fileRepository.findByStoragePathId(testStoragePath.getId())).thenReturn(userFiles);

        // Act
        List<File> result = fileService.getUserFiles(userId);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(File::getDisplayName).contains("file1.txt", "file2.txt");

        verify(storagePathService).findByAppUserId(userId);
        verify(fileRepository).findByStoragePathId(testStoragePath.getId());
    }

    @Test
    @DisplayName("Should get folder files successfully")
    void getFolderFiles_validFolder_returnsFolderFiles() {
        // Arrange
        Long folderId = 1L;
        Long userId = 1L;

        List<File> folderFiles = Arrays.asList(
                File.builder().id(1L).displayName("folder-file1.txt").build(),
                File.builder().id(2L).displayName("folder-file2.txt").build()
        );

        // Mock folder validation
        when(folderService.findById(folderId)).thenReturn(Optional.of(testFolder));
        when(storagePathService.findByAppUserId(userId)).thenReturn(Optional.of(testStoragePath));
        when(fileRepository.findByFolderId(folderId)).thenReturn(folderFiles);

        // Act
        List<File> result = fileService.getFolderFiles(folderId, userId);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(File::getDisplayName).contains("folder-file1.txt", "folder-file2.txt");

        verify(folderService).findById(folderId);
        verify(storagePathService).findByAppUserId(userId);
        verify(fileRepository).findByFolderId(folderId);
    }

    @Test
    @DisplayName("Should handle large file uploads within size limits")
    void uploadFile_largeFileWithinLimits_uploadsSuccessfully() throws IOException {
        // Arrange
        byte[] largeContent = new byte[1024 * 1024]; // 1MB
        Arrays.fill(largeContent, (byte) 'A');

        MultipartFile mockFile = new MockMultipartFile(
                "file",
                "large-file.txt",
                "text/plain",
                largeContent
        );
        Long userId = 1L;

        File savedFile = File.builder()
                .id(1L)
                .displayName("large-file.txt")
                .size((long) largeContent.length)
                .folder(testFolder)
                .build();

        when(storagePathService.findByAppUserId(userId)).thenReturn(Optional.of(testStoragePath));
        when(folderService.findByStoragePathIdAndParentFolderIsNull(testStoragePath.getId()))
                .thenReturn(Arrays.asList(testFolder));
        when(fileRepository.save(any(File.class))).thenReturn(savedFile);

        // Act
        File result = fileService.uploadFile(mockFile, null, userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSize()).isEqualTo(1024 * 1024);
        assertThat(result.getDisplayName()).isEqualTo("large-file.txt");
    }

    @Test
    @DisplayName("Should reject file that exceeds size limit")
    void uploadFile_fileTooLarge_throwsFileStorageException() {
        // Arrange
        byte[] tooLargeContent = new byte[51 * 1024 * 1024]; // 51MB - exceeds 50MB limit

        MultipartFile mockFile = new MockMultipartFile(
                "file",
                "too-large.txt",
                "text/plain",
                tooLargeContent
        );
        Long userId = 1L;

        // Don't stub storagePathService - validation happens before that call

        // Act & Assert
        assertThatThrownBy(() -> fileService.uploadFile(mockFile, null, userId))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("File size exceeds maximum allowed size");

        // No verification needed - validation happens before service calls
    }
}