package com.cerebra.secure_file_sharing_app.Services;

import com.cerebra.secure_file_sharing_app.Entities.File;
import com.cerebra.secure_file_sharing_app.Entities.Folder;
import com.cerebra.secure_file_sharing_app.Repositories.FolderRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import com.cerebra.secure_file_sharing_app.Entities.StoragePath;
import com.cerebra.secure_file_sharing_app.Exceptions.CustomExceptions.*;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class FolderServiceImpl implements FolderService {
    
    private final FolderRepository folderRepository;
    private final StoragePathService storagePathService;


    @Override
    public Folder save(Folder folder) {
        return folderRepository.save(folder);
    }
    
    @Override
    public Optional<Folder> findById(Long id) {
        return folderRepository.findById(id);
    }
    
    @Override
    public List<Folder> findByStoragePathId(Long storagePathId) {
        return folderRepository.findByStoragePathId(storagePathId);
    }
    
    @Override
    public List<Folder> findByParentFolderId(Long parentFolderId) {
        return folderRepository.findByParentFolderId(parentFolderId);
    }
    
    @Override
    public List<Folder> findRootFolders(Long storagePathId) {
        return folderRepository.findByStoragePathIdAndParentFolderIsNull(storagePathId);
    }
    
    @Override
    public List<Folder> findAll() {
        return folderRepository.findAll();
    }
    
    @Override
    public void deleteById(Long id) {
        folderRepository.deleteById(id);
    }

    @Override
    public List<Folder> findByStoragePathIdAndParentFolderIsNull(Long id) {
        return folderRepository.findByStoragePathIdAndParentFolderIsNull(id);
    }

    @Override
    public Folder createFolder(String name, Long parentFolderId, Long userId) {
        log.info("Creating folder '{}' for user: {}, parent: {}", name, userId, parentFolderId);

        // Validate folder name
        validateFolderName(name);

        // Get user's storage path (validates user exists)
        StoragePath storagePath = getUserStoragePath(userId);

        // Validate parent folder access if specified
        Folder parentFolder = null;
        if (parentFolderId != null) {
            parentFolder = validateFolderAccess(parentFolderId, userId);
        }

        // Check for duplicate folder names in same location
        if (isDuplicateFolderName(name, parentFolderId, storagePath.getId())) {
            throw new InvalidFolderNameException("Folder with name '" + name + "' already exists in this location");
        }

        // Create folder
        Folder newFolder = Folder.builder()
                .name(name)
                .storagePath(storagePath)
                .parentFolder(parentFolder)
                .build();

        Folder savedFolder = save(newFolder);
        log.info("Folder created successfully: {} (ID: {})", name, savedFolder.getId());

        return savedFolder;
    }

    @Override
    public void deleteFolder(Long folderId, Long userId) {
        log.info("Deleting folder: {} for user: {}", folderId, userId);

        // Validate folder exists and user has access
        Folder folder = validateFolderAccess(folderId, userId);

        // Check for child folders (prevention approach)
        List<Folder> childFolders = findByParentFolderId(folderId);
        if (!childFolders.isEmpty()) {
            throw new FolderAccessDeniedException(
                    "Cannot delete folder '" + folder.getName() + "': contains " + childFolders.size() + " subfolder(s). Please delete subfolders first."
            );
        }

        // Check for files in folder (prevention approach)
        // You'll need to inject FileService or FileRepository for this
        List<File> files = folderRepository.findFilesByFolderId(folderId);
        if (!files.isEmpty()) {
            throw new FolderAccessDeniedException(
                    "Cannot delete folder '" + folder.getName() + "': contains " + files.size() + " file(s). Please move or delete files first."
            );
        }

        // Safe to delete
        deleteById(folderId);
        log.info("Folder deleted successfully: {}", folder.getName());
    }

    @Override
    public boolean hasAccess(Long folderId, Long userId) {
        try {
            validateFolderAccess(folderId, userId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isValidFolderName(String name) {
        try {
            validateFolderName(name);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    // Helper Methods
    private StoragePath getUserStoragePath(Long userId) {
        return storagePathService.findByAppUserId(userId)
                .orElseThrow(() -> new FolderAccessDeniedException("User storage path not found"));
    }

    private Folder validateFolderAccess(Long folderId, Long userId) {
        Optional<Folder> folderOpt = findById(folderId);
        if (folderOpt.isEmpty()) {
            throw new FolderNotFoundException("Folder not found: " + folderId);
        }

        Folder folder = folderOpt.get();
        StoragePath userStoragePath = getUserStoragePath(userId);

        if (!folder.getStoragePath().getId().equals(userStoragePath.getId())) {
            throw new FolderAccessDeniedException("Access denied to folder: " + folderId);
        }

        return folder;
    }

    private void validateFolderName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidFolderNameException("Folder name cannot be empty");
        }

        String trimmedName = name.trim();

        if (trimmedName.length() > 255) {
            throw new InvalidFolderNameException("Folder name too long (max 255 characters)");
        }

        // Check for invalid characters
        String invalidChars = "\\/:*?\"<>|";
        for (char c : invalidChars.toCharArray()) {
            if (trimmedName.indexOf(c) >= 0) {
                throw new InvalidFolderNameException("Folder name contains invalid character: " + c);
            }
        }

        // Check for reserved names
        String[] reservedNames = {"CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"};
        for (String reserved : reservedNames) {
            if (trimmedName.equalsIgnoreCase(reserved)) {
                throw new InvalidFolderNameException("Folder name is reserved: " + reserved);
            }
        }
    }

    private boolean isDuplicateFolderName(String name, Long parentFolderId, Long storagePathId) {
        List<Folder> existingFolders;

        if (parentFolderId != null) {
            existingFolders = findByParentFolderId(parentFolderId);
        } else {
            existingFolders = findRootFolders(storagePathId);
        }

        return existingFolders.stream()
                .anyMatch(folder -> folder.getName().equalsIgnoreCase(name.trim()));
    }

}
