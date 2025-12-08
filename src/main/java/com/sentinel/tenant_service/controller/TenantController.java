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
 * REST API Controller for Tenant management.
 * Public authenticated endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    /**
     * Get ALL tenants for user (owned + member)
     * GET /api/tenants/me
     */
    @GetMapping("/me")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TenantDTO>> getMyTenants(
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("Fetching ALL tenants for user: {}", userId);

        List<TenantDTO> tenants = tenantService.getAllTenantsForUser(userId);

        log.info("✅ Returning {} tenants for user {}", tenants.size(), userId);

        return ResponseEntity.ok(tenants);
    }

    /**
     * Admin: Get all tenants.
     * GET /api/tenants
     */
    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<org.springframework.data.domain.Page<TenantDTO>> getAllTenants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching ALL tenants (admin)");
        return ResponseEntity
                .ok(tenantService.getAllTenants(org.springframework.data.domain.PageRequest.of(page, size)));
    }

    /**
     * Get tenant by ID.
     * GET /api/tenants/{id}
     */
    @GetMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ResponseEntity<TenantDTO> getTenantById(@PathVariable UUID id) {
        log.info("Fetching tenant: {}", id);
        return ResponseEntity.ok(tenantService.getTenantById(id));
    }

    /**
     * Create tenant manually.
     * POST /api/tenants
     */
    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ResponseEntity<TenantDTO> createTenant(
            @Valid @RequestBody CreateTenantRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("Creating tenant for user: {}", userId);
        TenantDTO tenant = tenantService.createTenant(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(tenant);
    }

    /**
     * Update tenant.
     * PUT /api/tenants/{id}
     */
    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ResponseEntity<TenantDTO> updateTenant(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTenantRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("Updating tenant: {}", id);
        return ResponseEntity.ok(tenantService.updateTenant(id, request, userId));
    }

    /**
     * Delete tenant (soft delete).
     * DELETE /api/tenants/{id}
     */
    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteTenant(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("Deleting tenant: {}", id);
        tenantService.deleteTenant(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * View tenant limits.
     * GET /api/tenants/{id}/limits
     */
    @GetMapping("/{id}/limits")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ResponseEntity<TenantDTO> getTenantLimits(@PathVariable UUID id) {
        log.info("Fetching limits for tenant: {}", id);
        return ResponseEntity.ok(tenantService.getTenantById(id));
    }
}
