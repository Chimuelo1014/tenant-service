package com.sentinel.tenant_service.controller;

import com.sentinel.tenant_service.dto.request.CreateTenantRequest;
import com.sentinel.tenant_service.dto.response.LimitValidationResponse;
import com.sentinel.tenant_service.dto.response.TenantDTO;
import com.sentinel.tenant_service.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Internal API Controller para comunicación inter-servicios.
 * NO exponer públicamente - solo acceso interno.
 */
@Slf4j
@RestController
@RequestMapping("/api/tenants/internal")
@RequiredArgsConstructor
public class TenantInternalController {

    private final TenantService tenantService;

    /**
     * Crear tenant desde auth-service (auto-creación).
     * POST /api/tenants/internal/create
     */
    @PostMapping("/create")
    public ResponseEntity<TenantDTO> createTenantInternal(
            @Valid @RequestBody CreateTenantRequest request
    ) {
        log.info("Internal: Creating tenant for user: {}", request.getOwnerId());
        
        TenantDTO tenant = tenantService.createTenantForUser(
                request.getOwnerId(),
                request.getOwnerEmail()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(tenant);
    }

    /**
     * Validar límite de recurso.
     * POST /api/tenants/internal/{tenantId}/validate-limit
     */
    @PostMapping("/{tenantId}/validate-limit")
    public ResponseEntity<LimitValidationResponse> validateLimit(
            @PathVariable UUID tenantId,
            @RequestBody Map<String, Object> request
    ) {
        String resourceType = (String) request.get("resource");
        int currentCount = (int) request.get("currentCount");
        
        log.debug("Validating {} limit for tenant: {}", resourceType, tenantId);
        
        LimitValidationResponse response = tenantService.validateLimit(
                tenantId,
                resourceType,
                currentCount
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Incrementar contador de recurso.
     * POST /api/tenants/internal/{tenantId}/increment
     */
    @PostMapping("/{tenantId}/increment")
    public ResponseEntity<Void> incrementResource(
            @PathVariable UUID tenantId,
            @RequestBody Map<String, String> request
    ) {
        String resourceType = request.get("resource");
        
        log.debug("Incrementing {} for tenant: {}", resourceType, tenantId);
        
        tenantService.incrementResourceCount(tenantId, resourceType);
        
        return ResponseEntity.ok().build();
    }

    /**
     * Decrementar contador de recurso.
     * POST /api/tenants/internal/{tenantId}/decrement
     */
    @PostMapping("/{tenantId}/decrement")
    public ResponseEntity<Void> decrementResource(
            @PathVariable UUID tenantId,
            @RequestBody Map<String, String> request
    ) {
        String resourceType = request.get("resource");
        
        log.debug("Decrementing {} for tenant: {}", resourceType, tenantId);
        
        tenantService.decrementResourceCount(tenantId, resourceType);
        
        return ResponseEntity.ok().build();
    }

    /**
     * Obtener tenant por ID (interno).
     * GET /api/tenants/internal/{tenantId}
     */
    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantDTO> getTenant(@PathVariable UUID tenantId) {
        log.debug("Internal: Fetching tenant: {}", tenantId);
        return ResponseEntity.ok(tenantService.getTenantById(tenantId));
    }
}