package com.cerebra.secure_file_sharing_app.Repositories;

import com.cerebra.secure_file_sharing_app.Entities.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {
    List<Folder> findByStoragePathId(Long storagePathId);
    List<Folder> findByParentFolderId(Long parentFolderId);
    List<Folder> findByStoragePathIdAndParentFolderIsNull(Long storagePathId);
}
