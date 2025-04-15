package com.enit.satellite_platform.modules.resource_management.image_management.services;

import com.enit.satellite_platform.modules.project_management.entities.Project;
import com.enit.satellite_platform.modules.project_management.repositories.ProjectRepository;
import com.enit.satellite_platform.modules.resource_management.image_management.entities.Image;
import com.enit.satellite_platform.modules.resource_management.image_management.repositories.ImageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.bson.types.ObjectId;


import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class ImageCleanupService {
    private static final Logger logger = LoggerFactory.getLogger(ImageCleanupService.class);

    @Value("${image.cleanup.default-retention-days:7}")
    private int defaultRetentionDays;

    @Value("${project.cleanup.default-retention-days:7}") // Use project default if image specific is not set
    private int projectDefaultRetentionDays;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ProjectRepository projectRepository; // Needed to check project status/retention

    @Autowired
    private ImageService imageService; // To call permanentlyDeleteImage

    /**
     * Scheduled task that runs daily to clean up soft-deleted images
     * that have exceeded their retention period.
     */
    @Scheduled(cron = "${image.cleanup.cron:0 0 1 * * ?}")
    @Transactional
    public void cleanupExpiredImages() {
        logger.info("Starting scheduled cleanup of expired soft-deleted images");

        Date now = new Date();
        List<Image> deletedImages = imageRepository.findByDeletedTrue(); // Need to add this method

        for (Image image : deletedImages) {
            try {
                if (shouldPermanentlyDelete(image, now)) {
                    imageService.permanentlyDeleteImage(image.getImageId());
                    logger.info("Permanently deleted expired image: {}", image.getImageId());
                }
            } catch (Exception e) {
                logger.error("Error processing cleanup for image: {}", image.getImageId(), e);
                // Decide if one failure should stop the whole batch
            }
        }

        logger.info("Completed scheduled cleanup of expired soft-deleted images");
    }

    /**
     * Determines if an image should be permanently deleted based on its retention period.
     * Considers both independently deleted images and images deleted due to project deletion.
     */
    private boolean shouldPermanentlyDelete(Image image, Date currentDate) {
        if (!image.isDeleted() || image.getDeletedAt() == null) {
            return false; // Not deleted or no deletion timestamp
        }

        Project project = image.getProject();
        int retentionDays;
        Date deletionTimestamp;

        if (project != null && project.isDeleted() && project.getDeletedAt() != null) {
            // Image was likely deleted as part of project deletion cascade
            // Use project's retention period and deletion time
            retentionDays = project.getRetentionDays() != null ? project.getRetentionDays() : projectDefaultRetentionDays;
            deletionTimestamp = project.getDeletedAt();
            logger.debug("Image {} associated with deleted project {}. Using project retention: {} days from {}",
                         image.getImageId(), project.getId(), retentionDays, deletionTimestamp);
        } else {
            // Image was deleted independently
            retentionDays = defaultRetentionDays;
            deletionTimestamp = image.getDeletedAt();
             logger.debug("Image {} deleted independently. Using default retention: {} days from {}",
                         image.getImageId(), retentionDays, deletionTimestamp);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(deletionTimestamp);
        calendar.add(Calendar.DAY_OF_MONTH, retentionDays);

        Date expirationDate = calendar.getTime();
        boolean expired = currentDate.after(expirationDate);
        if(expired) {
             logger.info("Image {} expired. Expiration date: {}, Current date: {}", image.getImageId(), expirationDate, currentDate);
        } else {
             logger.debug("Image {} not expired yet. Expiration date: {}, Current date: {}", image.getImageId(), expirationDate, currentDate);
        }
        return expired;
    }
}
