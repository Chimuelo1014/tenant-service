package com.sentinel.tenant_service.entity;

import com.sentinel.tenant_service.enums.InvitationStatus;
import com.sentinel.tenant_service.enums.TenantRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad para invitaciones a tenants.
 * Permite invitar usuarios por email a unirse a un workspace.
 */
@Entity
@Table(name = "tenant_invitations", indexes = {
    @Index(name = "idx_invitations_email", columnList = "invited_email"),
    @Index(name = "idx_invitations_tenant", columnList = "tenant_id"),
    @Index(name = "idx_invitations_token", columnList = "invitation_token"),
    @Index(name = "idx_invitations_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantInvitationEntity {

    @Id
    @GeneratedValue
    private UUID id;

    // Tenant info
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "tenant_name", nullable = false, length = 255)
    private String tenantName;

    // Inviter (quien envía la invitación)
    @Column(name = "invited_by_user_id", nullable = false)
    private UUID invitedByUserId;

    @Column(name = "invited_by_email", nullable = false, length = 255)
    private String invitedByEmail;

    // Invitee (quien recibe la invitación)
    @Column(name = "invited_email", nullable = false, length = 255)
    private String invitedEmail;

    @Column(name = "invited_user_id")
    private UUID invitedUserId; // Se llena cuando el usuario acepta

    // Role
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private TenantRole role = TenantRole.TENANT_USER;

    // Invitation token (para link de invitación)
    @Column(name = "invitation_token", nullable = false, unique = true, length = 64)
    private String invitationToken;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InvitationStatus status = InvitationStatus.PENDING;

    // Expiration
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    // Audit
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isPending() {
        return status == InvitationStatus.PENDING && !isExpired();
    }

    public void accept(UUID userId) {
        this.status = InvitationStatus.ACCEPTED;
        this.invitedUserId = userId;
        this.acceptedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = InvitationStatus.REJECTED;
        this.rejectedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = InvitationStatus.CANCELLED;
    }
}