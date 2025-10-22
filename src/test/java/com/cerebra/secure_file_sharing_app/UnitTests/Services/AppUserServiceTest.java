package com.cerebra.secure_file_sharing_app.UnitTests.Services;

import com.cerebra.secure_file_sharing_app.Entities.AppUser;
import com.cerebra.secure_file_sharing_app.Repositories.AppUserRepository;
import com.cerebra.secure_file_sharing_app.Services.AppUserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppUserService Tests")
class AppUserServiceTest {

    @Mock private AppUserRepository appUserRepository;

    private AppUserServiceImpl appUserService;

    private AppUser testUser;

    @BeforeEach
    void setUp() {
        appUserService = new AppUserServiceImpl(appUserRepository);

        testUser = AppUser.builder()
                .id(1L)
                .phoneNumber("+1234567890")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should save user successfully")
    void save_validUser_returnsUser() {
        // Arrange
        AppUser userToSave = AppUser.builder()
                .phoneNumber("+1234567890")
                .build();

        AppUser savedUser = AppUser.builder()
                .id(1L)
                .phoneNumber("+1234567890")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(appUserRepository.save(userToSave)).thenReturn(savedUser);

        // Act
        AppUser result = appUserService.save(userToSave);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getPhoneNumber()).isEqualTo("+1234567890");
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();

        verify(appUserRepository).save(userToSave);
    }

    @Test
    @DisplayName("Should save user with null fields")
    void save_userWithNullFields_savesSuccessfully() {
        // Arrange
        AppUser userToSave = AppUser.builder()
                .phoneNumber("+1234567890")
                .createdAt(null)
                .updatedAt(null)
                .build();

        when(appUserRepository.save(userToSave)).thenReturn(userToSave);

        // Act
        AppUser result = appUserService.save(userToSave);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPhoneNumber()).isEqualTo("+1234567890");

