package com.cerebra.secure_file_sharing_app.Repositories;

import com.cerebra.secure_file_sharing_app.Entities.File;
import com.cerebra.secure_file_sharing_app.Entities.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {
    List<Folder> findByStoragePathId(Long storagePathId);
    List<Folder> findByParentFolderId(Long parentFolderId);
    List<Folder> findByStoragePathIdAndParentFolderIsNull(Long storagePathId);

    @Query("SELECT f FROM File f WHERE f.folder.id = :folderId")
    List<File> findFilesByFolderId(@Param("folderId") Long folderId);
}
