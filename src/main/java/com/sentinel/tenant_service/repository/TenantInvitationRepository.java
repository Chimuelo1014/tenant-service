package com.sentinel.tenant_service.repository;

import com.sentinel.tenant_service.entity.TenantInvitationEntity;
import com.sentinel.tenant_service.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantInvitationRepository extends JpaRepository<TenantInvitationEntity, UUID> {

    /**
     * Buscar invitación por token.
     */
    Optional<TenantInvitationEntity> findByInvitationToken(String token);

    /**
     * Buscar invitaciones para un email específico.
     */
    List<TenantInvitationEntity> findByInvitedEmailOrderByCreatedAtDesc(String email);

    /**
     * Buscar invitaciones pendientes para un email.
     */
    @Query("SELECT i FROM TenantInvitationEntity i WHERE i.invitedEmail = :email " +
           "AND i.status = :status AND i.expiresAt > :now")
    List<TenantInvitationEntity> findPendingByEmail(
        @Param("email") String email,
        @Param("status") InvitationStatus status,
        @Param("now") LocalDateTime now
    );

    /**
     * Buscar invitaciones de un tenant.
     */
    List<TenantInvitationEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    /**
     * Buscar invitaciones pendientes de un tenant.
     */
    List<TenantInvitationEntity> findByTenantIdAndStatus(UUID tenantId, InvitationStatus status);

    /**
     * Verificar si existe invitación pendiente para email en tenant.
     */
    @Query("SELECT COUNT(i) > 0 FROM TenantInvitationEntity i " +
           "WHERE i.tenantId = :tenantId AND i.invitedEmail = :email " +
           "AND i.status = :status AND i.expiresAt > :now")
    boolean existsPendingInvitation(
        @Param("tenantId") UUID tenantId,
        @Param("email") String email,
        @Param("status") InvitationStatus status,
        @Param("now") LocalDateTime now
    );

    /**
     * Marcar invitaciones expiradas.
     */
    @Query("UPDATE TenantInvitationEntity i SET i.status = :expiredStatus " +
           "WHERE i.status = :pendingStatus AND i.expiresAt < :now")
    int markExpiredInvitations(
        @Param("expiredStatus") InvitationStatus expiredStatus,
        @Param("pendingStatus") InvitationStatus pendingStatus,
        @Param("now") LocalDateTime now
    );
}