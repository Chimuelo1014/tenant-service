package com.sentinel.tenant_service.entity;

// import com.sentinel.tenant_service.enums.TenantPlan; // REMOVED - using planId
import com.sentinel.tenant_service.enums.TenantStatus;
import com.sentinel.tenant_service.enums.TenantType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenants", indexes = {
        @Index(name = "idx_tenants_owner_id", columnList = "owner_id"),
        @Index(name = "idx_tenants_slug", columnList = "slug"),
        @Index(name = "idx_tenants_status", columnList = "status"),
        @Index(name = "idx_tenants_plan_id", columnList = "plan_id"), // Changed from plan to plan_id
        @Index(name = "idx_tenants_nit", columnList = "nit")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantEntity {

    @Id
    @GeneratedValue
    private UUID id;

    // Identificación
    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenantType type;

    // Owner
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "owner_email", nullable = false, length = 255)
    private String ownerEmail;

    // Business specific (solo para type = BUSINESS)
    @Column(name = "business_name", length = 255)
    private String businessName;

    @Column(unique = true, length = 50)
    private String nit;

    // Plan & Subscription from billing-service
    /**
     * ID del plan desde billing-service (ej: "BASIC", "PRO", "ENTERPRISE").
     * Null si no tiene suscripción activa.
     */
    @Column(name = "plan_id", length = 50)
    private String planId;

    /**
     * Estado de la suscripción del tenant.
     */
    @Column(name = "subscription_status", length = 20)
    @Builder.Default
    private String subscriptionStatus = "PENDING"; // ACTIVE, PENDING, SUSPENDED, CANCELLED

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TenantStatus status = TenantStatus.ACTIVE;

    @Column(name = "max_users", nullable = false)
    @Builder.Default
    private int maxUsers = 1;

    @Column(name = "max_projects", nullable = false)
    @Builder.Default
    private int maxProjects = 1;

    @Column(name = "max_domains", nullable = false)
    @Builder.Default
    private int maxDomains = 1;

    @Column(name = "max_repos", nullable = false)
    @Builder.Default
    private int maxRepos = 0;

    @Column(name = "blockchain_enabled")
    @Builder.Default
    private boolean blockchainEnabled = false;

    // Current usage (denormalized for performance)
    @Column(name = "current_users", nullable = false)
    @Builder.Default
    private int currentUsers = 1;

    @Column(name = "current_projects", nullable = false)
    @Builder.Default
    private int currentProjects = 0;

    @Column(name = "current_domains", nullable = false)
    @Builder.Default
    private int currentDomains = 0;

    @Column(name = "current_repos", nullable = false)
    @Builder.Default
    private int currentRepos = 0;

    // Billing
    @Column(name = "subscription_id")
    private UUID subscriptionId;

    @Column(name = "next_billing_date")
    private LocalDateTime nextBillingDate;

    // Audit
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    // updateLimitsFromPlan() REMOVED - Los límites ahora se actualizan desde
    // BillingEventListener

    /**
     * Verifica si puede crear más proyectos.
     */
    public boolean canCreateProject() {
        // -1 significa proyectos ilimitados
        return maxProjects == -1 || currentProjects < maxProjects;
    }

    /**
     * Verifica si puede agregar más usuarios.
     */
    public boolean canAddUser() {
        return currentUsers < maxUsers;
    }

    /**
     * Verifica si puede agregar más dominios.
     */
    public boolean canAddDomain() {
        return currentDomains < maxDomains;
    }

    /**
     * Verifica si puede agregar más repositorios.
     */
    public boolean canAddRepo() {
        return currentRepos < maxRepos;
    }

    /**
     * Incrementa el contador de proyectos.
     */
    public void incrementProjects() {
        this.currentProjects++;
    }

    /**
     * Decrementa el contador de proyectos.
     */
    public void decrementProjects() {
        if (this.currentProjects > 0) {
            this.currentProjects--;
        }
    }

    /**
     * Incrementa el contador de dominios.
     */
    public void incrementDomains() {
        this.currentDomains++;
    }

    /**
     * Decrementa el contador de dominios.
     */
    public void decrementDomains() {
        if (this.currentDomains > 0) {
            this.currentDomains--;
        }
    }
}