package com.cerebra.secure_file_sharing_app.UnitTests.Services;

import com.cerebra.secure_file_sharing_app.Entities.AppUser;
import com.cerebra.secure_file_sharing_app.Entities.StoragePath;
import com.cerebra.secure_file_sharing_app.Repositories.StoragePathRepository;
import com.cerebra.secure_file_sharing_app.Services.StoragePathServiceImpl;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoragePathService Tests")
class StoragePathServiceTest {

    @Mock private StoragePathRepository storagePathRepository;

    private StoragePathServiceImpl storagePathService;

    private StoragePath testStoragePath;
    private AppUser testUser;

    @BeforeEach
    void setUp() {
        storagePathService = new StoragePathServiceImpl(storagePathRepository);

        testUser = AppUser.builder()
                .id(1L)
                .phoneNumber("+1234567890")
                .build();

        testStoragePath = StoragePath.builder()
                .id(1L)
                .basePath("/users/user1")
                .appUser(testUser)
                .build();
    }

    @Test
    @DisplayName("Should save storage path successfully")
    void save_validStoragePath_returnsStoragePath() {
        // Arrange
        StoragePath storagePathToSave = StoragePath.builder()
                .basePath("/users/user1")
                .appUser(testUser)
                .build();

        StoragePath savedStoragePath = StoragePath.builder()
                .id(1L)
                .basePath("/users/user1")
                .appUser(testUser)
                .build();

        when(storagePathRepository.save(storagePathToSave)).thenReturn(savedStoragePath);

        // Act
        StoragePath result = storagePathService.save(storagePathToSave);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getBasePath()).isEqualTo("/users/user1");
        assertThat(result.getAppUser()).isEqualTo(testUser);

