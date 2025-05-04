package com.enit.satellite_platform.modules.messaging.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.enit.satellite_platform.modules.messaging.entities.Message;

/**
 * Repository interface for Message objects.
 * Primarily used for checking message existence for idempotency,
 * even though Messages are embedded within Conversations.
 */
@Repository
public interface MessageRepository extends MongoRepository<Message, String> {

    /**
     * Checks if a message with the given ID exists within any conversation.
     * Note: This requires a specific query implementation as Messages are embedded.
     * The default existsById might not work as expected on embedded documents directly.
     *
     * @param messageId The ID of the message to check.
     * @return true if a message with this ID exists in any conversation, false otherwise.
     */
    @Query(value = "{ 'messages._id': ?0 }", exists = true)
    boolean existsByMessageId(String messageId);

    // Define other message-specific queries if needed, operating on the embedded structure.
}
