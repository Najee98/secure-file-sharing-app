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
        return storagePathRepository.save(storagePath);
    }

    @Override
    public Optional<StoragePath> findById(Long id) {
        return storagePathRepository.findById(id);
    }

    @Override
    public Optional<StoragePath> findByAppUserId(Long appUserId) {
        return storagePathRepository.findByAppUserId(appUserId);
    }

    @Override
    public List<StoragePath> findAll() {
        return storagePathRepository.findAll();
    }

    @Override
    public void deleteById(Long id) {
        storagePathRepository.deleteById(id);
    }
}
