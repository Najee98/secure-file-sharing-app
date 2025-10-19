package com.cerebra.secure_file_sharing_app.Services;

import com.cerebra.secure_file_sharing_app.Entities.Folder;
import com.cerebra.secure_file_sharing_app.Repositories.FolderRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import com.cerebra.secure_file_sharing_app.Entities.StoragePath;
import com.cerebra.secure_file_sharing_app.Services.StoragePathService;
import com.cerebra.secure_file_sharing_app.Exceptions.CustomExceptions.*;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class FolderServiceImpl implements FolderService {
    
    private final FolderRepository folderRepository;
    private final StoragePathService storagePathService;


    @Override
    public Folder save(Folder folder) {
        return folderRepository.save(folder);
    }
    
    @Override
    public Optional<Folder> findById(Long id) {
        return folderRepository.findById(id);
    }
    
    @Override
    public List<Folder> findByStoragePathId(Long storagePathId) {
        return folderRepository.findByStoragePathId(storagePathId);
    }
    
    @Override
    public List<Folder> findByParentFolderId(Long parentFolderId) {
        return folderRepository.findByParentFolderId(parentFolderId);
    }
    
    @Override
    public List<Folder> findRootFolders(Long storagePathId) {
        return folderRepository.findByStoragePathIdAndParentFolderIsNull(storagePathId);
    }
    
    @Override
    public List<Folder> findAll() {
        return folderRepository.findAll();
    }
    
    @Override
    public void deleteById(Long id) {
        folderRepository.deleteById(id);
    }

    @Override
    public List<Folder> findByStoragePathIdAndParentFolderIsNull(Long id) {
        return folderRepository.findByStoragePathIdAndParentFolderIsNull(id);
    }


}
