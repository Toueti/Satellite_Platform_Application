package com.enit.satellite_platform.modules.resource_management.image_management.services;

import com.enit.satellite_platform.modules.resource_management.image_management.entities.ProcessingResults;
import com.enit.satellite_platform.modules.resource_management.image_management.repositories.ResultsRepository;
import com.enit.satellite_platform.shared.service.AbstractCleanupService; // Import base class
import org.bson.types.ObjectId; // Import ObjectId
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@EnableScheduling // Ensure scheduling is enabled
public class ProcessingResultsCleanupService extends AbstractCleanupService<ProcessingResults, ObjectId> {

    private static final Logger logger = LoggerFactory.getLogger(ProcessingResultsCleanupService.class);

    @Autowired
    private ResultsRepository resultsRepository;

    @Autowired
    private ProcessingResultsService processingResultsService; // Needed for permanent delete logic

    @Value("${cleanup.processing-results.enabled:true}")
    private boolean cleanupEnabled;

    @Value("${cleanup.processing-results.retention-days:7}") // Default retention: 7 days
    private int retentionDays;

    // Scheduled task entry point
    @Scheduled(cron = "${cleanup.processing-results.cron:0 0 3 * * ?}") // Default: Run daily at 3 AM
    public void scheduledCleanupTask() {
        super.cleanupExpiredEntitiesTask(); // Call the abstract class's core logic
    }

    @Override
    protected Optional<List<ProcessingResults>> findExpiredEntities(Date cutoffDate) {
        return resultsRepository.findByDeletedTrueAndDeletedAtBefore(cutoffDate);
    }

    @Override
    protected void permanentlyDeleteEntity(ProcessingResults result) {
        logger.info("Permanently deleting ProcessingResults ID: {}", result.getId());
        try {
            // ProcessingResultsService.permanentlyDeleteProcessingResults handles file deletion
            processingResultsService.permanentlyDeleteProcessingResults((ObjectId) result.getId());
            logger.info("Successfully permanently deleted ProcessingResults: {}", result.getId());
        } catch (ClassCastException cce) {
             logger.error("Error casting ProcessingResults ID to ObjectId for permanent deletion: ID={}", result.getId(), cce);
             throw new RuntimeException("Invalid ID type for ProcessingResults " + result.getId(), cce);
        } catch (Exception e) {
            logger.error("Error during permanent deletion of ProcessingResults {}: {}", result.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to permanently delete ProcessingResults " + result.getId(), e);
        }
    }

    @Override
    protected int getRetentionDays() {
        return retentionDays;
    }

    @Override
    protected boolean isEnabled() {
        return cleanupEnabled;
    }

    @Override
    protected String getEntityName() {
        return "ProcessingResults";
    }
}
