package com.cerebra.secure_file_sharing_app.Services;

import com.cerebra.secure_file_sharing_app.Entities.AppUser;
import com.cerebra.secure_file_sharing_app.Entities.StoragePath;
import com.cerebra.secure_file_sharing_app.Exceptions.CustomExceptions.InvalidOTPException;
import com.cerebra.secure_file_sharing_app.Exceptions.CustomExceptions.OTPExpiredException;
import com.cerebra.secure_file_sharing_app.SMS.SMSService;
import com.cerebra.secure_file_sharing_app.Security.DTO.AuthResponse;
import com.cerebra.secure_file_sharing_app.Security.DTO.OTPResponse;
import com.cerebra.secure_file_sharing_app.Security.JWT.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    
    private final SMSService smsService;
    private final JwtService jwtService;
    private final AppUserService appUserService;
    private final StoragePathService storagePathService;
    
    // In-memory OTP storage - consider Redis for production
    private final Map<String, OTPSession> otpStorage = new ConcurrentHashMap<>();
    private final Random random = new Random();
    
    @Override
    public OTPResponse requestOTP(String phoneNumber) {
        log.info("OTP request for phone number: {}", phoneNumber);
        
        try {
            // Generate 6-digit OTP
            String otp = generateOTP();
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);
            
            // Store OTP in memory
            otpStorage.put(phoneNumber, new OTPSession(otp, expiresAt));
            
            // Send SMS
            String message = String.format("Your verification code is: %s. Valid for 5 minutes.", otp);
            var smsResponse = smsService.sendSMS(phoneNumber, message);
            
            if (smsResponse.isSuccess()) {
                log.info("OTP sent successfully to: {}", phoneNumber);
                return OTPResponse.success(phoneNumber, expiresAt);
            } else {
                log.error("Failed to send SMS to {}: {}", phoneNumber, smsResponse.getMessage());
                otpStorage.remove(phoneNumber); // Clean up on SMS failure
                return OTPResponse.failure("Failed to send OTP: " + smsResponse.getMessage());
            }
            
        } catch (Exception e) {
            log.error("Error generating OTP for {}: {}", phoneNumber, e.getMessage(), e);
            return OTPResponse.failure("Internal server error");
        }
    }
    
    @Override
    public AuthResponse verifyOTP(String phoneNumber, String otp) {
        log.info("OTP verification for phone number: {}", phoneNumber);
        
        // Check if OTP exists
        OTPSession session = otpStorage.get(phoneNumber);
        if (session == null) {
            log.warn("No OTP found for phone number: {}", phoneNumber);
            throw new InvalidOTPException("Invalid or expired OTP");
        }
        
        // Check if OTP is expired
        if (LocalDateTime.now().isAfter(session.getExpiresAt())) {
            log.warn("Expired OTP for phone number: {}", phoneNumber);
            otpStorage.remove(phoneNumber);
            throw new OTPExpiredException("OTP has expired");
        }
        
        // Verify OTP
        if (!session.getOtp().equals(otp)) {
            log.warn("Invalid OTP for phone number: {}", phoneNumber);
            throw new InvalidOTPException("Invalid OTP");
        }
        
        // OTP is valid - clean up
        otpStorage.remove(phoneNumber);
        
        // Find or create user
        AppUser user = appUserService.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> createNewUser(phoneNumber));
        
        // Generate JWT token
        String token = jwtService.generateToken(user);
        
        log.info("Authentication successful for phone number: {}", phoneNumber);
        return AuthResponse.success(token, phoneNumber);
    }
    
    private String generateOTP() {
        return String.format("%06d", random.nextInt(1000000));
    }
    
    private AppUser createNewUser(String phoneNumber) {
        log.info("Creating new user for phone number: {}", phoneNumber);
        
        // Create user
        AppUser newUser = appUserService.save(AppUser.builder()
                .phoneNumber(phoneNumber)
                .build());
        
        // Create storage path for user
        String basePath = "/app-storage/users/user" + newUser.getId();
        storagePathService.save(StoragePath.builder()
                .basePath(basePath)
                .appUser(newUser)
                .build());
        
        return newUser;
    }
    
    // Inner class for OTP session
    private static class OTPSession {
        private final String otp;
        private final LocalDateTime expiresAt;
        
        public OTPSession(String otp, LocalDateTime expiresAt) {
            this.otp = otp;
            this.expiresAt = expiresAt;
        }
        
        public String getOtp() { return otp; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
    }
}