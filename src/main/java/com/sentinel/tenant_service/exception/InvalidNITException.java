package com.sentinel.tenant_service.exception;

/**
 * Exception thrown when NIT validation fails.
 */
public class InvalidNITException extends RuntimeException {
    public InvalidNITException(String message) {
        super(message);
    }
}