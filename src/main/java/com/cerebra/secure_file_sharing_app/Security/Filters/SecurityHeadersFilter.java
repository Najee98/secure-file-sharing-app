package com.cerebra.secure_file_sharing_app.Security.Filders;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1)
@Slf4j
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (response instanceof HttpServletResponse httpResponse) {
            
            // Prevent XSS attacks
            httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
            
            // Prevent content type sniffing
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");
            
            // Prevent clickjacking
            httpResponse.setHeader("X-Frame-Options", "DENY");
            
            // Force HTTPS in production
            httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            
            // Content Security Policy
            httpResponse.setHeader("Content-Security-Policy",
                    "default-src 'self'; script-src 'self'; object-src 'none'; base-uri 'self';");
            
            // Restrict cross-domain policies
            httpResponse.setHeader("X-Permitted-Cross-Domain-Policies", "none");
            
            // Referrer Policy
            httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        }

        chain.doFilter(request, response);
    }
}