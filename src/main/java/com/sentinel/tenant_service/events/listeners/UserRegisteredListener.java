package com.sentinel.tenant_service.events.listeners;

import com.sentinel.tenant_service.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Listener para el evento auth.user.registered.
 * Crea automáticamente un tenant cuando un usuario se registra.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredListener {

    private final TenantService tenantService;

    /**
     * Consume evento auth.user.registered desde auth-service.
     */
    @RabbitListener(queues = "#{autoCreateQueue}")
    public void handleUserRegistered(Map<String, Object> event) {
        try {
            log.info("Received auth.user.registered event");

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.get("data");

            String userIdStr = (String) data.get("userId");
            String email = (String) data.get("email");

            UUID userId = UUID.fromString(userIdStr);

            log.info("Creating tenant for new user: {}", userId);

            tenantService.createTenantForUser(userId, email);

            log.info("Tenant created successfully for user: {}", userId);

        } catch (Exception e) {
            log.error("Error processing auth.user.registered event: {}", e.getMessage(), e);
            // TODO: Implementar retry logic o dead letter queue
        }
    }

    /**
     * Bean para auto-crear la queue.
     */
    @org.springframework.context.annotation.Bean
    public org.springframework.amqp.core.Queue autoCreateQueue() {
        return new org.springframework.amqp.core.Queue("auth.user.registered.queue", true);
    }

    /**
     * Binding entre exchange y queue.
     */
    @org.springframework.context.annotation.Bean
    public org.springframework.amqp.core.Binding binding(
            org.springframework.amqp.core.Queue autoCreateQueue,
            org.springframework.amqp.core.TopicExchange authExchange
    ) {
        return org.springframework.amqp.core.BindingBuilder
                .bind(autoCreateQueue)
                .to(authExchange)
                .with("auth.user.registered");
    }

    /**
     * Declara el auth-exchange (debe existir en auth-service).
     */
    @org.springframework.context.annotation.Bean
    public org.springframework.amqp.core.TopicExchange authExchange() {
        return new org.springframework.amqp.core.TopicExchange("auth-exchange", true, false);
    }
}