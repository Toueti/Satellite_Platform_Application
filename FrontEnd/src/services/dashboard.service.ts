import { httpClient } from '../utils/api/http-client';
import { PROJECT_ENDPOINTS } from '@/config/api';
import { Project, ProjectStatus } from '@/types/api';
import { authService } from './auth.service';

export interface DashboardStats {
    totalProjects: number;
    totalImages: number;
    totalTreatments: number;
    totalProjectAccesses: number;
    lastProjectAccessTime: string | null;
    lastPlatformLoginTime: string | null;
    recentActivityFeed: Array<any>;
    mostFrequentAction: string;
    activityTrend: {
        last7DaysCount: number;
        previous7DaysCount: number;
    };
    recentlyAccessedProjects: Array<{
        projectId: string;
        projectName: string;
        lastAccessedTime: string;
    }>;
    sharedByUserCount: number;
    sharedWithUserCount: number;
    recentImageUploads: Array<{
        imageId: string;
        imageName: string;
        projectId: string;
        uploadTime: string;
    }>;
    totalStorageUsedBytes: number;
    processingStatusSummary: {
        pendingCount: number;
        processingCount: number;
        completedCount: number;
        failedCount: number;
        unknownCount: number;
    };
    mostUsedProcessingType: string;
    averageImagesPerProject: number;
    averageTreatmentsPerImage: number;
}

export interface DashboardData extends DashboardStats {
    storageUsed: string;
}

class DashboardService {
    private readonly baseUrl = 'http://localhost:8080/api/dashboard';

    private formatBytes(bytes: number): string {
        if (bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }

    async getDashboardData(): Promise<DashboardData> {
        try {
            // Check authentication
            if (!authService.isAuthenticated()) {
                console.error('User not authenticated');
                return this.getDefaultDashboardData();
            }

            // Get request (httpClient will automatically add auth headers)
            // Pass relative path directly to httpClient
            const response = await httpClient.get(`/api/dashboard/stats`);

            // Log the response data
            console.log('Raw stats response:', response);

            // Check if the response itself contains the expected data
            // (httpClient might return raw data if not wrapped in { data: ... })
            const stats = response && typeof response === 'object' && 'totalProjects' in response ? response : null;

            if (!stats) {
                console.error('Invalid stats data received:', response);
                throw new Error('Invalid response from server');
            }

            // const stats = response.data; // Old logic removed

            // Create mapped data with storage formatting
            const mappedData: DashboardData = {
                ...stats,
                storageUsed: this.formatBytes(stats.totalStorageUsedBytes)
            };

            console.log('Mapped dashboard data:', mappedData);
            return mappedData;

        } catch (error) {
            console.error("Error fetching dashboard data:", error);
            return this.getDefaultDashboardData();
        }
    }
    
    private getDefaultDashboardData(): DashboardData {
        return {
            totalProjects: 0,
            totalImages: 0,
            totalTreatments: 0,
            totalProjectAccesses: 0,
            lastProjectAccessTime: null,
            lastPlatformLoginTime: null,
            recentActivityFeed: [],
            mostFrequentAction: 'N/A',
            activityTrend: {
                last7DaysCount: 0,
                previous7DaysCount: 0
            },
            recentlyAccessedProjects: [],
            sharedByUserCount: 0,
            sharedWithUserCount: 0,
            recentImageUploads: [],
            totalStorageUsedBytes: 0,
            processingStatusSummary: {
                pendingCount: 0,
                processingCount: 0,
                completedCount: 0,
                failedCount: 0,
                unknownCount: 0
            },
            mostUsedProcessingType: 'N/A',
            averageImagesPerProject: 0,
            averageTreatmentsPerImage: 0,
            storageUsed: '0 B'
        };
    }
}

export const dashboardService = new DashboardService();
