package com.cerebra.secure_file_sharing_app.Services;

import com.cerebra.secure_file_sharing_app.Entities.File;
import com.cerebra.secure_file_sharing_app.Entities.Folder;
import com.cerebra.secure_file_sharing_app.Entities.StoragePath;
import com.cerebra.secure_file_sharing_app.Exceptions.CustomExceptions.*;
import com.cerebra.secure_file_sharing_app.Repositories.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {
    
    private final FileRepository fileRepository;
    private final StoragePathService storagePathService;
    private final FolderService folderService;

    @Value("${app.storage.root-path:/app-storage}")
    private String rootStoragePath;

    @Override
    public File save(File file) {
        return fileRepository.save(file);
    }
    
    @Override
    public Optional<File> findById(Long id) {
        return fileRepository.findById(id);
    }
    
    @Override
    public List<File> findByStoragePathId(Long storagePathId) {
        return fileRepository.findByStoragePathId(storagePathId);
    }
    
    @Override
    public List<File> findByFolderId(Long folderId) {
        return fileRepository.findByFolderId(folderId);
    }

    @Override
    public List<File> findRootFiles(Long storagePathId) {
        return fileRepository.findByStoragePathIdAndFolderIsNull(storagePathId);
    }

    @Override
    public Optional<File> findByPhysicalName(String physicalName) {
        return fileRepository.findByPhysicalName(physicalName);
    }

    @Override
    public List<File> findAll() {
        return fileRepository.findAll();
    }

    @Override
    public void deleteById(Long id) {
        fileRepository.deleteById(id);
    }

    // Core File Operations
    @Override
    public File uploadFile(MultipartFile multipartFile, Long folderId, Long userId) {
        log.info("Uploading file for user: {}, folder: {}", userId, folderId);

        // Validate file
        validateFile(multipartFile);

        // Get user's storage path (this validates user has storage)
        StoragePath storagePath = getUserStoragePath(userId);

        // Handle folder logic
        Folder targetFolder;
        if (folderId != null) {
            // Use specified folder (with validation)
            targetFolder = validateFolderAccess(folderId, userId);
        } else {
            // Create or get default folder
            targetFolder = getOrCreateDefaultFolder(storagePath);
        }

        try {
            // Generate unique physical file name
            String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(multipartFile.getOriginalFilename()));
            String physicalFileName = generatePhysicalFileName(originalFilename);

            // Create full physical path
            Path userStorageDir = Paths.get(rootStoragePath, storagePath.getBasePath());
            Path targetLocation = userStorageDir.resolve(physicalFileName);

            // Create directories if they don't exist
            Files.createDirectories(userStorageDir);

            // Copy file to target location
            Files.copy(multipartFile.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Create File entity
            File fileEntity = File.builder()
                    .displayName(originalFilename)
                    .physicalName(physicalFileName)
                    .physicalPath(targetLocation.toString())
                    .size(multipartFile.getSize())
                    .mimeType(multipartFile.getContentType())
                    .storagePath(storagePath)
                    .folder(targetFolder)
                    .build();

            File savedFile = save(fileEntity);
            log.info("File uploaded successfully: {} -> {}", originalFilename, physicalFileName);

            return savedFile;

        } catch (IOException e) {
            log.error("Failed to store file: {}", e.getMessage(), e);
            throw new FileStorageException("Failed to store file: " + e.getMessage());
        }
    }

    @Override
    public Resource downloadFile(Long fileId, Long userId) {
        log.info("Downloading file: {} for user: {}", fileId, userId);

        // Find and validate file access
        File file = findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found: " + fileId));

        validateFileAccess(file, userId);

        try {
            Path filePath = Paths.get(file.getPhysicalPath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                log.info("File download successful: {}", file.getDisplayName());
                return resource;
            } else {
                log.error("File not found or not readable: {}", file.getPhysicalPath());
                throw new FileNotFoundException("File not found or not readable");
            }

        } catch (MalformedURLException e) {
            log.error("Malformed file path: {}", file.getPhysicalPath(), e);
            throw new FileStorageException("Invalid file path: " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(Long fileId, Long userId) {
        log.info("Deleting file: {} for user: {}", fileId, userId);

        // Find and validate file access
        File file = findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found: " + fileId));

        validateFileAccess(file, userId);

        try {
            // Delete physical file
            Path filePath = Paths.get(file.getPhysicalPath());
            Files.deleteIfExists(filePath);

            // Delete database record
            deleteById(fileId);

            log.info("File deleted successfully: {}", file.getDisplayName());

        } catch (IOException e) {
            log.error("Failed to delete physical file: {}", file.getPhysicalPath(), e);
            throw new FileStorageException("Failed to delete file: " + e.getMessage());
        }
    }

    @Override
    public List<File> getUserFiles(Long userId) {
        log.info("Getting all files for user: {}", userId);

        StoragePath storagePath = getUserStoragePath(userId);
        return findByStoragePathId(storagePath.getId());
    }

    @Override
    public List<File> getFolderFiles(Long folderId, Long userId) {
        log.info("Getting files for folder: {} and user: {}", folderId, userId);

        if (userId != null) {
            // Normal authenticated access with validation
            validateFolderAccess(folderId, userId);
        }

        return findByFolderId(folderId);
    }

    @Override
    public Resource downloadSharedFile(Long fileId) {
        log.info("Downloading shared file: {}", fileId);

        // Find file (no user validation for shared downloads)
        File file = findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found: " + fileId));

        try {
            Path filePath = Paths.get(file.getPhysicalPath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                log.info("Shared file download successful: {}", file.getDisplayName());
                return resource;
            } else {
                log.error("Shared file not found or not readable: {}", file.getPhysicalPath());
                throw new FileNotFoundException("File not found or not readable");
            }

        } catch (MalformedURLException e) {
            log.error("Malformed file path: {}", file.getPhysicalPath(), e);
            throw new FileStorageException("Invalid file path: " + e.getMessage());
        }
    }

    // Private Helper Methods
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileStorageException("Cannot store empty file");
        }

        String filename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        if (filename.contains("..")) {
            throw new FileStorageException("Invalid file path: " + filename);
        }

        // Add file size validation (e.g., max 50MB)
        long maxFileSize = 50 * 1024 * 1024; // 50MB
        if (file.getSize() > maxFileSize) {
            throw new FileStorageException("File size exceeds maximum allowed size");
        }
    }

    private String generatePhysicalFileName(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return UUID.randomUUID().toString() + extension;
    }

    private String getFileExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return "";
    }

    private StoragePath getUserStoragePath(Long userId) {
        return storagePathService.findByAppUserId(userId)
                .orElseThrow(() -> new FileStorageException("User storage path not found"));
    }

    private Folder validateFolderAccess(Long folderId, Long userId) {
        Optional<Folder> folderOpt = folderService.findById(folderId);
        if (folderOpt.isEmpty()) {
            throw new FileNotFoundException("Folder not found: " + folderId);
        }

        Folder folder = folderOpt.get();
        StoragePath userStoragePath = getUserStoragePath(userId);

        if (!folder.getStoragePath().getId().equals(userStoragePath.getId())) {
            throw new FileAccessDeniedException("Access denied to folder: " + folderId);
        }

        return folder;
    }

    private void validateFileAccess(File file, Long userId) {
        StoragePath userStoragePath = getUserStoragePath(userId);

        if (!file.getStoragePath().getId().equals(userStoragePath.getId())) {
            throw new FileAccessDeniedException("Access denied to file: " + file.getId());
        }
    }

    private Folder getOrCreateDefaultFolder(StoragePath storagePath) {
        // Try to find existing default folder
        List<Folder> rootFolders = folderService.findByStoragePathIdAndParentFolderIsNull(storagePath.getId());

        if (!rootFolders.isEmpty()) {
            return rootFolders.get(0);  // Use first root folder
        }

        // Create default folder if none exists
        return folderService.save(Folder.builder()
                .name("My Files")
                .storagePath(storagePath)
                .parentFolder(null)
                .build());
    }

}
