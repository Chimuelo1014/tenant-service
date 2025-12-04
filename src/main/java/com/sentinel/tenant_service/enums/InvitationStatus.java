
package com.sentinel.tenant_service.enums;

/**
 * Estados de una invitación a tenant.
 */
public enum InvitationStatus {
    /**
     * Invitación enviada, esperando respuesta.
     */
    PENDING,
    
    /**
     * Invitación aceptada por el usuario.
     */
    ACCEPTED,
    
    /**
     * Invitación rechazada por el usuario.
     */
    REJECTED,
    
    /**
     * Invitación cancelada por el invitador.
     */
    CANCELLED,
    
    /**
     * Invitación expirada (sin respuesta).
     */
    EXPIRED
}