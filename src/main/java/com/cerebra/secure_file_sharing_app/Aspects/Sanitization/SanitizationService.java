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
            return input;
        }

        log.debug("Sanitizing input: {}", input);

        String sanitized = input;

        // Remove potential SQL injection patterns
        sanitized = sanitized.replaceAll("--", "")                     // SQL comments
                             .replaceAll(";(?=\\s|$)", "")              // Standalone semicolons
                             .replaceAll("'(?=\\s*(or|and|union|select|drop|insert|update|delete))", "")  // SQL keywords
                             .replaceAll("\\bunion\\b", "")             // UNION keyword
                             .replaceAll("\\bselect\\b", "")            // SELECT keyword
                             .replaceAll("\\bdrop\\b", "")              // DROP keyword
                             .replaceAll("\\binsert\\b", "")            // INSERT keyword
                             .replaceAll("\\bupdate\\b", "")            // UPDATE keyword
                             .replaceAll("\\bdelete\\b", "");           // DELETE keyword

        // XSS protection - escape HTML characters
        sanitized = sanitized.replaceAll("<", "&lt;")
                             .replaceAll(">", "&gt;")
                             .replaceAll("\"", "&quot;")
                             .replaceAll("'", "&#x27;")
                             .replaceAll("&(?![a-zA-Z]+;)", "&amp;");   // Don't double-encode

        // Remove excessive whitespace
        sanitized = sanitized.replaceAll("\\s+", " ").trim();

        if (!input.equals(sanitized)) {
            log.info("Input sanitized. Original length: {}, Sanitized length: {}", input.length(), sanitized.length());
        }

        return sanitized;
    }
}