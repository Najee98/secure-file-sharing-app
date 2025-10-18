package com.cerebra.secure_file_sharing_app.Services;

import com.cerebra.secure_file_sharing_app.Entities.File;

import java.util.List;
import java.util.Optional;


public interface FileService {
    File save(File file);
    Optional<File> findById(Long id);
    List<File> findByStoragePathId(Long storagePathId);
    List<File> findByFolderId(Long folderId);
    List<File> findRootFiles(Long storagePathId);
    Optional<File> findByPhysicalName(String physicalName);
    List<File> findAll();
    void deleteById(Long id);
}
