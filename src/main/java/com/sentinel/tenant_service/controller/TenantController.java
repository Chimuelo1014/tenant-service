package com.sentinel.tenant_service.controller;

import com.sentinel.tenant_service.dto.request.CreateTenantRequest;
import com.sentinel.tenant_service.dto.request.UpdateTenantRequest;
import com.sentinel.tenant_service.dto.response.TenantDTO;
import com.sentinel.tenant_service.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API Controller para gestión de Tenants.
 * Endpoints públicos autenticados.
 */
@Slf4j
@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    /**
     * Obtener mis tenants (como owner).
     * GET /api/tenants/me
     */
    @GetMapping("/me")
    public ResponseEntity<List<TenantDTO>> getMyTenants(
            @RequestHeader("X-User-Id") UUID userId
    ) {
        log.info("Fetching tenants for user: {}", userId);
        return ResponseEntity.ok(tenantService.getTenantsByOwner(userId));
    }

    /**
     * Obtener tenant por ID.
     * GET /api/tenants/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TenantDTO> getTenantById(@PathVariable UUID id) {
        log.info("Fetching tenant: {}", id);
        return ResponseEntity.ok(tenantService.getTenantById(id));
    }

    /**
     * Crear tenant manual.
     * POST /api/tenants
     */
    @PostMapping
    public ResponseEntity<TenantDTO> createTenant(
            @Valid @RequestBody CreateTenantRequest request,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        log.info("Creating tenant for user: {}", userId);
        TenantDTO tenant = tenantService.createTenant(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(tenant);
    }

    /**
     * Actualizar tenant.
     * PUT /api/tenants/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<TenantDTO> updateTenant(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTenantRequest request,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        log.info("Updating tenant: {}", id);
        return ResponseEntity.ok(tenantService.updateTenant(id, request, userId));
    }

    /**
     * Eliminar tenant (soft delete).
     * DELETE /api/tenants/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTenant(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        log.info("Deleting tenant: {}", id);
        tenantService.deleteTenant(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Ver límites del tenant.
     * GET /api/tenants/{id}/limits
     */
    @GetMapping("/{id}/limits")
    public ResponseEntity<TenantDTO> getTenantLimits(@PathVariable UUID id) {
        log.info("Fetching limits for tenant: {}", id);
        return ResponseEntity.ok(tenantService.getTenantById(id));
    }
}