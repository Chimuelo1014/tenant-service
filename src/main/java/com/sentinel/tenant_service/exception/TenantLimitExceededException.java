package com.sentinel.tenant_service.exception;

/**
 * Exception thrown when a tenant exceeds their plan limits.
 */
public class TenantLimitExceededException extends RuntimeException {
    public TenantLimitExceededException(String message) {
        super(message);
    }
}