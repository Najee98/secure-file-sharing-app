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
        return appUserRepository.save(appUser);
    }

    @Override
    public Optional<AppUser> findByPhoneNumber(String phoneNumber) {
        return appUserRepository.findByPhoneNumber(phoneNumber);
    }

    @Override
    public Optional<AppUser> findById(Long id) {
        return appUserRepository.findById(id);
    }

    @Override
    public List<AppUser> findAll() {
        return appUserRepository.findAll();
    }

    @Override
    public void deleteById(Long id) {
        appUserRepository.deleteById(id);
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        return appUserRepository.existsByPhoneNumber(phoneNumber);
    }
}
