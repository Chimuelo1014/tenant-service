package com.sentinel.tenant_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${tenant.events.exchange:tenant-exchange}")
    private String tenantExchangeName;

    @Value("${tenant.events.created-routing-key:tenant.created}")
    private String createdRoutingKey;

    @Value("${tenant.events.upgraded-routing-key:tenant.plan.upgraded}")
    private String upgradedRoutingKey;

    // -------------------------------
    //  EXCHANGE
    // -------------------------------

    @Bean
    public TopicExchange tenantExchange() {
        return new TopicExchange(tenantExchangeName, true, false);
    }

    // -------------------------------
    //  QUEUES
    // -------------------------------

    @Bean
    public Queue tenantCreatedQueue() {
        return new Queue("tenant.created.queue", true);
    }

    @Bean
    public Queue tenantUpgradedQueue() {
        return new Queue("tenant.upgraded.queue", true);
    }

    // -------------------------------
    //  BINDINGS (cola → exchange → routing key)
    // -------------------------------

    @Bean
    public Binding tenantCreatedBinding() {
        return BindingBuilder
                .bind(tenantCreatedQueue())
                .to(tenantExchange())
                .with(createdRoutingKey);
    }

    @Bean
    public Binding tenantUpgradedBinding() {
        return BindingBuilder
                .bind(tenantUpgradedQueue())
                .to(tenantExchange())
                .with(upgradedRoutingKey);
    }

    // -------------------------------
    //  JSON CONVERTER + RABBIT TEMPLATE
    // -------------------------------

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
