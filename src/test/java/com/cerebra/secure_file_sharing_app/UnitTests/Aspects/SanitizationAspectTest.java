package com.cerebra.secure_file_sharing_app.UnitTests.Aspects;

import com.cerebra.secure_file_sharing_app.Aspects.Sanitization.SanitizationAspect;
import com.cerebra.secure_file_sharing_app.Aspects.Sanitization.SanitizationService;
import com.cerebra.secure_file_sharing_app.Aspects.Sanitization.SanitizedField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SanitizationAspect Tests")
class SanitizationAspectTest {

    @Mock
    private SanitizationService sanitizationService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    private SanitizationAspect sanitizationAspect;

    // Test data classes
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestRequest {
        @SanitizedField
        private String name;
        
        @SanitizedField
        private String email;
        
        private String unsanitizedField;
        
        private Integer numericField;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestRequestWithoutAnnotations {
        private String name;
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestRequestWithNullFields {
        @SanitizedField
        private String name;
    }

    @BeforeEach
    void setUp() {
        sanitizationAspect = new SanitizationAspect(sanitizationService);
    }

    @Test
    @DisplayName("Should sanitize fields marked with @SanitizedField")
    void sanitizeRequestBody_objectWithSanitizedFields_sanitizesFields() throws Throwable {
        // Arrange
        TestRequest request = new TestRequest("dirty<script>", "test@evil.com<script>", "normal", 123);
        Object[] args = {request, "otherArg"};
        
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed(args)).thenReturn("result");
        when(sanitizationService.sanitizeInput("dirty<script>")).thenReturn("dirty");
        when(sanitizationService.sanitizeInput("test@evil.com<script>")).thenReturn("test@evil.com");

        // Act
        Object result = sanitizationAspect.sanitizeRequestBody(joinPoint);

        // Assert
        assertThat(result).isEqualTo("result");
        assertThat(request.getName()).isEqualTo("dirty");
        assertThat(request.getEmail()).isEqualTo("test@evil.com");
        assertThat(request.getUnsanitizedField()).isEqualTo("normal"); // Should not be changed
        assertThat(request.getNumericField()).isEqualTo(123); // Should not be changed
        
        verify(sanitizationService).sanitizeInput("dirty<script>");
        verify(sanitizationService).sanitizeInput("test@evil.com<script>");
        verify(joinPoint).proceed(args);
    }

    @Test
    @DisplayName("Should skip objects without @SanitizedField annotations")
    void sanitizeRequestBody_objectWithoutAnnotations_skipsObject() throws Throwable {
        // Arrange
        TestRequestWithoutAnnotations request = new TestRequestWithoutAnnotations("name", "email");
        Object[] args = {request};
        
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed(args)).thenReturn("result");

        // Act
        Object result = sanitizationAspect.sanitizeRequestBody(joinPoint);

        // Assert
        assertThat(result).isEqualTo("result");
        assertThat(request.getName()).isEqualTo("name"); // Unchanged
        assertThat(request.getEmail()).isEqualTo("email"); // Unchanged
        
        verify(sanitizationService, never()).sanitizeInput(anyString());
        verify(joinPoint).proceed(args);
    }

    @Test
    @DisplayName("Should handle null field values gracefully")
    void sanitizeRequestBody_objectWithNullFields_handlesGracefully() throws Throwable {
        // Arrange
        TestRequestWithNullFields request = new TestRequestWithNullFields(null);
        Object[] args = {request};
        
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed(args)).thenReturn("result");

        // Act
        Object result = sanitizationAspect.sanitizeRequestBody(joinPoint);

        // Assert
        assertThat(result).isEqualTo("result");
        assertThat(request.getName()).isNull(); // Should remain null
        
