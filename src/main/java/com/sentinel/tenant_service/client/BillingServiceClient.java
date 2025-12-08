package com.sentinel.tenant_service.client;

import com.sentinel.tenant_service.client.dto.PlanLimitsDTO;
import com.sentinel.tenant_service.client.dto.TenantSubscriptionLimitsDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign Client para comunicarse con billing-service.
 * Obtiene información de planes y límites de suscripciones.
 */
@FeignClient(name = "billing-service", url = "${services.billing.url}")
public interface BillingServiceClient {

    /**
     * Obtiene los límites del plan de un tenant.
     * Endpoint principal para validar límites.
     */
    @CircuitBreaker(name = "billingService", fallbackMethod = "getTenantLimitsFallback")
    @Retry(name = "billingService")
    @GetMapping("/api/internal/subscriptions/tenant/{tenantId}/limits")
    TenantSubscriptionLimitsDTO getTenantLimits(@PathVariable String tenantId);

    /**
     * Obtiene detalles de un plan específico.
     */
    @CircuitBreaker(name = "billingService", fallbackMethod = "getPlanLimitsFallback")
    @Retry(name = "billingService")
    @GetMapping("/api/internal/plans/{planId}/limits")
    PlanLimitsDTO getPlanLimits(@PathVariable String planId);

    /**
     * Fallback cuando billing-service no está disponible.
     * Retorna límites por defecto mínimos para permitir operación básica.
     */
    default TenantSubscriptionLimitsDTO getTenantLimitsFallback(String tenantId, Exception ex) {
        // Retornar límites mínimos por defecto para no bloquear completamente
        TenantSubscriptionLimitsDTO fallback = new TenantSubscriptionLimitsDTO();
        fallback.setTenantId(tenantId);
        fallback.setPlanId("FALLBACK");
        fallback.setPlanName("Service Unavailable");
        fallback.setSubscriptionStatus("UNKNOWN");
        fallback.setMaxUsers(1);
        fallback.setMaxProjects(1);
        fallback.setMaxDomains(1);
        fallback.setMaxRepos(0);
        fallback.setIncludesBlockchain(false);
        return fallback;
    }

    /**
     * Fallback para getPlanLimits.
     */
    default PlanLimitsDTO getPlanLimitsFallback(String planId, Exception ex) {
        PlanLimitsDTO fallback = new PlanLimitsDTO();
        fallback.setPlanId(planId);
        fallback.setPlanName("Unknown Plan");
        fallback.setMaxUsers(1);
        fallback.setMaxProjects(1);
        fallback.setMaxDomains(1);
        fallback.setMaxRepos(0);
        fallback.setIncludesBlockchain(false);
        return fallback;
    }
}
