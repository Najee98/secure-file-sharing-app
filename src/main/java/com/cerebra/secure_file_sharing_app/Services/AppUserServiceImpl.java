package com.cerebra.secure_file_sharing_app.Services;

import com.cerebra.secure_file_sharing_app.Entities.AppUser;
import com.cerebra.secure_file_sharing_app.Repositories.AppUserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AppUserServiceImpl implements AppUserService {
    
    private final AppUserRepository appUserRepository;
    
    @Override
    public AppUser save(AppUser appUser) {
        // TODO: Implement
        return null;
    }
    
    @Override
    public Optional<AppUser> findById(Long id) {
        // TODO: Implement
        return Optional.empty();
    }
    
    @Override
    public Optional<AppUser> findByPhoneNumber(String phoneNumber) {
        // TODO: Implement
        return Optional.empty();
    }
    
    @Override
    public List<AppUser> findAll() {
        // TODO: Implement
        return null;
    }
    
    @Override
    public void deleteById(Long id) {
        // TODO: Implement
    }
    
    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        // TODO: Implement
        return false;
    }
}
