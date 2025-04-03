package com.enit.satellite_platform.modules.messaging.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
// Note: No @Document annotation as this is intended to be embedded within Conversation

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single message within a conversation.
 * This class is intended to be embedded within the Conversation document.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id // Crucial for idempotency check even when embedded
    private String id;

    private String senderId; // ID of the user who sent the message

    private String content; // Text content of the message

    private LocalDateTime timestamp; // Time when the message was sent/created

    private MessageType messageType; // Type of message (USER_TO_USER, USER_TO_ADMIN, etc.)

    private List<Attachment> attachments = new ArrayList<>(); // List of attached file metadata

    private List<Reaction> reactions = new ArrayList<>(); // List of reactions to this message

    // Optional fields: readStatus, editTimestamp, etc.
}
