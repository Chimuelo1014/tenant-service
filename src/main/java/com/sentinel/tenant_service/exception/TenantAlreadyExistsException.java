package com.sentinel.tenant_service.exception;

/**
 * Exception thrown when attempting to create a tenant that already exists.
 */
public class TenantAlreadyExistsException extends RuntimeException {
    public TenantAlreadyExistsException(String message) {
        super(message);
    }
}