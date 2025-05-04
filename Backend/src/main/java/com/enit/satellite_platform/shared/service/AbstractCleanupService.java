package com.enit.satellite_platform.shared.service;

import com.enit.satellite_platform.shared.model.SoftDeletable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Abstract base class for scheduled cleanup services that permanently delete
 * soft-deleted entities after a configurable retention period.
 *
 * @param <T>  The type of the entity, must implement SoftDeletable.
 * @param <ID> The type of the entity's ID.
 */
public abstract class AbstractCleanupService<T extends SoftDeletable, ID> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Finds entities that are marked as deleted and whose deletion timestamp
     * is older than the specified cutoff date.
     *
     * @param cutoffDate The date threshold.
     * @return An Optional list of expired entities.
     */
    protected abstract Optional<List<T>> findExpiredEntities(Date cutoffDate);

    /**
     * Permanently deletes a single entity. This method should handle any
     * associated resource cleanup (e.g., deleting files from storage).
     *
     * @param entity The entity to permanently delete.
     */
    protected abstract void permanentlyDeleteEntity(T entity);

    /**
     * Gets the retention period in days for the specific entity type.
     *
     * @return The retention period in days.
     */
    protected abstract int getRetentionDays();

    /**
     * Checks if the cleanup job is enabled for the specific entity type.
     *
     * @return true if enabled, false otherwise.
     */
    protected abstract boolean isEnabled();

    /**
     * Gets the name of the entity type for logging purposes.
     *
     * @return The entity name (e.g., "Project", "Image").
     */
    protected abstract String getEntityName();

    /**
     * The core scheduled method. Subclasses should override this and add the
     * {@link Scheduled} annotation with the appropriate cron expression property.
     * This method contains the common logic for finding and deleting expired entities.
     */
    @Transactional
    public void cleanupExpiredEntitiesTask() {
        if (!isEnabled()) {
            logger.info("{} cleanup job is disabled.", getEntityName());
            return;
        }

        int retention = getRetentionDays();
        logger.info("Starting scheduled cleanup job for soft-deleted {} entities older than {} days.", getEntityName(), retention);
        Instant cutoffInstant = Instant.now().minus(retention, ChronoUnit.DAYS);
        Date cutoffDate = Date.from(cutoffInstant);

        try {
            List<T> entitiesToDelete = findExpiredEntities(cutoffDate).orElse(List.of());

            if (entitiesToDelete.isEmpty()) {
                logger.info("No soft-deleted {} entities found older than the retention period ({} days).", getEntityName(), retention);
                return;
            }

            logger.info("Found {} soft-deleted {} entities older than {} days to permanently delete.",
                    entitiesToDelete.size(), getEntityName(), retention);

            int successCount = 0;
            int failureCount = 0;

            for (T entity : entitiesToDelete) {
                try {
                    permanentlyDeleteEntity(entity);
                    successCount++;
                } catch (Exception e) {
                    // Log error but continue with the next entity
                    logger.error("Failed to permanently delete {} with ID {}: {}", getEntityName(), entity.getId(), e.getMessage(), e);
                    failureCount++;
                }
            }

            logger.info("{} cleanup job finished. Successfully deleted: {}, Failed: {}", getEntityName(), successCount, failureCount);

        } catch (Exception e) {
            logger.error("Error during {} cleanup job execution: {}", getEntityName(), e.getMessage(), e);
        }
    }
}
