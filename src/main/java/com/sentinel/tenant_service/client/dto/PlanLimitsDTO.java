package com.sentinel.tenant_service.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO con límites de un plan (desde billing-service).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanLimitsDTO {

    private String planId;
    private String planName;
    private Integer maxUsers;
    private Integer maxProjects;
    private Integer maxDomains;
    private Integer maxRepos;
    private Boolean includesBlockchain;
}
