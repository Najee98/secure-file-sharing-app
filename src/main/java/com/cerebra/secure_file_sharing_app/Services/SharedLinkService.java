package com.cerebra.secure_file_sharing_app.Services;

import com.cerebra.secure_file_sharing_app.Entities.SharedLink;
import org.springframework.core.io.Resource;
import java.util.List;
import java.util.Optional;

public interface SharedLinkService {

    // Basic CRUD
    SharedLink save(SharedLink sharedLink);
    Optional<SharedLink> findById(Long id);
    Optional<SharedLink> findByLinkToken(String linkToken);
    List<SharedLink> findByFileId(Long fileId);
    List<SharedLink> findAll();
    void deleteById(Long id);
    void deleteExpiredLinks();

    // Business operations
    SharedLink createFileShare(Long fileId, Long userId, String recipientPhone);
    SharedLink createFolderShare(Long folderId, Long userId, String recipientPhone);
    Resource downloadSharedFile(String linkToken);
    void revokeShare(Long shareId, Long userId);
    List<SharedLink> getUserShares(Long userId);
    boolean isValidShareToken(String linkToken);
}