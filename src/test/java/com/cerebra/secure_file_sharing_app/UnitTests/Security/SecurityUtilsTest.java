package com.cerebra.secure_file_sharing_app.UnitTests.Security;

import com.cerebra.secure_file_sharing_app.Security.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityUtils Tests")
class SecurityUtilsTest {

    @Test
    @DisplayName("Should generate AES key with 128-bit size")
    void generateKey_128BitSize_generatesValidKey() throws NoSuchAlgorithmException {
        // Act
        SecretKey result = SecurityUtils.generateKey(128);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAlgorithm()).isEqualTo("AES");
        assertThat(result.getEncoded()).hasSize(16); // 128 bits = 16 bytes
        assertThat(result.getFormat()).isEqualTo("RAW");
    }

    @Test
    @DisplayName("Should generate AES key with 192-bit size")
    void generateKey_192BitSize_generatesValidKey() throws NoSuchAlgorithmException {
        // Act
        SecretKey result = SecurityUtils.generateKey(192);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAlgorithm()).isEqualTo("AES");
        assertThat(result.getEncoded()).hasSize(24); // 192 bits = 24 bytes
        assertThat(result.getFormat()).isEqualTo("RAW");
    }

    @Test
    @DisplayName("Should generate AES key with 256-bit size")
    void generateKey_256BitSize_generatesValidKey() throws NoSuchAlgorithmException {
        // Act
        SecretKey result = SecurityUtils.generateKey(256);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAlgorithm()).isEqualTo("AES");
        assertThat(result.getEncoded()).hasSize(32); // 256 bits = 32 bytes
        assertThat(result.getFormat()).isEqualTo("RAW");
    }

    @Test
    @DisplayName("Should generate different keys on multiple calls")
    void generateKey_multipleCalls_generatesDifferentKeys() throws NoSuchAlgorithmException {
        // Act
        SecretKey key1 = SecurityUtils.generateKey(256);
        SecretKey key2 = SecurityUtils.generateKey(256);
        SecretKey key3 = SecurityUtils.generateKey(256);
        
        // Assert
        assertThat(key1.getEncoded()).isNotEqualTo(key2.getEncoded());
        assertThat(key1.getEncoded()).isNotEqualTo(key3.getEncoded());
        assertThat(key2.getEncoded()).isNotEqualTo(key3.getEncoded());
    }

    @Test
    @DisplayName("Should throw exception for invalid key size")
    void generateKey_invalidKeySize_throwsNoSuchAlgorithmException() {
        // Act & Assert
        assertThatThrownBy(() -> SecurityUtils.generateKey(127)) // Invalid AES key size
                .isInstanceOf(RuntimeException.class); // KeyGenerator will throw RuntimeException for invalid size
    }

    @Test
    @DisplayName("Should convert SecretKey to Base64 string")
    void keyToString_validSecretKey_returnsBase64String() throws NoSuchAlgorithmException {
        // Arrange
        SecretKey secretKey = SecurityUtils.generateKey(256);
        
        // Act
        String result = SecurityUtils.keyToString(secretKey);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result).matches("^[A-Za-z0-9+/]*={0,2}$"); // Valid Base64 pattern
        
        // Verify it's proper Base64 by decoding
        byte[] decoded = Base64.getDecoder().decode(result);
        assertThat(decoded).isEqualTo(secretKey.getEncoded());
    }

    @Test
    @DisplayName("Should handle null SecretKey in keyToString")
    void keyToString_nullSecretKey_throwsNullPointerException() {
        // Act & Assert
        assertThatThrownBy(() -> SecurityUtils.keyToString(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should generate secret key string with 256-bit size")
    void secretKey_noParameters_generates256BitKeyString() throws NoSuchAlgorithmException {
        // Act
        String result = SecurityUtils.secretKey();
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result).matches("^[A-Za-z0-9+/]*={0,2}$"); // Valid Base64 pattern
        
        // Verify it decodes to 32 bytes (256 bits)
        byte[] decoded = Base64.getDecoder().decode(result);
        assertThat(decoded).hasSize(32);
    }

    @Test
    @DisplayName("Should generate different secret key strings on multiple calls")
    void secretKey_multipleCalls_generatesDifferentStrings() throws NoSuchAlgorithmException {
        // Act
        String key1 = SecurityUtils.secretKey();
        String key2 = SecurityUtils.secretKey();
        String key3 = SecurityUtils.secretKey();
        
        // Assert
        assertThat(key1).isNotEqualTo(key2);
        assertThat(key1).isNotEqualTo(key3);
        assertThat(key2).isNotEqualTo(key3);
    }

    @Test
    @DisplayName("Should create HMAC-SHA256 key from encoded string")
    void getSignInKey_validEncodedKey_returnsHmacSHA256Key() throws NoSuchAlgorithmException {
        // Arrange
        String encodedKey = SecurityUtils.secretKey();
        
        // Act
        SecretKey result = SecurityUtils.getSignInKey(encodedKey);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAlgorithm()).isEqualTo("HmacSHA256");
        assertThat(result.getFormat()).isEqualTo("RAW");
        
        // Verify the key data matches the original
        byte[] originalKeyBytes = Base64.getDecoder().decode(encodedKey);
        assertThat(result.getEncoded()).isEqualTo(originalKeyBytes);
    }

    @Test
    @DisplayName("Should handle null encoded key in getSignInKey")
    void getSignInKey_nullEncodedKey_throwsNullPointerException() {
        // Act & Assert
        assertThatThrownBy(() -> SecurityUtils.getSignInKey(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should handle empty encoded key in getSignInKey")
    void getSignInKey_emptyEncodedKey_throwsIllegalArgumentException() {
        // Act & Assert
        assertThatThrownBy(() -> SecurityUtils.getSignInKey(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should handle invalid Base64 string in getSignInKey")
    void getSignInKey_invalidBase64_throwsIllegalArgumentException() {
        // Act & Assert
        assertThatThrownBy(() -> SecurityUtils.getSignInKey("invalid-base64!@#"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should perform successful roundtrip conversion")
    void roundtripConversion_keyToStringToKey_preservesKeyData() throws NoSuchAlgorithmException {
        // Arrange
        SecretKey originalKey = SecurityUtils.generateKey(256);
        
        // Act
        String keyString = SecurityUtils.keyToString(originalKey);
        SecretKey reconstructedKey = SecurityUtils.getSignInKey(keyString);
        
        // Assert
        assertThat(reconstructedKey.getEncoded()).isEqualTo(originalKey.getEncoded());
        assertThat(reconstructedKey.getAlgorithm()).isEqualTo("HmacSHA256"); // Different algorithm as expected
    }

    @Test
    @DisplayName("Should handle multiple roundtrip conversions")
    void roundtripConversion_multipleKeys_preservesAllKeyData() throws NoSuchAlgorithmException {
        // Arrange
        SecretKey[] originalKeys = {
            SecurityUtils.generateKey(128),
            SecurityUtils.generateKey(192),
            SecurityUtils.generateKey(256)
        };
        
        // Act & Assert
        for (SecretKey originalKey : originalKeys) {
            String keyString = SecurityUtils.keyToString(originalKey);
            SecretKey reconstructedKey = SecurityUtils.getSignInKey(keyString);
            
            assertThat(reconstructedKey.getEncoded()).isEqualTo(originalKey.getEncoded());
            assertThat(keyString).matches("^[A-Za-z0-9+/]*={0,2}$"); // Valid Base64
        }
    }

    @Test
    @DisplayName("Should generate keys with sufficient entropy")
    void generateKey_multipleGenerations_showsSufficientEntropy() throws NoSuchAlgorithmException {
        // Arrange
        Set<String> generatedKeys = new HashSet<>();
        int iterations = 100;
        
        // Act
        for (int i = 0; i < iterations; i++) {
            String keyString = SecurityUtils.secretKey();
            generatedKeys.add(keyString);
        }
        
        // Assert
        // All keys should be unique (very high probability with proper entropy)
        assertThat(generatedKeys).hasSize(iterations);
    }

    @Test
    @DisplayName("Should create consistent HMAC key from same encoded string")
    void getSignInKey_sameEncodedString_returnsConsistentKey() throws NoSuchAlgorithmException {
        // Arrange
        String encodedKey = SecurityUtils.secretKey();
        
        // Act
        SecretKey key1 = SecurityUtils.getSignInKey(encodedKey);
        SecretKey key2 = SecurityUtils.getSignInKey(encodedKey);
        
        // Assert
        assertThat(key1.getEncoded()).isEqualTo(key2.getEncoded());
        assertThat(key1.getAlgorithm()).isEqualTo(key2.getAlgorithm());
    }

    @Test
    @DisplayName("Should handle very short Base64 strings")
    void getSignInKey_shortBase64String_createsValidKey() {
        // Arrange
        String shortKey = Base64.getEncoder().encodeToString("test".getBytes());
        
        // Act
        SecretKey result = SecurityUtils.getSignInKey(shortKey);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAlgorithm()).isEqualTo("HmacSHA256");
        assertThat(result.getEncoded()).isEqualTo("test".getBytes());
    }

    @Test
    @DisplayName("Should handle very long Base64 strings")
    void getSignInKey_longBase64String_createsValidKey() {
        // Arrange
        byte[] longKeyBytes = new byte[512]; // 4096 bits
        for (int i = 0; i < longKeyBytes.length; i++) {
            longKeyBytes[i] = (byte) (i % 256);
        }
        String longKey = Base64.getEncoder().encodeToString(longKeyBytes);
        
        // Act
        SecretKey result = SecurityUtils.getSignInKey(longKey);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAlgorithm()).isEqualTo("HmacSHA256");
        assertThat(result.getEncoded()).isEqualTo(longKeyBytes);
    }

    @Test
    @DisplayName("Should handle Base64 strings with padding")
    void getSignInKey_base64WithPadding_createsValidKey() throws NoSuchAlgorithmException {
        // Arrange
        SecretKey originalKey = SecurityUtils.generateKey(256);
        String keyString = SecurityUtils.keyToString(originalKey);
        
        // Ensure we have a string that requires padding
        if (!keyString.endsWith("=")) {
            // Create a key that will have padding
            byte[] testBytes = "This string will need padding".getBytes();
            keyString = Base64.getEncoder().encodeToString(testBytes);
        }
        
        // Act
        SecretKey result = SecurityUtils.getSignInKey(keyString);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAlgorithm()).isEqualTo("HmacSHA256");
        
        // Verify roundtrip works
        byte[] originalBytes = Base64.getDecoder().decode(keyString);
        assertThat(result.getEncoded()).isEqualTo(originalBytes);
    }
}