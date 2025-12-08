package com.sentinel.tenant_service.events;

import com.sentinel.tenant_service.entity.TenantEntity;
// import com.sentinel.tenant_service.enums.TenantPlan; // REMOVED - using planId
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Publica eventos de Tenant a RabbitMQ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${tenant.events.exchange:tenant-exchange}")
    private String exchange;

    @Value("${tenant.events.created-routing-key:tenant.created}")
    private String createdRoutingKey;

    @Value("${tenant.events.upgraded-routing-key:tenant.plan.upgraded}")
    private String upgradedRoutingKey;

    /**
     * Publica evento tenant.created.
     */
    public void publishTenantCreated(TenantEntity tenant) {
        log.info("Publishing tenant.created event for tenant: {}", tenant.getId());

        Map<String, Object> event = buildBaseEvent("tenant.created");

        Map<String, Object> data = new HashMap<>();
        data.put("tenantId", tenant.getId().toString());
        data.put("userId", tenant.getOwnerId().toString());
        data.put("name", tenant.getName());
        data.put("slug", tenant.getSlug());
        data.put("type", tenant.getType().name());
        data.put("planId", tenant.getPlanId()); // Cambiado de tenant.getPlan().name()

        event.put("data", data);

        rabbitTemplate.convertAndSend(exchange, createdRoutingKey, event);

        log.debug("Event published: {}", event);
    }

    /**
     * Publica evento tenant.plan.upgraded.
     * Ahora usa String planIds.
     */
    public void publishTenantPlanUpgraded(TenantEntity tenant, String oldPlanId, String newPlanId) {
        log.info("Publishing tenant.plan.upgraded event for tenant: {}", tenant.getId());

        Map<String, Object> event = buildBaseEvent("tenant.plan.upgraded");

        Map<String, Object> data = new HashMap<>();
        data.put("tenantId", tenant.getId().toString());
        data.put("oldPlanId", oldPlanId);
        data.put("newPlanId", newPlanId);

        Map<String, Object> newLimits = new HashMap<>();
        newLimits.put("maxUsers", tenant.getMaxUsers());
        newLimits.put("maxProjects", tenant.getMaxProjects());
        newLimits.put("maxDomains", tenant.getMaxDomains());
        newLimits.put("maxRepos", tenant.getMaxRepos());
        newLimits.put("blockchainEnabled", tenant.isBlockchainEnabled());

        data.put("newLimits", newLimits);

        event.put("data", data);

        rabbitTemplate.convertAndSend(exchange, upgradedRoutingKey, event);

        log.debug("Event published: {}", event);
    }

    /**
     * Construye estructura base del evento.
     */
    private Map<String, Object> buildBaseEvent(String eventType) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("eventId", UUID.randomUUID().toString());
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("version", "1.0");

        Map<String, String> metadata = new HashMap<>();
        metadata.put("source", "tenant-service");
        metadata.put("correlationId", UUID.randomUUID().toString());

        event.put("metadata", metadata);

        return event;
    }
}