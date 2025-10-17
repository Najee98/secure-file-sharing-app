package com.cerebra.secure_file_sharing_app.Services;

import com.cerebra.secure_file_sharing_app.Entities.SharedLink;
import com.cerebra.secure_file_sharing_app.Repositories.SharedLinkRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SharedLinkServiceImpl implements SharedLinkService {
    
    private final SharedLinkRepository sharedLinkRepository;
    
    @Override
    public SharedLink save(SharedLink sharedLink) {
        // TODO: Implement
        return null;
    }
    
    @Override
    public Optional<SharedLink> findById(Long id) {
        // TODO: Implement
        return Optional.empty();
    }
    
    @Override
    public Optional<SharedLink> findByLinkToken(String linkToken) {
        // TODO: Implement
        return Optional.empty();
    }
    
    @Override
    public List<SharedLink> findByFileId(Long fileId) {
        // TODO: Implement
        return null;
    }
    
    @Override
    public List<SharedLink> findAll() {
        // TODO: Implement
        return null;
    }
    
    @Override
    public void deleteById(Long id) {
        // TODO: Implement
    }
    
    @Override
    public void deleteExpiredLinks() {
        // TODO: Implement
    }
}
