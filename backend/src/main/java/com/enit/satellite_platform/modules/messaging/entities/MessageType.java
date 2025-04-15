package com.enit.satellite_platform.modules.messaging.entities;

/**
 * Enum representing the different types of messages in the system.
 */
public enum MessageType {
    /**
     * A message sent directly between two regular users.
     */
    USER_TO_USER,

    /**
     * A message sent from a user to an administrator (e.g., support request).
     */
    USER_TO_ADMIN,

    /**
     * A message sent from a user to an automated chatbot.
     */
    USER_TO_BOT,

    /**
     * A reply from an administrator to a user.
     */
    ADMIN_TO_USER,

    /**
     * A message from a chatbot to a user.
     */
    BOT_TO_USER
}
