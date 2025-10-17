package com.cerebra.secure_file_sharing_app.Services;

import com.cerebra.secure_file_sharing_app.Entities.SharedLink;

import java.util.List;
import java.util.Optional;

public interface SharedLinkService {
    SharedLink save(SharedLink sharedLink);
    Optional<SharedLink> findById(Long id);
    Optional<SharedLink> findByLinkToken(String linkToken);
    List<SharedLink> findByFileId(Long fileId);
    List<SharedLink> findAll();
    void deleteById(Long id);
    void deleteExpiredLinks();
}
