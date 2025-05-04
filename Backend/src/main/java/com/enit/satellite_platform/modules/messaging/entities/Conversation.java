package com.enit.satellite_platform.modules.messaging.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Represents a conversation thread between participants.
 * Contains an embedded list of messages.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "conversations")
public class Conversation {

    @Id
    private String id;

    @Indexed // Index for faster querying by participants
    private Set<String> participants; // Set of user IDs involved in the conversation

    private List<Message> messages = new ArrayList<>(); // Embedded list of messages

    private LocalDateTime createdAt;

    private LocalDateTime lastUpdatedAt; // Timestamp of the last message or update

    // Optional: Add conversation title, type (direct, group), etc.
}
