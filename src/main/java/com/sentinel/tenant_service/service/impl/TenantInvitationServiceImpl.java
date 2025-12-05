package com.sentinel.tenant_service.service.impl;

import com.sentinel.tenant_service.client.UserManagementServiceClient;
import com.sentinel.tenant_service.dto.request.InviteMemberRequest;
import com.sentinel.tenant_service.dto.response.InvitationDTO;
import com.sentinel.tenant_service.entity.TenantEntity;
import com.sentinel.tenant_service.entity.TenantInvitationEntity;
import com.sentinel.tenant_service.entity.TenantMemberEntity;
import com.sentinel.tenant_service.enums.InvitationStatus;
import com.sentinel.tenant_service.enums.TenantRole;
import com.sentinel.tenant_service.repository.TenantInvitationRepository;
import com.sentinel.tenant_service.repository.TenantMemberRepository;
import com.sentinel.tenant_service.repository.TenantRepository;
import com.sentinel.tenant_service.service.TenantInvitationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantInvitationServiceImpl implements TenantInvitationService {

    private final TenantRepository tenantRepository;
    private final TenantInvitationRepository invitationRepository;
    private final TenantMemberRepository memberRepository;
    private final UserManagementServiceClient userMgmtClient;

    @Value("${app.url:http://localhost:3000}")
    private String appUrl;

    @Value("${invitation.expiration.hours:168}")
    private int invitationExpirationHours;

    @Override
    @Transactional
    public InvitationDTO inviteMember(UUID tenantId, InviteMemberRequest request, UUID invitedByUserId) {
        try {
            log.info("🔄 Inviting member {} to tenant {} by user {}", 
                request.getEmail(), tenantId, invitedByUserId);

            // ========================================
            // 1. VERIFICAR QUE EL TENANT EXISTE
            // ========================================
            log.debug("Step 1: Finding tenant {}", tenantId);
            TenantEntity tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> {
                        log.error("❌ Tenant not found: {}", tenantId);
                        return new RuntimeException("Tenant not found: " + tenantId);
                    });

            log.debug("✅ Tenant found: {} (owner: {})", tenant.getName(), tenant.getOwnerId());

            // ========================================
            // 2. VERIFICAR PERMISOS
            // ========================================
            log.debug("Step 2: Validating permissions");
            boolean hasPermission = validateInvitePermission(tenant, invitedByUserId);

            if (!hasPermission) {
                log.error("❌ User {} has no permission to invite members to tenant {}", 
                    invitedByUserId, tenantId);
                throw new RuntimeException("Only tenant owner or admin can invite members");
            }

            log.debug("✅ User {} has permission to invite", invitedByUserId);

            // ========================================
            // 3. VALIDAR EMAIL DEL OWNER
            // ========================================
            log.debug("Step 3: Validating invited email");
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                log.error("❌ Email is null or empty");
                throw new RuntimeException("Email is required");
            }

            if (request.getEmail().equalsIgnoreCase(tenant.getOwnerEmail())) {
                log.warn("❌ Cannot invite the owner");
                throw new RuntimeException("Cannot invite the tenant owner");
            }

            // ========================================
            // 4. VERIFICAR INVITACIÓN PENDIENTE
            // ========================================
            log.debug("Step 4: Checking pending invitations for {}", request.getEmail());
            boolean hasPendingInvitation = invitationRepository.existsPendingInvitation(
                    tenantId, 
                    request.getEmail(), 
                    InvitationStatus.PENDING, 
                    LocalDateTime.now()
            );

            if (hasPendingInvitation) {
                log.warn("❌ Invitation already exists for {}", request.getEmail());
                throw new RuntimeException("Invitation already sent to this email");
            }

            log.debug("✅ No pending invitation found");

            // ========================================
            // 5. VERIFICAR LÍMITE DE USUARIOS
            // ========================================
            log.debug("Step 5: Checking user limit (current: {}, max: {})", 
                tenant.getCurrentUsers(), tenant.getMaxUsers());

            if (tenant.getCurrentUsers() >= tenant.getMaxUsers()) {
                log.error("❌ User limit reached: {}/{}", 
                    tenant.getCurrentUsers(), tenant.getMaxUsers());
                throw new RuntimeException(
                    String.format("User limit reached (%d/%d). Upgrade your plan.", 
                        tenant.getCurrentUsers(), tenant.getMaxUsers())
                );
            }

            log.debug("✅ User limit OK");

            // ========================================
            // 6. OBTENER EMAIL DEL INVITADOR
            // ========================================
            log.debug("Step 6: Getting inviter email");
            String inviterEmail = getInviterEmail(tenant, invitedByUserId);
            log.debug("Inviter email: {}", inviterEmail);

            // ========================================
            // 7. VALIDAR ROL
            // ========================================
            log.debug("Step 7: Validating role: {}", request.getRole());
            TenantRole tenantRole;
            try {
                tenantRole = TenantRole.valueOf(request.getRole());
            } catch (IllegalArgumentException e) {
                log.error("❌ Invalid role: {}", request.getRole());
                throw new RuntimeException("Invalid role: " + request.getRole() + 
                    ". Must be TENANT_USER or TENANT_ADMIN");
            }

            log.debug("✅ Role validated: {}", tenantRole);

            // ========================================
            // 8. GENERAR TOKEN Y CREAR INVITACIÓN
            // ========================================
            log.debug("Step 8: Creating invitation");
            String token = UUID.randomUUID().toString();
            log.debug("Generated token: {}", token);

            LocalDateTime expiresAt = LocalDateTime.now().plusHours(invitationExpirationHours);
            log.debug("Expires at: {}", expiresAt);

            TenantInvitationEntity invitation = TenantInvitationEntity.builder()
                    .tenantId(tenantId)
                    .tenantName(tenant.getName())
                    .invitedByUserId(invitedByUserId)
                    .invitedByEmail(inviterEmail)
                    .invitedEmail(request.getEmail())
                    .role(tenantRole)
                    .invitationToken(token)
                    .status(InvitationStatus.PENDING)
                    .expiresAt(expiresAt)
                    .build();

            log.debug("Saving invitation entity...");
            invitationRepository.save(invitation);
            log.info("✅ Invitation saved with ID: {}", invitation.getId());

            // ========================================
            // 9. GENERAR URL Y RETORNAR
            // ========================================
            String invitationUrl = String.format("%s/invitations/accept?token=%s", appUrl, token);
            log.info("📧 Invitation URL: {}", invitationUrl);

            InvitationDTO dto = mapToDTO(invitation);
            log.info("✅ Invitation created successfully for {}", request.getEmail());
            
            return dto;

        } catch (Exception e) {
            log.error("❌ ERROR in inviteMember: {}", e.getMessage(), e);
            log.error("Stack trace:", e);
            throw new RuntimeException("Failed to create invitation: " + e.getMessage(), e);
        }
    }

    /**
     * ✅ SOLUCIÓN HÍBRIDA: Validación de permisos en 3 niveles
     */
    private boolean validateInvitePermission(TenantEntity tenant, UUID userId) {
        log.debug("🔐 Validating invite permission for user {} in tenant {}", userId, tenant.getId());

        // NIVEL 1: VERIFICAR SI ES EL OWNER
        if (tenant.getOwnerId().equals(userId)) {
            log.debug("✅ User is OWNER of tenant");
            return true;
        }

        // NIVEL 2: VERIFICAR CACHE LOCAL
        try {
            var localMember = memberRepository.findByTenantIdAndUserId(tenant.getId(), userId);
            
            if (localMember.isPresent()) {
                boolean isAdmin = localMember.get().getRole() == TenantRole.TENANT_ADMIN;
                log.debug("✅ Found in local cache: role={}, isAdmin={}", 
                    localMember.get().getRole(), isAdmin);
                
                if (isAdmin) {
                    return true;
                }
            } else {
                log.debug("⚠️ User not found in local cache");
            }
        } catch (Exception e) {
            log.warn("⚠️ Error checking local cache: {}", e.getMessage());
        }

        // NIVEL 3: CONSULTAR A USER-MANAGEMENT-SERVICE
        try {
            log.debug("🌐 Calling user-management-service to verify role...");
            
            String role = userMgmtClient.getTenantRole(tenant.getId(), userId);
            
            if (role != null) {
                log.debug("✅ Remote role found: {}", role);
                
                // Sincronizar cache local
                syncLocalCache(tenant.getId(), userId, role);
                
                return role.equals("TENANT_ADMIN");
            }
            
            log.warn("⚠️ User {} is not a member of tenant {}", userId, tenant.getId());
            return false;
            
        } catch (Exception e) {
            log.error("❌ Error calling user-management-service: {}", e.getMessage());
            log.warn("⚠️ Falling back to owner-only validation");
            return false;
        }
    }

    private void syncLocalCache(UUID tenantId, UUID userId, String role) {
        try {
            if (memberRepository.existsByTenantIdAndUserId(tenantId, userId)) {
                log.debug("Cache entry already exists");
                return;
            }

            log.debug("💾 Syncing local cache");

            TenantMemberEntity member = TenantMemberEntity.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .userEmail("")
                    .role(TenantRole.valueOf(role))
                    .isOwner(false)
                    .joinedAt(LocalDateTime.now())
                    .build();

            memberRepository.save(member);
            log.debug("✅ Cache synced");
            
        } catch (Exception e) {
            log.warn("⚠️ Failed to sync cache: {}", e.getMessage());
        }
    }

    private String getInviterEmail(TenantEntity tenant, UUID invitedByUserId) {
        if (tenant.getOwnerId().equals(invitedByUserId)) {
            return tenant.getOwnerEmail();
        }

        return memberRepository.findByTenantIdAndUserId(tenant.getId(), invitedByUserId)
                .map(TenantMemberEntity::getUserEmail)
                .orElse(tenant.getOwnerEmail());
    }

    @Override
    @Transactional
    public void acceptInvitation(String token, UUID userId) {
        try {
            log.info("✅ Accepting invitation with token: {} by user: {}", token, userId);

            TenantInvitationEntity invitation = invitationRepository.findByInvitationToken(token)
                    .orElseThrow(() -> new RuntimeException("Invitation not found"));

            if (!invitation.isPending()) {
                throw new RuntimeException("Invitation is not valid or has expired");
            }

            if (memberRepository.existsByTenantIdAndUserId(invitation.getTenantId(), userId)) {
                throw new RuntimeException("User is already a member of this tenant");
            }

            invitation.accept(userId);
            invitationRepository.save(invitation);

            TenantMemberEntity member = TenantMemberEntity.builder()
                    .tenantId(invitation.getTenantId())
                    .userId(userId)
                    .userEmail(invitation.getInvitedEmail())
                    .role(invitation.getRole())
                    .isOwner(false)
                    .joinedAt(LocalDateTime.now())
                    .build();

            memberRepository.save(member);

            TenantEntity tenant = tenantRepository.findById(invitation.getTenantId())
                    .orElseThrow(() -> new RuntimeException("Tenant not found"));
            
            tenant.setCurrentUsers(tenant.getCurrentUsers() + 1);
            tenantRepository.save(tenant);

            log.info("✅ User {} added to tenant {} as {}", 
                userId, invitation.getTenantId(), invitation.getRole());

        } catch (Exception e) {
            log.error("❌ Error accepting invitation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to accept invitation: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void rejectInvitation(String token, UUID userId) {
        try {
            log.info("❌ Rejecting invitation with token: {}", token);

            TenantInvitationEntity invitation = invitationRepository.findByInvitationToken(token)
                    .orElseThrow(() -> new RuntimeException("Invitation not found"));

            if (!invitation.isPending()) {
                throw new RuntimeException("Invitation is not valid");
            }

            invitation.reject();
            invitationRepository.save(invitation);

            log.info("✅ Invitation rejected: {}", invitation.getId());

        } catch (Exception e) {
            log.error("❌ Error rejecting invitation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to reject invitation: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void cancelInvitation(UUID invitationId, UUID userId) {
        try {
            log.info("🚫 Cancelling invitation: {}", invitationId);

            TenantInvitationEntity invitation = invitationRepository.findById(invitationId)
                    .orElseThrow(() -> new RuntimeException("Invitation not found"));

            TenantEntity tenant = tenantRepository.findById(invitation.getTenantId())
                    .orElseThrow(() -> new RuntimeException("Tenant not found"));

            if (!tenant.getOwnerId().equals(userId) && !invitation.getInvitedByUserId().equals(userId)) {
                throw new RuntimeException("Only owner or inviter can cancel invitations");
            }

            invitation.cancel();
            invitationRepository.save(invitation);

            log.info("✅ Invitation cancelled: {}", invitationId);

        } catch (Exception e) {
            log.error("❌ Error cancelling invitation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to cancel invitation: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationDTO> getPendingInvitations(String email) {
        try {
            log.debug("📨 Getting pending invitations for: {}", email);
            
            List<InvitationDTO> invitations = invitationRepository.findPendingByEmail(
                    email, 
                    InvitationStatus.PENDING, 
                    LocalDateTime.now()
            )
            .stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());

            log.debug("Found {} pending invitations", invitations.size());
            
            return invitations;

        } catch (Exception e) {
            log.error("❌ Error getting pending invitations: {}", e.getMessage(), e);
            return List.of(); // Retornar lista vacía en caso de error
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationDTO> getTenantInvitations(UUID tenantId) {
        try {
            log.debug("📨 Getting all invitations for tenant: {}", tenantId);
            
            return invitationRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                    .stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("❌ Error getting tenant invitations: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private InvitationDTO mapToDTO(TenantInvitationEntity entity) {
        return InvitationDTO.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .tenantName(entity.getTenantName())
                .invitedByEmail(entity.getInvitedByEmail())
                .invitedEmail(entity.getInvitedEmail())
                .role(entity.getRole().name())
                .status(entity.getStatus().name())
                .invitationToken(entity.getInvitationToken())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}