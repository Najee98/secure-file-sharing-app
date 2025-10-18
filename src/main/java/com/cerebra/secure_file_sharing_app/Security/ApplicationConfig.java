package com.cerebra.secure_file_sharing_app.Security;

import com.cerebra.secure_file_sharing_app.Repositories.AppUserRepository;
import com.cerebra.secure_file_sharing_app.Security.OTPAuthenticationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final AppUserRepository userRepository;

    @Bean
    public UserDetailsService userDetailsService(){
        return phoneNumber -> userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with phone: " + phoneNumber));
    }

//    @Bean
//    public AuthenticationProvider authenticationProvider(){
//        return new OTPAuthenticationProvider();
//    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}