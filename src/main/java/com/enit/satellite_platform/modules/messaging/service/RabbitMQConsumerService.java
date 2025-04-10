package com.enit.satellite_platform.modules.messaging.service;

import com.enit.satellite_platform.modules.messaging.config.RabbitMQConfig;
import com.enit.satellite_platform.modules.messaging.entities.Message;
import com.enit.satellite_platform.modules.messaging.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.transaction.annotation.Transactional; // Optional: if db ops need transaction

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMQConsumerService {

    private final MessageRepository messageRepository;
    private final MessagingService messagingService; // Will be created next

    @RabbitListener(queues = RabbitMQConfig.ADMIN_QUEUE_NAME)
    @Transactional // Example: Make processing transactional
    public void receiveAdminMessage(@Payload Message message) {
        log.info("Received ADMIN message: {}", message.getId());
        processReceivedMessage(message);
    }

    @RabbitListener(queues = RabbitMQConfig.BOT_QUEUE_NAME)
    @Transactional
    public void receiveBotMessage(@Payload Message message) {
        log.info("Received BOT message: {}", message.getId());
        processReceivedMessage(message);
    }

    @RabbitListener(queues = RabbitMQConfig.USER_DIRECT_QUEUE_NAME)
    @Transactional
    public void receiveUserDirectMessage(@Payload Message message) {
        log.info("Received USER_DIRECT message: {}", message.getId());
        // Additional logic might be needed here to ensure the message is for the correct user
        // if this queue handles messages for multiple users based on consumer filtering.
        processReceivedMessage(message);
    }

    /**
     * Common logic to process received messages, including idempotency check.
     *
     * @param message The received message.
     */
    private void processReceivedMessage(Message message) {
        try {
            // Idempotency Check: See if a message with this ID already exists in any conversation
            if (messageRepository.existsByMessageId(message.getId())) {
                log.warn("Duplicate message detected, skipping processing for message ID: {}", message.getId());
                // Acknowledge the message without processing further to remove it from the queue
                return;
            }

            // If not a duplicate, delegate to MessagingService to save/handle the message
            log.info("Processing message ID: {}", message.getId());
            messagingService.saveReceivedMessage(message); // This method needs to be implemented in MessagingService

            log.info("Successfully processed message ID: {}", message.getId());

        } catch (Exception e) {
            log.error("Error processing message ID {}: {}", message.getId(), e.getMessage(), e);
            // Depending on configuration, the message might be rejected and sent to DLQ
            // Throwing the exception might be necessary for proper DLQ handling
            throw new RuntimeException("Failed to process message " + message.getId(), e);
        }
    }

    // Optional: Listener for the Dead Letter Queue (DLQ) for monitoring/manual intervention
    @RabbitListener(queues = RabbitMQConfig.DLQ_NAME)
    public void receiveDlqMessage(@Payload org.springframework.amqp.core.Message failedMessage) {
         log.error("Received message on DLQ: {}", failedMessage.toString());
         // Log message properties, headers, and body for investigation
         // Consider storing failed messages or alerting administrators
         String originalExchange = failedMessage.getMessageProperties().getReceivedExchange();
         String originalRoutingKey = failedMessage.getMessageProperties().getReceivedRoutingKey();
         log.error("Original Exchange: {}, Original Routing Key: {}", originalExchange, originalRoutingKey);
         // Log the message body (might need deserialization)
         // String body = new String(failedMessage.getBody());
         // log.error("Message Body: {}", body);
    }
}