        verify(appUserRepository).save(userToSave);
    }

    @Test
    @DisplayName("Should find user by phone number successfully")
    void findByPhoneNumber_existingUser_returnsUser() {
        // Arrange
        String phoneNumber = "+1234567890";

        when(appUserRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(testUser));

        // Act
        Optional<AppUser> result = appUserService.findByPhoneNumber(phoneNumber);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUser);
        assertThat(result.get().getPhoneNumber()).isEqualTo(phoneNumber);

        verify(appUserRepository).findByPhoneNumber(phoneNumber);
    }

    @Test
    @DisplayName("Should return empty when phone number does not exist")
    void findByPhoneNumber_nonExistentUser_returnsEmpty() {
        // Arrange
        String phoneNumber = "+9999999999";

        when(appUserRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.empty());

        // Act
        Optional<AppUser> result = appUserService.findByPhoneNumber(phoneNumber);

        // Assert
        assertThat(result).isEmpty();

        verify(appUserRepository).findByPhoneNumber(phoneNumber);
    }

    @Test
    @DisplayName("Should handle null phone number in findByPhoneNumber")
    void findByPhoneNumber_nullPhoneNumber_returnsEmpty() {
        // Arrange
        when(appUserRepository.findByPhoneNumber(null)).thenReturn(Optional.empty());

        // Act
        Optional<AppUser> result = appUserService.findByPhoneNumber(null);

        // Assert
        assertThat(result).isEmpty();

        verify(appUserRepository).findByPhoneNumber(null);
    }

    @Test
    @DisplayName("Should find user by ID successfully")
    void findById_existingUser_returnsUser() {
        // Arrange
        Long userId = 1L;

        when(appUserRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Act
        Optional<AppUser> result = appUserService.findById(userId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUser);
        assertThat(result.get().getId()).isEqualTo(userId);

        verify(appUserRepository).findById(userId);
    }

    @Test
    @DisplayName("Should return empty when user ID does not exist")
    void findById_nonExistentUser_returnsEmpty() {
        // Arrange
        Long userId = 999L;

        when(appUserRepository.findById(userId)).thenReturn(Optional.empty());

        // Act
        Optional<AppUser> result = appUserService.findById(userId);

        // Assert
        assertThat(result).isEmpty();

        verify(appUserRepository).findById(userId);
    }

    @Test
    @DisplayName("Should handle null ID in findById")
    void findById_nullId_returnsEmpty() {
        // Arrange
        when(appUserRepository.findById(null)).thenReturn(Optional.empty());

        // Act
        Optional<AppUser> result = appUserService.findById(null);

        // Assert
        assertThat(result).isEmpty();

        verify(appUserRepository).findById(null);
    }

    @Test
    @DisplayName("Should return all users")
    void findAll_usersExist_returnsAllUsers() {
        // Arrange
        AppUser user2 = AppUser.builder()
                .id(2L)
                .phoneNumber("+0987654321")
                .build();

        List<AppUser> users = Arrays.asList(testUser, user2);

        when(appUserRepository.findAll()).thenReturn(users);

        // Act
        List<AppUser> result = appUserService.findAll();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).contains(testUser, user2);
        assertThat(result).extracting(AppUser::getPhoneNumber)
                .contains("+1234567890", "+0987654321");

        verify(appUserRepository).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no users exist")
    void findAll_noUsers_returnsEmptyList() {
        // Arrange
        when(appUserRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<AppUser> result = appUserService.findAll();

        // Assert
        assertThat(result).isEmpty();

        verify(appUserRepository).findAll();
    }

    @Test
    @DisplayName("Should delete user by ID successfully")
    void deleteById_existingUser_deletesUser() {
        // Arrange
        Long userId = 1L;

        // Act
        appUserService.deleteById(userId);

        // Assert
        verify(appUserRepository).deleteById(userId);
    }

    @Test
    @DisplayName("Should handle null ID in deleteById")
    void deleteById_nullId_callsRepository() {
        // Act
        appUserService.deleteById(null);

        // Assert
        verify(appUserRepository).deleteById(null);
    }

    @Test
    @DisplayName("Should check if user exists by phone number")
    void existsByPhoneNumber_existingUser_returnsTrue() {
        // Arrange
        String phoneNumber = "+1234567890";

        when(appUserRepository.existsByPhoneNumber(phoneNumber)).thenReturn(true);

        // Act
        boolean result = appUserService.existsByPhoneNumber(phoneNumber);

        // Assert
        assertThat(result).isTrue();

        verify(appUserRepository).existsByPhoneNumber(phoneNumber);
    }

    @Test
    @DisplayName("Should check if user exists by phone number returns false")
    void existsByPhoneNumber_nonExistentUser_returnsFalse() {
        // Arrange
        String phoneNumber = "+9999999999";

        when(appUserRepository.existsByPhoneNumber(phoneNumber)).thenReturn(false);

        // Act
        boolean result = appUserService.existsByPhoneNumber(phoneNumber);

        // Assert
        assertThat(result).isFalse();

        verify(appUserRepository).existsByPhoneNumber(phoneNumber);
    }

    @Test
    @DisplayName("Should handle null phone number in existsByPhoneNumber")
    void existsByPhoneNumber_nullPhoneNumber_returnsFalse() {
        // Arrange
        when(appUserRepository.existsByPhoneNumber(null)).thenReturn(false);

        // Act
        boolean result = appUserService.existsByPhoneNumber(null);

        // Assert
        assertThat(result).isFalse();

        verify(appUserRepository).existsByPhoneNumber(null);
    }

    @Test
    @DisplayName("Should handle empty phone number in existsByPhoneNumber")
    void existsByPhoneNumber_emptyPhoneNumber_returnsFalse() {
        // Arrange
        String emptyPhoneNumber = "";

        when(appUserRepository.existsByPhoneNumber(emptyPhoneNumber)).thenReturn(false);

        // Act
        boolean result = appUserService.existsByPhoneNumber(emptyPhoneNumber);

        // Assert
        assertThat(result).isFalse();

        verify(appUserRepository).existsByPhoneNumber(emptyPhoneNumber);
    }

    @Test
    @DisplayName("Should save user and update timestamps correctly")
    void save_userWithTimestamps_preservesTimestamps() {
        // Arrange
        LocalDateTime createdTime = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedTime = LocalDateTime.now();

        AppUser userToSave = AppUser.builder()
                .id(1L)
                .phoneNumber("+1234567890")
                .createdAt(createdTime)
                .updatedAt(updatedTime)
                .build();

        when(appUserRepository.save(userToSave)).thenReturn(userToSave);

        // Act
        AppUser result = appUserService.save(userToSave);

        // Assert
        assertThat(result.getCreatedAt()).isEqualTo(createdTime);
        assertThat(result.getUpdatedAt()).isEqualTo(updatedTime);

        verify(appUserRepository).save(userToSave);
    }

    @Test
    @DisplayName("Should handle repository exceptions during save")
    void save_repositoryException_propagatesException() {
        // Arrange
        AppUser userToSave = AppUser.builder()
                .phoneNumber("+1234567890")
                .build();

        RuntimeException repositoryException = new RuntimeException("Database error");
        when(appUserRepository.save(userToSave)).thenThrow(repositoryException);

        // Act & Assert
        try {
            appUserService.save(userToSave);
        } catch (RuntimeException e) {
            assertThat(e).isSameAs(repositoryException);
        }

        verify(appUserRepository).save(userToSave);
    }

    @Test
    @DisplayName("Should pass through repository response for findByPhoneNumber")
    void findByPhoneNumber_repositoryResponse_passesThrough() {
        // Arrange
        String phoneNumber = "+1234567890";
        Optional<AppUser> repositoryResponse = Optional.of(testUser);

        when(appUserRepository.findByPhoneNumber(phoneNumber)).thenReturn(repositoryResponse);

        // Act
        Optional<AppUser> result = appUserService.findByPhoneNumber(phoneNumber);

        // Assert
        assertThat(result).isSameAs(repositoryResponse);

        verify(appUserRepository).findByPhoneNumber(phoneNumber);
    }

    @Test
    @DisplayName("Should handle different phone number formats")
    void findByPhoneNumber_differentFormats_callsRepository() {
        // Arrange
        String[] phoneNumbers = {
                "+1234567890",
                "+12345678901",
                "+123456789012",
                "1234567890",
                "(123) 456-7890"
        };

        when(appUserRepository.findByPhoneNumber(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        for (String phoneNumber : phoneNumbers) {
            Optional<AppUser> result = appUserService.findByPhoneNumber(phoneNumber);
            assertThat(result).isEmpty();
            verify(appUserRepository).findByPhoneNumber(phoneNumber);
        }
    }
}