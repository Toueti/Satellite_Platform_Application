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
import com.enit.satellite_platform.modules.user_management.models.User;
import com.enit.satellite_platform.modules.user_management.user_service.repositories.UserRepository; // Corrected path
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
    private UserRepository userRepository; // Assuming this exists and works

    // Constants for recent items/trends
    private static final int RECENT_ACTIVITY_LIMIT = 5;
    private static final int RECENT_PROJECTS_LIMIT = 5;
    private static final int RECENT_IMAGES_LIMIT = 5;
    private static final int ACTIVITY_TREND_DAYS = 7;


    public DashboardStatsDto getDashboardStats(String userEmail) {
        logger.info("Generating dashboard stats for user: {}", userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
        String userId = user.getId().toString(); // Assuming User ID is ObjectId

        DashboardStatsDto stats = new DashboardStatsDto();

        // --- Project Stats ---
        List<Project> ownedProjects = projectRepository.findByOwner(user);
        stats.setTotalProjects(ownedProjects.size());
        stats.setSharedByUserCount(ownedProjects.stream().filter(p -> !p.getSharedUsers().isEmpty()).count());
        stats.setSharedWithUserCount(projectRepository.countBySharedUsersContainsKey(user)); // Need count method
        stats.setRecentlyAccessedProjects(calculateRecentlyAccessedProjects(ownedProjects, user));

        // --- Image & Storage Stats ---
        List<Image> allUserImages = imageRepository.findAllByOwnerId(userId); // Need this method
        stats.setTotalImages(allUserImages.size());
        stats.setTotalStorageUsedBytes(allUserImages.stream().mapToLong(Image::getFileSize).sum());
        stats.setRecentImageUploads(calculateRecentImageUploads(allUserImages));

        // --- Processing Results (Treatments) Stats ---
        List<ProcessingResults> allUserResults = resultsRepository.findAllByOwnerId(userId); // Need this method
        stats.setTotalTreatments(allUserResults.size());
        stats.setProcessingStatusSummary(calculateProcessingStatusSummary(allUserResults));
        stats.setMostUsedProcessingType(calculateMostUsedProcessingType(allUserResults));

        // --- Averages ---
        stats.setAverageImagesPerProject(stats.getTotalProjects() == 0 ? 0 : (double) stats.getTotalImages() / stats.getTotalProjects());
        stats.setAverageTreatmentsPerImage(stats.getTotalImages() == 0 ? 0 : (double) stats.getTotalTreatments() / stats.getTotalImages());

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

    private Instant findLastEventTime(String userId, String actionType) {
        AuditEvent latestEvent = auditEventRepository.findTopByUserIdAndActionTypeOrderByTimestampDesc(userId, actionType);
        if (latestEvent != null && latestEvent.getTimestamp() != null) {
            return latestEvent.getTimestamp().toInstant(ZoneOffset.UTC); // Adjust ZoneOffset if needed
        }
        return null;
    }

    private List<DashboardStatsDto.ProjectSummaryDto> calculateRecentlyAccessedProjects(List<Project> ownedProjects, User user) {
         // Combine owned and shared, sort by last accessed, take limit
         List<Project> sharedProjects = projectRepository.findBySharedUsersContainsKey(user); // Fetch shared
         Set<Project> allAccessibleProjects = new HashSet<>(ownedProjects);
         allAccessibleProjects.addAll(sharedProjects);

         return allAccessibleProjects.stream()
                 .sorted(Comparator.comparing(Project::getLastAccessedTime, Comparator.nullsLast(Comparator.reverseOrder())))
                 .limit(RECENT_PROJECTS_LIMIT)
                 .map(p -> new DashboardStatsDto.ProjectSummaryDto(
                         p.getProjectId().toString(),
                         p.getProjectName(),
                         p.getLastAccessedTime() != null ? p.getLastAccessedTime().toInstant() : null))
                 .collect(Collectors.toList());
    }

     private List<DashboardStatsDto.ImageSummaryDto> calculateRecentImageUploads(List<Image> allUserImages) {
         return allUserImages.stream()
                 .sorted(Comparator.comparing(Image::getRequestTime, Comparator.nullsLast(Comparator.reverseOrder())))
                 .limit(RECENT_IMAGES_LIMIT)
                 .map(img -> new DashboardStatsDto.ImageSummaryDto(
                         img.getImageId(),
                         img.getImageName(),
                         img.getProject() != null ? img.getProject().getProjectId().toString() : null,
                         img.getRequestTime() != null ? img.getRequestTime().toInstant() : null))
                 .collect(Collectors.toList());
     }


    private DashboardStatsDto.ProcessingStatusSummaryDto calculateProcessingStatusSummary(List<ProcessingResults> allUserResults) {
        long pending = 0, processing = 0, completed = 0, failed = 0;
        for (ProcessingResults result : allUserResults) {
            if (result.getStatus() != null) {
                switch (result.getStatus()) {
                    case PENDING: pending++; break;
                    case PROCESSING: processing++; break;
                    case COMPLETED: completed++; break;
                    case FAILED: failed++; break;
                }
            } else {
                 // Treat null status as completed based on service logic, or maybe 'unknown'?
                 completed++;
            }
        }
        return new DashboardStatsDto.ProcessingStatusSummaryDto(pending, processing, completed, failed);
    }

    private String calculateMostUsedProcessingType(List<ProcessingResults> allUserResults) {
        if (allUserResults.isEmpty()) return "N/A";
        return allUserResults.stream()
                .map(ProcessingResults::getType)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
    }

     private List<DashboardStatsDto.ActivityEventDto> calculateRecentActivityFeed(String userId) {
         Pageable limit = PageRequest.of(0, RECENT_ACTIVITY_LIMIT, Sort.by(Sort.Direction.DESC, "timestamp"));
         List<AuditEvent> recentEvents = auditEventRepository.findByUserIdOrderByTimestampDesc(userId, limit); // Corrected method name

         return recentEvents.stream()
                 .map(event -> new DashboardStatsDto.ActivityEventDto(
                         event.getActionType(),
                         resolveTargetDescription(event.getActionType(), event.getTargetId()), // Helper needed
                         event.getTimestamp().toInstant(ZoneOffset.UTC)))
                 .collect(Collectors.toList());
     }

     private String calculateMostFrequentAction(String userId) {
         // TODO: Need an aggregation query in AuditEventRepository for this
         // Or fetch all events and calculate in memory (less efficient for many events)
         // Example (in-memory, potentially slow):
         List<AuditEvent> allEvents = auditEventRepository.findByUserIdOrderByTimestampDesc(userId); // Might be too large
          if (allEvents.isEmpty()) return "N/A";
          return allEvents.stream()
                 .map(AuditEvent::getActionType)
                 .filter(Objects::nonNull)
                 .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                 .entrySet().stream()
                 .max(Map.Entry.comparingByValue())
                 .map(Map.Entry::getKey)
                 .orElse("N/A");
     }

     private DashboardStatsDto.ActivityTrendDto calculateActivityTrend(String userId) {
         LocalDateTime now = LocalDateTime.now();
         LocalDateTime sevenDaysAgo = now.minusDays(ACTIVITY_TREND_DAYS);
         LocalDateTime fourteenDaysAgo = now.minusDays(ACTIVITY_TREND_DAYS * 2);

         // TODO: Need repository method findByUserIdAndTimestampBetween
         long last7DaysCount = auditEventRepository.countByUserIdAndTimestampBetween(userId, sevenDaysAgo, now); // Assuming this exists
         long previous7DaysCount = auditEventRepository.countByUserIdAndTimestampBetween(userId, fourteenDaysAgo, sevenDaysAgo); // Assuming this exists

         return new DashboardStatsDto.ActivityTrendDto(last7DaysCount, previous7DaysCount);
     }

     // Helper to get meaningful names for target IDs in activity feed
     private String resolveTargetDescription(String actionType, String targetId) {
         if (targetId == null) return null;
         try {
             ObjectId objectId = new ObjectId(targetId); // Assume targetId is usually an ObjectId string
             if (actionType.startsWith("PROJECT_")) {
                 return projectRepository.findById(objectId).map(Project::getProjectName).orElse(targetId);
             } else if (actionType.startsWith("IMAGE_")) { // Assuming IMAGE_UPLOAD etc.
                 return imageRepository.findById(targetId).map(Image::getImageName).orElse(targetId); // Image ID is String
             } else if (actionType.startsWith("USER_")) {
                  return userRepository.findById(objectId).map(User::getEmail).orElse(targetId);
             }
             // Add more types as needed (e.g., ProcessingResults)
         } catch (IllegalArgumentException e) {
             // targetId might not be an ObjectId (e.g., username in LOGIN_FAILURE)
             return targetId;
         } catch (Exception e) {
             logger.warn("Error resolving target description for type {} and ID {}: {}", actionType, targetId, e.getMessage());
         }
         return targetId; // Fallback to raw ID
     }
}
