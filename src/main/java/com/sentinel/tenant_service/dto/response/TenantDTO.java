package com.sentinel.tenant_service.dto.response;

// import com.sentinel.tenant_service.enums.TenantPlan; // REMOVED
import com.sentinel.tenant_service.enums.TenantStatus;
import com.sentinel.tenant_service.enums.TenantType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantDTO {

    private UUID id;
    private String name;
    private String slug;
    private TenantType type;

    private UUID ownerId;
    private String ownerEmail;

    private String businessName;
    private String nit;

    private String planId; // Changed from TenantPlan enum
    private String subscriptionStatus; // PENDING, ACTIVE, CANCELLED
    private TenantStatus status;

    private TenantLimitsDTO limits;
    private TenantUsageDTO usage;

    private UUID subscriptionId;
    private LocalDateTime nextBillingDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenantLimitsDTO {
        private int maxUsers;
        private int maxProjects;
        private int maxDomains;
        private int maxRepos;
        private boolean blockchainEnabled;
        private boolean aiEnabled;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenantUsageDTO {
        private int currentUsers;
        private int currentProjects;
        private int currentDomains;
        private int currentRepos;
    }
}