package com.sentinel.tenant_service.service.impl;

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
    // TODO: Agregar EmailService cuando lo implementes

    @Value("${app.url:http://localhost:3000}")
    private String appUrl;

    @Value("${invitation.expiration.hours:72}")
    private int invitationExpirationHours;

    @Override
    @Transactional
    public InvitationDTO inviteMember(UUID tenantId, InviteMemberRequest request, UUID invitedByUserId) {
        log.info("Inviting member {} to tenant {}", request.getEmail(), tenantId);

        // Verificar que el tenant existe
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));

        // Verificar que el invitador es admin o owner
        if (!memberRepository.isAdminOrOwner(tenantId, invitedByUserId)) {
            throw new RuntimeException("Only admins can invite members");
        }

        // Verificar que el email no es del owner
        if (request.getEmail().equals(tenant.getOwnerEmail())) {
            throw new RuntimeException("Cannot invite the tenant owner");
        }

        // Verificar si ya existe invitación pendiente
        if (invitationRepository.existsPendingInvitation(
                tenantId, 
                request.getEmail(), 
                InvitationStatus.PENDING, 
                LocalDateTime.now()
        )) {
            throw new RuntimeException("Invitation already sent to this email");
        }

        // Obtener info del invitador
        TenantMemberEntity inviter = memberRepository.findByTenantIdAndUserId(tenantId, invitedByUserId)
                .orElseThrow(() -> new RuntimeException("Inviter not found"));

        // Generar token de invitación
        String token = UUID.randomUUID().toString();

        // Crear invitación
        TenantInvitationEntity invitation = TenantInvitationEntity.builder()
                .tenantId(tenantId)
                .tenantName(tenant.getName())
                .invitedByUserId(invitedByUserId)
                .invitedByEmail(inviter.getUserEmail())
                .invitedEmail(request.getEmail())
                .role(TenantRole.valueOf(request.getRole()))
                .invitationToken(token)
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusHours(invitationExpirationHours))
                .build();

        invitationRepository.save(invitation);

        log.info("Invitation created: {}", invitation.getId());

        // TODO: Enviar email
        String invitationUrl = String.format("%s/invitations/%s", appUrl, token);
        log.info("Invitation URL: {}", invitationUrl);
        // emailService.sendInvitationEmail(request.getEmail(), tenant.getName(), invitationUrl);

        return mapToDTO(invitation);
    }

    @Override
    @Transactional
    public void acceptInvitation(String token, UUID userId) {
        log.info("Accepting invitation with token: {}", token);

        TenantInvitationEntity invitation = invitationRepository.findByInvitationToken(token)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        // Validar estado
        if (!invitation.isPending()) {
            throw new RuntimeException("Invitation is not valid or has expired");
        }

        // Verificar que el usuario no sea ya miembro
        if (memberRepository.existsByTenantIdAndUserId(invitation.getTenantId(), userId)) {
            throw new RuntimeException("User is already a member of this tenant");
        }

        // Aceptar invitación
        invitation.accept(userId);
        invitationRepository.save(invitation);

        // Agregar como miembro
        TenantMemberEntity member = TenantMemberEntity.builder()
                .tenantId(invitation.getTenantId())
                .userId(userId)
                .userEmail(invitation.getInvitedEmail())
                .role(invitation.getRole())
                .isOwner(false)
                .joinedAt(LocalDateTime.now())
                .build();

        memberRepository.save(member);

        // Incrementar contador de usuarios en tenant
        TenantEntity tenant = tenantRepository.findById(invitation.getTenantId())
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        
        tenant.setCurrentUsers(tenant.getCurrentUsers() + 1);
        tenantRepository.save(tenant);

        log.info("User {} added to tenant {}", userId, invitation.getTenantId());
    }

    @Override
    @Transactional
    public void rejectInvitation(String token, UUID userId) {
        log.info("Rejecting invitation with token: {}", token);

        TenantInvitationEntity invitation = invitationRepository.findByInvitationToken(token)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        if (!invitation.isPending()) {
            throw new RuntimeException("Invitation is not valid");
        }

        invitation.reject();
        invitationRepository.save(invitation);

        log.info("Invitation rejected: {}", invitation.getId());
    }

    @Override
    @Transactional
    public void cancelInvitation(UUID invitationId, UUID userId) {
        log.info("Cancelling invitation: {}", invitationId);

        TenantInvitationEntity invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        // Verificar que el usuario puede cancelar
        if (!memberRepository.isAdminOrOwner(invitation.getTenantId(), userId)) {
            throw new RuntimeException("Only admins can cancel invitations");
        }

        invitation.cancel();
        invitationRepository.save(invitation);

        log.info("Invitation cancelled: {}", invitationId);
    }

    @Override
    public List<InvitationDTO> getPendingInvitations(String email) {
        log.debug("Getting pending invitations for: {}", email);

        return invitationRepository.findPendingByEmail(
                email, 
                InvitationStatus.PENDING, 
                LocalDateTime.now()
        )
        .stream()
        .map(this::mapToDTO)
        .collect(Collectors.toList());
    }

    @Override
    public List<InvitationDTO> getTenantInvitations(UUID tenantId) {
        log.debug("Getting invitations for tenant: {}", tenantId);

        return invitationRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
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