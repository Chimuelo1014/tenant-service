package com.sentinel.tenant_service.enums;

/**
 * Roles de usuario dentro de un tenant.
 */
public enum TenantRole {
    /**
     * Administrador del tenant.
     * Puede gestionar miembros, configuración, billing.
     */
    TENANT_ADMIN,
    
    /**
     * Usuario regular del tenant.
     * Puede crear y gestionar proyectos.
     */
    TENANT_USER,
    
    /**
     * Usuario de solo lectura (futuro).
     */
    TENANT_VIEWER
}
