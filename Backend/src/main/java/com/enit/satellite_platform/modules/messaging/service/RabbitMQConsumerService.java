package com.enit.satellite_platform.modules.messaging.service;

import com.enit.satellite_platform.modules.messaging.config.RabbitMQConfig;
import com.enit.satellite_platform.modules.messaging.entities.Message;
import com.enit.satellite_platform.modules.messaging.repository.MessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsible for consuming messages from RabbitMQ queues.
 * Handles message processing, idempotency checks, and error handling.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMQConsumerService {

    private final MessageRepository messageRepository;
    private final MessagingService messagingService;
    private final RabbitMQConfig rabbitMQConfig;
    private final ObjectMapper objectMapper;
    
    @Value("${app.user.id:}")
    private String currentUserId;
    
    // Track message processing attempts for retry management
    private final Map<String, Integer> messageProcessingAttempts = new ConcurrentHashMap<>();
    private static final int MAX_PROCESSING_ATTEMPTS = 3;
    
    @PostConstruct
    public void initialize() {
        // If there's a current user ID set for this instance, ensure their queue exists
        if (currentUserId != null && !currentUserId.isEmpty()) {
            rabbitMQConfig.setupUserMessaging(currentUserId);
            log.info("User messaging queue setup completed for user ID: {}", currentUserId);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.ADMIN_QUEUE_NAME)
    @Transactional
    public void receiveAdminMessage(@Payload Message message, 
                                    @Header(required = false, name = "x-retry-count") Integer retryCount) {
        log.info("Received ADMIN message with ID: {}, retry count: {}", 
                message.getId(), retryCount != null ? retryCount : 0);
        processReceivedMessage(message, "ADMIN");
    }

    @RabbitListener(queues = RabbitMQConfig.BOT_QUEUE_NAME)
    @Transactional
    public void receiveBotMessage(@Payload Message message,
                                  @Header(required = false, name = "x-retry-count") Integer retryCount) {
        log.info("Received BOT message with ID: {}, retry count: {}", 
                message.getId(), retryCount != null ? retryCount : 0);
        processReceivedMessage(message, "BOT");
    }

    /**
     * Dynamic queue listener for user-specific messages.
     * Uses Spring's SpEL to configure the queue name based on the current user ID.
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = RabbitMQConfig.USER_QUEUE_PREFIX + "#{@environment.getProperty('app.user.id')}",
                   durable = "true",
                   arguments = {
                       @org.springframework.amqp.rabbit.annotation.Argument(
                           name = "x-dead-letter-exchange", 
                           value = RabbitMQConfig.DLX_EXCHANGE_NAME),
                       @org.springframework.amqp.rabbit.annotation.Argument(
                           name = "x-dead-letter-routing-key", 
                           value = "dlq.user.#{@environment.getProperty('app.user.id')}")
                   }),
            exchange = @Exchange(value = RabbitMQConfig.USER_EXCHANGE_NAME, type = "direct"),
            key = RabbitMQConfig.USER_ROUTING_KEY_PREFIX + "#{@environment.getProperty('app.user.id')}"
    ))
    @Transactional
    public void receiveUserDirectMessage(@Payload Message message,
                                        @Header(required = false, name = "x-retry-count") Integer retryCount) {
        String userId = message.getRecipientId();
        log.info("Received USER message for user: {}, message ID: {}, retry count: {}", 
                userId, message.getId(), retryCount != null ? retryCount : 0);
        
        // Verify this message is intended for the current user
        if (currentUserId != null && !currentUserId.equals(userId)) {
            log.warn("Received message for user {} but current user is {}. Message might be misrouted.", 
                    userId, currentUserId);
        }
        
        processReceivedMessage(message, "USER");
    }

    /**
     * Common logic to process received messages, with idempotency check and error handling.
     *
     * @param message The received message.
     * @param source The source of the message (ADMIN, BOT, USER) for tracking.
     */
    private void processReceivedMessage(Message message, String source) {
        String messageId = message.getId();
        
        try {
            // Get current attempt count
            int attempts = messageProcessingAttempts.getOrDefault(messageId, 0) + 1;
            messageProcessingAttempts.put(messageId, attempts);
            
            // If we've exceeded retry attempts, reject permanently
            if (attempts > MAX_PROCESSING_ATTEMPTS) {
                log.error("Message {} has exceeded maximum retry attempts ({}). Rejecting permanently.", 
                        messageId, MAX_PROCESSING_ATTEMPTS);
                messageProcessingAttempts.remove(messageId);
                throw new AmqpRejectAndDontRequeueException(
                        "Failed to process message after " + MAX_PROCESSING_ATTEMPTS + " attempts");
            }
            
            // Idempotency Check: See if a message with this ID already exists
            if (messageRepository.existsByMessageId(messageId)) {
                log.warn("Duplicate message detected, skipping processing for message ID: {}", messageId);
                // Clean up tracking
                messageProcessingAttempts.remove(messageId);
                return;
            }

            // Process the message
            log.info("[{}] Processing message ID: {}, attempt: {}", source, messageId, attempts);
            messagingService.saveReceivedMessage(message);
            
            log.info("[{}] Successfully processed message ID: {}", source, messageId);
            
            // Clean up tracking
            messageProcessingAttempts.remove(messageId);

        } catch (AmqpRejectAndDontRequeueException e) {
            // This will be rejected and not requeued
            throw e;
        } catch (Exception e) {
            // Log error details
            log.error("[{}] Error processing message ID {}: {}", source, messageId, e.getMessage(), e);
            
            // Determine whether to retry or send to DLQ
            int attempts = messageProcessingAttempts.getOrDefault(messageId, 0);
            if (attempts >= MAX_PROCESSING_ATTEMPTS) {
                log.error("Message {} has exceeded maximum retry attempts. Sending to DLQ.", messageId);
                messageProcessingAttempts.remove(messageId);
                throw new AmqpRejectAndDontRequeueException(
                        "Failed to process message after " + MAX_PROCESSING_ATTEMPTS + " attempts", e);
            } else {
                // Throw RuntimeException to trigger redelivery/retry mechanism
                throw new RuntimeException("Failed to process message " + messageId + ", attempt " + attempts, e);
            }
        }
    }

    /**
     * Listener for the Dead Letter Queue (DLQ) for monitoring and manual intervention.
     * Logs detailed information about failed messages.
     */
    @RabbitListener(queues = RabbitMQConfig.DLQ_NAME)
    public void receiveDlqMessage(@Payload org.springframework.amqp.core.Message failedMessage) {
        try {
            MessageProperties props = failedMessage.getMessageProperties();
            String originalExchange = props.getReceivedExchange();
            String originalRoutingKey = props.getReceivedRoutingKey();
            
            log.error("Received message on DLQ. Original Exchange: {}, Original Routing Key: {}", 
                    originalExchange, originalRoutingKey);
            
            // Extract message body - try to deserialize if possible
            try {
                Message originalMessage = objectMapper.readValue(failedMessage.getBody(), Message.class);
                log.error("Failed message content: ID={}, Sender={}, Recipient={}, Type={}", 
                        originalMessage.getId(), 
                        originalMessage.getSenderId(),
                        originalMessage.getRecipientId(),
                        originalMessage.getType());
            } catch (IOException e) {
                // If we can't deserialize as Message, log the raw content
                String body = new String(failedMessage.getBody());
                log.error("Unable to deserialize message. Raw content: {}", body);
            }
            
            // Extract headers for debugging
            Map<String, Object> headers = props.getHeaders();
            log.error("Message headers: {}", headers);
            
            // Optional: Store failed message in a database table for manual review
            // deadLetterRepository.save(new DeadLetterMessage(failedMessage));
            
        } catch (Exception e) {
            log.error("Error processing DLQ message: {}", e.getMessage(), e);
        }
    }
}