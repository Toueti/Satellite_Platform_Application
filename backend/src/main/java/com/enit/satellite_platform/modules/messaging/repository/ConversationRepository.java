package com.enit.satellite_platform.modules.messaging.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.enit.satellite_platform.modules.messaging.entities.Conversation;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ConversationRepository extends MongoRepository<Conversation, String> {

    /**
     * Finds conversations where the participant set exactly matches the provided set.
     * Useful for finding existing 1-on-1 chats.
     *
     * @param participants Set of participant user IDs.
     * @return Optional containing the conversation if found.
     */
    Optional<Conversation> findByParticipants(Set<String> participants);

    /**
     * Finds all conversations where the given user ID is a participant.
     *
     * @param participantId The user ID to search for.
     * @return List of conversations the user is part of.
     */
    List<Conversation> findByParticipantsContaining(String participantId);

    // Add other custom query methods as needed, e.g., find by participants and last updated time.
}
