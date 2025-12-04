package com.sentinel.tenant_service.service;

import com.sentinel.tenant_service.dto.request.InviteMemberRequest;
import com.sentinel.tenant_service.dto.response.InvitationDTO;

import java.util.List;
import java.util.UUID;

public interface TenantInvitationService {
    
    /**
     * Enviar invitación a un usuario para unirse al tenant.
     */
    InvitationDTO inviteMember(UUID tenantId, InviteMemberRequest request, UUID invitedByUserId);
    
    /**
     * Aceptar invitación.
     */
    void acceptInvitation(String token, UUID userId);
    
    /**
     * Rechazar invitación.
     */
    void rejectInvitation(String token, UUID userId);
    
    /**
     * Cancelar invitación (solo quien invitó o admin).
     */
    void cancelInvitation(UUID invitationId, UUID userId);
    
    /**
     * Obtener invitaciones pendientes para un usuario.
     */
    List<InvitationDTO> getPendingInvitations(String email);
    
    /**
     * Obtener invitaciones de un tenant.
     */
    List<InvitationDTO> getTenantInvitations(UUID tenantId);
}