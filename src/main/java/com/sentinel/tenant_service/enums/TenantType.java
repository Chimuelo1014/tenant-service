package com.sentinel.tenant_service.enums;

/**
 * Tipos de tenant según el perfil del cliente.
 */
public enum TenantType {
    
    /**
     * Workspace personal (persona natural).
     * - 1 usuario por defecto (el owner)
     * - No requiere NIT
     */
    PERSONAL,
    
    /**
     * Workspace empresarial (persona jurídica).
     * - Múltiples usuarios
     * - Requiere NIT válido (Colombia)
     * - business_name obligatorio
     */
    BUSINESS
}