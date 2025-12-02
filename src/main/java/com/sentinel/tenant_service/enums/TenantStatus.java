package com.sentinel.tenant_service.enums;

/**
 * Estados posibles de un tenant en el sistema.
 */
public enum TenantStatus {
    
    /**
     * Tenant activo y operativo.
     */
    ACTIVE,
    
    /**
     * Tenant suspendido temporalmente (por falta de pago, violación de términos, etc.).
     */
    SUSPENDED,
    
    /**
     * Tenant eliminado (soft delete).
     */
    DELETED
}