package com.enit.satellite_platform.modules.resource_management.image_management.services;

import com.enit.satellite_platform.modules.resource_management.image_management.entities.Image;
import com.enit.satellite_platform.modules.resource_management.image_management.repositories.ImageRepository;
import com.enit.satellite_platform.shared.service.AbstractCleanupService;
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
public class ImageCleanupService extends AbstractCleanupService<Image, String> {

    private static final Logger logger = LoggerFactory.getLogger(ImageCleanupService.class);

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ImageService imageService; // Needed for permanent delete logic

    @Value("${image.cleanup.enabled:true}")
    private boolean cleanupEnabled;

    @Value("${image.cleanup.retention-days:7}") // Default retention: 7 days
    private int retentionDays;

    // Note: The complex logic considering project deletion time is removed.
    // The AbstractCleanupService uses the entity's own deletedAt timestamp.
    // If an image is soft-deleted when a project is soft-deleted, its deletedAt
    // will be set then. If the project is restored, the image remains deleted
    // unless explicitly restored. If the project is permanently deleted, the
    // image (and its results) are permanently deleted immediately by cascade.
    // This simplifies the cleanup logic here.

    // Scheduled task entry point
    @Scheduled(cron = "${image.cleanup.cron:0 0 2 * * ?}") // Default: Run daily at 2 AM
    public void scheduledCleanupTask() {
        super.cleanupExpiredEntitiesTask(); // Call the abstract class's core logic
    }

    @Override
    protected Optional<List<Image>> findExpiredEntities(Date cutoffDate) {
        return imageRepository.findByDeletedTrueAndDeletedAtBefore(cutoffDate);
    }

    @Override
    protected void permanentlyDeleteEntity(Image image) {
        logger.info("Permanently deleting image ID: {}, Name: {}", image.getId(), image.getImageName());
        try {
            // ImageService.permanentlyDeleteImage handles cascade to results and file deletion
            // Cast the ID to String as expected by the service method
            imageService.permanentlyDeleteImage((String) image.getId());
            logger.info("Successfully permanently deleted image: {}", image.getId());
        } catch (ClassCastException cce) {
             logger.error("Error casting image ID to String for permanent deletion: ID={}", image.getId(), cce);
             throw new RuntimeException("Invalid ID type for image " + image.getId(), cce);
        } catch (Exception e) {
            logger.error("Error during permanent deletion of image {}: {}", image.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to permanently delete image " + image.getId(), e);
        }
    }

    @Override
    protected int getRetentionDays() {
        // Could add logic here to check parent project's retention if needed,
        // but keeping it simple based on image properties for now.
        return retentionDays;
    }

    @Override
    protected boolean isEnabled() {
        return cleanupEnabled;
    }

    @Override
    protected String getEntityName() {
        return "Image";
    }
}
