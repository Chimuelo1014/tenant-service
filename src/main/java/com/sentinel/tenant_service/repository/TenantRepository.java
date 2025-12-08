package com.sentinel.tenant_service.repository;

import com.sentinel.tenant_service.entity.TenantEntity;
import com.sentinel.tenant_service.enums.TenantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<TenantEntity, UUID> {

    /**
     * Buscar tenant por slug único.
     */
    Optional<TenantEntity> findBySlug(String slug);

    /**
     * Verificar si existe un tenant con el slug.
     */
    boolean existsBySlug(String slug);

    /**
     * Verificar si existe un tenant con el NIT.
     */
    boolean existsByNit(String nit);

    /**
     * Buscar todos los tenants de un owner.
     */
    List<TenantEntity> findByOwnerId(UUID ownerId);

    /**
     * Buscar tenants por owner y estado.
     */
    List<TenantEntity> findByOwnerIdAndStatus(UUID ownerId, TenantStatus status);

    /**
     * Buscar tenants por estado.
     */
    List<TenantEntity> findByStatus(TenantStatus status);

    /**
     * Contar tenants de un owner con estado ACTIVE.
     */
    @Query("SELECT COUNT(t) FROM TenantEntity t WHERE t.ownerId = :ownerId AND t.status = :status")
    long countByOwnerIdAndStatus(@Param("ownerId") UUID ownerId, @Param("status") TenantStatus status);

    /**
     * Contar tenants de un owner (solo los ACTIVE).
     */
    @Query("SELECT COUNT(t) FROM TenantEntity t WHERE t.ownerId = :ownerId AND t.status = 'ACTIVE'")
    long countByOwnerId(@Param("ownerId") UUID ownerId);

    /**
     * Buscar tenant por owner y slug.
     */
    Optional<TenantEntity> findByOwnerIdAndSlug(UUID ownerId, String slug);

    /**
     * Buscar tenants con proyectos cerca del límite (para notificaciones).
     */
    @Query("SELECT t FROM TenantEntity t WHERE t.currentProjects >= (t.maxProjects * 0.8) AND t.status = :status")
    List<TenantEntity> findTenantsNearProjectLimit(@Param("status") TenantStatus status);

    /**
     * Buscar tenants por NIT (para validación).
     */
    Optional<TenantEntity> findByNit(String nit);
}