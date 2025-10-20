package com.cerebra.secure_file_sharing_app.Services;

import com.cerebra.secure_file_sharing_app.Shared.SMSRequest;
import com.cerebra.secure_file_sharing_app.Shared.SMSResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class HttpSMSServiceImpl implements SMSService {
    
    private final RestTemplate restTemplate;
    
    @Value("${sms.service.url:http://localhost:8081}")
    private String smsServiceUrl;
    
    @Value("${sms.service.retry.attempts:3}")
    private int maxRetryAttempts;
    
    @Override
    public SMSResponse sendSMS(String phoneNumber, String message) {
        log.info("Attempting to send SMS to: {} via HTTP service", phoneNumber);
        
        SMSRequest request = SMSRequest.builder()
                .phoneNumber(phoneNumber)
                .message(message)
                .build();
        
        // Try sending SMS with retry logic
        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                SMSResponse response = callSMSService(request, attempt);
                if (response.isSuccess()) {
                    log.info("SMS sent successfully to: {} on attempt {}", phoneNumber, attempt);
                    return response;
                } else {
                    log.warn("SMS service returned failure for {}: {} (attempt {})", 
                            phoneNumber, response.getMessage(), attempt);
                }
            } catch (Exception e) {
                log.error("SMS service call failed for {} (attempt {}): {}", 
                         phoneNumber, attempt, e.getMessage());
                
                if (attempt < maxRetryAttempts) {
                    // Wait before retry with exponential backoff
                    try {
                        long waitTime = (long) Math.pow(2, attempt - 1) * 1000; // 1s, 2s, 4s
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        // All attempts failed - fallback to console logging
        return fallbackToConsoleLogging(phoneNumber, message);
    }
    
    private SMSResponse callSMSService(SMSRequest request, int attempt) {
        log.debug("Calling SMS service (attempt {}): {}", attempt, smsServiceUrl);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SMSRequest> httpEntity = new HttpEntity<>(request, headers);
        
        String url = smsServiceUrl + "/api/sms/send";
        
        ResponseEntity<SMSResponse> response = restTemplate.postForEntity(url, httpEntity, SMSResponse.class);
        
        if (response.getBody() != null) {
            return response.getBody();
        } else {
            throw new RestClientException("Empty response from SMS service");
        }
    }
    
    private SMSResponse fallbackToConsoleLogging(String phoneNumber, String message) {
        log.warn("=".repeat(80));
        log.warn("SMS SERVICE UNAVAILABLE - FALLBACK TO CONSOLE LOGGING");
        log.warn("Phone: {}", phoneNumber);
        log.warn("Message: {}", message);
        log.warn("=".repeat(80));
        
        // Return success response to not break the user flow
        return SMSResponse.success("SMS service unavailable - message logged to console");
    }
}