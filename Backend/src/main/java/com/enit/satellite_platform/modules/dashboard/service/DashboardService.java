package com.enit.satellite_platform.modules.dashboard.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.enit.satellite_platform.audit.AuditEvent;
import com.enit.satellite_platform.audit.AuditEventRepository;
import com.enit.satellite_platform.modules.dashboard.dto.DashboardStatsDto;
import com.enit.satellite_platform.modules.project_management.entities.Project;
import com.enit.satellite_platform.modules.project_management.repositories.ProjectRepository;
import com.enit.satellite_platform.modules.resource_management.image_management.entities.Image;
import com.enit.satellite_platform.modules.resource_management.image_management.entities.ProcessingResults;
import com.enit.satellite_platform.modules.resource_management.image_management.entities.ProcessingType;
import com.enit.satellite_platform.modules.resource_management.image_management.repositories.ImageRepository;
import com.enit.satellite_platform.modules.resource_management.image_management.repositories.ResultsRepository;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.User;
import com.enit.satellite_platform.modules.user_management.normal_user_service.repositories.UserRepository;

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
        logger.debug("User retrieved: {}", user);

        String userId = user.getId();
        DashboardStatsDto stats = new DashboardStatsDto();

        logger.info("Fetching project statistics...");
        List<Project> ownedProjects = projectRepository.findByOwnerId(new ObjectId(userId));
        logger.debug("Owned projects retrieved: {}", ownedProjects.size());

        stats.setTotalProjects(ownedProjects.size());
        stats.setSharedByUserCount(ownedProjects.stream().filter(p -> !p.getSharedUsers().isEmpty()).count());
        stats.setSharedWithUserCount(projectRepository.countBySharedUsersContainsKey(user));
        stats.setRecentlyAccessedProjects(calculateRecentlyAccessedProjects(ownedProjects, user));
        logger.info("Project statistics calculated.");

        logger.info("Fetching image and storage statistics...");
        List<Image> allUserImages = new ArrayList<>();
        for (Project project : ownedProjects) {
            allUserImages.addAll(imageRepository.findAllByProject_Id(new ObjectId(project.getId())));
        }
        logger.debug("Images retrieved: {}", allUserImages.size());

        stats.setTotalImages(allUserImages.size());
        stats.setTotalStorageUsedBytes(allUserImages.stream().mapToLong(Image::getFileSize).sum());
        stats.setRecentImageUploads(calculateRecentImageUploads(allUserImages));
        logger.info("Image and storage statistics calculated.");

        logger.info("Fetching processing results statistics...");
        List<ProcessingResults> allUserResults = resultsRepository.findAllByOwnerId(userId);
        logger.debug("Processing results retrieved: {}", allUserResults.size());

        stats.setTotalTreatments(allUserResults.size());
        stats.setProcessingStatusSummary(calculateProcessingStatusSummary(allUserResults));
        stats.setMostUsedProcessingType(calculateMostUsedProcessingType(allUserResults));
        logger.info("Processing results statistics calculated.");

        logger.info("Calculating averages...");
        stats.setAverageImagesPerProject(
                stats.getTotalProjects() == 0 ? 0 : (double) stats.getTotalImages() / stats.getTotalProjects());
        stats.setAverageTreatmentsPerImage(
                stats.getTotalImages() == 0 ? 0 : (double) stats.getTotalTreatments() / stats.getTotalImages());
        logger.info("Averages calculated.");

        logger.info("Fetching audit event statistics...");
        stats.setLastPlatformLoginTime(findLastEventTime(userId, LOGIN_SUCCESS));
        stats.setLastProjectAccessTime(findLastEventTime(userId, PROJECT_ACCESS_SUCCESS));
        stats.setTotalProjectAccesses(auditEventRepository.countByUserIdAndActionType(userId, PROJECT_ACCESS_SUCCESS));
        stats.setRecentActivityFeed(calculateRecentActivityFeed(userId));
        stats.setMostFrequentAction(calculateMostFrequentAction(userId));
        stats.setActivityTrend(calculateActivityTrend(userId));
        logger.info("Audit event statistics calculated.");

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
        logger.debug("Finding last event time for user: {}, actionType: {}", userId, actionType);
        AuditEvent latestEvent = auditEventRepository.findTopByUserIdAndActionTypeOrderByTimestampDesc(userId,
                actionType);
        logger.debug("Last event time found: {}", latestEvent != null ? latestEvent.getTimestamp() : null);
        return latestEvent != null ? latestEvent.getTimestamp().toInstant(ZoneOffset.UTC) : null;
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
        logger.debug("Calculating recently accessed projects...");
        // Combine owned and shared, sort by last accessed, take limit
        List<Project> sharedProjects = projectRepository.findBySharedUsersContainsKey(user);
        Set<Project> allAccessibleProjects = new HashSet<>(ownedProjects);
        allAccessibleProjects.addAll(sharedProjects);

        List<DashboardStatsDto.ProjectSummaryDto> result = allAccessibleProjects.stream()
                .sorted(Comparator.comparing(Project::getLastAccessedTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(RECENT_PROJECTS_LIMIT)
                .map(p -> new DashboardStatsDto.ProjectSummaryDto(
                        p.getId().toString(),
                        p.getProjectName(),
                        p.getLastAccessedTime() != null ? p.getLastAccessedTime().toInstant() : null))
                .collect(Collectors.toList());
        logger.debug("Recently accessed projects calculated: {}", result.size());
        return result;
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
        logger.debug("Calculating recent image uploads...");
        List<DashboardStatsDto.ImageSummaryDto> result = allUserImages.stream()
                .sorted(Comparator.comparing(Image::getRequestTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(RECENT_IMAGES_LIMIT)
                .map(img -> new DashboardStatsDto.ImageSummaryDto(
                        img.getImageId(),
                        img.getImageName(),
                        img.getProject() != null ? img.getProject().getId().toString() : null,
                        img.getRequestTime() != null ? img.getRequestTime().toInstant() : null))
                .collect(Collectors.toList());
        logger.debug("Recent image uploads calculated: {}", result.size());
        return result;
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
        logger.debug("Calculating processing status summary...");
        long pending = 0, processing = 0, completed = 0, failed = 0, unknown = 0; // Added unknown counter
        for (ProcessingResults result : allUserResults) {
            if (result.getStatus() != null) {
                switch (result.getStatus()) {
                    case PENDING:
                        pending++;
                        break;
                    case RUNNING:
                        processing++;
                        break;
                    case COMPLETED:
                        completed++;
                        break;
                    case FAILED:
                        failed++;
                        break;
                    case CANCELLED:
                        unknown++;
                        break;
                    default:
                        unknown++;
                        break;
                }
            } else {
                // Count null status as unknown/other as requested
                unknown++;
            }
        }
        // Pass all five counts to the constructor
        DashboardStatsDto.ProcessingStatusSummaryDto summary = new DashboardStatsDto.ProcessingStatusSummaryDto(pending, processing, completed, failed, unknown);
        logger.debug("Processing status summary calculated: pending={}, processing={}, completed={}, failed={}, unknown={}",
            pending, processing, completed, failed, unknown);
        return summary;
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
        logger.debug("Calculating most used processing type...");
        if (allUserResults.isEmpty())
            return "N/A";
        ProcessingType result = allUserResults.stream()
                .map(ProcessingResults::getType)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
                
        if (result != null) {
            logger.debug("Most used processing type: {}", result.name());
            return result.getDescription(); // Return the human-readable description
        }
        
        logger.debug("Most used processing type: N/A");
        return "N/A";
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
        logger.debug("Calculating recent activity feed for user: {}", userId);
        Pageable limit = PageRequest.of(0, RECENT_ACTIVITY_LIMIT, Sort.by(Sort.Direction.DESC, "timestamp"));
        List<AuditEvent> recentEvents = auditEventRepository.findByUserIdOrderByTimestampDesc(userId, limit);

        List<DashboardStatsDto.ActivityEventDto> result = recentEvents.stream()
                .map(event -> new DashboardStatsDto.ActivityEventDto(
                        event.getActionType(),
                        resolveTargetDescription(event.getActionType(), event.getTargetId()),
                        event.getTimestamp().toInstant(ZoneOffset.UTC)))
                .collect(Collectors.toList());
        logger.debug("Recent activity feed calculated: {}", result.size());
        return result;
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
        logger.debug("Calculating most frequent action for user: {}", userId);
        // Use the specific DTO returned by the repository method
        AuditEventRepository.ActionFrequencyDto result = auditEventRepository.findMostFrequentActionTypeByUserId(userId);
        // Check if a result was found and return the action type from the DTO's 'id' field
        if (result != null) {
            logger.debug("Most frequent action: {}", result.getId());
            return result.getId();
        }
        logger.debug("Most frequent action: N/A");
        return "N/A";
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
        logger.debug("Calculating activity trend for user: {}", userId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(ACTIVITY_TREND_DAYS);
        LocalDateTime fourteenDaysAgo = now.minusDays(ACTIVITY_TREND_DAYS * 2);

        // Use the existing repository method
        long last7DaysCount = auditEventRepository.countByUserIdAndTimestampBetween(userId, sevenDaysAgo, now);
        long previous7DaysCount = auditEventRepository.countByUserIdAndTimestampBetween(userId, fourteenDaysAgo, sevenDaysAgo);

        DashboardStatsDto.ActivityTrendDto trend = new DashboardStatsDto.ActivityTrendDto(last7DaysCount, previous7DaysCount);
        logger.debug("Activity trend calculated: last7Days={}, previous7Days={}", last7DaysCount, previous7DaysCount);
        return trend;
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
        logger.debug("Resolving target description for actionType: {}, targetId: {}", actionType, targetId);
        if (targetId == null)
            return null;
        try {
            ObjectId objectId = new ObjectId(targetId); // Assume targetId is usually an ObjectId string
            if (actionType.startsWith("PROJECT_")) {
                String description = projectRepository.findById(objectId).map(Project::getProjectName).orElse(targetId);
                logger.debug("Resolved target description: {}", description);
                return description;
            } else if (actionType.startsWith("IMAGE_")) { // Assuming IMAGE_UPLOAD etc.
                String description = imageRepository.findById(targetId).map(Image::getImageName).orElse(targetId); // Image ID is
                                                                                                     // String
                logger.debug("Resolved target description: {}", description);
                return description;
            } else if (actionType.startsWith("USER_")) {
                String description = userRepository.findById(objectId).map(User::getEmail).orElse(targetId);
                logger.debug("Resolved target description: {}", description);
                return description;
            }
            // Add more types as needed (e.g., ProcessingResults)
        } catch (IllegalArgumentException e) {
            // targetId might not be an ObjectId (e.g., username in LOGIN_FAILURE)
            logger.debug("Resolved target description: {}", targetId);
            return targetId;
        } catch (Exception e) {
            logger.warn("Error resolving target description for type {} and ID {}: {}", actionType, targetId,
                    e.getMessage());
        }
        logger.debug("Resolved target description: {}", targetId);
        return targetId; // Fallback to raw ID
    }
}
