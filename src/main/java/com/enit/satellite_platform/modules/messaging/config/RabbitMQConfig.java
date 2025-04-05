package com.enit.satellite_platform.modules.messaging.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired; // Import Autowired
import org.springframework.cloud.context.config.annotation.RefreshScope; // Import RefreshScope
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment; // Import Environment

@Configuration
@RefreshScope // Add RefreshScope annotation
public class RabbitMQConfig {

    @Autowired
    private Environment environment; // Inject Environment

    // Config key for DLX enablement
    private static final String DLX_ENABLED_PROPERTY = "messaging.queue.dlx.enabled";

    // Exchange Names
    public static final String DIRECT_EXCHANGE_NAME = "messaging.direct.exchange";
    public static final String TOPIC_EXCHANGE_NAME = "messaging.topic.exchange";
    public static final String DLX_EXCHANGE_NAME = "messaging.dlx.exchange"; // Dead Letter Exchange

    // Queue Names
    public static final String ADMIN_QUEUE_NAME = "messaging.admin.queue";
    public static final String BOT_QUEUE_NAME = "messaging.bot.queue";
    // User-specific queues might be handled dynamically or via routing keys
    public static final String USER_DIRECT_QUEUE_NAME = "messaging.user.direct.queue"; // Example queue for direct messages if not dynamic
    public static final String DLQ_NAME = "messaging.dlq"; // Dead Letter Queue

    // Routing Keys
    public static final String ADMIN_ROUTING_KEY = "admin.#"; // Listens to all admin messages
    public static final String BOT_ROUTING_KEY = "bot.#";     // Listens to all bot messages
    // User-specific routing keys will likely be the userId

    // === Exchanges ===
    @Bean
    DirectExchange directExchange() {
        return new DirectExchange(DIRECT_EXCHANGE_NAME);
    }

    @Bean
    TopicExchange topicExchange() {
        return new TopicExchange(TOPIC_EXCHANGE_NAME);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE_NAME);
    }

    // === Queues ===
    @Bean
    Queue adminQueue() {
        QueueBuilder builder = QueueBuilder.durable(ADMIN_QUEUE_NAME);
        if (isDlxEnabled()) {
            builder.withArgument("x-dead-letter-exchange", DLX_EXCHANGE_NAME)
                   .withArgument("x-dead-letter-routing-key", "dlq.admin");
        }
        return builder.build();
    }

    @Bean
    Queue botQueue() {
         QueueBuilder builder = QueueBuilder.durable(BOT_QUEUE_NAME);
        if (isDlxEnabled()) {
            builder.withArgument("x-dead-letter-exchange", DLX_EXCHANGE_NAME)
                   .withArgument("x-dead-letter-routing-key", "dlq.bot");
        }
        return builder.build();
    }

     @Bean
    Queue userDirectQueue() {
        // Example queue for handling direct user messages if not using dynamic queues per user.
        QueueBuilder builder = QueueBuilder.durable(USER_DIRECT_QUEUE_NAME);
        if (isDlxEnabled()) {
            builder.withArgument("x-dead-letter-exchange", DLX_EXCHANGE_NAME)
                   .withArgument("x-dead-letter-routing-key", "dlq.user.direct");
        }
        return builder.build();
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_NAME).build();
    }

    // === Bindings ===
    @Bean
    Binding adminBinding(Queue adminQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(adminQueue).to(topicExchange).with(ADMIN_ROUTING_KEY);
    }

    @Bean
    Binding botBinding(Queue botQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(botQueue).to(topicExchange).with(BOT_ROUTING_KEY);
    }

     @Bean
    Binding userDirectBinding(Queue userDirectQueue, DirectExchange directExchange) {
        // This binds the general user queue. Messages sent with a specific user ID as routing key
        // will go here if no specific queue for that user ID exists and is bound.
        // A more robust approach uses dynamic queues or consistent hashing exchange.
        // For simplicity now, routing key might need to be static e.g., "user.direct"
         return BindingBuilder.bind(userDirectQueue).to(directExchange).with(USER_DIRECT_QUEUE_NAME); // Example static routing key
    }


    @Bean
    Binding dlqBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        // Binds the DLQ to the DLX. We use "#" or specific keys based on origin queue DL routing keys
        // For simplicity, let's route all dead letters with a generic key for now.
        // Or bind with specific keys like "dlq.admin", "dlq.bot", "dlq.user.direct"
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with("dlq.#"); // Catch all DLQ messages
    }

    // === Message Converter ===
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // === RabbitTemplate Customization ===
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        // Add other configurations like retry policies if needed
        return rabbitTemplate;
    }

    // Helper method to check DLX configuration
    private boolean isDlxEnabled() {
        return environment.getProperty(DLX_ENABLED_PROPERTY, Boolean.class, true); // Default true
    }
}
