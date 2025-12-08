package com.sentinel.tenant_service.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO combinado con suscripción y límites del plan (desde billing-service).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantSubscriptionLimitsDTO {

    private String tenantId;
    private String subscriptionId;
    private String planId;
    private String planName;
    private String subscriptionStatus;

    // Límites del plan
    private Integer maxUsers;
    private Integer maxProjects;
    private Integer maxDomains;
    private Integer maxRepos;
    private Boolean includesBlockchain;
}
