package com.sentinel.tenant_service.service;

import com.sentinel.tenant_service.dto.request.CreateTenantRequest;
import com.sentinel.tenant_service.dto.request.UpdateTenantRequest;
import com.sentinel.tenant_service.dto.response.LimitValidationResponse;
import com.sentinel.tenant_service.dto.response.TenantDTO;
// import com.sentinel.tenant_service.enums.TenantPlan; // REMOVED

import java.util.List;
import java.util.UUID;

/**
 * Servicio de gestión de Tenants (Workspaces/Organizaciones).
 */
public interface TenantService {

    /**
     * Crear tenant (manual desde API).
     */
    TenantDTO createTenant(CreateTenantRequest request, UUID userId);

    /**
     * Crear tenant automáticamente para un nuevo usuario.
     * Llamado desde UserRegisteredListener.
     */
    TenantDTO createTenantForUser(UUID userId, String email);

    /**
     * Obtener tenant por ID.
     */
    TenantDTO getTenantById(UUID tenantId);

    /**
     * Obtener todos los tenants de un owner.
     */
    List<TenantDTO> getTenantsByOwner(UUID ownerId);

    /**
     * ✅ NUEVO: Obtener TODOS los tenants donde el usuario es owner O miembro
     */
    List<TenantDTO> getAllTenantsForUser(UUID userId);

    /**
     * Admin: Obtener todos los tenants (paginado).
     */
    org.springframework.data.domain.Page<TenantDTO> getAllTenants(org.springframework.data.domain.Pageable pageable);

    /**
     * Actualizar tenant.
     */
    TenantDTO updateTenant(UUID tenantId, UpdateTenantRequest request, UUID userId);

    /**
     * Eliminar tenant (soft delete).
     */
    void deleteTenant(UUID tenantId, UUID userId);

    /**
     * Upgrade de plan desde billing-service.
     * Ahora recibe el planId (String) en lugar del enum.
     */
    TenantDTO upgradePlan(UUID tenantId, String newPlanId, UUID subscriptionId);

    /**
     * Actualiza el plan y límites del tenant (usado por el listener).
     */
    void updateTenantPlan(UUID tenantId, String planId);

    /**
     * Validar si se puede crear un recurso (proyecto, dominio, etc.).
     */
    LimitValidationResponse validateLimit(UUID tenantId, String resourceType, int currentCount);

    /**
     * Incrementar contador de recurso.
     */
    void incrementResourceCount(UUID tenantId, String resourceType);

    /**
     * Decrementar contador de recurso.
     */
    void decrementResourceCount(UUID tenantId, String resourceType);

    /**
     * Suspender tenant.
     */
    void suspendTenant(UUID tenantId, String reason);

    /**
     * Activar tenant suspendido.
     */
    void activateTenant(UUID tenantId);
}