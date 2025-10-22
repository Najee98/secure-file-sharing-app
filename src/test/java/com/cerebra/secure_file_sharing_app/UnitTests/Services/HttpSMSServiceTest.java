package com.cerebra.secure_file_sharing_app.UnitTests.Services;

import com.cerebra.secure_file_sharing_app.Services.HttpSMSServiceImpl;
import com.cerebra.secure_file_sharing_app.Shared.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HttpSMSService Tests")
class HttpSMSServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private HttpSMSServiceImpl smsService;

    @BeforeEach
    void setUp() {
        smsService = new HttpSMSServiceImpl(restTemplate);
        
        // Set up configuration values
        ReflectionTestUtils.setField(smsService, "smsServiceUrl", "http://localhost:8081");
        ReflectionTestUtils.setField(smsService, "maxRetryAttempts", 3);
    }

    @Test
    @DisplayName("Should return success response when SMS service is available")
    void sendSMS_serviceAvailable_returnsSuccessResponse() {
        // Arrange
        String phoneNumber = "+1234567890";
        String message = "Test message";
        SMSResponse expectedResponse = SMSResponse.success("SMS sent successfully");
        
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(SMSResponse.class)))
                .thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));
        
        // Act
        SMSResponse result = smsService.sendSMS(phoneNumber, message);
        
        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("SMS sent successfully");
        
        // Verify RestTemplate was called exactly once
        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), eq(SMSResponse.class));
    }

    @Test
    @DisplayName("Should retry and fall back to console when service is unavailable")
    void sendSMS_serviceUnavailable_retriesAndFallsBack() {
        // Arrange
        String phoneNumber = "+1234567890";
        String message = "Test message";
        
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(SMSResponse.class)))
                .thenThrow(new RestClientException("Service unavailable"));
        
        // Act
        SMSResponse result = smsService.sendSMS(phoneNumber, message);
        
        // Assert
        assertThat(result.isSuccess()).isTrue(); // Fallback returns success
        assertThat(result.getMessage()).contains("SMS service unavailable");
        
        // Verify RestTemplate was called 3 times (max retries)
        verify(restTemplate, times(3)).postForEntity(anyString(), any(HttpEntity.class), eq(SMSResponse.class));
    }

    @Test
    @DisplayName("Should succeed on second retry attempt")
    void sendSMS_firstRetrySucceeds_returnsSuccessWithoutFallback() {
        // Arrange
        String phoneNumber = "+1234567890";
        String message = "Test message";
        SMSResponse successResponse = SMSResponse.success("SMS sent successfully");
        
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(SMSResponse.class)))
                .thenThrow(new RestClientException("Temporary failure"))
                .thenReturn(new ResponseEntity<>(successResponse, HttpStatus.OK));
        
        // Act
        SMSResponse result = smsService.sendSMS(phoneNumber, message);
        
        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("SMS sent successfully");
        
        // Verify RestTemplate was called exactly 2 times (failed once, succeeded on retry)
        verify(restTemplate, times(2)).postForEntity(anyString(), any(HttpEntity.class), eq(SMSResponse.class));
    }

    @Test
    @DisplayName("Should handle SMS service returning error response")
    void sendSMS_serviceReturnsError_retriesAndFallsBack() {
        // Arrange
        String phoneNumber = "+1234567890";
        String message = "Test message";
        SMSResponse errorResponse = SMSResponse.failure("SMS sending failed");
        
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(SMSResponse.class)))
                .thenReturn(new ResponseEntity<>(errorResponse, HttpStatus.OK));
        
        // Act
        SMSResponse result = smsService.sendSMS(phoneNumber, message);
        
        // Assert
        assertThat(result.isSuccess()).isTrue(); // Fallback returns success
        assertThat(result.getMessage()).contains("SMS service unavailable");
        
        // Verify all retry attempts were made
        verify(restTemplate, times(3)).postForEntity(anyString(), any(HttpEntity.class), eq(SMSResponse.class));
    }

    @Test
    @DisplayName("Should use correct URL and request structure")
    void sendSMS_validRequest_usesCorrectUrlAndRequestStructure() {
        // Arrange
        String phoneNumber = "+1234567890";
        String message = "Test message";
        SMSResponse successResponse = SMSResponse.success("SMS sent successfully");
        
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(SMSResponse.class)))
                .thenReturn(new ResponseEntity<>(successResponse, HttpStatus.OK));
        
        // Act
        smsService.sendSMS(phoneNumber, message);
        
        // Assert
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        
        verify(restTemplate).postForEntity(urlCaptor.capture(), entityCaptor.capture(), eq(SMSResponse.class));
        
        // Verify URL
        assertThat(urlCaptor.getValue()).isEqualTo("http://localhost:8081/api/sms/send");
        
        // Verify request structure
        HttpEntity<SMSRequest> capturedEntity = entityCaptor.getValue();
        SMSRequest capturedRequest = capturedEntity.getBody();
        assertThat(capturedRequest.getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(capturedRequest.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("Should handle null phone number gracefully")
    void sendSMS_nullPhoneNumber_handlesGracefully() {
        // Arrange
        String phoneNumber = null;
        String message = "Test message";
        
        // Act
        SMSResponse result = smsService.sendSMS(phoneNumber, message);
        
        // Assert
        assertThat(result.isSuccess()).isTrue(); // Should still return success (fallback)
        
        // SMS service should still be called (let service handle validation)
        verify(restTemplate, atLeast(1)).postForEntity(anyString(), any(HttpEntity.class), eq(SMSResponse.class));
    }

    @Test
    @DisplayName("Should handle empty message gracefully")
    void sendSMS_emptyMessage_handlesGracefully() {
        // Arrange
        String phoneNumber = "+1234567890";
        String message = "";
        SMSResponse successResponse = SMSResponse.success("SMS sent successfully");
        
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(SMSResponse.class)))
                .thenReturn(new ResponseEntity<>(successResponse, HttpStatus.OK));
        
        // Act
        SMSResponse result = smsService.sendSMS(phoneNumber, message);
        
        // Assert
        assertThat(result.isSuccess()).isTrue();
        verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(SMSResponse.class));
    }

    @Test
    @DisplayName("Should handle null response from SMS service")
    void sendSMS_nullResponse_handlesGracefully() {
        // Arrange
        String phoneNumber = "+1234567890";
        String message = "Test message";
        
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(SMSResponse.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));
        
        // Act
        SMSResponse result = smsService.sendSMS(phoneNumber, message);
        
        // Assert
        assertThat(result.isSuccess()).isTrue(); // Should fall back to console logging
        verify(restTemplate, times(3)).postForEntity(anyString(), any(HttpEntity.class), eq(SMSResponse.class));
    }

    @Test
    @DisplayName("Should implement exponential backoff between retries")
    void sendSMS_multipleFailures_implementsExponentialBackoff() {
        // Arrange
        String phoneNumber = "+1234567890";
        String message = "Test message";

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(SMSResponse.class)))
                .thenThrow(new RestClientException("Service unavailable"));

        // Act
        long startTime = System.currentTimeMillis();
        smsService.sendSMS(phoneNumber, message);
        long endTime = System.currentTimeMillis();

        // Assert
        long totalTime = endTime - startTime;
        // With exponential backoff: 1s + 2s = 3s minimum (plus processing time)
        // Check for at least 2.5 seconds to account for test execution time
        assertThat(totalTime).isGreaterThan(2500);
        assertThat(totalTime).isLessThan(5000); // Upper bound to ensure it's not too slow

        verify(restTemplate, times(3)).postForEntity(anyString(), any(HttpEntity.class), eq(SMSResponse.class));
    }

    @Test
    @DisplayName("Should respect configured retry attempts")
    void sendSMS_customRetryAttempts_respectsConfiguration() {
        // Arrange
        ReflectionTestUtils.setField(smsService, "maxRetryAttempts", 2); // Change to 2 retries
        
        String phoneNumber = "+1234567890";
        String message = "Test message";
        
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(SMSResponse.class)))
                .thenThrow(new RestClientException("Service unavailable"));
        
        // Act
        smsService.sendSMS(phoneNumber, message);
        
        // Assert
        verify(restTemplate, times(2)).postForEntity(anyString(), any(HttpEntity.class), eq(SMSResponse.class));
    }

    @Test
    @DisplayName("Should handle HTTP timeout exceptions")
    void sendSMS_timeoutException_retriesAndFallsBack() {
        // Arrange
        String phoneNumber = "+1234567890";
        String message = "Test message";
        
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(SMSResponse.class)))
                .thenThrow(new RestClientException("Read timeout"));
        
        // Act
        SMSResponse result = smsService.sendSMS(phoneNumber, message);
        
        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).contains("SMS service unavailable");
        verify(restTemplate, times(3)).postForEntity(anyString(), any(HttpEntity.class), eq(SMSResponse.class));
    }

    @Test
    @DisplayName("Should not retry when Thread is interrupted")
    void sendSMS_threadInterrupted_stopsRetrying() {
        // Arrange
        String phoneNumber = "+1234567890";
        String message = "Test message";
        
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(SMSResponse.class)))
                .thenThrow(new RestClientException("Service unavailable"));
        
        // Interrupt current thread to simulate interruption during sleep
        Thread.currentThread().interrupt();
        
        // Act
        SMSResponse result = smsService.sendSMS(phoneNumber, message);
        
        // Assert
        assertThat(result.isSuccess()).isTrue(); // Should still return success via fallback
        
        // Should have attempted at least once, but may not complete all retries due to interruption
        verify(restTemplate, atLeast(1)).postForEntity(anyString(), any(HttpEntity.class), eq(SMSResponse.class));
        
        // Clear interrupted status
        Thread.interrupted();
    }
}