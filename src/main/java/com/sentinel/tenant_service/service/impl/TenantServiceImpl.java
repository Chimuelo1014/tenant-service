package com.sentinel.tenant_service.service.impl;

import com.sentinel.tenant_service.client.UserManagementServiceClient;
import com.sentinel.tenant_service.dto.request.CreateTenantRequest;
import com.sentinel.tenant_service.dto.request.UpdateTenantRequest;
import com.sentinel.tenant_service.dto.response.LimitValidationResponse;
import com.sentinel.tenant_service.dto.response.TenantDTO;
import com.sentinel.tenant_service.entity.TenantEntity;
// import com.sentinel.tenant_service.enums.TenantPlan; // REMOVED - usando planId
import com.sentinel.tenant_service.enums.TenantStatus;
import com.sentinel.tenant_service.enums.TenantType;
import com.sentinel.tenant_service.events.TenantEventPublisher;
import com.sentinel.tenant_service.exception.*;
import com.sentinel.tenant_service.repository.TenantRepository;
import com.sentinel.tenant_service.service.TenantService;
import com.sentinel.tenant_service.service.UserLimitsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;
    private final TenantEventPublisher eventPublisher;
    private final UserManagementServiceClient userMgmtClient;
    private final UserLimitsService userLimitsService; // <-- INYECCIÓN NUEVA

    @Override
    @Transactional
    public TenantDTO createTenant(CreateTenantRequest request, UUID userId) {
        log.info("Creating tenant for user: {}", userId);

        // Validar campos de negocio si es tipo BUSINESS
        if (request.getType() == TenantType.BUSINESS && !request.isBusinessFieldsComplete()) {
            throw new IllegalArgumentException("Business name and NIT are required for BUSINESS type");
        }

        // Validar NIT si es tipo BUSINESS
        if (request.getType() == TenantType.BUSINESS) {
            validateNIT(request.getNit());

            if (tenantRepository.existsByNit(request.getNit())) {
                throw new TenantAlreadyExistsException("NIT already registered: " + request.getNit());
            }
        }

        // ✅ Validar límite de tenants según plan del usuario
        LimitValidationResponse limitResponse = userLimitsService.validateUserTenantLimit(userId);
        if (!limitResponse.isAllowed()) {
            throw new TenantLimitExceededException("User tenant limit reached: " + limitResponse.getMessage());
        }

        // Generar slug único
        String slug = generateSlug(request.getName(), userId);

        // Crear tenant SIN plan asignado - esperando que compre suscripción
        TenantEntity tenant = TenantEntity.builder()
                .name(request.getName())
                .slug(slug)
                .type(request.getType())
                .ownerId(request.getOwnerId() != null ? request.getOwnerId() : userId)
                .ownerEmail(request.getOwnerEmail())
                .businessName(request.getBusinessName())
                .nit(request.getNit())
                .planId(null) // Sin plan hasta que compre suscripción
                .subscriptionStatus("PENDING") // Esperando suscripción
                .status(TenantStatus.ACTIVE)
                // Límites mínimos por defecto
                .maxUsers(1)
                .maxProjects(0)
                .maxDomains(0)
                .maxRepos(0)
                .blockchainEnabled(false)
                .build();
        tenantRepository.save(tenant);

        log.info("Tenant created with ID: {}", tenant.getId());

        // Publicar evento
        eventPublisher.publishTenantCreated(tenant);

        return mapToDTO(tenant);
    }

    @Override
    @Transactional
    public TenantDTO createTenantForUser(UUID userId, String email) {
        log.info("Auto-creating tenant for new user: {}", userId);

        // ✅ Validar límite de tenants según plan del usuario
        LimitValidationResponse limitResponse = userLimitsService.validateUserTenantLimit(userId);
        if (!limitResponse.isAllowed()) {
            throw new TenantLimitExceededException("User tenant limit reached: " + limitResponse.getMessage());
        }

        String workspaceName = email.split("@")[0] + "'s Workspace";
        String slug = generateSlug(workspaceName, userId);

        TenantEntity tenant = TenantEntity.builder()
                .name(workspaceName)
                .slug(slug)
                .type(TenantType.PERSONAL)
                .ownerId(userId)
                .ownerEmail(email)
                .planId(null) // Sin plan - debe comprar suscripción
                .subscriptionStatus("PENDING")
                .status(TenantStatus.ACTIVE)
                // Límites mínimos
                .maxUsers(1)
                .maxProjects(0)
                .maxDomains(0)
                .maxRepos(0)
                .blockchainEnabled(false)
                .build();
        tenantRepository.save(tenant);

        log.info("Auto-tenant created with ID: {} for user: {}", tenant.getId(), userId);

        // Publicar evento
        eventPublisher.publishTenantCreated(tenant);

        return mapToDTO(tenant);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantDTO getTenantById(UUID tenantId) {
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantId));

        return mapToDTO(tenant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantDTO> getTenantsByOwner(UUID ownerId) {
        return tenantRepository.findByOwnerIdAndStatus(ownerId, TenantStatus.ACTIVE)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantDTO> getAllTenantsForUser(UUID userId) {
        log.info("🔍 Fetching ALL tenants for user: {}", userId);

        try {
            List<TenantEntity> ownedTenants = tenantRepository
                    .findByOwnerIdAndStatus(userId, TenantStatus.ACTIVE);

            log.info("👤 User {} OWNS {} tenants", userId, ownedTenants.size());

            List<UUID> memberTenantIds = new ArrayList<>();

            try {
                memberTenantIds = userMgmtClient.getUserTenants(userId);
                log.info("👥 User {} is MEMBER of {} tenants", userId, memberTenantIds.size());
            } catch (Exception e) {
                log.warn("⚠️ Could not fetch member tenants from user-management-service: {}",
                        e.getMessage());
            }

            Set<UUID> ownedTenantIds = ownedTenants.stream()
                    .map(TenantEntity::getId)
                    .collect(Collectors.toSet());

            List<UUID> memberOnlyIds = memberTenantIds.stream()
                    .filter(id -> !ownedTenantIds.contains(id))
                    .collect(Collectors.toList());

            log.debug("🔍 Member-only tenant IDs: {}", memberOnlyIds);

            List<TenantEntity> memberTenants = memberOnlyIds.isEmpty()
                    ? List.of()
                    : tenantRepository.findAllById(memberOnlyIds);

            log.info("✅ User {} has access to {} additional tenants as member",
                    userId, memberTenants.size());

            List<TenantEntity> allTenants = new ArrayList<>();
            allTenants.addAll(ownedTenants);
            allTenants.addAll(memberTenants);

            log.info("📊 TOTAL tenants for user {}: {} (owned) + {} (member) = {}",
                    userId, ownedTenants.size(), memberTenants.size(), allTenants.size());

            return allTenants.stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("❌ Error fetching tenants for user {}: {}", userId, e.getMessage(), e);
            log.warn("⚠️ Falling back to owned tenants only");
            return tenantRepository.findByOwnerIdAndStatus(userId, TenantStatus.ACTIVE)
                    .stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<TenantDTO> getAllTenants(
            org.springframework.data.domain.Pageable pageable) {
        return tenantRepository.findAll(pageable)
                .map(this::mapToDTO);
    }

    @Override
    @Transactional
    public TenantDTO updateTenant(UUID tenantId, UpdateTenantRequest request, UUID userId) {
        log.info("Updating tenant: {}", tenantId);

        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantId));

        if (!tenant.getOwnerId().equals(userId)) {
            throw new IllegalArgumentException("Only tenant owner can update");
        }

        if (request.getName() != null) {
            tenant.setName(request.getName());
        }

        if (request.getBusinessName() != null) {
            tenant.setBusinessName(request.getBusinessName());
        }

        if (request.getNit() != null) {
            validateNIT(request.getNit());

            if (!request.getNit().equals(tenant.getNit()) &&
                    tenantRepository.existsByNit(request.getNit())) {
                throw new TenantAlreadyExistsException("NIT already registered: " + request.getNit());
            }

            tenant.setNit(request.getNit());
        }

        tenantRepository.save(tenant);

        log.info("Tenant updated: {}", tenantId);

        return mapToDTO(tenant);
    }

    @Override
    @Transactional
    public void deleteTenant(UUID tenantId, UUID userId) {
        log.info("Deleting tenant: {}", tenantId);

        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantId));

        if (!tenant.getOwnerId().equals(userId)) {
            throw new IllegalArgumentException("Only tenant owner can delete");
        }

        tenant.setStatus(TenantStatus.DELETED);
        tenantRepository.save(tenant);

        log.info("Tenant deleted: {}", tenantId);
    }

    @Override
    @Transactional
    public TenantDTO upgradePlan(UUID tenantId, String newPlanId, UUID subscriptionId) {
        log.info("Upgrading tenant {} to plan: {}", tenantId, newPlanId);
        updateTenantPlan(tenantId, newPlanId);

        TenantEntity tenant = tenantRepository.findById(tenantId).orElseThrow();
        tenant.setSubscriptionId(subscriptionId);
        tenant.setSubscriptionStatus("ACTIVE");
        tenantRepository.save(tenant);

        return mapToDTO(tenant);
    }

    @Override
    @Transactional
    public void updateTenantPlan(UUID tenantId, String planId) {
        log.info("Applying plan {} to tenant {}", planId, tenantId);

        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantId));

        tenant.setPlanId(planId);

        // Update limits based on plan (Simple hardcoded logic for MVP, ideal: fetch
        // from Billing)
        switch (planId.toUpperCase()) {
            case "FREE":
                tenant.setMaxProjects(1);
                tenant.setMaxUsers(1);
                tenant.setMaxRepos(1);
                tenant.setMaxDomains(0);
                tenant.setBlockchainEnabled(false);
                break;
            case "STANDARD":
                tenant.setMaxProjects(10);
                tenant.setMaxUsers(5);
                tenant.setMaxRepos(10);
                tenant.setMaxDomains(1);
                tenant.setBlockchainEnabled(false);
                break;
            case "PRO":
                tenant.setMaxProjects(50);
                tenant.setMaxUsers(20);
                tenant.setMaxRepos(50);
                tenant.setMaxDomains(5);
                tenant.setBlockchainEnabled(true);
                break;
            case "ENTERPRISE":
                tenant.setMaxProjects(-1); // Unlimited
                tenant.setMaxUsers(100);
                tenant.setMaxRepos(-1);
                tenant.setMaxDomains(10);
                tenant.setBlockchainEnabled(true);
                break;
            default:
                log.warn("Unknown plan ID: {}", planId);
        }

        tenantRepository.save(tenant);
        log.info("Plan updated for tenant {}. New limits applied.", tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public LimitValidationResponse validateLimit(UUID tenantId, String resourceType, int currentCount) {
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantId));

        return switch (resourceType.toUpperCase()) {
            case "PROJECT" -> validateProjectLimit(tenant, currentCount);
            case "DOMAIN" -> validateDomainLimit(tenant, currentCount);
            case "REPO" -> validateRepoLimit(tenant, currentCount);
            case "USER" -> validateUserLimit(tenant, currentCount);
            default -> throw new IllegalArgumentException("Unknown resource type: " + resourceType);
        };
    }

    @Override
    @Transactional
    public void incrementResourceCount(UUID tenantId, String resourceType) {
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantId));

        switch (resourceType.toUpperCase()) {
            case "PROJECT" -> tenant.incrementProjects();
            case "DOMAIN" -> tenant.incrementDomains();
            default -> throw new IllegalArgumentException("Unknown resource type: " + resourceType);
        }

        tenantRepository.save(tenant);
    }

    @Override
    @Transactional
    public void decrementResourceCount(UUID tenantId, String resourceType) {
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantId));

        switch (resourceType.toUpperCase()) {
            case "PROJECT" -> tenant.decrementProjects();
            case "DOMAIN" -> tenant.decrementDomains();
            default -> throw new IllegalArgumentException("Unknown resource type: " + resourceType);
        }

        tenantRepository.save(tenant);
    }

    @Override
    @Transactional
    public void suspendTenant(UUID tenantId, String reason) {
        log.warn("Suspending tenant {} - Reason: {}", tenantId, reason);

        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantId));

        tenant.setStatus(TenantStatus.SUSPENDED);
        tenantRepository.save(tenant);

        log.info("Tenant suspended: {}", tenantId);
    }

    @Override
    @Transactional
    public void activateTenant(UUID tenantId) {
        log.info("Activating tenant: {}", tenantId);

        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantId));

        tenant.setStatus(TenantStatus.ACTIVE);
        tenantRepository.save(tenant);

        log.info("Tenant activated: {}", tenantId);
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private String generateSlug(String name, UUID userId) {
        String baseSlug = name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .substring(0, Math.min(
                        name.toLowerCase()
                                .replaceAll("[^a-z0-9\\s-]", "")
                                .replaceAll("\\s+", "-")
                                .length(),
                        50));

        String shortUuid = userId.toString().substring(0, 8);
        String slug = baseSlug + "-" + shortUuid;

        int counter = 1;
        while (tenantRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + shortUuid + "-" + counter;
            counter++;
        }

        return slug;
    }

    private void validateNIT(String nit) {
        if (nit == null || nit.isBlank()) {
            return;
        }

        if (!nit.matches("^[0-9]{9}-[0-9]{1}$")) {
            throw new InvalidNITException("Invalid NIT format. Expected: XXX-XXXXXX-X");
        }

        String[] parts = nit.split("-");
        String number = parts[0];
        int checkDigit = Integer.parseInt(parts[1]);

        int[] weights = { 71, 67, 59, 53, 47, 43, 41, 37, 29 };
        int sum = 0;

        for (int i = 0; i < 9; i++) {
            sum += Character.getNumericValue(number.charAt(i)) * weights[i];
        }

        int calculatedCheckDigit = sum % 11;
        if (calculatedCheckDigit >= 2) {
            calculatedCheckDigit = 11 - calculatedCheckDigit;
        }

        if (calculatedCheckDigit != checkDigit) {
            throw new InvalidNITException("Invalid NIT check digit");
        }
    }

    private LimitValidationResponse validateProjectLimit(TenantEntity tenant, int currentCount) {
        // -1 significa proyectos ilimitados
        if (tenant.getMaxProjects() == -1) {
            return LimitValidationResponse.allowed(-1, currentCount);
        }

        if (currentCount < tenant.getMaxProjects()) {
            return LimitValidationResponse.allowed(tenant.getMaxProjects(), currentCount);
        }

        return LimitValidationResponse.denied(
                tenant.getMaxProjects(),
                currentCount,
                "Project limit reached",
                "Upgrade your plan to create more projects");
    }

    private LimitValidationResponse validateDomainLimit(TenantEntity tenant, int currentCount) {
        if (currentCount < tenant.getMaxDomains()) {
            return LimitValidationResponse.allowed(tenant.getMaxDomains(), currentCount);
        }

        return LimitValidationResponse.denied(
                tenant.getMaxDomains(),
                currentCount,
                "Domain limit reached",
                "Upgrade to PRO plan to add more domains");
    }

    private LimitValidationResponse validateRepoLimit(TenantEntity tenant, int currentCount) {
        if (currentCount < tenant.getMaxRepos()) {
            return LimitValidationResponse.allowed(tenant.getMaxRepos(), currentCount);
        }

        return LimitValidationResponse.denied(
                tenant.getMaxRepos(),
                currentCount,
                "Repository limit reached",
                "Upgrade to BASIC plan to add repositories");
    }

    private LimitValidationResponse validateUserLimit(TenantEntity tenant, int currentCount) {
        if (currentCount < tenant.getMaxUsers()) {
            return LimitValidationResponse.allowed(tenant.getMaxUsers(), currentCount);
        }

        return LimitValidationResponse.denied(
                tenant.getMaxUsers(),
                currentCount,
                "User limit reached",
                "Upgrade to BASIC plan to invite more users");
    }

    private TenantDTO mapToDTO(TenantEntity entity) {
        return TenantDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .slug(entity.getSlug())
                .type(entity.getType())
                .ownerId(entity.getOwnerId())
                .ownerEmail(entity.getOwnerEmail())
                .businessName(entity.getBusinessName())
                .nit(entity.getNit())
                .planId(entity.getPlanId()) // Ahora es String
                .subscriptionStatus(entity.getSubscriptionStatus())
                .status(entity.getStatus())
                .limits(TenantDTO.TenantLimitsDTO.builder()
                        .maxUsers(entity.getMaxUsers())
                        .maxProjects(entity.getMaxProjects())
                        .maxDomains(entity.getMaxDomains())
                        .maxRepos(entity.getMaxRepos())
                        .blockchainEnabled(entity.isBlockchainEnabled())
                        .aiEnabled(false) // TODO: obtener de billing si el plan incluye AI
                        .build())
                .usage(TenantDTO.TenantUsageDTO.builder()
                        .currentUsers(entity.getCurrentUsers())
                        .currentProjects(entity.getCurrentProjects())
                        .currentDomains(entity.getCurrentDomains())
                        .currentRepos(entity.getCurrentRepos())
                        .build())
                .subscriptionId(entity.getSubscriptionId())
                .nextBillingDate(entity.getNextBillingDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
