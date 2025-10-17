package com.cerebra.secure_file_sharing_app.Repositories;

import com.cerebra.secure_file_sharing_app.Entities.StoragePath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoragePathRepository extends JpaRepository<StoragePath, Long> {
    Optional<StoragePath> findByAppUserId(Long appUserId);
}