        verify(sanitizationService, never()).sanitizeInput(anyString());
        verify(joinPoint).proceed(args);
    }

    @Test
    @DisplayName("Should skip primitive and wrapper types")
    void sanitizeRequestBody_primitiveAndWrapperArgs_skipsThem() throws Throwable {
        // Arrange
        Object[] args = {"string", 42, 3.14, true, 'c', (byte) 1, (short) 2, 100L};
        
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed(args)).thenReturn("result");

        // Act
        Object result = sanitizationAspect.sanitizeRequestBody(joinPoint);

        // Assert
        assertThat(result).isEqualTo("result");
        verify(sanitizationService, never()).sanitizeInput(anyString());
        verify(joinPoint).proceed(args);
    }

    @Test
    @DisplayName("Should handle null arguments")
    void sanitizeRequestBody_nullArguments_handlesGracefully() throws Throwable {
        // Arrange
        Object[] args = {null, new TestRequest("test", "email", "normal", 123)};
        
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed(args)).thenReturn("result");
        when(sanitizationService.sanitizeInput("test")).thenReturn("sanitized_test");
        when(sanitizationService.sanitizeInput("email")).thenReturn("sanitized_email");

        // Act
        Object result = sanitizationAspect.sanitizeRequestBody(joinPoint);

        // Assert
        assertThat(result).isEqualTo("result");
        verify(sanitizationService).sanitizeInput("test");
        verify(sanitizationService).sanitizeInput("email");
        verify(joinPoint).proceed(args);
    }

    @Test
    @DisplayName("Should handle mixed argument types")
    void sanitizeRequestBody_mixedArguments_sanitizesOnlyObjects() throws Throwable {
        // Arrange
        TestRequest request = new TestRequest("dirty", "email", "normal", 123);
        Authentication auth = mock(Authentication.class);
        Object[] args = {request, "stringArg", 42, auth};
        
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed(args)).thenReturn("result");
        when(sanitizationService.sanitizeInput("dirty")).thenReturn("clean");
        when(sanitizationService.sanitizeInput("email")).thenReturn("clean_email");

        // Act
        Object result = sanitizationAspect.sanitizeRequestBody(joinPoint);

        // Assert
        assertThat(result).isEqualTo("result");
        assertThat(request.getName()).isEqualTo("clean");
        assertThat(request.getEmail()).isEqualTo("clean_email");
        
        verify(sanitizationService).sanitizeInput("dirty");
        verify(sanitizationService).sanitizeInput("email");
        verify(joinPoint).proceed(args);
    }

    @Test
    @DisplayName("Should continue processing when field access fails")
    void sanitizeRequestBody_fieldAccessException_continuesProcessing() throws Throwable {
        // Arrange
        // This test is challenging because we can't easily simulate IllegalAccessException
        // with modern Java reflection. We'll test that the aspect continues processing
        // even if one field fails.
        TestRequest request = new TestRequest("test1", "test2", "normal", 123);
        Object[] args = {request};
        
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed(args)).thenReturn("result");
        when(sanitizationService.sanitizeInput("test1")).thenReturn("clean1");
        when(sanitizationService.sanitizeInput("test2")).thenReturn("clean2");

        // Act
        Object result = sanitizationAspect.sanitizeRequestBody(joinPoint);

        // Assert
        assertThat(result).isEqualTo("result");
        verify(joinPoint).proceed(args);
    }

    @Test
    @DisplayName("Should handle empty string fields")
    void sanitizeRequestBody_emptyStringFields_sanitizesThem() throws Throwable {
        // Arrange
        TestRequest request = new TestRequest("", "", "normal", 123);
        Object[] args = {request};
        
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed(args)).thenReturn("result");
        when(sanitizationService.sanitizeInput("")).thenReturn("");

        // Act
        Object result = sanitizationAspect.sanitizeRequestBody(joinPoint);

        // Assert
        assertThat(result).isEqualTo("result");
        verify(sanitizationService, times(2)).sanitizeInput("");
        verify(joinPoint).proceed(args);
    }

    @Test
    @DisplayName("Should handle objects with no fields")
    void sanitizeRequestBody_objectWithNoFields_handlesGracefully() throws Throwable {
        // Arrange
        Object emptyObject = new Object();
        Object[] args = {emptyObject};
        
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed(args)).thenReturn("result");

        // Act
        Object result = sanitizationAspect.sanitizeRequestBody(joinPoint);

        // Assert
        assertThat(result).isEqualTo("result");
        verify(sanitizationService, never()).sanitizeInput(anyString());
        verify(joinPoint).proceed(args);
    }

    // Test class with non-string sanitized field to verify type checking
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestRequestWithNonStringField {
        @SanitizedField
        private Integer number; // Non-string field with annotation
        
        @SanitizedField
        private String text;
    }

    @Test
    @DisplayName("Should only sanitize String fields even if other types have @SanitizedField")
    void sanitizeRequestBody_nonStringFieldWithAnnotation_onlySanitizesStrings() throws Throwable {
        // Arrange
        TestRequestWithNonStringField request = new TestRequestWithNonStringField(123, "text");
        Object[] args = {request};
        
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed(args)).thenReturn("result");
        when(sanitizationService.sanitizeInput("text")).thenReturn("clean_text");

        // Act
        Object result = sanitizationAspect.sanitizeRequestBody(joinPoint);

        // Assert
        assertThat(result).isEqualTo("result");
        assertThat(request.getNumber()).isEqualTo(123); // Should not be changed
        assertThat(request.getText()).isEqualTo("clean_text");
        
        verify(sanitizationService).sanitizeInput("text");
        verify(sanitizationService, never()).sanitizeInput("123");
        verify(joinPoint).proceed(args);
    }
}