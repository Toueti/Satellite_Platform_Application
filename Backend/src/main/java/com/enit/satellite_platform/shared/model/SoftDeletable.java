package com.enit.satellite_platform.shared.model;

import java.util.Date;

/**
 * Interface for entities that support soft deletion.
 */
public interface SoftDeletable {

    /**
     * Gets the unique identifier of the entity.
     * Should return String or ObjectId depending on the entity.
     *
     * @return The ID of the entity.
     */
    Object getId(); // Use Object to accommodate String or ObjectId

    /**
     * Checks if the entity is marked as deleted.
     *
     * @return true if the entity is soft-deleted, false otherwise.
     */
    boolean isDeleted();

    /**
     * Gets the timestamp when the entity was soft-deleted.
     *
     * @return The deletion timestamp, or null if not deleted.
     */
    Date getDeletedAt();

    /**
     * Sets the soft-deleted status of the entity.
     *
     * @param deleted The deleted status to set.
     */
    void setDeleted(boolean deleted);
    /**
     * Sets the timestamp when the entity was soft-deleted.
     *
     * @param deletedAt The deletion timestamp to set.
     */
    void setDeletedAt(Date deletedAt);
}
