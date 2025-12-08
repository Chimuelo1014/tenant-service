package com.sentinel.tenant_service.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
     * Obtiene lista de tenants donde el usuario es miembro
     * GET /api/internal/users/{userId}/tenants
     * 
     * @return Lista de tenant IDs
     */
    @CircuitBreaker(name = "userMgmtService", fallbackMethod = "getUserTenantsFallback")
    @Retry(name = "userMgmtService")
    @GetMapping("/api/internal/users/{userId}/tenants")
    List<UUID> getUserTenants(@PathVariable UUID userId);

    /**
     * ✅ NUEVO: Obtiene lista de proyectos donde el usuario participa
     * GET /api/internal/users/{userId}/projects
     * 
     * @return Lista de project IDs
     */
    @CircuitBreaker(name = "userMgmtService", fallbackMethod = "getUserProjectsFallback")
    @Retry(name = "userMgmtService")
    @GetMapping("/api/internal/users/{userId}/projects")
    List<UUID> getUserProjects(@PathVariable UUID userId);

    /**
     * ✅ NUEVO: Obtiene el plan del usuario
     * GET /api/internal/users/{userId}/plan
     * 
     * @return UserPlanDTO con información del plan
     */
    @CircuitBreaker(name = "userMgmtService", fallbackMethod = "getUserPlanFallback")
    @Retry(name = "userMgmtService")
    @GetMapping("/api/internal/users/{userId}/plan")
    UserPlanResponse getUserPlan(@PathVariable UUID userId);

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
        return null;
    }

    /**
     * Fallback para getUserTenants
     */
    default List<UUID> getUserTenantsFallback(UUID userId, Exception ex) {
        return List.of();
    }

    /**
     * ✅ NUEVO: Fallback para getUserProjects
     */
    default List<UUID> getUserProjectsFallback(UUID userId, Exception ex) {
        return List.of();
    }

    /**
     * ✅ NUEVO: Fallback para getUserPlan - retorna plan FREE por defecto
     */
    default UserPlanResponse getUserPlanFallback(UUID userId, Exception ex) {
        return new UserPlanResponse("FREE");
    }

    /**
     * DTO simple para la respuesta del plan del usuario
     */
    class UserPlanResponse {
        private String plan;

        public UserPlanResponse() {}
        
        public UserPlanResponse(String plan) {
            this.plan = plan;
        }

        public String getPlan() {
            return plan;
        }

        public void setPlan(String plan) {
            this.plan = plan;
        }
    }
}
