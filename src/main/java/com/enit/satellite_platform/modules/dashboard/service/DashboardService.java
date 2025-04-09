package com.enit.satellite_platform.modules.dashboard.service;

import com.enit.satellite_platform.audit.AuditEvent;
import com.enit.satellite_platform.audit.AuditEventRepository;
import com.enit.satellite_platform.modules.dashboard.dto.DashboardStatsDto;
import com.enit.satellite_platform.modules.project_management.model.Project;
import com.enit.satellite_platform.modules.project_management.repositories.ProjectRepository;
import com.enit.satellite_platform.modules.resource_management.image_management.models.Image;
import com.enit.satellite_platform.modules.resource_management.image_management.models.ProcessingResults;
import com.enit.satellite_platform.modules.resource_management.image_management.repositories.ImageRepository;
import com.enit.satellite_platform.modules.resource_management.image_management.repositories.ResultsRepository;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.models.User;
import com.enit.satellite_platform.modules.user_management.normal_user_service.repositories.UserRepository;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    // Action types from AuditAspect (consider defining these in a shared place)
    private static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    private static final String PROJECT_ACCESS_SUCCESS = "PROJECT_ACCESS_SUCCESS";
    // Add other relevant action types if needed for feed/trends

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ResultsRepository resultsRepository;

    @Autowired
    private UserRepository userRepository;

    private static final int RECENT_ACTIVITY_LIMIT = 5;
    private static final int RECENT_PROJECTS_LIMIT = 5;
    private static final int RECENT_IMAGES_LIMIT = 5;
    private static final int ACTIVITY_TREND_DAYS = 7;

    /**
     * Retrieves dashboard statistics for the given user email.
     *
     * @param userEmail The email of the user to generate stats for.
     * @return A DashboardStatsDto object containing statistics about the user's
     *         projects, images, treatments, and audit events.
     * @throws UsernameNotFoundException If the user with the given email is not
     *                                   found.
     */
    public DashboardStatsDto getDashboardStats(String userEmail) {
        logger.info("Generating dashboard stats for user: {}", userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
        String userId = user.getId();

        DashboardStatsDto stats = new DashboardStatsDto();

        // --- Project Stats ---
        List<Project> ownedProjects = projectRepository.findByOwner(user);
        stats.setTotalProjects(ownedProjects.size());
        stats.setSharedByUserCount(ownedProjects.stream().filter(p -> !p.getSharedUsers().isEmpty()).count());
        stats.setSharedWithUserCount(projectRepository.countBySharedUsersContainsKey(user));
        stats.setRecentlyAccessedProjects(calculateRecentlyAccessedProjects(ownedProjects, user));

        // --- Image & Storage Stats ---
        List<Image> allUserImages = imageRepository.findAllByOwnerId(userId);
        stats.setTotalImages(allUserImages.size());
        stats.setTotalStorageUsedBytes(allUserImages.stream().mapToLong(Image::getFileSize).sum());
        stats.setRecentImageUploads(calculateRecentImageUploads(allUserImages));

        // --- Processing Results (Treatments) Stats ---
        List<ProcessingResults> allUserResults = resultsRepository.findAllByOwnerId(userId);
        stats.setTotalTreatments(allUserResults.size());
        stats.setProcessingStatusSummary(calculateProcessingStatusSummary(allUserResults));
        stats.setMostUsedProcessingType(calculateMostUsedProcessingType(allUserResults));

        // --- Averages ---
        stats.setAverageImagesPerProject(
                stats.getTotalProjects() == 0 ? 0 : (double) stats.getTotalImages() / stats.getTotalProjects());
        stats.setAverageTreatmentsPerImage(
                stats.getTotalImages() == 0 ? 0 : (double) stats.getTotalTreatments() / stats.getTotalImages());

        // --- Audit Event Stats ---
        stats.setLastPlatformLoginTime(findLastEventTime(userId, LOGIN_SUCCESS));
        stats.setLastProjectAccessTime(findLastEventTime(userId, PROJECT_ACCESS_SUCCESS));
        stats.setTotalProjectAccesses(auditEventRepository.countByUserIdAndActionType(userId, PROJECT_ACCESS_SUCCESS));
        stats.setRecentActivityFeed(calculateRecentActivityFeed(userId));
        stats.setMostFrequentAction(calculateMostFrequentAction(userId));
        stats.setActivityTrend(calculateActivityTrend(userId));

        logger.info("Dashboard stats generated successfully for user: {}", userEmail);
        return stats;
    }

    // --- Private Helper Methods ---

    /**
     * Finds the timestamp of the most recent event for the given user and action
     * type.
     *
     * @param userId     The ID of the user.
     * @param actionType The type of the action (e.g. {@link #LOGIN_SUCCESS} or
     *                   {@link #PROJECT_ACCESS_SUCCESS}).
     * @return The timestamp of the most recent event, or null if no such event was
     *         found.
     */
    private Instant findLastEventTime(String userId, String actionType) {
        AuditEvent latestEvent = auditEventRepository.findTopByUserIdAndActionTypeOrderByTimestampDesc(userId,
                actionType);
        if (latestEvent != null && latestEvent.getTimestamp() != null) {
            return latestEvent.getTimestamp().toInstant(ZoneOffset.UTC);
        }
        return null;
    }

    /**
     * Calculates a list of recently accessed projects for a user, including both
     * owned
     * and shared projects, sorted by last accessed time in descending order.
     *
     * @param ownedProjects A list of projects owned by the user.
     * @param user          The user for whom the recently accessed projects are
     *                      being calculated.
     * @return A list of ProjectSummaryDto objects representing the recently
     *         accessed projects.
     */
    private List<DashboardStatsDto.ProjectSummaryDto> calculateRecentlyAccessedProjects(List<Project> ownedProjects,
        User user) {
        // Combine owned and shared, sort by last accessed, take limit
        List<Project> sharedProjects = projectRepository.findBySharedUsersContainsKey(user);
        Set<Project> allAccessibleProjects = new HashSet<>(ownedProjects);
        allAccessibleProjects.addAll(sharedProjects);

        return allAccessibleProjects.stream()
                .sorted(Comparator.comparing(Project::getLastAccessedTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(RECENT_PROJECTS_LIMIT)
                .map(p -> new DashboardStatsDto.ProjectSummaryDto(
                        p.getId().toString(),
                        p.getProjectName(),
                        p.getLastAccessedTime() != null ? p.getLastAccessedTime().toInstant() : null))
                .collect(Collectors.toList());
    }

    /**
     * Calculates a list of recently uploaded images for a user, sorted by request
     * time in descending order.
     *
     * @param allUserImages A list of images owned by the user.
     * @return A list of ImageSummaryDto objects representing the recently uploaded
     *         images.
     */
    private List<DashboardStatsDto.ImageSummaryDto> calculateRecentImageUploads(List<Image> allUserImages) {
        return allUserImages.stream()
                .sorted(Comparator.comparing(Image::getRequestTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(RECENT_IMAGES_LIMIT)
                .map(img -> new DashboardStatsDto.ImageSummaryDto(
                        img.getImageId(),
                        img.getImageName(),
                        img.getProject() != null ? img.getProject().getId().toString() : null,
                        img.getRequestTime() != null ? img.getRequestTime().toInstant() : null))
                .collect(Collectors.toList());
    }

    /**
     * Calculates a summary of the processing status of all results for the given
     * user.
     * The summary includes counts of results in each status (pending, processing,
     * completed, failed).
     * If a result has a null status, it is counted as completed.
     * 
     * @param allUserResults A list of processing results for the user.
     * @return A DashboardStatsDto.ProcessingStatusSummaryDto containing the
     *         summary.
     */
    private DashboardStatsDto.ProcessingStatusSummaryDto calculateProcessingStatusSummary(
            List<ProcessingResults> allUserResults) {
        long pending = 0, processing = 0, completed = 0, failed = 0;
        for (ProcessingResults result : allUserResults) {
            if (result.getStatus() != null) {
                switch (result.getStatus()) {
                    case PENDING:
                        pending++;
                        break;
                    case PROCESSING:
                        processing++;
                        break;
                    case COMPLETED:
                        completed++;
                        break;
                    case FAILED:
                        failed++;
                        break;
                }
            } else {
                // Treat null status as completed based on service logic, or maybe 'unknown'?
                completed++;
            }
        }
        return new DashboardStatsDto.ProcessingStatusSummaryDto(pending, processing, completed, failed);
    }

    /**
     * Calculates the most used processing type from the given list of results.
     * If the list is empty, returns "N/A". Otherwise, returns the type with the
     * highest count,
     * or "N/A" if no type has a count greater than 0.
     * 
     * @param allUserResults A list of processing results for the user.
     * @return The most used processing type, or "N/A" if none or unknown.
     */
    private String calculateMostUsedProcessingType(List<ProcessingResults> allUserResults) {
        if (allUserResults.isEmpty())
            return "N/A";
        return allUserResults.stream()
                .map(ProcessingResults::getType)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
    }

    /**
     * Retrieves the most recent audit events for the given user, limited to the
     * constant
     * {@link #RECENT_ACTIVITY_LIMIT}. The events are sorted in descending order by
     * timestamp.
     * Each event is converted to a {@link DashboardStatsDto.ActivityEventDto}
     * containing
     * the action type, a resolved target description, and the timestamp.
     * 
     * @param userId The ID of the user.
     * @return A list of {@link DashboardStatsDto.ActivityEventDto} objects.
     */
    private List<DashboardStatsDto.ActivityEventDto> calculateRecentActivityFeed(String userId) {
        Pageable limit = PageRequest.of(0, RECENT_ACTIVITY_LIMIT, Sort.by(Sort.Direction.DESC, "timestamp"));
        List<AuditEvent> recentEvents = auditEventRepository.findByUserIdOrderByTimestampDesc(userId, limit);

        return recentEvents.stream()
                .map(event -> new DashboardStatsDto.ActivityEventDto(
                        event.getActionType(),
                        resolveTargetDescription(event.getActionType(), event.getTargetId()),
                        event.getTimestamp().toInstant(ZoneOffset.UTC)))
                .collect(Collectors.toList());
    }

    /**
     * Calculates the most frequent action type from the audit events for the given
     * user.
     * If the user has no events, returns "N/A". Otherwise, returns the action type
     * with the highest count,
     * or "N/A" if no type has a count greater than 0.
     * 
     * @param userId The ID of the user.
     * @return The most frequent action type, or "N/A" if none or unknown.
     */
    private String calculateMostFrequentAction(String userId) {
        // TODO: Need an aggregation query in AuditEventRepository for this
        // Or fetch all events and calculate in memory (less efficient for many events)
        // Example (in-memory, potentially slow):
        List<AuditEvent> allEvents = auditEventRepository.findByUserIdOrderByTimestampDesc(userId); // Might be too
                                                                                                    // large
        if (allEvents.isEmpty())
            return "N/A";
        return allEvents.stream()
                .map(AuditEvent::getActionType)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
    }

    /**
     * Calculates activity trend data for a user over the last two weeks.
     * The trend is represented by counts of audit events from the last 7 days
     * and the 7 days preceding that.
     *
     * @param userId The ID of the user whose activity trend is to be calculated.
     * @return An ActivityTrendDto containing the count of events for the last 7
     *         days
     *         and the previous 7 days.
     */
    private DashboardStatsDto.ActivityTrendDto calculateActivityTrend(String userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(ACTIVITY_TREND_DAYS);
        LocalDateTime fourteenDaysAgo = now.minusDays(ACTIVITY_TREND_DAYS * 2);

        // TODO: Need repository method findByUserIdAndTimestampBetween
        long last7DaysCount = auditEventRepository.countByUserIdAndTimestampBetween(userId, sevenDaysAgo, now); // Assuming
                                                                                                                // this
                                                                                                                // exists
        long previous7DaysCount = auditEventRepository.countByUserIdAndTimestampBetween(userId, fourteenDaysAgo,
                sevenDaysAgo); // Assuming this exists

        return new DashboardStatsDto.ActivityTrendDto(last7DaysCount, previous7DaysCount);
    }

    /**
     * Resolves the target description for an audit event based on the given action
     * type and target ID. The target ID is usually an ObjectId string, but may not
     * be for certain actions (e.g., username in LOGIN_FAILURE).
     *
     * @param actionType The type of the action that generated the event.
     * @param targetId   The ID of the target entity being acted upon.
     * @return The resolved target description, or null if the target ID is null.
     *         Returns the raw target ID if the type is not recognized or if there
     *         is an error resolving the target.
     */
    private String resolveTargetDescription(String actionType, String targetId) {
        if (targetId == null)
            return null;
        try {
            ObjectId objectId = new ObjectId(targetId); // Assume targetId is usually an ObjectId string
            if (actionType.startsWith("PROJECT_")) {
                return projectRepository.findById(objectId).map(Project::getProjectName).orElse(targetId);
            } else if (actionType.startsWith("IMAGE_")) { // Assuming IMAGE_UPLOAD etc.
                return imageRepository.findById(targetId).map(Image::getImageName).orElse(targetId); // Image ID is
                                                                                                     // String
            } else if (actionType.startsWith("USER_")) {
                return userRepository.findById(objectId).map(User::getEmail).orElse(targetId);
            }
            // Add more types as needed (e.g., ProcessingResults)
        } catch (IllegalArgumentException e) {
            // targetId might not be an ObjectId (e.g., username in LOGIN_FAILURE)
            return targetId;
        } catch (Exception e) {
            logger.warn("Error resolving target description for type {} and ID {}: {}", actionType, targetId,
                    e.getMessage());
        }
        return targetId; // Fallback to raw ID
    }
}
