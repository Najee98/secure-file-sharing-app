package com.cerebra.secure_file_sharing_app.Services;

import com.cerebra.secure_file_sharing_app.Entities.Folder;
import com.cerebra.secure_file_sharing_app.Repositories.FolderRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FolderServiceImpl implements FolderService {
    
    private final FolderRepository folderRepository;
    
    @Override
    public Folder save(Folder folder) {
        // TODO: Implement
        return null;
    }
    
    @Override
    public Optional<Folder> findById(Long id) {
        // TODO: Implement
        return Optional.empty();
    }
    
    @Override
    public List<Folder> findByStoragePathId(Long storagePathId) {
        // TODO: Implement
        return null;
    }
    
    @Override
    public List<Folder> findByParentFolderId(Long parentFolderId) {
        // TODO: Implement
        return null;
    }
    
    @Override
    public List<Folder> findRootFolders(Long storagePathId) {
        // TODO: Implement
        return null;
    }
    
    @Override
    public List<Folder> findAll() {
        // TODO: Implement
        return null;
    }
    
    @Override
    public void deleteById(Long id) {
        // TODO: Implement
    }
}
