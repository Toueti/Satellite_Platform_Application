package com.enit.satellite_platform.modules.messaging.entities;

/**
 * Enum representing the types of reactions a user can add to a message.
 */
public enum ReactionType {
    LIKE("👍"),
    LOVE("❤️"),
    LAUGH("😂"),
    WOW("😮"),
    SAD("😢"),
    ANGRY("😠");

    private final String emoji;

    ReactionType(String emoji) {
        this.emoji = emoji;
    }

    public String getEmoji() {
        return emoji;
    }
}
