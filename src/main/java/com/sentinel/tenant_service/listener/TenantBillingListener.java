package com.sentinel.tenant_service.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.tenant_service.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantBillingListener {

    private final TenantService tenantService;

    @RabbitListener(queues = "tenant.billing.subscription.queue")
    public void handleSubscriptionCreated(Map<String, Object> event) {
        log.info("Received billing.subscription.created event: {}", event);

        try {
            String tenantIdStr = (String) event.get("tenantId");
            String planId = (String) event.get("planId");

            // Handle incorrect tenantId from mock if necessary (e.g., all zeros)
            if (tenantIdStr == null || tenantIdStr.equals("00000000-0000-0000-0000-000000000000")) {
                log.warn("Received invalid tenantId: {}", tenantIdStr);
                // In a real scenario we might stop, but for the MVP check loop or ignore
                return;
            }

            UUID tenantId = UUID.fromString(tenantIdStr);
            log.info("Updating plan for tenant {} to {}", tenantId, planId);

            // Call service to update
            tenantService.updateTenantPlan(tenantId, planId);

        } catch (Exception e) {
            log.error("Error processing subscription created event", e);
        }
    }
}
