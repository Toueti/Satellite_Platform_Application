package com.enit.satellite_platform.modules.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {

    // Original requested stats (adjusted types)
    private long totalProjects;
    private long totalImages; // Across all user's projects
    private long totalTreatments; // Across all user's images
    private long totalProjectAccesses; // Count of PROJECT_ACCESS events
    private Instant lastProjectAccessTime;
    private Instant lastPlatformLoginTime;

    // Additional stats
    private List<ActivityEventDto> recentActivityFeed; // e.g., last 5 events
    private String mostFrequentAction; // e.g., "PROJECT_ACCESS"
    private ActivityTrendDto activityTrend; // e.g., last 7 days vs previous 7
    private List<ProjectSummaryDto> recentlyAccessedProjects; // e.g., last 3-5
    private long sharedByUserCount;
    private long sharedWithUserCount;
    private List<ImageSummaryDto> recentImageUploads; // e.g., last 3-5
    private long totalStorageUsedBytes;
    private ProcessingStatusSummaryDto processingStatusSummary;
    private String mostUsedProcessingType;
    private double averageImagesPerProject;
    private double averageTreatmentsPerImage;

    // --- Inner DTOs for nested data ---

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ActivityEventDto {
        private String actionType;
        private String targetDescription; // e.g., Project Name, Image Name, or just ID
        private Instant timestamp;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ActivityTrendDto {
        private long last7DaysCount;
        private long previous7DaysCount;
        // Could add percentage change etc.
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ProjectSummaryDto {
        private String projectId;
        private String projectName;
        private Instant lastAccessedTime;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ImageSummaryDto {
        private String imageId;
        private String imageName;
        private String projectId; // ID of the project it belongs to
        private Instant uploadTime; // Assuming 'requestTime' in Image model
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ProcessingStatusSummaryDto {
        private long pendingCount;
        private long processingCount;
        private long completedCount;
        private long failedCount;
        private long unknownCount; // Added for null status
    }
}
