package com.cerebra.secure_file_sharing_app.Services;

import com.cerebra.secure_file_sharing_app.Entities.AppUser;

import java.util.List;
import java.util.Optional;

public interface AppUserService {
    AppUser save(AppUser appUser);
    Optional<AppUser> findById(Long id);
    Optional<AppUser> findByPhoneNumber(String phoneNumber);
    List<AppUser> findAll();
    void deleteById(Long id);
    boolean existsByPhoneNumber(String phoneNumber);
}
