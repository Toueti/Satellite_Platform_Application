package com.enit.satellite_platform.modules.messaging.service;

import com.enit.satellite_platform.modules.messaging.config.RabbitMQConfig;
import com.enit.satellite_platform.modules.messaging.entities.Message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cloud.context.config.annotation.RefreshScope; // Import RefreshScope
import org.springframework.core.env.Environment; // Import Environment
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@RefreshScope // Add RefreshScope annotation
public class RabbitMQProducerService {

    private final RabbitTemplate rabbitTemplate;
    private final Environment environment; // Inject Environment

    // Config keys for routing keys
    private static final String ROUTING_KEY_USER_DIRECT = "messaging.rabbitmq.routing.user.direct";
    private static final String ROUTING_KEY_ADMIN_TOPIC = "messaging.rabbitmq.routing.admin.topic";
    private static final String ROUTING_KEY_BOT_TOPIC = "messaging.rabbitmq.routing.bot.topic";

    // Default routing keys (can fallback to RabbitMQConfig constants or define here)
    private static final String DEFAULT_ROUTING_KEY_USER_DIRECT = RabbitMQConfig.DIRECT_EXCHANGE_NAME; // Example fallback
    private static final String DEFAULT_ROUTING_KEY_ADMIN_TOPIC = "admin.message"; // Default if not configured
    private static final String DEFAULT_ROUTING_KEY_BOT_TOPIC = "bot.message"; // Default if not configured


    /**
     * Sends a message to the appropriate RabbitMQ exchange based on its type.
     *
     * @param message The message object to send.
     * @param recipientId The ID of the intended recipient (used for direct routing). Can be null for topic messages.
     */
    public void sendMessage(Message message, String recipientId) {
        String exchangeName;
        String routingKey;

        switch (message.getType()) {
            case USER_TO_USER:
                exchangeName = RabbitMQConfig.DIRECT_EXCHANGE_NAME;
                // Use recipientId or configured static key
                // routingKey = recipientId; // Ideal direct routing if using dynamic queues/bindings per user
                routingKey = environment.getProperty(ROUTING_KEY_USER_DIRECT, DEFAULT_ROUTING_KEY_USER_DIRECT); // Use configured static key
                log.info("Sending USER_TO_USER message {} to exchange {} with routing key {}", message.getId(), exchangeName, routingKey);
                break;

            case USER_TO_ADMIN:
            case ADMIN_TO_USER: // Admins might reply via the topic exchange as well
                exchangeName = RabbitMQConfig.TOPIC_EXCHANGE_NAME;
                routingKey = environment.getProperty(ROUTING_KEY_ADMIN_TOPIC, DEFAULT_ROUTING_KEY_ADMIN_TOPIC); // Use configured key
                log.info("Sending ADMIN related message {} to exchange {} with routing key {}", message.getId(), exchangeName, routingKey);
                break;

            case USER_TO_BOT:
            case BOT_TO_USER: // Bots might reply via the topic exchange
                exchangeName = RabbitMQConfig.TOPIC_EXCHANGE_NAME;
                routingKey = environment.getProperty(ROUTING_KEY_BOT_TOPIC, DEFAULT_ROUTING_KEY_BOT_TOPIC); // Use configured key
                log.info("Sending BOT related message {} to exchange {} with routing key {}", message.getId(), exchangeName, routingKey);
                break;

            default:
                log.error("Unknown message type {} for message {}, cannot determine exchange/routing key.", message.getType(), message.getId());
                // Optionally throw an exception or send to an error queue
                return;
        }

        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
            log.info("Message {} sent successfully.", message.getId());
        } catch (Exception e) {
            log.error("Failed to send message {} to exchange {} with routing key {}: {}", message.getId(), exchangeName, routingKey, e.getMessage(), e);
            // Implement retry logic or handle failure appropriately
        }
    }
}
