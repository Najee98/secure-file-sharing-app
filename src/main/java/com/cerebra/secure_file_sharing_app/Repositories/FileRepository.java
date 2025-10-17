package com.cerebra.secure_file_sharing_app.Repositories;

import com.cerebra.secure_file_sharing_app.Entities.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {
    List<File> findByStoragePathId(Long storagePathId);
    List<File> findByFolderId(Long folderId);
    List<File> findByStoragePathIdAndFolderIsNull(Long storagePathId);
    Optional<File> findByPhysicalName(String physicalName);
}
