package com.cerebra.secure_file_sharing_app.Aspects.Sanitization;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Aspect
@Component
@Order(1) // Execute before validation aspects
@RequiredArgsConstructor
@Slf4j
public class SanitizationAspect {

    private final SanitizationService sanitizationService;

    /**
     * Intercepts controller method calls and sanitizes request body objects
     */
    @Around("@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PatchMapping)")
    public Object sanitizeRequestBody(ProceedingJoinPoint joinPoint) throws Throwable {
        
        Object[] args = joinPoint.getArgs();
        
        // Sanitize each argument that might contain user input
        for (Object arg : args) {
            if (arg != null && !isPrimitiveOrWrapper(arg.getClass())) {
                sanitizeObject(arg);
            }
        }
        
        return joinPoint.proceed(args);
    }

    /**
     * Sanitizes fields marked with @SanitizedField annotation
     */
    private void sanitizeObject(Object inputObject) {
        if (inputObject == null) {
            return;
        }

        Class<?> clazz = inputObject.getClass();
        log.debug("Sanitizing object of type: {}", clazz.getSimpleName());

        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(SanitizedField.class)) {
                sanitizeField(inputObject, field);
            }
        }
    }

    /**
     * Sanitizes a specific field
     */
    private void sanitizeField(Object object, Field field) {
        try {
            field.setAccessible(true);
            Object fieldValue = field.get(object);
            
            if (fieldValue instanceof String) {
                String originalValue = (String) fieldValue;
                String sanitizedValue = sanitizationService.sanitizeInput(originalValue);
                field.set(object, sanitizedValue);
                
                log.debug("Sanitized field '{}' in {}", field.getName(), object.getClass().getSimpleName());
            }
        } catch (IllegalAccessException e) {
            log.error("Failed to sanitize field '{}' in {}: {}", 
                     field.getName(), object.getClass().getSimpleName(), e.getMessage());
            // Continue processing - don't break the flow for sanitization failures
        } catch (Exception e) {
            log.error("Unexpected error during sanitization of field '{}': {}", 
                     field.getName(), e.getMessage());
            // Continue processing
        }
    }

    /**
     * Checks if a class is a primitive or wrapper type
     */
    private boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() || 
               clazz == String.class ||
               clazz == Integer.class ||
               clazz == Long.class ||
               clazz == Double.class ||
               clazz == Float.class ||
               clazz == Boolean.class ||
               clazz == Character.class ||
               clazz == Byte.class ||
               clazz == Short.class;
    }
}