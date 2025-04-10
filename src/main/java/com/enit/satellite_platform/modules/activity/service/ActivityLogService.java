package com.enit.satellite_platform.modules.activity.service;

import com.enit.satellite_platform.modules.activity.entities.ActivityLog;
import com.enit.satellite_platform.modules.activity.repository.ActivityLogRepository;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.User;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private static final Logger logger = LoggerFactory.getLogger(ActivityLogService.class);
    private final ActivityLogRepository activityLogRepository;

    /**
     * Logs a user activity asynchronously.
     *
     * @param user    The User object performing the action. Can be null for system actions.
     * @param action  A string identifier for the action (e.g., "USER_LOGIN").
     * @param details Optional details about the activity.
     */
    @Async // Perform logging asynchronously to avoid blocking the main thread
    public void logActivity(User user, String action, String details) {
        Assert.hasText(action, "Action cannot be empty");

        String userId = (user != null && user.getId() != null) ? user.getId().toString() : null;
        String username = (user != null) ? user.getEmail() : "SYSTEM"; // Use email as username

        try {
            ActivityLog logEntry = new ActivityLog(userId, username, action, details);
            activityLogRepository.save(logEntry);
            logger.debug("Logged activity: User='{}', Action='{}'", username, action);
        } catch (Exception e) {
            // Log error but don't let logging failure break the main operation
            logger.error("Failed to log activity: User='{}', Action='{}', Details='{}'", username, action, details, e);
        }
    }

     /**
     * Logs a user activity asynchronously using userId and username directly.
     *
     * @param userId   The ID of the user performing the action. Can be null.
     * @param username The username (email) of the user. Can be "SYSTEM".
     * @param action   A string identifier for the action (e.g., "USER_LOGIN").
     * @param details  Optional details about the activity.
     */
    @Async
    public void logActivity(String userId, String username, String action, String details) {
        Assert.hasText(action, "Action cannot be empty");
        Assert.hasText(username, "Username cannot be empty"); // Require username even if SYSTEM

        try {
            ActivityLog logEntry = new ActivityLog(userId, username, action, details);
            activityLogRepository.save(logEntry);
            logger.debug("Logged activity: User='{}', Action='{}'", username, action);
        } catch (Exception e) {
            logger.error("Failed to log activity: User='{}', Action='{}', Details='{}'", username, action, details, e);
        }
    }


    // --- Retrieval Methods (Example) ---

    /**
     * Retrieves paginated activity logs for a specific user.
     *
     * @param userId   The ID of the user.
     * @param pageable Pagination information.
     * @return A Page of ActivityLog entries.
     */
    public Page<ActivityLog> getActivityLogsForUser(String userId, Pageable pageable) {
        Assert.hasText(userId, "User ID cannot be empty");
        return activityLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
    }

     /**
     * Retrieves paginated activity logs for a specific username (email).
     *
     * @param username The username (email) of the user.
     * @param pageable Pagination information.
     * @return A Page of ActivityLog entries.
     */
    public Page<ActivityLog> getActivityLogsForUsername(String username, Pageable pageable) {
        Assert.hasText(username, "Username cannot be empty");
        return activityLogRepository.findByUsernameOrderByTimestampDesc(username, pageable);
    }

    /**
     * Retrieves paginated activity logs by action type.
     *
     * @param action   The action type (e.g., "USER_LOGIN").
     * @param pageable Pagination information.
     * @return A Page of ActivityLog entries.
     */
    public Page<ActivityLog> getActivityLogsByAction(String action, Pageable pageable) {
         Assert.hasText(action, "Action cannot be empty");
        return activityLogRepository.findByActionOrderByTimestampDesc(action, pageable);
    }

     /**
     * Retrieves paginated activity logs within a time range.
     *
     * @param start    The start timestamp.
     * @param end      The end timestamp.
     * @param pageable Pagination information.
     * @return A Page of ActivityLog entries.
     */
    public Page<ActivityLog> getActivityLogsByTimeRange(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        Assert.notNull(start, "Start timestamp cannot be null");
        Assert.notNull(end, "End timestamp cannot be null");
        return activityLogRepository.findByTimestampBetweenOrderByTimestampDesc(start, end, pageable);
    }

}
