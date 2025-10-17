package com.cerebra.secure_file_sharing_app.Repositories;

import com.cerebra.secure_file_sharing_app.Entities.SharedLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SharedLinkRepository extends JpaRepository<SharedLink, Long> {
    Optional<SharedLink> findByLinkToken(String linkToken);
    List<SharedLink> findByFileId(Long fileId);
    List<SharedLink> findByExpiresAtBefore(LocalDateTime dateTime);
}
