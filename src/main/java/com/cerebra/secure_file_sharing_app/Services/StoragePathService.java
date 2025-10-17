package com.cerebra.secure_file_sharing_app.Services;

import com.cerebra.secure_file_sharing_app.Entities.StoragePath;

import java.util.List;
import java.util.Optional;

public interface StoragePathService {
    StoragePath save(StoragePath storagePath);
    Optional<StoragePath> findById(Long id);
    Optional<StoragePath> findByAppUserId(Long appUserId);
    List<StoragePath> findAll();
    void deleteById(Long id);
}
