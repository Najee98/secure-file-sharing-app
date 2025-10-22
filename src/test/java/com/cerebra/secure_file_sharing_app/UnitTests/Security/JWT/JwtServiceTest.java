package com.cerebra.secure_file_sharing_app.UnitTests.Security.JWT;

import com.cerebra.secure_file_sharing_app.Entities.AppUser;
import com.cerebra.secure_file_sharing_app.Security.JWT.JwtService;
import com.cerebra.secure_file_sharing_app.Security.SecurityUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService Tests")
class JwtServiceTest {

    private MockedStatic<SecurityUtils> securityUtilsMock;
    private JwtService jwtService;
    private AppUser testUser;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        // Create mock static
        securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);

        // Setup mocks
        String testSecret = "testSecretKeyThatIsLongEnoughForHS256Algorithm";
        SecretKey testSecretKey = Keys.hmacShaKeyFor(testSecret.getBytes());

        securityUtilsMock.when(SecurityUtils::secretKey).thenReturn(testSecret);
        securityUtilsMock.when(() -> SecurityUtils.getSignInKey(anyString())).thenReturn(testSecretKey);

        jwtService = new JwtService();
        testUser = AppUser.builder()
                .id(1L)
                .phoneNumber("+1234567890")
                .build();
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close(); // Important: close the static mock
    }

    @Test
    @DisplayName("Should generate valid JWT token for user")
    void generateToken_validUser_returnsValidJWT() {
        // Act
        String token = jwtService.generateToken(testUser);
        
        // Assert
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts separated by dots
        
        // Verify we can extract username from generated token
        String extractedUsername = jwtService.extractUsername(token);
        assertThat(extractedUsername).isEqualTo(testUser.getPhoneNumber());
    }

    @Test
    @DisplayName("Should extract username from valid token")
    void extractUsername_validToken_returnsPhoneNumber() {
        // Arrange
        String token = jwtService.generateToken(testUser);
        
        // Act
        String extractedUsername = jwtService.extractUsername(token);
        
        // Assert
        assertThat(extractedUsername).isEqualTo("+1234567890");
    }

    @Test
    @DisplayName("Should extract subject claim from valid token")
    void extractClaim_validTokenSubjectClaim_returnsCorrectClaim() {
        // Arrange
        String token = jwtService.generateToken(testUser);
        
        // Act
        String subject = jwtService.extractClaim(token, Claims::getSubject);
        
        // Assert
        assertThat(subject).isEqualTo("+1234567890");
    }

    @Test
    @DisplayName("Should extract expiration claim from valid token")
    void extractClaim_validTokenExpirationClaim_returnsCorrectClaim() {
        // Arrange
        String token = jwtService.generateToken(testUser);
        
        // Act
        Date expiration = jwtService.extractClaim(token, Claims::getExpiration);
        
        // Assert
        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(new Date()); // Should be in the future
    }

    @Test
    @DisplayName("Should return true for valid token")
    void isTokenValid_validToken_returnsTrue() {
        // Arrange
        String token = jwtService.generateToken(testUser);
        
        // Act
        boolean isValid = jwtService.isTokenValid(token, testUser);
        
        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should return false for token with wrong user")
    void isTokenValid_tokenWithWrongUser_returnsFalse() {
        // Arrange
        String token = jwtService.generateToken(testUser);
        AppUser differentUser = AppUser.builder()
                .id(2L)
                .phoneNumber("+9876543210")
                .build();
        
        // Act
        boolean isValid = jwtService.isTokenValid(token, differentUser);
        
        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should throw exception for malformed token")
    void extractUsername_malformedToken_throwsException() {
        // Arrange
        String malformedToken = "not.a.valid.jwt.token";
        
        // Act & Assert
        assertThatThrownBy(() -> jwtService.extractUsername(malformedToken))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    @DisplayName("Should throw exception for token with invalid signature")
    void extractUsername_invalidSignature_throwsException() {
        // Arrange
        String token = jwtService.generateToken(testUser);
        String tamperedToken = token.substring(0, token.length() - 5) + "12345"; // Change last 5 chars
        
        // Act & Assert
        assertThatThrownBy(() -> jwtService.extractUsername(tamperedToken))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("Should return false for null token")
    void isTokenValid_nullToken_returnsFalse() {
        // Act
        boolean isValid = jwtService.isTokenValid(null, testUser);
        
        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should return false for empty token")
    void isTokenValid_emptyToken_returnsFalse() {
        // Act
        boolean isValid = jwtService.isTokenValid("", testUser);
        
        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should handle token validation with null user")
    void isTokenValid_nullUser_returnsFalse() {
        // Arrange
        String token = jwtService.generateToken(testUser);
        
        // Act
        boolean isValid = jwtService.isTokenValid(token, null);
        
        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should generate different tokens for same user at different times")
    void generateToken_sameUserDifferentTimes_generatesDifferentTokens() {
        // Act
        String token1 = jwtService.generateToken(testUser);

        // Sleep for 1 second to ensure different timestamp
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String token2 = jwtService.generateToken(testUser);

        // Assert
        assertThat(token1).isNotEqualTo(token2);
        assertThat(jwtService.isTokenValid(token1, testUser)).isTrue();
        assertThat(jwtService.isTokenValid(token2, testUser)).isTrue();
    }

    @Test
    @DisplayName("Should extract correct expiration time based on configuration")
    void generateToken_validUser_setsCorrectExpirationTime() {
        // Arrange
        long expectedExpiration = 86400000L; // 24 hours in milliseconds
        Date beforeGeneration = new Date();

        // Act
        String token = jwtService.generateToken(testUser);
        Date afterGeneration = new Date();

        // Assert
        Date tokenExpiration = jwtService.extractClaim(token, Claims::getExpiration);

        // More flexible time range (allow for processing time)
        Date expectedMinExpiration = new Date(beforeGeneration.getTime() + expectedExpiration - 1000); // 1 sec tolerance
        Date expectedMaxExpiration = new Date(afterGeneration.getTime() + expectedExpiration + 1000);   // 1 sec tolerance

        assertThat(tokenExpiration).isBetween(expectedMinExpiration, expectedMaxExpiration);
    }
}