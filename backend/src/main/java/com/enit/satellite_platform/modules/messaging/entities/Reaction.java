package com.enit.satellite_platform.modules.messaging.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a reaction added by a user to a message.
 * This class is intended to be embedded within the Message document.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reaction {

    private String userId; // ID of the user who reacted

    private ReactionType reactionType; // The type of reaction (e.g., LIKE, LOVE)

    private LocalDateTime timestamp; // When the reaction was added

    // Note: No @Id or @Document annotation as this is meant to be embedded.
}
