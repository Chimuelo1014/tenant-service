package com.sentinel.tenant_service.exception;

/**
 * Exception thrown when attempting to access a suspended tenant.
 */
public class TenantSuspendedException extends RuntimeException {
    public TenantSuspendedException(String message) {
        super(message);
    }
}