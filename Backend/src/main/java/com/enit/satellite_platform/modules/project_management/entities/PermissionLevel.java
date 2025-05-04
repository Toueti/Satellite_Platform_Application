package com.enit.satellite_platform.modules.project_management.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.stream.Stream;
import java.util.Arrays;

public enum PermissionLevel {
    READ,    // Can view project and image metadata
    EDITOR,  // Can view, add/delete/rename images, edit project details (implies READ)
    WRITE;   // Can do everything EDITOR can, plus share/unshare, delete project (implies EDITOR and READ)

    /**
     * Checks if this permission level includes the required permission level.
     * Assumes a hierarchy: WRITE > EDITOR > READ.
     *
     * @param requiredLevel The level required.
     * @return True if this level grants the required level, false otherwise.
     */
    public boolean includes(PermissionLevel requiredLevel) {
        if (requiredLevel == null) {
            return true;
        }
        return this.compareTo(requiredLevel) >= 0;
    }

    @JsonCreator
    public static PermissionLevel fromString(String value) {
        if (value == null) {
            return null; // Or throw exception
        }
        return Stream.of(PermissionLevel.values())
                .filter(level -> level.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown PermissionLevel value: '" + value +
                                   "'. Accepted values (case-insensitive) are: " +
                                   Arrays.toString(PermissionLevel.values())));
    }
}
