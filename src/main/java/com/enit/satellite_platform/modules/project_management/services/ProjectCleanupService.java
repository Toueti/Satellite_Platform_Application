package com.enit.satellite_platform.modules.project_management.services;

import com.enit.satellite_platform.modules.project_management.entities.Project;
import com.enit.satellite_platform.modules.project_management.repositories.ProjectRepository;
import com.enit.satellite_platform.shared.utils.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.bson.types.ObjectId;

@Service
public class ProjectCleanupService {
    private static final Logger logger = LoggerFactory.getLogger(ProjectCleanupService.class);

    @Value("${project.cleanup.default-retention-days:7}")
    private int defaultRetentionDays;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private NotificationService notificationService;

    /**
     * Scheduled task that runs daily to clean up soft-deleted projects
     * that have exceeded their retention period.
     */
    @Scheduled(cron = "${project.cleanup.cron:0 0 0 * * ?}") // Default: Run at midnight every day
    @Transactional
    public void cleanupExpiredProjects() {
        logger.info("Starting scheduled cleanup of expired soft-deleted projects");
        
        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        
        List<Project> deletedProjects = projectRepository.findByDeletedTrue();
        
        for (Project project : deletedProjects) {
            try {
                if (shouldPermanentlyDelete(project, now)) {
                    // Notify all users who had access to the project
                    notifyUsersAboutPermanentDeletion(project);
                    
                    // Perform permanent deletion
                    projectService.deleteProject(new ObjectId(project.getId()));
                    
                    logger.info("Permanently deleted expired project: {}", project.getId());
                }
            } catch (Exception e) {
                logger.error("Error processing cleanup for project: {}", project.getId(), e);
            }
        }
        
        logger.info("Completed scheduled cleanup of expired soft-deleted projects");
    }

    /**
     * Determines if a project should be permanently deleted based on its retention period.
     */
    private boolean shouldPermanentlyDelete(Project project, Date currentDate) {
        if (!project.isDeleted() || project.getDeletedAt() == null) {
            return false;
        }

        int retentionDays = project.getRetentionDays() != null ? 
                           project.getRetentionDays() : 
                           defaultRetentionDays;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(project.getDeletedAt());
        calendar.add(Calendar.DAY_OF_MONTH, retentionDays);
        
        Date expirationDate = calendar.getTime();
        return currentDate.after(expirationDate);
    }

    /**
     * Notifies all users who had access to the project about its permanent deletion.
     */
    private void notifyUsersAboutPermanentDeletion(Project project) {
        // Notify the owner
        notificationService.sendNotification(
            project.getOwner().getEmail(),
            "Project Permanently Deleted",
            "Your project '" + project.getProjectName() + "' has been permanently deleted after the retention period."
        );

        // Notify users with shared access
        project.getSharedUsers().forEach((userId, permission) -> {
            try {
                String userEmail = projectService.getUserEmailById(userId);
                if (userEmail != null) {
                    notificationService.sendNotification(
                        userEmail,
                        "Shared Project Permanently Deleted",
                        "The project '" + project.getProjectName() + "' that was shared with you has been permanently deleted."
                    );
                }
            } catch (Exception e) {
                logger.error("Failed to notify user {} about project deletion", userId, e);
            }
        });
    }
}
