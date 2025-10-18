package com.cerebra.secure_file_sharing_app.Security;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class SecurityUtils {

    public static SecretKey generateKey(int keySize) throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(keySize);
        return keyGenerator.generateKey();
    }

    public static String keyToString(SecretKey secretKey) {
        byte[] rawData = secretKey.getEncoded();
        return Base64.getEncoder().encodeToString(rawData);
    }

    public static String secretKey() throws NoSuchAlgorithmException {
        return keyToString(generateKey(256)); // Changed to 256 for better security
    }

    public static SecretKey getSignInKey(String encodedKey) {
        byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, "HmacSHA256");
    }
}