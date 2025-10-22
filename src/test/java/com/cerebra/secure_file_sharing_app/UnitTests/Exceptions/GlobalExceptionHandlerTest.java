package com.cerebra.secure_file_sharing_app.UnitTests.Exceptions;

import com.cerebra.secure_file_sharing_app.Exceptions.CustomExceptions.*;
import com.cerebra.secure_file_sharing_app.Exceptions.ErrorResponse;
import com.cerebra.secure_file_sharing_app.Exceptions.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    @Mock private WebRequest webRequest;
    @Mock private MethodArgumentNotValidException methodArgumentNotValidException;
    @Mock private BindingResult bindingResult;

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");
    }

    @Test
    @DisplayName("Should handle InvalidOTPException with UNAUTHORIZED status")
    void handleInvalidOTP_invalidOTPException_returnsUnauthorizedResponse() {
        // Arrange
        InvalidOTPException exception = new InvalidOTPException("Invalid OTP provided");
        
        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleInvalidOTP(exception, webRequest);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid or incorrect OTP");
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
        assertThat(response.getBody().getTimestamp()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should handle OTPExpiredException with UNAUTHORIZED status")
    void handleOTPExpired_otpExpiredException_returnsUnauthorizedResponse() {
        // Arrange
        OTPExpiredException exception = new OTPExpiredException("OTP has expired");
        
        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleOTPExpired(exception, webRequest);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("OTP has expired. Please request a new one");
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("Should handle FileNotFoundException with NOT_FOUND status")
    void handleNotFound_fileNotFoundException_returnsNotFoundResponse() {
        // Arrange
        FileNotFoundException exception = new FileNotFoundException("File not found");
        
        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleNotFound(exception, webRequest);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Requested resource not found");
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("Should handle FolderNotFoundException with NOT_FOUND status")
    void handleNotFound_folderNotFoundException_returnsNotFoundResponse() {
        // Arrange
        FolderNotFoundException exception = new FolderNotFoundException("Folder not found");
        
        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleNotFound(exception, webRequest);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Requested resource not found");
        assertThat(response.getBody().getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("Should handle ShareNotFoundException with NOT_FOUND status")
    void handleNotFound_shareNotFoundException_returnsNotFoundResponse() {
        // Arrange
        ShareNotFoundException exception = new ShareNotFoundException("Share not found");
        
        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleNotFound(exception, webRequest);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Requested resource not found");
        assertThat(response.getBody().getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("Should handle FileAccessDeniedException with FORBIDDEN status")
    void handleAccessDenied_fileAccessDeniedException_returnsForbiddenResponse() {
        // Arrange
        FileAccessDeniedException exception = new FileAccessDeniedException("Access denied to file");
        
        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleAccessDenied(exception, webRequest);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Access denied to the requested resource");
        assertThat(response.getBody().getStatus()).isEqualTo(403);
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("Should handle FolderAccessDeniedException with FORBIDDEN status")
    void handleAccessDenied_folderAccessDeniedException_returnsForbiddenResponse() {
        // Arrange
        FolderAccessDeniedException exception = new FolderAccessDeniedException("Access denied to folder");
        
        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleAccessDenied(exception, webRequest);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Access denied to the requested resource");
        assertThat(response.getBody().getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("Should handle Spring Security AccessDeniedException with FORBIDDEN status")
    void handleAccessDenied_springSecurityAccessDeniedException_returnsForbiddenResponse() {
        // Arrange
        AccessDeniedException exception = new AccessDeniedException("Access is denied");
        
        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleAccessDenied(exception, webRequest);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Access denied to the requested resource");
        assertThat(response.getBody().getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("Should handle FileStorageException with BAD_REQUEST status")
    void handleBadRequest_fileStorageException_returnsBadRequestResponse() {
        // Arrange
        FileStorageException exception = new FileStorageException("Cannot store empty file");
        
        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBadRequest(exception, webRequest);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Cannot store empty file");
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("Should handle InvalidFolderNameException with BAD_REQUEST status")
    void handleBadRequest_invalidFolderNameException_returnsBadRequestResponse() {
        // Arrange
        InvalidFolderNameException exception = new InvalidFolderNameException("Invalid folder name");
        
        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBadRequest(exception, webRequest);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid folder name");
        assertThat(response.getBody().getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("Should handle ShareExpiredException with GONE status")
    void handleShareExpired_shareExpiredException_returnsGoneResponse() {
        // Arrange
        ShareExpiredException exception = new ShareExpiredException("Share has expired");
        
        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleShareExpired(exception, webRequest);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Share link has expired");
        assertThat(response.getBody().getStatus()).isEqualTo(410);
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("Should handle MaxUploadSizeExceededException with PAYLOAD_TOO_LARGE status")
    void handleMaxUploadSize_maxUploadSizeExceededException_returnsPayloadTooLargeResponse() {
        // Arrange
        MaxUploadSizeExceededException exception = new MaxUploadSizeExceededException(10485760L);
        
        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMaxUploadSize(exception, webRequest);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("File size exceeds maximum allowed limit");
        assertThat(response.getBody().getStatus()).isEqualTo(413);
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException with validation errors")
    void handleValidationErrors_methodArgumentNotValidException_returnsBadRequestWithValidationErrors() {
        // Arrange
        FieldError fieldError1 = new FieldError("user", "phoneNumber", "Phone number is required");
        FieldError fieldError2 = new FieldError("user", "name", "Name cannot be blank");
        
        when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(Arrays.asList(fieldError1, fieldError2));
        
        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidationErrors(methodArgumentNotValidException, webRequest);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
        
        Map<String, String> validationErrors = response.getBody().getValidationErrors();
        assertThat(validationErrors).isNotNull();
        assertThat(validationErrors).hasSize(2);
        assertThat(validationErrors.get("phoneNumber")).isEqualTo("Phone number is required");
        assertThat(validationErrors.get("name")).isEqualTo("Name cannot be blank");
    }

    @Test
    @DisplayName("Should handle generic Exception with INTERNAL_SERVER_ERROR status")
    void handleGeneral_genericException_returnsInternalServerErrorResponse() {
        // Arrange
        Exception exception = new RuntimeException("Unexpected database error");
        
        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGeneral(exception, webRequest);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
        assertThat(response.getBody().getValidationErrors()).isNull();
    }

    @Test
    @DisplayName("Should extract path correctly from WebRequest")
    void getPath_webRequestWithUri_extractsPathCorrectly() {
        // Arrange
        when(webRequest.getDescription(false)).thenReturn("uri=/api/files/upload");
        FileStorageException exception = new FileStorageException("Test error");
        
        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBadRequest(exception, webRequest);
        
        // Assert
        assertThat(response.getBody().getPath()).isEqualTo("/api/files/upload");
    }

    @Test
    @DisplayName("Should handle WebRequest without uri prefix")
    void getPath_webRequestWithoutUriPrefix_returnsAsIs() {
        // Arrange
        when(webRequest.getDescription(false)).thenReturn("/api/test/path");
        FileStorageException exception = new FileStorageException("Test error");
        
        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBadRequest(exception, webRequest);
        
        // Assert
        assertThat(response.getBody().getPath()).isEqualTo("/api/test/path");
    }

    @Test
    @DisplayName("Should set timestamp within reasonable time range")
    void handleException_anyException_setsReasonableTimestamp() {
        // Arrange
        long beforeTimestamp = System.currentTimeMillis();
        FileStorageException exception = new FileStorageException("Test error");
        
        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBadRequest(exception, webRequest);
        long afterTimestamp = System.currentTimeMillis();
        
        // Assert
        assertThat(response.getBody().getTimestamp()).isBetween(beforeTimestamp, afterTimestamp);
    }

    @Test
    @DisplayName("Should handle null exception message gracefully")
    void handleBadRequest_exceptionWithNullMessage_handlesGracefully() {
        // Arrange
        FileStorageException exception = new FileStorageException(null);
        
        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBadRequest(exception, webRequest);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isNull(); // Should preserve null message
        assertThat(response.getBody().getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("Should handle validation errors with empty binding result")
    void handleValidationErrors_emptyBindingResult_returnsValidationFailedResponse() {
        // Arrange
        when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(Arrays.asList());
        
        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidationErrors(methodArgumentNotValidException, webRequest);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(response.getBody().getValidationErrors()).isEmpty();
    }
}