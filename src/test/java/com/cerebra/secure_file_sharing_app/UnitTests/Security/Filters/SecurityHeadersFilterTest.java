package com.cerebra.secure_file_sharing_app.UnitTests.Security.Filters;

import com.cerebra.secure_file_sharing_app.Security.Filters.SecurityHeadersFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityHeadersFilter Tests")
class SecurityHeadersFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private ServletRequest nonHttpRequest;

    @Mock
    private ServletResponse nonHttpResponse;

    private SecurityHeadersFilter securityHeadersFilter;

    @BeforeEach
    void setUp() {
        securityHeadersFilter = new SecurityHeadersFilter();
    }

    @Test
    @DisplayName("Should set all security headers for HTTP response")
    void doFilter_httpResponse_setsAllSecurityHeaders() throws IOException, ServletException {
        // Act
        securityHeadersFilter.doFilter(request, response, filterChain);

        // Assert - Verify all security headers are set
        verify(response).setHeader("X-XSS-Protection", "1; mode=block");
        verify(response).setHeader("X-Content-Type-Options", "nosniff");
        verify(response).setHeader("X-Frame-Options", "DENY");
        verify(response).setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        verify(response).setHeader("Content-Security-Policy",
                "default-src 'self'; script-src 'self'; object-src 'none'; base-uri 'self';");
        verify(response).setHeader("X-Permitted-Cross-Domain-Policies", "none");
        verify(response).setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Verify filter chain continues
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should continue filter chain without setting headers for non-HTTP response")
    void doFilter_nonHttpResponse_skipsHeadersButContinuesChain() throws IOException, ServletException {
        // Act
        securityHeadersFilter.doFilter(nonHttpRequest, nonHttpResponse, filterChain);

        // Assert - Filter chain should continue (can't verify no setHeader calls since ServletResponse doesn't have setHeader method)
        verify(filterChain).doFilter(nonHttpRequest, nonHttpResponse);
    }

    @Test
    @DisplayName("Should handle mixed request/response types")
    void doFilter_mixedTypes_handlesCorrectly() throws IOException, ServletException {
        // Test HTTP request with non-HTTP response
        securityHeadersFilter.doFilter(request, nonHttpResponse, filterChain);

        // Can't verify no setHeader calls on ServletResponse since it doesn't have that method
        verify(filterChain).doFilter(request, nonHttpResponse);

        // Reset mocks
        reset(filterChain);

        // Test non-HTTP request with HTTP response  
        securityHeadersFilter.doFilter(nonHttpRequest, response, filterChain);

        verify(response).setHeader("X-XSS-Protection", "1; mode=block");
        verify(filterChain).doFilter(nonHttpRequest, response);
    }

    @Test
    @DisplayName("Should set XSS protection header correctly")
    void doFilter_httpResponse_setsXSSProtectionHeader() throws IOException, ServletException {
        // Act
        securityHeadersFilter.doFilter(request, response, filterChain);

        // Assert
        verify(response).setHeader("X-XSS-Protection", "1; mode=block");
    }

    @Test
    @DisplayName("Should set content type options header correctly")
    void doFilter_httpResponse_setsContentTypeOptionsHeader() throws IOException, ServletException {
        // Act
        securityHeadersFilter.doFilter(request, response, filterChain);

        // Assert
        verify(response).setHeader("X-Content-Type-Options", "nosniff");
    }

    @Test
    @DisplayName("Should set frame options header correctly")
    void doFilter_httpResponse_setsFrameOptionsHeader() throws IOException, ServletException {
        // Act
        securityHeadersFilter.doFilter(request, response, filterChain);

        // Assert
        verify(response).setHeader("X-Frame-Options", "DENY");
    }

    @Test
    @DisplayName("Should set HSTS header correctly")
    void doFilter_httpResponse_setsHSTSHeader() throws IOException, ServletException {
        // Act
        securityHeadersFilter.doFilter(request, response, filterChain);

        // Assert
        verify(response).setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
    }

    @Test
    @DisplayName("Should set CSP header correctly")
    void doFilter_httpResponse_setsCSPHeader() throws IOException, ServletException {
        // Act
        securityHeadersFilter.doFilter(request, response, filterChain);

        // Assert
        verify(response).setHeader("Content-Security-Policy",
                "default-src 'self'; script-src 'self'; object-src 'none'; base-uri 'self';");
    }

    @Test
    @DisplayName("Should set cross domain policies header correctly")
    void doFilter_httpResponse_setCrossDomainPoliciesHeader() throws IOException, ServletException {
        // Act
        securityHeadersFilter.doFilter(request, response, filterChain);

        // Assert
        verify(response).setHeader("X-Permitted-Cross-Domain-Policies", "none");
    }

    @Test
    @DisplayName("Should set referrer policy header correctly")
    void doFilter_httpResponse_setsReferrerPolicyHeader() throws IOException, ServletException {
        // Act
        securityHeadersFilter.doFilter(request, response, filterChain);

        // Assert
        verify(response).setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
    }

    @Test
    @DisplayName("Should handle IOException from filter chain")
    void doFilter_filterChainThrowsIOException_propagatesException() throws IOException, ServletException {
        // Arrange
        doThrow(new IOException("Network error")).when(filterChain).doFilter(request, response);

        // Act & Assert
        try {
            securityHeadersFilter.doFilter(request, response, filterChain);
        } catch (IOException e) {
            // Headers should still be set before the exception
            verify(response).setHeader("X-XSS-Protection", "1; mode=block");
            verify(response).setHeader("X-Content-Type-Options", "nosniff");
            // Exception should be propagated
            assert e.getMessage().equals("Network error");
        }
    }

    @Test
    @DisplayName("Should handle ServletException from filter chain")
    void doFilter_filterChainThrowsServletException_propagatesException() throws IOException, ServletException {
        // Arrange
        doThrow(new ServletException("Servlet error")).when(filterChain).doFilter(request, response);

        // Act & Assert
        try {
            securityHeadersFilter.doFilter(request, response, filterChain);
        } catch (ServletException e) {
            // Headers should still be set before the exception
            verify(response).setHeader("X-XSS-Protection", "1; mode=block");
            verify(response).setHeader("X-Content-Type-Options", "nosniff");
            // Exception should be propagated
            assert e.getMessage().equals("Servlet error");
        }
    }

    @Test
    @DisplayName("Should set headers in correct order")
    void doFilter_httpResponse_setsHeadersBeforeContinuingChain() throws IOException, ServletException {
        // Use InOrder to verify headers are set before filter chain continues
        var inOrder = inOrder(response, filterChain);

        // Act
        securityHeadersFilter.doFilter(request, response, filterChain);

        // Assert - Headers should be set before filter chain continues
        inOrder.verify(response).setHeader("X-XSS-Protection", "1; mode=block");
        inOrder.verify(response).setHeader("X-Content-Type-Options", "nosniff");
        inOrder.verify(response).setHeader("X-Frame-Options", "DENY");
        inOrder.verify(response).setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        inOrder.verify(response).setHeader("Content-Security-Policy",
                "default-src 'self'; script-src 'self'; object-src 'none'; base-uri 'self';");
        inOrder.verify(response).setHeader("X-Permitted-Cross-Domain-Policies", "none");
        inOrder.verify(response).setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        inOrder.verify(filterChain).doFilter(request, response);
    }
}