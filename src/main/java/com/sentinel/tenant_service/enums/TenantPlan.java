package com.sentinel.tenant_service.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Planes de suscripción disponibles en Sentinel.
 * Define los límites de recursos para cada tenant.
 */
@Getter
@RequiredArgsConstructor
public enum TenantPlan {
    
    FREE(
        "Free Plan",
        1,      // maxUsers
        1,      // maxProjects
        1,      // maxDomains
        0,      // maxRepos
        false,  // blockchainEnabled
        false   // aiEnabled
    ),
    
    BASIC(
        "Basic Plan",
        3,      // maxUsers
        3,      // maxProjects
        3,      // maxDomains
        1,      // maxRepos
        false,  // blockchainEnabled
        false   // aiEnabled
    ),
    
    PRO(
        "Pro Plan",
        10,     // maxUsers
        10,     // maxProjects
        10,     // maxDomains
        5,      // maxRepos
        false,  // blockchainEnabled
        true    // aiEnabled
    ),
    
    ENTERPRISE(
        "Enterprise Plan",
        50,     // maxUsers
        -1,     // maxProjects (-1 = unlimited)
        50,     // maxDomains
        20,     // maxRepos
        true,   // blockchainEnabled
        true    // aiEnabled
    );

    private final String displayName;
    private final int maxUsers;
    private final int maxProjects;
    private final int maxDomains;
    private final int maxRepos;
    private final boolean blockchainEnabled;
    private final boolean aiEnabled;

    /**
     * Verifica si el plan permite recursos ilimitados en proyectos.
     */
    public boolean hasUnlimitedProjects() {
        return maxProjects == -1;
    }

    /**
     * Obtiene el plan por su nombre.
     */
    public static TenantPlan fromString(String plan) {
        try {
            return TenantPlan.valueOf(plan.toUpperCase());
        } catch (IllegalArgumentException e) {
            return FREE;
        }
    }
}