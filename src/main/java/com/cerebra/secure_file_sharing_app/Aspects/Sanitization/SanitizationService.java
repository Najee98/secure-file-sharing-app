package com.cerebra.secure_file_sharing_app.Aspects.Sanitization;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SanitizationService {

    /**
     * Sanitizes the input to protect against SQL Injection and XSS.
     * @param input The raw input string.
     * @return A sanitized string.
     */
    public String sanitizeInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input == null ? null : "";
        }

        log.debug("Sanitizing input: {}", input);

        String sanitized = input;

        // Remove potential SQL injection patterns (case-insensitive)
        sanitized = sanitized.replaceAll("--", "")                     // SQL comments
                .replaceAll("(?i)\\b(union|select|drop|insert|update|delete|from)\\b", "")  // SQL keywords
                .replaceAll("'(?=\\s*(?i)(or|and|union|select|drop|insert|update|delete))", ""); // SQL injection attempts

        // XSS protection - escape HTML characters (do ' BEFORE & to avoid double encoding)
        sanitized = sanitized.replaceAll("'", "&#x27;")               // Do this FIRST
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;")
                .replaceAll("&(?![a-zA-Z0-9#]+;)", "&amp;");   // Don't double-encode existing entities

        // Remove semicolons ONLY if they're not part of HTML entities
        sanitized = sanitized.replaceAll("(?<!&[a-zA-Z0-9#]+);", "");  // Remove ; not preceded by HTML entity pattern

        // Remove excessive whitespace
        sanitized = sanitized.replaceAll("\\s+", " ").trim();

        if (!input.equals(sanitized)) {
            log.info("Input sanitized. Original length: {}, Sanitized length: {}", input.length(), sanitized.length());
        }

        return sanitized;
    }
}