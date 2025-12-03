package com.sentinel.tenant_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Queues y Bindings para escuchar eventos de auth-service.
 */
@Configuration
public class RabbitMQListenerConfig {

    /**
     * Declara el exchange de auth-service (debe existir).
     */
    @Bean
    public TopicExchange authExchange() {
        return new TopicExchange("auth-exchange", true, false);
    }

    /**
     * Queue para recibir eventos de usuarios registrados.
     */
    @Bean
    public Queue userRegisteredQueue() {
        return new Queue("auth.user.registered.queue", true);
    }

    /**
     * Binding entre el exchange y la queue.
     */
    @Bean
    public Binding userRegisteredBinding(Queue userRegisteredQueue, TopicExchange authExchange) {
        return BindingBuilder
                .bind(userRegisteredQueue)
                .to(authExchange)
                .with("auth.user.registered");
    }
}