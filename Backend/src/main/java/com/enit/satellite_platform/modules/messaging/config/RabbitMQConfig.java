package com.enit.satellite_platform.modules.messaging.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


/**
 * RabbitMQ Configuration for the messaging module.
 * This class configures exchanges, queues, bindings, and templates for RabbitMQ.
 * It supports direct messaging, topic-based routing, and dead letter queues for error handling.
 */
@Configuration
@RefreshScope
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "messaging")
public class RabbitMQConfig {
    
    @Value("${messaging.queue.dlx.enabled:true}")
    private boolean dlxEnabled;
    
    @Value("${messaging.retry.max-attempts:3}")
    private int maxRetryAttempts;
    
    @Value("${messaging.retry.initial-interval:1000}")
    private long initialRetryInterval;
    
    @Value("${messaging.retry.multiplier:2.0}")
    private double retryMultiplier;

    // Exchange Names
    public static final String DIRECT_EXCHANGE_NAME = "messaging.direct.exchange";
    public static final String TOPIC_EXCHANGE_NAME = "messaging.topic.exchange";
    public static final String DLX_EXCHANGE_NAME = "messaging.dlx.exchange";
    public static final String USER_EXCHANGE_NAME = "messaging.user.exchange";

    // Queue Names
    public static final String ADMIN_QUEUE_NAME = "messaging.admin.queue";
    public static final String BOT_QUEUE_NAME = "messaging.bot.queue";
    public static final String USER_QUEUE_PREFIX = "messaging.user.queue.";
    public static final String DLQ_NAME = "messaging.dlq";

    // Routing Keys
    public static final String ADMIN_ROUTING_KEY = "admin.#";
    public static final String BOT_ROUTING_KEY = "bot.#";
    public static final String USER_ROUTING_KEY_PREFIX = "user.";
    public static final String DLQ_ROUTING_KEY = "dlq.#";

    // === Exchanges ===
    @Bean
    DirectExchange directExchange() {
        return ExchangeBuilder.directExchange(DIRECT_EXCHANGE_NAME)
                .durable(true)
                .build();
    }

    @Bean
    TopicExchange topicExchange() {
        return ExchangeBuilder.topicExchange(TOPIC_EXCHANGE_NAME)
                .durable(true)
                .build();
    }

    @Bean
    DirectExchange userExchange() {
        return ExchangeBuilder.directExchange(USER_EXCHANGE_NAME)
                .durable(true)
                .build();
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(DLX_EXCHANGE_NAME)
                .durable(true)
                .build();
    }

    // === Queues ===
    @Bean
    Queue adminQueue() {
        QueueBuilder builder = QueueBuilder.durable(ADMIN_QUEUE_NAME);
        if (dlxEnabled) {
            builder.withArgument("x-dead-letter-exchange", DLX_EXCHANGE_NAME)
                   .withArgument("x-dead-letter-routing-key", "dlq.admin");
        }
        return builder.build();
    }

    @Bean
    Queue botQueue() {
        QueueBuilder builder = QueueBuilder.durable(BOT_QUEUE_NAME);
        if (dlxEnabled) {
            builder.withArgument("x-dead-letter-exchange", DLX_EXCHANGE_NAME)
                   .withArgument("x-dead-letter-routing-key", "dlq.bot");
        }
        return builder.build();
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_NAME)
                .build();
    }

    // === Bindings ===
    @Bean
    Binding adminBinding() {
        return BindingBuilder.bind(adminQueue())
                .to(topicExchange())
                .with(ADMIN_ROUTING_KEY);
    }

    @Bean
    Binding botBinding() {
        return BindingBuilder.bind(botQueue())
                .to(topicExchange())
                .with(BOT_ROUTING_KEY);
    }

    @Bean
    Binding dlqBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(DLQ_ROUTING_KEY);
    }

    // === Message Converter ===
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // === Retry Template ===
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Configure backoff policy
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(initialRetryInterval);
        backOffPolicy.setMultiplier(retryMultiplier);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        // Configure retry policy
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(maxRetryAttempts);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        return retryTemplate;
    }

    // === Message Recoverer ===
    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, DLX_EXCHANGE_NAME, "dlq.error");
    }

    // === RabbitTemplate Customization ===
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        rabbitTemplate.setRetryTemplate(retryTemplate());
        rabbitTemplate.setConfirmCallback((correlation, ack, reason) -> {
            if (!ack) {
                // Handle nack - message wasn't delivered to exchange
                // Log the failure
                System.err.println("Message wasn't delivered to exchange: " + reason);
            }
        });
        rabbitTemplate.setReturnsCallback(returned -> {
            // Handle return - message wasn't delivered to queue
            // Log the return
            System.err.println("Message returned: " + returned.getMessage() + 
                               " reply: " + returned.getReplyText());
        });
        return rabbitTemplate;
    }
    
    /**
     * Creates a user-specific queue dynamically.
     * @param userId The ID of the user for whom to create a queue
     * @return A configured Queue instance
     */
    public Queue createUserQueue(String userId) {
        String queueName = USER_QUEUE_PREFIX + userId;
        QueueBuilder builder = QueueBuilder.durable(queueName);
        if (dlxEnabled) {
            builder.withArgument("x-dead-letter-exchange", DLX_EXCHANGE_NAME)
                   .withArgument("x-dead-letter-routing-key", "dlq.user." + userId);
        }
        return builder.build();
    }
    
    /**
     * Binds a user queue to the user exchange with the appropriate routing key.
     * @param userQueue The queue to bind
     * @param userId The user ID to use in the routing key
     * @return A Binding for the user queue
     */
    public Binding bindUserQueue(Queue userQueue, String userId) {
        return BindingBuilder.bind(userQueue)
                .to(userExchange())
                .with(USER_ROUTING_KEY_PREFIX + userId);
    }
    
    /**
     * Helper method to create and bind a user queue in one operation.
     * This is useful for services that need to dynamically set up messaging for new users.
     * @param userId The ID of the user
     * @return The created and bound Queue instance
     */
    public Queue setupUserMessaging(String userId) {
        Queue userQueue = createUserQueue(userId);
        // Register the queue with RabbitMQ
        AmqpAdmin amqpAdmin = new RabbitAdmin((CachingConnectionFactory) 
                                             ((RabbitTemplate)rabbitTemplate(null))
                                             .getConnectionFactory());
        amqpAdmin.declareQueue(userQueue);
        
        // Create and register the binding
        Binding binding = bindUserQueue(userQueue, userId);
        amqpAdmin.declareBinding(binding);
        
        return userQueue;
    }
}