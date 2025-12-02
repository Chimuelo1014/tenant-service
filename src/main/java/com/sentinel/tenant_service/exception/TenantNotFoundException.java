package com.sentinel.tenant_service.exception;

/**
 * Tenant not found exception.
 */
public class TenantNotFoundException extends RuntimeException {
    public TenantNotFoundException(String message) {
        super(message);
    }
}