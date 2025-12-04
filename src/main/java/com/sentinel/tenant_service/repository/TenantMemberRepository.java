package com.sentinel.tenant_service.repository;

import com.sentinel.tenant_service.entity.TenantMemberEntity;
import com.sentinel.tenant_service.enums.TenantRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantMemberRepository extends JpaRepository<TenantMemberEntity, UUID> {

    /**
     * Buscar miembros de un tenant.
     */
    List<TenantMemberEntity> findByTenantId(UUID tenantId);

    /**
     * Buscar tenants de un usuario.
     */
    List<TenantMemberEntity> findByUserId(UUID userId);

    /**
     * Buscar membresía específica.
     */
    Optional<TenantMemberEntity> findByTenantIdAndUserId(UUID tenantId, UUID userId);

    /**
     * Verificar si usuario es miembro de tenant.
     */
    boolean existsByTenantIdAndUserId(UUID tenantId, UUID userId);

    /**
     * Contar miembros de un tenant.
     */
    long countByTenantId(UUID tenantId);

    /**
     * Buscar owner de un tenant.
     */
    Optional<TenantMemberEntity> findByTenantIdAndIsOwnerTrue(UUID tenantId);

    /**
     * Buscar admins de un tenant.
     */
    List<TenantMemberEntity> findByTenantIdAndRole(UUID tenantId, TenantRole role);

    /**
     * Verificar si usuario es admin/owner de tenant.
     */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN TRUE ELSE FALSE END " +
           "FROM TenantMemberEntity m " +
           "WHERE m.tenantId = :tenantId AND m.userId = :userId " +
           "AND (m.isOwner = true OR m.role = 'TENANT_ADMIN')")
    boolean isAdminOrOwner(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId);
}