package com.enit.satellite_platform.modules.resource_management.utils.storage_management;

import com.enit.satellite_platform.modules.resource_management.utils.storage_management.storageImp.TemporaryFileSystemStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TemporaryFileCleanupService {

    private static final Logger log = LoggerFactory.getLogger(TemporaryFileCleanupService.class);

    private final TemporaryFileMetadataService metadataService;
    private final TemporaryFileSystemStorageService storageService; // Inject concrete type to call delete

    public TemporaryFileCleanupService(TemporaryFileMetadataService metadataService,
                                       TemporaryFileSystemStorageService storageService) {
        this.metadataService = metadataService;
        this.storageService = storageService;
        log.info("TemporaryFileCleanupService initialized.");
    }

    @Scheduled(cron = "${storage.temporary.cleanup-cron}")
    public void cleanupTemporaryFiles() {
        log.info("Starting temporary file cleanup task...");
        Instant now = Instant.now();
        Duration inactivityDuration = metadataService.getInactivityDuration();
        Duration postAccessDuration = metadataService.getPostAccessDuration();

        Set<String> fileIds = metadataService.getAllMetadataFileIds();
        AtomicInteger cleanedCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        log.debug("Found {} temporary file metadata entries to check.", fileIds.size());

        for (String fileId : fileIds) {
            try {
                metadataService.getMetadata(fileId).ifPresent(metadata -> {
                    boolean shouldDelete = false;
                    String reason = "";

                    // Rule 1: Delete if accessed and post-access duration exceeded
                    Duration timeSinceLastAccess = Duration.between(metadata.getLastAccessTime(), now);
                    if (metadata.getLastAccessTime().isAfter(metadata.getCreationTime()) && // Check if accessed at least once after creation
                        timeSinceLastAccess.compareTo(postAccessDuration) > 0) {
                        shouldDelete = true;
                        reason = "post-access duration exceeded (" + timeSinceLastAccess + " > " + postAccessDuration + ")";
                    }

                    // Rule 2: Delete if never accessed and inactivity duration exceeded
                    Duration timeSinceCreation = Duration.between(metadata.getCreationTime(), now);
                    // Check if lastAccessTime is effectively the same as creationTime (within a small tolerance if needed)
                    boolean neverAccessed = metadata.getLastAccessTime().equals(metadata.getCreationTime());
                    if (!shouldDelete && neverAccessed && timeSinceCreation.compareTo(inactivityDuration) > 0) {
                        shouldDelete = true;
                        reason = "inactivity duration exceeded (" + timeSinceCreation + " > " + inactivityDuration + ")";
                    }

                    if (shouldDelete) {
                        log.info("Scheduling deletion for temporary file {}: {}", fileId, reason);
                        try {
                            if(storageService.delete(fileId)) // This also deletes metadata
                                cleanedCount.incrementAndGet(); // Incrementing inside the try block after successful deletion
                        } catch (IOException e) {
                            log.error("Error deleting temporary file {}: {}", fileId, e.getMessage());
                            errorCount.incrementAndGet(); // Increment error count if deletion fails
                            // Metadata might still be deleted by the delete method's finally block
                        }
                    }
                });
                 // If metadata wasn't found (e.g., Redis TTL expired), try deleting the file just in case
                 if (!metadataService.getMetadata(fileId).isPresent()) {
                    log.warn("Metadata for file ID {} not found during cleanup. Attempting file deletion anyway.", fileId);
                     try {
                         storageService.delete(fileId);
                     } catch (IOException e) {
                         log.error("Error deleting orphaned temporary file {}: {}", fileId, e.getMessage());
                     }
                 }

            } catch (Exception e) {
                log.error("Unexpected error processing file ID {} during cleanup: {}", fileId, e.getMessage(), e);
                errorCount.incrementAndGet(); // Increment error count for unexpected errors
            }
        }

        // Note: Accurate cleanedCount requires tracking success within the delete call or re-checking metadata.
        // The current logic focuses on initiating deletion based on rules.
        log.info("Temporary file cleanup task finished. Checked {} entries. Errors encountered: {}", fileIds.size(), errorCount);
    }
}
