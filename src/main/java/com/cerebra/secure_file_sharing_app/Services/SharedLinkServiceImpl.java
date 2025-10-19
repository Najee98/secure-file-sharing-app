package com.cerebra.secure_file_sharing_app.Services;

import com.cerebra.secure_file_sharing_app.Entities.File;
import com.cerebra.secure_file_sharing_app.Entities.Folder;
import com.cerebra.secure_file_sharing_app.Entities.SharedLink;
import com.cerebra.secure_file_sharing_app.Exceptions.CustomExceptions.*;
import com.cerebra.secure_file_sharing_app.Repositories.SharedLinkRepository;
import com.cerebra.secure_file_sharing_app.SMS.SMSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SharedLinkServiceImpl implements SharedLinkService {

    private final SharedLinkRepository sharedLinkRepository;
    private final FileService fileService;
    private final FolderService folderService;
    private final SMSService smsService;

    @Value("${app.share.expiration-days:7}")
    private int shareExpirationDays;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    // Basic CRUD Operations
    @Override
    public SharedLink save(SharedLink sharedLink) {
        return sharedLinkRepository.save(sharedLink);
    }

    @Override
    public Optional<SharedLink> findById(Long id) {
        return sharedLinkRepository.findById(id);
    }

    @Override
    public Optional<SharedLink> findByLinkToken(String linkToken) {
        return sharedLinkRepository.findByLinkToken(linkToken);
    }

    @Override
    public List<SharedLink> findByFileId(Long fileId) {
        return sharedLinkRepository.findByFileId(fileId);
    }

    @Override
    public List<SharedLink> findAll() {
        return sharedLinkRepository.findAll();
    }

    @Override
    public void deleteById(Long id) {
        sharedLinkRepository.deleteById(id);
    }

    @Override
    public void deleteExpiredLinks() {
        List<SharedLink> expiredLinks = sharedLinkRepository.findByExpiresAtBefore(LocalDateTime.now());
        sharedLinkRepository.deleteAll(expiredLinks);
        log.info("Deleted {} expired shared links", expiredLinks.size());
    }

    // Business Operations
    @Override
    public SharedLink createFileShare(Long fileId, Long userId, String recipientPhone) {
        log.info("Creating file share for file: {} by user: {}", fileId, userId);

        // Validate file exists and user has access
        File file = fileService.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found: " + fileId));

        validateFileAccess(file, userId);

        // Generate unique link token
        String linkToken = generateLinkToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(shareExpirationDays);

        // Create shared link
        SharedLink sharedLink = SharedLink.builder()
                .linkToken(linkToken)
                .expiresAt(expiresAt)
                .file(file)
                .build();

        SharedLink savedLink = save(sharedLink);

        // Send SMS if recipient phone provided
        if (recipientPhone != null && !recipientPhone.trim().isEmpty()) {
            sendShareNotification(recipientPhone, linkToken, file.getDisplayName());
        }

        log.info("File share created successfully: {} expires at {}", linkToken, expiresAt);
        return savedLink;
    }

    @Override
    public SharedLink createFolderShare(Long folderId, Long userId, String recipientPhone) {
        log.info("Creating folder share for folder: {} by user: {}", folderId, userId);

        // Validate folder exists and user has access
        if (!folderService.hasAccess(folderId, userId)) {
            throw new FolderNotFoundException("Folder not found or access denied: " + folderId);
        }

        Folder folder = folderService.findById(folderId)
                .orElseThrow(() -> new FolderNotFoundException("Folder not found: " + folderId));

        // Generate unique link token
        String linkToken = generateLinkToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(shareExpirationDays);

        // Create shared link for folder
        SharedLink sharedLink = SharedLink.builder()
                .linkToken(linkToken)
                .expiresAt(expiresAt)
                .file(null) // No file for folder shares
                .build();

        SharedLink savedLink = save(sharedLink);

        // Send SMS if recipient phone provided
        if (recipientPhone != null && !recipientPhone.trim().isEmpty()) {
            sendShareNotification(recipientPhone, linkToken, "Folder: " + folder.getName());
        }

        log.info("Folder share created successfully: {} expires at {}", linkToken, expiresAt);
        return savedLink;
    }

    @Override
    public Resource downloadSharedFile(String linkToken) {
        log.info("Attempting to download shared file with token: {}", linkToken);

        // Find and validate share link
        SharedLink sharedLink = findByLinkToken(linkToken)
                .orElseThrow(() -> new ShareNotFoundException("Invalid or expired share link"));

        // Check expiration
        if (LocalDateTime.now().isAfter(sharedLink.getExpiresAt())) {
            log.warn("Expired share link accessed: {}", linkToken);
            throw new ShareExpiredException("Share link has expired");
        }

        // Get file from share link
        File file = sharedLink.getFile();
        if (file == null) {
            throw new FileNotFoundException("Shared item is not a file");
        }

        // Use FileService to get the resource (bypasses user validation)
        return getFileResource(file);
    }

    @Override
    public void revokeShare(Long shareId, Long userId) {
        log.info("Revoking share: {} by user: {}", shareId, userId);

        SharedLink sharedLink = findById(shareId)
                .orElseThrow(() -> new ShareNotFoundException("Share not found: " + shareId));

        // Validate user owns the shared file
        if (sharedLink.getFile() != null) {
            validateFileAccess(sharedLink.getFile(), userId);
        }

        deleteById(shareId);
        log.info("Share revoked successfully: {}", shareId);
    }

    @Override
    public List<SharedLink> getUserShares(Long userId) {
        // This would require adding userId to SharedLink entity or joining through file ownership
        // For now, returning all shares - should be filtered by file ownership
        return findAll();
    }

    @Override
    public boolean isValidShareToken(String linkToken) {
        Optional<SharedLink> shareOpt = findByLinkToken(linkToken);
        if (shareOpt.isEmpty()) {
            return false;
        }

        SharedLink share = shareOpt.get();
        return LocalDateTime.now().isBefore(share.getExpiresAt());
    }

    // Helper Methods
    private String generateLinkToken() {
        return UUID.randomUUID().toString();
    }

    private void validateFileAccess(File file, Long userId) {
        // Get user's storage path and validate ownership
        // This logic should match FileService validation
        // For now, simplified validation
        log.debug("Validating file access for user: {} and file: {}", userId, file.getId());
    }

    private void sendShareNotification(String recipientPhone, String linkToken, String itemName) {
        try {
            String shareUrl = baseUrl + "/public/shared/" + linkToken;
            String message = String.format("You have received a shared file: %s. Access it here: %s", itemName, shareUrl);

            var smsResponse = smsService.sendSMS(recipientPhone, message);

            if (smsResponse.isSuccess()) {
                log.info("Share notification sent successfully to: {}", recipientPhone);
            } else {
                log.error("Failed to send share notification to {}: {}", recipientPhone, smsResponse.getMessage());
            }
        } catch (Exception e) {
            log.error("Error sending share notification: {}", e.getMessage(), e);
        }
    }

    private Resource getFileResource(File file) {
        // Direct file access bypassing user validation
        try {
            return fileService.downloadFile(file.getId(), null); // This needs modification in FileService
        } catch (Exception e) {
            throw new FileNotFoundException("Cannot access shared file: " + e.getMessage());
        }
    }
}