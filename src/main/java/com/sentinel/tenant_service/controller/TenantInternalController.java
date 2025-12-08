package com.sentinel.tenant_service.controller;

import com.sentinel.tenant_service.dto.response.LimitValidationResponse;
import com.sentinel.tenant_service.dto.response.TenantDTO;
import com.sentinel.tenant_service.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Internal API Controller for inter-service communication.
 * Endpoints without authentication for Feign Clients.
 */
@Slf4j
@RestController
@RequestMapping("/api/tenants/internal")
@RequiredArgsConstructor
public class TenantInternalController {

    private final TenantService tenantService;

    /**
     * Get tenant by ID (internal).
     * GET /api/tenants/internal/{tenantId}
     * 
     * Used by: project-service (Feign Client)
     */
    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantDTO> getTenant(@PathVariable UUID tenantId) {
        log.debug("Internal: Fetching tenant: {}", tenantId);
        return ResponseEntity.ok(tenantService.getTenantById(tenantId));
    }

    /**
     * Validate resource limit.
     * POST /api/tenants/internal/{tenantId}/validate-limit
     * 
     * Query params: resource (PROJECT|DOMAIN|REPO|USER), currentCount
     */
    @PostMapping("/{tenantId}/validate-limit")
    public ResponseEntity<LimitValidationResponse> validateLimit(
            @PathVariable UUID tenantId,
            @RequestParam String resource,
            @RequestParam int currentCount) {
        log.debug("Validating {} limit for tenant: {} (current: {})",
                resource, tenantId, currentCount);

        LimitValidationResponse response = tenantService.validateLimit(
                tenantId,
                resource,
                currentCount);

        return ResponseEntity.ok(response);
    }

    /**
     * Increment resource counter.
     * POST /api/tenants/internal/{tenantId}/resources/increment?resource=PROJECT
     */
    @PostMapping("/{tenantId}/resources/increment")
    public ResponseEntity<Void> incrementResource(
            @PathVariable UUID tenantId,
            @RequestParam String resource) {
        log.debug("Incrementing {} for tenant: {}", resource, tenantId);
        tenantService.incrementResourceCount(tenantId, resource);
        return ResponseEntity.ok().build();
    }

    /**
     * Decrement resource counter.
     * POST /api/tenants/internal/{tenantId}/resources/decrement?resource=PROJECT
     */
    @PostMapping("/{tenantId}/resources/decrement")
    public ResponseEntity<Void> decrementResource(
            @PathVariable UUID tenantId,
            @RequestParam String resource) {
        log.debug("Decrementing {} for tenant: {}", resource, tenantId);
        tenantService.decrementResourceCount(tenantId, resource);
        return ResponseEntity.ok().build();
    }
}