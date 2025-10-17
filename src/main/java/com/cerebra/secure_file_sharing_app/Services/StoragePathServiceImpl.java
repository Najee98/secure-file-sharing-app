package com.cerebra.secure_file_sharing_app.Services;

import com.cerebra.secure_file_sharing_app.Entities.StoragePath;
import com.cerebra.secure_file_sharing_app.Repositories.StoragePathRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StoragePathServiceImpl implements StoragePathService {
    
    private final StoragePathRepository storagePathRepository;
    
    @Override
    public StoragePath save(StoragePath storagePath) {
        // TODO: Implement
        return null;
    }
    
    @Override
    public Optional<StoragePath> findById(Long id) {
        // TODO: Implement
        return Optional.empty();
    }
    
    @Override
    public Optional<StoragePath> findByAppUserId(Long appUserId) {
        // TODO: Implement
        return Optional.empty();
    }
    
    @Override
    public List<StoragePath> findAll() {
        // TODO: Implement
        return null;
    }
    
    @Override
    public void deleteById(Long id) {
        // TODO: Implement
    }
}
