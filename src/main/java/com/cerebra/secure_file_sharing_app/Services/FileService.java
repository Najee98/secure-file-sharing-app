package com.cerebra.secure_file_sharing_app.Services;

import com.cerebra.secure_file_sharing_app.Entities.File;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

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

    File uploadFile(MultipartFile multipartFile, Long folderId, Long userId);
    Resource downloadFile(Long fileId, Long userId);
    void deleteFile(Long fileId, Long userId);
    List<File> getUserFiles(Long userId);
    List<File> getFolderFiles(Long folderId, Long userId);

}
