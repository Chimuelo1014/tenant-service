package com.sentinel.tenant_service.entity;

import com.sentinel.tenant_service.enums.TenantRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Relación Many-to-Many entre Users y Tenants.
 * Un usuario puede pertenecer a múltiples tenants con diferentes roles.
 */
@Entity
@Table(name = "tenant_members", 
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "user_id"})
    },
    indexes = {
        @Index(name = "idx_members_tenant", columnList = "tenant_id"),
        @Index(name = "idx_members_user", columnList = "user_id"),
        @Index(name = "idx_members_role", columnList = "role")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantMemberEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_email", nullable = false, length = 255)
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TenantRole role = TenantRole.TENANT_USER;

    @Column(name = "is_owner")
    @Builder.Default
    private boolean isOwner = false;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}