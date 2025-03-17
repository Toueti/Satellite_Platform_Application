import { httpClient } from '../utils/http-client';

interface DashboardData {
    totalProjects: number;
    activeAnalyses: number;
    storageUsed: string;
    mapCoverage: string;
    recentProjects: {
        id: number;
        name: string;
        description: string;
        lastModified: string;
    }[];
}

class DashboardService {
    async getDashboardData(): Promise<DashboardData> {
        try {
            const response = await httpClient.get('/api/dashboard'); // Assuming /api/dashboard endpoint
            return response.data;
        } catch (error) {
            console.error("Error fetching dashboard data:", error);
            throw error;
        }
    }
}

export const dashboardService = new DashboardService();
