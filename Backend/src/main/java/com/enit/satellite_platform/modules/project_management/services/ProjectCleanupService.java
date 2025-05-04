/**
 * Service responsible for cleaning up "soft-deleted" projects after a retention period.
 * 
 * <p>This service extends AbstractCleanupService to provide scheduled cleanup of project entities
 * that have been marked as deleted. Once the retention period has passed, projects are permanently
 * removed from the system.</p>
 * 
 * <p>Configuration properties:</p>
 * <ul>
 *     <li>{@code project.cleanup.enabled} - Controls whether cleanup is active (default: true)</li>
 *     <li>{@code project.cleanup.retention-days} - Days to retain deleted projects before permanent removal (default: 7)</li>
 *     <li>{@code project.cleanup.cron} - Schedule for cleanup job execution (default: daily at 1 AM)</li>
 * </ul>
 * 
 * <p>When a project is permanently deleted, notifications are sent to the project owner and all users
 * with whom the project was shared.</p>
 */
package com.enit.satellite_platform.modules.project_management.services;

import com.enit.satellite_platform.modules.project_management.entities.Project;
import com.enit.satellite_platform.modules.project_management.repositories.ProjectRepository;
import com.enit.satellite_platform.shared.service.AbstractCleanupService;
import com.enit.satellite_platform.shared.utils.NotificationService;
import org.bson.types.ObjectId;
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
@EnableScheduling
public class ProjectCleanupService extends AbstractCleanupService<Project, ObjectId> {

    private static final Logger logger = LoggerFactory.getLogger(ProjectCleanupService.class);

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectService projectService; // Needed for permanent delete logic

    @Autowired
    private NotificationService notificationService; // Needed for notifications

    @Value("${project.cleanup.enabled:true}")
    private boolean cleanupEnabled;

    @Value("${project.cleanup.retention-days:7}") // Default retention: 7 days
    private int retentionDays;

    @Value("${project.cleanup.cron:0 0 1 * * ?}") // Default: Run daily at 1 AM
    private String cronExpression;


    // Scheduled task entry point
    @Scheduled(cron = "${project.cleanup.cron:0 0 1 * * ?}")
    public void scheduledCleanupTask() {
        super.cleanupExpiredEntitiesTask(); // Call the abstract class's core logic
    }

    @Override
    protected Optional<List<Project>> findExpiredEntities(Date cutoffDate) {
        // Use the method inherited/defined in ProjectRepository
        return projectRepository.findByDeletedTrueAndDeletedAtBefore(cutoffDate);
    }

    @Override
    protected void permanentlyDeleteEntity(Project project) {
        logger.info("Permanently deleting project ID: {}, Name: {}", project.getId(), project.getProjectName());
        try {
            // Notify users before deletion
            notifyUsersAboutPermanentDeletion(project);

            // Call the permanent delete method in ProjectService
            // Assuming projectService has a method like permanentlyDeleteProject(ObjectId id)
            // If it only has deleteProject(ObjectId id) which does soft delete, this needs adjustment
            // or ProjectService needs a permanent delete method.
            projectService.permanentlyDeleteProject(new ObjectId(project.getId())); // Assuming this method exists and handles cascade

            logger.info("Successfully permanently deleted project: {}", project.getId());
        } catch (Exception e) {
            logger.error("Error during permanent deletion of project {}: {}", project.getId(), e.getMessage(), e);
            // Re-throw to ensure transaction rollback if desired, or handle accordingly
            throw new RuntimeException("Failed to permanently delete project " + project.getId(), e);
        }
    }

    @Override
    protected int getRetentionDays() {
        // Allow project-specific retention override if implemented, otherwise use default
        // For now, just using the configured default.
        // If project.getRetentionDays() was used, need to handle null check here.
        return retentionDays;
    }

    @Override
    protected boolean isEnabled() {
        return cleanupEnabled;
    }

    @Override
    protected String getEntityName() {
        return "Project";
    }

    /**
     * Notifies all users who had access to the project about its permanent deletion.
     * (Moved from old implementation)
     */
    private void notifyUsersAboutPermanentDeletion(Project project) {
        if (project == null) return;

        // Notify the owner
        if (project.getOwner() != null && project.getOwner().getEmail() != null) {
            notificationService.sendNotification(
                project.getOwner().getEmail(),
                "Project Permanently Deleted",
                "Your project '" + project.getProjectName() + "' has been permanently deleted after the retention period."
            );
        } else {
             logger.warn("Project {} has no owner or owner email for deletion notification.", project.getId());
        }

        // Notify users with shared access
        if (project.getSharedUsers() != null) {
            project.getSharedUsers().forEach((userId, permission) -> {
                try {
                    // Assuming projectService can resolve user email from ObjectId
                    String userEmail = projectService.getUserEmailById(userId);
                    if (userEmail != null) {
                        notificationService.sendNotification(
                            userEmail,
                            "Shared Project Permanently Deleted",
                            "The project '" + project.getProjectName() + "' that was shared with you has been permanently deleted."
                        );
                    } else {
                         logger.warn("Could not find email for user ID {} to notify about project {} deletion.", userId, project.getId());
                    }
                } catch (Exception e) {
                    logger.error("Failed to notify user {} about project {} deletion: {}", userId, project.getId(), e.getMessage());
                }
            });
        }
    }
}
