package com.cerebra.secure_file_sharing_app.Security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class OTPAuthenticationProvider implements AuthenticationProvider {
    
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        // OTP authentication is handled separately in AuthController
        // This provider exists to satisfy Spring Security's requirements
        throw new UnsupportedOperationException("OTP authentication is handled in AuthController");
    }
    
    @Override
    public boolean supports(Class<?> authentication) {
        return false; // We handle authentication manually in OTP flow
    }
}