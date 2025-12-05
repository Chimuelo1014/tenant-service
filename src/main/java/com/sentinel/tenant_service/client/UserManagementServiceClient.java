package com.sentinel.tenant_service.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Feign Client para comunicación con user-management-service.
 * Valida roles y permisos de usuarios.
 */
@FeignClient(
    name = "user-management-service",
    url = "${services.user_mgmt.url}"
)
public interface UserManagementServiceClient {

    /**
     * Obtiene el rol de un usuario en un tenant.
     * GET /api/internal/permissions/tenant/{tenantId}/user/{userId}/role
     * 
     * @return "TENANT_ADMIN" | "TENANT_USER" | null (si no es miembro)
     */
    @CircuitBreaker(name = "userMgmtService", fallbackMethod = "getTenantRoleFallback")
    @Retry(name = "userMgmtService")
    @GetMapping("/api/internal/permissions/tenant/{tenantId}/user/{userId}/role")
    String getTenantRole(
        @PathVariable UUID tenantId,
        @PathVariable UUID userId
    );

    /**
     * Verifica si un usuario es miembro de un tenant.
     */
    default boolean isTenantMember(UUID tenantId, UUID userId) {
        try {
            String role = getTenantRole(tenantId, userId);
            return role != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Fallback cuando user-management-service no responde.
     * Retorna null para indicar que no se pudo verificar.
     */
    default String getTenantRoleFallback(UUID tenantId, UUID userId, Exception ex) {
        // No lanzar excepción, retornar null para que el servicio decida
        return null;
    }
}