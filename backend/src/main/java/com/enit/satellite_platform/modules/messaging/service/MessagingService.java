package com.enit.satellite_platform.modules.messaging.service;

import com.enit.satellite_platform.modules.messaging.entities.Conversation;
import com.enit.satellite_platform.modules.messaging.entities.Message;
import com.enit.satellite_platform.modules.messaging.entities.MessageType;
import com.enit.satellite_platform.modules.messaging.repository.ConversationRepository;
import com.enit.satellite_platform.modules.user_management.normal_user_service.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.cloud.context.config.annotation.RefreshScope; // Import RefreshScope
import org.springframework.core.env.Environment; // Import Environment
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@RefreshScope // Add RefreshScope annotation
public class MessagingService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final RabbitMQProducerService producerService;
    private final Environment environment; // Inject Environment

    // Config keys for feature flags
    // Config keys for feature flags
    private static final String MESSAGING_ENABLED_PROPERTY = "messaging.rabbitmq.enabled"; // Global flag
    private static final String USER_TO_USER_ENABLED_PROPERTY = "messaging.user-to-user.enabled";
    private static final String USER_TO_ADMIN_ENABLED_PROPERTY = "messaging.user-to-admin.enabled";
    private static final String USER_TO_BOT_ENABLED_PROPERTY = "messaging.user-to-bot.enabled";

    /**
     * Handles saving a message received from RabbitMQ consumer.
     * Finds or creates the appropriate conversation and adds the message.
     *
     * @param message The message received from the queue.
     */
    @Transactional
    public void saveReceivedMessage(Message message) {
        log.info("Attempting to save received message ID: {}", message.getId());

        // Determine participants based on message type and sender/recipient logic
        Set<String> participants = determineParticipants(message);
        if (participants.isEmpty()) {
            log.error("Could not determine participants for message ID: {}. Skipping save.", message.getId());
            // Consider sending to an error queue or logging more details
            return;
        }

        // Find existing conversation or create a new one
        Conversation conversation = conversationRepository.findByParticipants(participants)
                .orElseGet(() -> createNewConversation(participants));

        // Add the message to the conversation's list
        // Ensure message ID is set if not already (though consumer check implies it is)
        if (message.getId() == null) {
            message.setId(UUID.randomUUID().toString());
            log.warn("Message ID was null, generated new ID: {}", message.getId());
        }
        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now());
        }

        conversation.getMessages().add(message);
        conversation.setLastUpdatedAt(message.getTimestamp());

        // Save the updated conversation (which includes the new message)
        conversationRepository.save(conversation);
        log.info("Message ID: {} successfully saved to conversation ID: {}", message.getId(), conversation.getId());
    }

    /**
     * Creates and sends a new message.
     * This involves setting message details, saving it via saveReceivedMessage (after producing/consuming),
     * and potentially sending it to RabbitMQ.
     *
     * @param senderId    ID of the user sending the message.
     * @param recipientId ID of the recipient user/admin/bot.
     * @param content     Message text content.
     * @param messageType Type of the message.
     * @return The created Message object.
     */
    @Transactional
    public Message sendMessage(String senderId, String recipientId, String content, MessageType messageType) {
        // *** Check if messaging is globally enabled ***
        if (!isMessagingGloballyEnabled()) {
             log.warn("Attempted to send message while messaging module is disabled globally.");
             throw new IllegalStateException("Messaging feature is currently disabled globally.");
        }
        // ********************************************

        // *** Check if the specific message type is enabled ***
        checkMessagingFeatureEnabled(messageType);
        // *****************************************************

        // Recipient validation might depend on type (user, admin role, bot ID)
        validateRecipientExists(recipientId, messageType);

        Message message = new Message();
        message.setId(UUID.randomUUID().toString()); // Generate unique ID
        message.setSenderId(senderId);
        message.setRecipientId(recipientId);
        message.setContent(content);
        message.setType(messageType);
        message.setTimestamp(LocalDateTime.now());
        // attachments and reactions are initially empty

        // Send message to RabbitMQ
        producerService.sendMessage(message, recipientId);
        log.info("Message ID: {} produced to RabbitMQ.", message.getId());

        // Note: The message is saved to DB *after* being consumed by RabbitMQConsumerService
        // This ensures atomicity via the consumer's transaction and handles idempotency.
        // If immediate feedback is needed, this method could return the message object
        // before it's confirmed saved, or wait for confirmation (more complex).

        return message; // Return the message object (not yet guaranteed saved)
    }


    private void validateRecipientExists(String recipientId, MessageType messageType) {
        switch (messageType) {
            case USER_TO_USER:
                validateUserExists(recipientId);
                break;
            case USER_TO_ADMIN:
                validateAdminExists(recipientId);
                break;
            case USER_TO_BOT:
                validateBotExists(recipientId);
                break;
            default:
                // No validation needed for other message types
                break;
        }
    }

    private void validateBotExists(String recipientId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'validateBotExists'");
    }

    private void validateAdminExists(String recipientId) {
        try {
            ObjectId userObjectId = new ObjectId(recipientId);
            if (!userRepository.existsById(userObjectId) || !userRepository.isAdmin(userObjectId)) {
                log.error("User with ID {} not found.", recipientId);
                throw new IllegalArgumentException("User not found: " + recipientId); // Or a custom exception
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid format for user ID {}: {}", recipientId, e.getMessage());
            throw new IllegalArgumentException("Invalid user ID format: " + recipientId);
        }
    }

    /**
     * Retrieves all conversations for a given user.
     *
     * @param userId The ID of the user.
     * @return List of conversations the user participates in.
     */
    public List<Conversation> getConversationsForUser(String userId) {
        validateUserExists(userId);
        return conversationRepository.findByParticipantsContaining(userId);
    }

    /**
     * Retrieves a specific conversation by its ID.
     *
     * @param conversationId The ID of the conversation.
     * @return Optional containing the conversation if found.
     */
    public Optional<Conversation> getConversationById(String conversationId) {
        return conversationRepository.findById(conversationId);
    }


    // --- Helper Methods ---

    private Conversation createNewConversation(Set<String> participants) {
        Conversation conversation = new Conversation();
        conversation.setParticipants(participants);
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setLastUpdatedAt(conversation.getCreatedAt());
        log.info("Creating new conversation for participants: {}", participants);
        // Initial save might happen here or rely on the first message save
        return conversation; // Return unsaved or initially saved conversation
    }

    private Set<String> determineParticipants(Message message) {
        Set<String> participants = new HashSet<>();
        // Basic logic, needs refinement based on actual recipient handling
        if (message.getSenderId() != null) {
            participants.add(message.getSenderId());
        }

        // How recipient is determined depends on how it's passed or stored
        // For now, assume a hypothetical recipientId field or logic based on type
        String recipientId = findRecipientId(message); // Placeholder for recipient logic

        if (recipientId != null) {
            participants.add(recipientId);
        } else if (message.getType() == MessageType.USER_TO_ADMIN || message.getType() == MessageType.ADMIN_TO_USER) {
            // Add a generic admin identifier or find specific admin based on context
            participants.add("ADMIN_GROUP"); // Placeholder
        } else if (message.getType() == MessageType.USER_TO_BOT || message.getType() == MessageType.BOT_TO_USER) {
            // Add a generic bot identifier
            participants.add("BOT_SERVICE"); // Placeholder
        }

        // Ensure at least two participants for a valid conversation (adjust if needed)
        if (participants.size() < 2 && message.getType() == MessageType.USER_TO_USER) {
             log.warn("Could not determine recipient for USER_TO_USER message ID: {}", message.getId());
             return Set.of(); // Return empty set if recipient unclear
        }

        return participants;
    }

    // Placeholder - needs actual implementation based on how recipient is identified
    private String findRecipientId(Message message) {
        // Logic to find the recipient ID.
        // This might involve looking at message properties, headers from RabbitMQ,
        // or having the recipient ID explicitly in the Message model (needs adding).
        // For USER_TO_USER, it's crucial.
        // Let's assume for now it needs to be added to the Message model.
        return message.getRecipientId(); // If added to Message model
    }


    private void validateUserExists(String userId) {
        try {
            ObjectId userObjectId = new ObjectId(userId);
            if (!userRepository.existsById(userObjectId)) {
                log.error("User with ID {} not found.", userId);
                throw new IllegalArgumentException("User not found: " + userId); // Or a custom exception
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid format for user ID {}: {}", userId, e.getMessage());
            throw new IllegalArgumentException("Invalid user ID format: " + userId);
        }
    }

    // Placeholder for recipient validation logic
    // private void validateRecipientExists(String recipientId, MessageType messageType) { ... }

    private void checkMessagingFeatureEnabled(MessageType messageType) {
        boolean enabled = true;
        String propertyKey = null;

        switch (messageType) {
            case USER_TO_USER:
                propertyKey = USER_TO_USER_ENABLED_PROPERTY;
                enabled = environment.getProperty(propertyKey, Boolean.class, true); // Default true
                break;
            case USER_TO_ADMIN:
                // Note: ADMIN_TO_USER replies might implicitly be allowed if USER_TO_ADMIN is.
                propertyKey = USER_TO_ADMIN_ENABLED_PROPERTY;
                enabled = environment.getProperty(propertyKey, Boolean.class, true); // Default true
                break;
            case USER_TO_BOT:
                 // Note: BOT_TO_USER replies might implicitly be allowed if USER_TO_BOT is.
                propertyKey = USER_TO_BOT_ENABLED_PROPERTY;
                enabled = environment.getProperty(propertyKey, Boolean.class, true); // Default true
                break;
            // ADMIN_TO_USER and BOT_TO_USER might not need explicit checks if they are replies
            // Or add separate flags if needed.
            case ADMIN_TO_USER:
            case BOT_TO_USER:
                 // Assuming replies are allowed if the initial direction is enabled.
                 break;
            default:
                // Should not happen with enum, but good practice
                log.warn("Unknown message type encountered in feature check: {}", messageType);
                break;
        }

        if (!enabled) {
            log.warn("Attempted to send message of type {} which is currently disabled by configuration ({}).", messageType, propertyKey);
            throw new IllegalStateException("Messaging feature (" + messageType + ") is currently disabled.");
        }
    }

     private boolean isMessagingGloballyEnabled() {
        return environment.getProperty(MESSAGING_ENABLED_PROPERTY, Boolean.class, true); // Default true
    }

    // TODO: Implement methods for adding reactions, handling attachments
    // TODO: Implement daily message limit check if required
}
