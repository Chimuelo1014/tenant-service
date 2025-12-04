package com.sentinel.tenant_service.controller;

import com.sentinel.tenant_service.dto.request.InviteMemberRequest;
import com.sentinel.tenant_service.dto.response.InvitationDTO;
import com.sentinel.tenant_service.service.TenantInvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller para gestión de invitaciones a tenants.
 */
@Slf4j
@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantInvitationController {

    private final TenantInvitationService invitationService;

    /**
     * Invitar miembro a un tenant.
     * POST /api/tenants/{tenantId}/invitations
     */
    @PostMapping("/{tenantId}/invitations")
    public ResponseEntity<InvitationDTO> inviteMember(
            @PathVariable UUID tenantId,
            @Valid @RequestBody InviteMemberRequest request,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        log.info("Inviting member to tenant: {}", tenantId);
        InvitationDTO invitation = invitationService.inviteMember(tenantId, request, userId);
        return ResponseEntity.ok(invitation);
    }

    /**
     * Obtener invitaciones pendientes del usuario actual.
     * GET /api/tenants/invitations/pending
     */
    @GetMapping("/invitations/pending")
    public ResponseEntity<List<InvitationDTO>> getPendingInvitations(
            @RequestHeader("X-User-Email") String email
    ) {
        log.info("Getting pending invitations for: {}", email);
        List<InvitationDTO> invitations = invitationService.getPendingInvitations(email);
        return ResponseEntity.ok(invitations);
    }

    /**
     * Aceptar invitación.
     * POST /api/tenants/invitations/{token}/accept
     */
    @PostMapping("/invitations/{token}/accept")
    public ResponseEntity<Map<String, String>> acceptInvitation(
            @PathVariable String token,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        log.info("Accepting invitation: {}", token);
        invitationService.acceptInvitation(token, userId);
        return ResponseEntity.ok(Map.of("message", "Invitation accepted successfully"));
    }

    /**
     * Rechazar invitación.
     * POST /api/tenants/invitations/{token}/reject
     */
    @PostMapping("/invitations/{token}/reject")
    public ResponseEntity<Map<String, String>> rejectInvitation(
            @PathVariable String token,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        log.info("Rejecting invitation: {}", token);
        invitationService.rejectInvitation(token, userId);
        return ResponseEntity.ok(Map.of("message", "Invitation rejected"));
    }

    /**
     * Cancelar invitación (solo admin).
     * DELETE /api/tenants/invitations/{invitationId}
     */
    @DeleteMapping("/invitations/{invitationId}")
    public ResponseEntity<Void> cancelInvitation(
            @PathVariable UUID invitationId,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        log.info("Cancelling invitation: {}", invitationId);
        invitationService.cancelInvitation(invitationId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtener todas las invitaciones de un tenant.
     * GET /api/tenants/{tenantId}/invitations
     */
    @GetMapping("/{tenantId}/invitations")
    public ResponseEntity<List<InvitationDTO>> getTenantInvitations(
            @PathVariable UUID tenantId
    ) {
        log.info("Getting invitations for tenant: {}", tenantId);
        List<InvitationDTO> invitations = invitationService.getTenantInvitations(tenantId);
        return ResponseEntity.ok(invitations);
    }
}