        verify(storagePathRepository).save(storagePathToSave);
    }

    @Test
    @DisplayName("Should save storage path with null app user")
    void save_storagePathWithNullAppUser_savesSuccessfully() {
        // Arrange
        StoragePath storagePathToSave = StoragePath.builder()
                .basePath("/users/user1")
                .appUser(null)
                .build();

        when(storagePathRepository.save(storagePathToSave)).thenReturn(storagePathToSave);

        // Act
        StoragePath result = storagePathService.save(storagePathToSave);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getBasePath()).isEqualTo("/users/user1");
        assertThat(result.getAppUser()).isNull();

        verify(storagePathRepository).save(storagePathToSave);
    }

    @Test
    @DisplayName("Should find storage path by ID successfully")
    void findById_existingStoragePath_returnsStoragePath() {
        // Arrange
        Long storagePathId = 1L;

        when(storagePathRepository.findById(storagePathId)).thenReturn(Optional.of(testStoragePath));

        // Act
        Optional<StoragePath> result = storagePathService.findById(storagePathId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testStoragePath);
        assertThat(result.get().getId()).isEqualTo(storagePathId);
        assertThat(result.get().getBasePath()).isEqualTo("/users/user1");

        verify(storagePathRepository).findById(storagePathId);
    }

    @Test
    @DisplayName("Should return empty when storage path ID does not exist")
    void findById_nonExistentStoragePath_returnsEmpty() {
        // Arrange
        Long storagePathId = 999L;

        when(storagePathRepository.findById(storagePathId)).thenReturn(Optional.empty());

        // Act
        Optional<StoragePath> result = storagePathService.findById(storagePathId);

        // Assert
        assertThat(result).isEmpty();

        verify(storagePathRepository).findById(storagePathId);
    }

    @Test
    @DisplayName("Should handle null ID in findById")
    void findById_nullId_returnsEmpty() {
        // Arrange
        when(storagePathRepository.findById(null)).thenReturn(Optional.empty());

        // Act
        Optional<StoragePath> result = storagePathService.findById(null);

        // Assert
        assertThat(result).isEmpty();

        verify(storagePathRepository).findById(null);
    }

    @Test
    @DisplayName("Should find storage path by app user ID successfully")
    void findByAppUserId_existingUser_returnsStoragePath() {
        // Arrange
        Long appUserId = 1L;

        when(storagePathRepository.findByAppUserId(appUserId)).thenReturn(Optional.of(testStoragePath));

        // Act
        Optional<StoragePath> result = storagePathService.findByAppUserId(appUserId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testStoragePath);
        assertThat(result.get().getAppUser()).isEqualTo(testUser);

        verify(storagePathRepository).findByAppUserId(appUserId);
    }

    @Test
    @DisplayName("Should return empty when app user ID does not exist")
    void findByAppUserId_nonExistentUser_returnsEmpty() {
        // Arrange
        Long appUserId = 999L;

        when(storagePathRepository.findByAppUserId(appUserId)).thenReturn(Optional.empty());

        // Act
        Optional<StoragePath> result = storagePathService.findByAppUserId(appUserId);

        // Assert
        assertThat(result).isEmpty();

        verify(storagePathRepository).findByAppUserId(appUserId);
    }

    @Test
    @DisplayName("Should handle null app user ID in findByAppUserId")
    void findByAppUserId_nullAppUserId_returnsEmpty() {
        // Arrange
        when(storagePathRepository.findByAppUserId(null)).thenReturn(Optional.empty());

        // Act
        Optional<StoragePath> result = storagePathService.findByAppUserId(null);

        // Assert
        assertThat(result).isEmpty();

        verify(storagePathRepository).findByAppUserId(null);
    }

    @Test
    @DisplayName("Should return all storage paths")
    void findAll_storagePathsExist_returnsAllStoragePaths() {
        // Arrange
        AppUser user2 = AppUser.builder()
                .id(2L)
                .phoneNumber("+0987654321")
                .build();

        StoragePath storagePath2 = StoragePath.builder()
                .id(2L)
                .basePath("/users/user2")
                .appUser(user2)
                .build();

        List<StoragePath> storagePaths = Arrays.asList(testStoragePath, storagePath2);

        when(storagePathRepository.findAll()).thenReturn(storagePaths);

        // Act
        List<StoragePath> result = storagePathService.findAll();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).contains(testStoragePath, storagePath2);
        assertThat(result).extracting(StoragePath::getBasePath)
                .contains("/users/user1", "/users/user2");

        verify(storagePathRepository).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no storage paths exist")
    void findAll_noStoragePaths_returnsEmptyList() {
        // Arrange
        when(storagePathRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<StoragePath> result = storagePathService.findAll();

        // Assert
        assertThat(result).isEmpty();

        verify(storagePathRepository).findAll();
    }

    @Test
    @DisplayName("Should delete storage path by ID successfully")
    void deleteById_existingStoragePath_deletesStoragePath() {
        // Arrange
        Long storagePathId = 1L;

        // Act
        storagePathService.deleteById(storagePathId);

        // Assert
        verify(storagePathRepository).deleteById(storagePathId);
    }

    @Test
    @DisplayName("Should handle null ID in deleteById")
    void deleteById_nullId_callsRepository() {
        // Act
        storagePathService.deleteById(null);

        // Assert
        verify(storagePathRepository).deleteById(null);
    }

    @Test
    @DisplayName("Should handle repository exceptions during save")
    void save_repositoryException_propagatesException() {
        // Arrange
        StoragePath storagePathToSave = StoragePath.builder()
                .basePath("/users/user1")
                .appUser(testUser)
                .build();

        RuntimeException repositoryException = new RuntimeException("Database error");
        when(storagePathRepository.save(storagePathToSave)).thenThrow(repositoryException);

        // Act & Assert
        try {
            storagePathService.save(storagePathToSave);
        } catch (RuntimeException e) {
            assertThat(e).isSameAs(repositoryException);
        }

        verify(storagePathRepository).save(storagePathToSave);
    }

    @Test
    @DisplayName("Should pass through repository response for findByAppUserId")
    void findByAppUserId_repositoryResponse_passesThrough() {
        // Arrange
        Long appUserId = 1L;
        Optional<StoragePath> repositoryResponse = Optional.of(testStoragePath);

        when(storagePathRepository.findByAppUserId(appUserId)).thenReturn(repositoryResponse);

        // Act
        Optional<StoragePath> result = storagePathService.findByAppUserId(appUserId);

        // Assert
        assertThat(result).isSameAs(repositoryResponse);

        verify(storagePathRepository).findByAppUserId(appUserId);
    }

    @Test
    @DisplayName("Should handle different base path formats")
    void save_differentBasePathFormats_savesSuccessfully() {
        // Arrange
        String[] basePaths = {
                "/users/user1",
                "/storage/app-users/1",
                "/app-storage/users/user1/files",
                "users/user1"
        };

        when(storagePathRepository.save(any(StoragePath.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act & Assert
        for (String basePath : basePaths) {
            StoragePath storagePathToSave = StoragePath.builder()
                    .basePath(basePath)
                    .appUser(testUser)
                    .build();

            StoragePath result = storagePathService.save(storagePathToSave);
            assertThat(result.getBasePath()).isEqualTo(basePath);
            verify(storagePathRepository).save(storagePathToSave);
        }
    }

    @Test
    @DisplayName("Should find storage path for users with different IDs")
    void findByAppUserId_differentUserIds_callsRepository() {
        // Arrange
        Long[] userIds = {1L, 2L, 100L, 999L};

        when(storagePathRepository.findByAppUserId(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        for (Long userId : userIds) {
            Optional<StoragePath> result = storagePathService.findByAppUserId(userId);
            assertThat(result).isEmpty();
            verify(storagePathRepository).findByAppUserId(userId);
        }
    }

    @Test
    @DisplayName("Should handle save with minimal required fields")
    void save_minimalStoragePath_savesSuccessfully() {
        // Arrange
        StoragePath minimalStoragePath = StoragePath.builder()
                .basePath("/minimal/path")
                .build();

        when(storagePathRepository.save(minimalStoragePath)).thenReturn(minimalStoragePath);

        // Act
        StoragePath result = storagePathService.save(minimalStoragePath);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getBasePath()).isEqualTo("/minimal/path");

        verify(storagePathRepository).save(minimalStoragePath);
    }
}