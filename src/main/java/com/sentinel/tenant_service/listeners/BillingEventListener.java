package com.sentinel.tenant_service.listeners;

import com.sentinel.tenant_service.entity.TenantEntity;
import com.sentinel.tenant_service.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Escucha eventos de billing-service para actualizar tenants.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillingEventListener {

    private final TenantRepository tenantRepository;

    /**
     * Evento: billing.subscription.created
     * Cuando se crea una suscripción, actualizar el tenant con el plan.
     */
    @RabbitListener(queues = "tenant.billing.subscription.created.queue")
    public void handleSubscriptionCreated(Map<String, Object> event) {
        try {
            String tenantId = (String) event.get("tenantId");
            String userId = (String) event.get("userId");
            String planId = (String) event.get("planId");
            @SuppressWarnings("unchecked")
            Map<String, Object> limits = (Map<String, Object>) event.get("planLimits");

            log.info("Processing subscription.created event for tenant: {} user: {}", tenantId, userId);

            // Buscar tenant por el userId real (ya que tenantId puede ser mock)
            List<TenantEntity> tenants = tenantRepository.findByOwnerId(UUID.fromString(userId));
            if (tenants.isEmpty()) {
                throw new RuntimeException("Tenant not found for user: " + userId);
            }
            TenantEntity tenant = tenants.get(0); // Tomar el primer tenant del usuario

            // Actualizar plan y límites
            tenant.setPlanId(planId);
            tenant.setSubscriptionStatus("ACTIVE");
            tenant.setMaxUsers((Integer) limits.get("maxUsers"));
            tenant.setMaxProjects((Integer) limits.get("maxProjects"));
            tenant.setMaxDomains((Integer) limits.get("maxDomains"));
            tenant.setMaxRepos((Integer) limits.get("maxRepos"));
            tenant.setBlockchainEnabled((Boolean) limits.get("blockchainEnabled"));

            tenantRepository.save(tenant);

            log.info("Tenant {} updated with plan {} - limits: users={}, projects={}",
                    tenant.getId(), planId, tenant.getMaxUsers(), tenant.getMaxProjects());

        } catch (Exception e) {
            log.error("Error processing subscription.created event", e);
            throw e; // Re-throw para que RabbitMQ maneje retry
        }
    }

    /**
     * Evento: billing.subscription.upgraded
     */
    @RabbitListener(queues = "tenant.billing.subscription.upgraded.queue")
    public void handleSubscriptionUpgraded(Map<String, Object> event) {
        try {
            String tenantId = (String) event.get("tenantId");
            String newPlanId = (String) event.get("newPlanId");
            @SuppressWarnings("unchecked")
            Map<String, Object> newLimits = (Map<String, Object>) event.get("newPlanLimits");

            log.info("Processing subscription.upgraded event for tenant: {} to plan: {}", tenantId, newPlanId);

            TenantEntity tenant = tenantRepository.findById(UUID.fromString(tenantId))
                    .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));

            // Actualizar a nuevo plan
            tenant.setPlanId(newPlanId);
            tenant.setMaxUsers((Integer) newLimits.get("maxUsers"));
            tenant.setMaxProjects((Integer) newLimits.get("maxProjects"));
            tenant.setMaxDomains((Integer) newLimits.get("maxDomains"));
            tenant.setMaxRepos((Integer) newLimits.get("maxRepos"));
            tenant.setBlockchainEnabled((Boolean) newLimits.get("includesBlockchain"));

            tenantRepository.save(tenant);

            log.info("Tenant {} upgraded to plan {}", tenantId, newPlanId);

        } catch (Exception e) {
            log.error("Error processing subscription.upgraded event", e);
            throw e;
        }
    }

    /**
     * Evento: billing.subscription.cancelled
     */
    @RabbitListener(queues = "tenant.billing.subscription.cancelled.queue")
    public void handleSubscriptionCancelled(Map<String, Object> event) {
        try {
            String tenantId = (String) event.get("tenantId");

            log.info("Processing subscription.cancelled event for tenant: {}", tenantId);

            TenantEntity tenant = tenantRepository.findById(UUID.fromString(tenantId))
                    .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));

            // Suspender tenant
            tenant.setSubscriptionStatus("CANCELLED");

            tenantRepository.save(tenant);

            log.warn("Tenant {} subscription cancelled", tenantId);

        } catch (Exception e) {
            log.error("Error processing subscription.cancelled event", e);
            throw e;
        }
    }
}
