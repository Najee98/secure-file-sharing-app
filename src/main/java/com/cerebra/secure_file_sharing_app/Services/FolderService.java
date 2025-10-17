package com.cerebra.secure_file_sharing_app.Services;

import com.cerebra.secure_file_sharing_app.Entities.Folder;

import java.util.List;
import java.util.Optional;


public interface FolderService {
    Folder save(Folder folder);
    Optional<Folder> findById(Long id);
    List<Folder> findByStoragePathId(Long storagePathId);
    List<Folder> findByParentFolderId(Long parentFolderId);
    List<Folder> findRootFolders(Long storagePathId);
    List<Folder> findAll();
    void deleteById(Long id);
}
