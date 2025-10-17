package com.cerebra.secure_file_sharing_app.Services;

import com.cerebra.secure_file_sharing_app.Entities.File;
import com.cerebra.secure_file_sharing_app.Repositories.FileRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
    
    private final FileRepository fileRepository;
    
    @Override
    public File save(File file) {
        // TODO: Implement
        return null;
    }
    
    @Override
    public Optional<File> findById(Long id) {
        // TODO: Implement
        return Optional.empty();
    }
    
    @Override
    public List<File> findByStoragePathId(Long storagePathId) {
        // TODO: Implement
        return null;
    }
    
    @Override
    public List<File> findByFolderId(Long folderId) {
        // TODO: Implement
        return null;
    }
    
    @Override
    public List<File> findRootFiles(Long storagePathId) {
        // TODO: Implement
        return null;
    }
    
    @Override
    public Optional<File> findByPhysicalName(String physicalName) {
        // TODO: Implement
        return Optional.empty();
    }
    
    @Override
    public List<File> findAll() {
        // TODO: Implement
        return null;
    }
    
    @Override
    public void deleteById(Long id) {
        // TODO: Implement
    }
}
