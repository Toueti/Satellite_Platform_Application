import { httpClient } from '../utils/http-client';

interface Project {
    id: string;
    name: string;
    description: string;
    status: string;
    updatedAt: string;
    owner: {
        email: string;
    };
}

interface ProjectStatistics {
    totalProjects: number;
    activeProjects: number;
    completedProjects: number;
    archivedProjects: number;
}

interface DashboardData {
    totalProjects: number;
    activeAnalyses: number;
    storageUsed: string;
    mapCoverage: string;
    recentProjects: Array<{
        id: number;
        name: string;
        description: string;
        lastModified: string;
    }>;
    recentAnalyses: Array<{
        id: number;
        name: string;
        description: string;
        lastModified: string;
    }>;
}

interface ApiResponse<T> {
    status: string;
    message: string;
    data: T;
}

class DashboardService {
    private readonly baseUrl = 'http://localhost:8080/api/thematician/projects';

    async getDashboardData(): Promise<DashboardData> {
        try {
            // Fetch data from multiple endpoints in parallel
            const [statsResponse, activeProjectsResponse, recentProjectsResponse] = await Promise.all([
                httpClient.get(`${this.baseUrl}/statistics`),
                httpClient.get(`${this.baseUrl}/all`), // Changed to get all projects, we'll filter active ones
                httpClient.get(`${this.baseUrl}/all`) // Changed to get all projects, we'll get the most recent ones
            ]);

            const stats = (statsResponse as ApiResponse<ProjectStatistics>).data;
            const allProjects = (activeProjectsResponse as ApiResponse<Project[]>).data;
            
            // Filter active projects
            const activeProjects = allProjects.filter(p => p.status === 'active');
            
            // Sort projects by updatedAt to get the most recent ones
            const recentProjects = [...allProjects]
                .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())
                .slice(0, 5);

            // Transform the data to match the dashboard interface
            return {
                totalProjects: stats.totalProjects,
                activeAnalyses: activeProjects.length,
                storageUsed: '0 GB', // This should come from a storage service if available
                mapCoverage: '0%', // This should come from a map service if available
                recentProjects: recentProjects.map(p => ({
                    id: parseInt(p.id),
                    name: p.name,
                    description: p.description || 'No description',
                    lastModified: new Date(p.updatedAt).toLocaleDateString()
                })),
                recentAnalyses: [] // This should come from an analysis service if available
            };
        } catch (error) {
            console.error("Error fetching dashboard data:", error);
            // Return empty state that matches the interface
            return {
                totalProjects: 0,
                activeAnalyses: 0,
                storageUsed: '0 GB',
                mapCoverage: '0%',
                recentProjects: [],
                recentAnalyses: []
            };
        }
    }
}

export const dashboardService = new DashboardService();
