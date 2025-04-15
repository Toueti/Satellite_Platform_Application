package com.enit.satellite_platform.modules.messaging.service;

import com.enit.satellite_platform.modules.messaging.entities.Conversation;
import com.enit.satellite_platform.modules.messaging.entities.Message;
import com.enit.satellite_platform.modules.messaging.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope; // Import RefreshScope
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@RefreshScope // Add RefreshScope annotation
public class MessageCleanupService {

    private final ConversationRepository conversationRepository;
    private final Environment environment;

    // Configuration keys
    private static final String CLEANUP_ENABLED_PROPERTY = "messaging.cleanup.enabled";
    private static final String RETENTION_DAYS_PROPERTY = "messaging.retention.days";
    private static final String MAX_MESSAGES_PROPERTY = "messaging.max.messages.per.conversation";

    // Default values
    private static final boolean DEFAULT_CLEANUP_ENABLED = true;
    private static final int DEFAULT_RETENTION_DAYS = 30;
    private static final int DEFAULT_MAX_MESSAGES = 500;

    // Schedule to run daily at 3 AM
    @Scheduled(cron = "0 0 3 * * ?") // Seconds Minutes Hours DayOfMonth Month DayOfWeek
    @Transactional
    public void cleanupOldMessages() {
        boolean cleanupEnabled = environment.getProperty(CLEANUP_ENABLED_PROPERTY, Boolean.class, DEFAULT_CLEANUP_ENABLED);
        if (!cleanupEnabled) {
            log.info("Message cleanup job is disabled via configuration.");
            return;
        }

        log.info("Starting scheduled message cleanup job...");

        int retentionDays = environment.getProperty(RETENTION_DAYS_PROPERTY, Integer.class, DEFAULT_RETENTION_DAYS);
        int maxMessages = environment.getProperty(MAX_MESSAGES_PROPERTY, Integer.class, DEFAULT_MAX_MESSAGES);

        LocalDateTime cutoffDate = (retentionDays > 0) ? LocalDateTime.now().minusDays(retentionDays) : null;

        if (cutoffDate == null && maxMessages <= 0) {
            log.info("Both retention days and max messages per conversation are disabled (<= 0). No cleanup needed.");
            return;
        }

        long conversationsProcessed = 0;
        long messagesRemoved = 0;

        // Process conversations in batches if necessary, for now fetching all
        // Consider using MongoTemplate for more efficient bulk updates if performance becomes an issue.
        List<Conversation> conversations = conversationRepository.findAll(); // Potential performance issue with huge number of conversations

        for (Conversation conversation : conversations) {
            List<Message> messages = conversation.getMessages();
            if (messages == null || messages.isEmpty()) {
                continue;
            }

            List<Message> messagesToKeep = new ArrayList<>(messages);
            long initialCount = messagesToKeep.size();
            boolean conversationUpdated = false;

            // 1. Apply retention policy (if enabled)
            if (cutoffDate != null) {
                messagesToKeep.removeIf(message -> message.getTimestamp() != null && message.getTimestamp().isBefore(cutoffDate));
                if (messagesToKeep.size() < initialCount) {
                    conversationUpdated = true;
                }
            }

            // 2. Apply max messages policy (if enabled and needed)
            if (maxMessages > 0 && messagesToKeep.size() > maxMessages) {
                // Sort by timestamp descending (newest first) and keep only the latest 'maxMessages'
                messagesToKeep = messagesToKeep.stream()
                        .sorted(Comparator.comparing(Message::getTimestamp, Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(maxMessages)
                        .collect(Collectors.toList());
                conversationUpdated = true;
            }

            // 3. Save if changes were made
            if (conversationUpdated) {
                long removedCount = initialCount - messagesToKeep.size();
                messagesRemoved += removedCount;
                // Ensure the remaining messages are sorted chronologically if needed (limit might mess order)
                messagesToKeep.sort(Comparator.comparing(Message::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder())));
                conversation.setMessages(messagesToKeep);
                // Update lastUpdatedAt if the latest message was removed? Optional.
                conversationRepository.save(conversation);
                log.debug("Cleaned up {} messages from conversation {}", removedCount, conversation.getId());
            }
            conversationsProcessed++;
        }

        log.info("Finished scheduled message cleanup job. Processed {} conversations, removed {} messages.", conversationsProcessed, messagesRemoved);
    }
}
