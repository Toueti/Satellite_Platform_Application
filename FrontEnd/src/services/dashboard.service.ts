import { httpClient } from '../utils/http-client';
import { PROJECT_ENDPOINTS } from '@/config/api';
import { Project, ProjectStatus } from '@/types/api';

const RETRY_DELAYS = [1000, 2000, 3000]; // Retry delays in milliseconds

export interface DashboardData {
    totalProjects: number;
    activeProjects: number;
    completedProjects: number;
    archivedProjects: number;
    recentProjects: Project[];
    lastAccessedProjects: Project[];
    storageUsed: string;
    totalImages: number;
    sharedProjectsCount: number;
}

class DashboardService {
    private readonly baseUrl = 'http://localhost:8080/api/thematician/projects';
    private retryCount = 0;
    private lastRequestTime = 0;
    private readonly minRequestInterval = 1000; // Minimum 1 second between requests

    /**
     * Maps raw project data from the API to our Project type
     */
    private mapProjectResponse(project: any): Project {
        // Debug log the incoming project structure
        console.log('Raw project data:', project);
        
        let mongoId: string | undefined;
        let timestampId: string | undefined;
        
        try {
            // First try to get MongoDB ObjectId
            if (project._id) {
                mongoId = project._id;
            } else if (project.id && /^[0-9a-fA-F]{24}$/.test(project.id)) {
                mongoId = project.id;
            }

            // Then get timestamp ID if available
            if (project.projectId) {
                if (typeof project.projectId === 'object') {
                    if (project.projectId._id) {
                        mongoId = project.projectId._id;
                    }
                    if (project.projectId.timestamp) {
                        timestampId = project.projectId.timestamp.toString();
                    }
                } else if (typeof project.projectId === 'string') {
                    if (/^[0-9a-fA-F]{24}$/.test(project.projectId)) {
                        mongoId = project.projectId;
                    } else {
                        timestampId = project.projectId;
                    }
                }
            }

            // Log what we found
            console.log('Found IDs:', { mongoId, timestampId });

            // If we don't have either ID, that's a problem
            if (!mongoId && !timestampId) {
                throw new Error('Could not find any valid ID in project data');
            }

            // Use MongoDB ID if available, otherwise use timestamp
            const id = mongoId || timestampId;
            
            return {
                ...project,
                id,                    // Primary ID for routing
                _id: mongoId,          // MongoDB ObjectId
                projectId: {           // Keep the original projectId structure
                    _id: mongoId,
                    timestamp: timestampId ? parseInt(timestampId) : undefined,
                    date: project.projectId?.date
                },
                name: project.projectName || project.name || 'Unnamed Project',
                description: project.description || '',
                createdAt: project.projectId?.date || project.createdAt || new Date().toISOString(),
                updatedAt: project.updatedAt ? new Date(project.updatedAt).toISOString() : new Date().toISOString(),
                status: project.status || 'DRAFT',
                owner: typeof project.owner === 'object' ? project.owner.email || '' : project.owner || '',
                collaborators: project.sharedUsers ? Object.keys(project.sharedUsers) : [],
                metadata: {
                    tags: Array.isArray(project.tags) ? project.tags : [],
                    location: project.metadata?.location || undefined,
                    satelliteData: project.metadata?.satelliteData || undefined
                }
            };
        } catch (error) {
            console.error('Error mapping project response:', error);
            throw error;
        }
    }

    /**
     * Safely extracts data from API responses
     */
    private safelyExtractData<T>(response: any, defaultValue: T = [] as any, options: { parseJson?: boolean } = {}): T {
        try {
            if (!response) return defaultValue;
            
            let data = response.data;
            if (options.parseJson && typeof data === 'string') {
                data = JSON.parse(data);
            }
            
            return data || defaultValue;
        } catch (error) {
            console.error('Error extracting data:', error);
            return defaultValue;
        }
    }

    private async delay(ms: number): Promise<void> {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    private async throttleRequest(): Promise<void> {
        const now = Date.now();
        const timeSinceLastRequest = now - this.lastRequestTime;
        
        if (timeSinceLastRequest < this.minRequestInterval) {
            await this.delay(this.minRequestInterval - timeSinceLastRequest);
        }
        
        this.lastRequestTime = Date.now();
    }

    private async retryRequest<T>(requestFn: () => Promise<T>): Promise<T> {
        try {
            await this.throttleRequest();
            return await requestFn();
        } catch (error: any) {
            if (error.message?.includes('429') && this.retryCount < RETRY_DELAYS.length) {
                const delay = RETRY_DELAYS[this.retryCount];
                this.retryCount++;
                console.log(`Rate limited. Retrying in ${delay}ms... (Attempt ${this.retryCount})`);
                await this.delay(delay);
                return this.retryRequest(requestFn);
            }
            throw error;
        }
    }

    /**
     * Fetches all dashboard data for the current user
     */
    async getDashboardData(): Promise<DashboardData> {
        try {
            this.retryCount = 0; // Reset retry count

            // Fetch all projects with retry logic
            const projectsResponse = await this.retryRequest(() => 
                httpClient.get(`${this.baseUrl}/all`)
            );
            const allProjects = this.safelyExtractData<any[]>(projectsResponse).map(this.mapProjectResponse.bind(this));
            
            // Fetch last accessed projects with retry logic
            const lastAccessedResponse = await this.retryRequest(() => 
                httpClient.get(`${this.baseUrl}/last-accessed?n=5`)
            );
            const lastAccessedProjects = this.safelyExtractData<any[]>(lastAccessedResponse).map(this.mapProjectResponse.bind(this));
            
            // Count projects by status
            const activeProjects = allProjects.filter(p => p.status === ProjectStatus.ACTIVE);
            const completedProjects = allProjects.filter(p => p.status === ProjectStatus.COMPLETED);
            const archivedProjects = allProjects.filter(p => p.status === ProjectStatus.ARCHIVED);
            
            // Sort projects by updatedAt to get the most recent ones
            const recentProjects = [...allProjects]
                .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())
                .slice(0, 5);
            
            // Calculate storage used (this is a placeholder - would need actual storage service)
            const totalStorageBytes = allProjects.reduce((total, project) => {
                // Since we don't have actual size data, we'll use a placeholder value
                return total + 1024 * 1024; // 1MB per project as a placeholder
            }, 0);
            
            const storageUsed = this.formatBytes(totalStorageBytes);
            
            // Count shared projects
            const sharedProjectsCount = allProjects.filter(p => p.collaborators && p.collaborators.length > 0).length;
            
            // Count total images across all projects
            const totalImages = allProjects.reduce((total, project) => {
                return total + (project.metadata?.satelliteData ? 1 : 0);
            }, 0);
            
            // Transform the data to match the dashboard interface
            const dashboardData: DashboardData = {
                totalProjects: allProjects.length,
                activeProjects: activeProjects.length,
                completedProjects: completedProjects.length,
                archivedProjects: archivedProjects.length,
                recentProjects,
                lastAccessedProjects,
                storageUsed,
                totalImages,
                sharedProjectsCount
            };
            
            return dashboardData;
        } catch (error) {
            console.error("Error fetching dashboard data:", error);
            return this.getDefaultDashboardData();
        }
    }
    
    /**
     * Returns default dashboard data when API calls fail
     */
    private getDefaultDashboardData(): DashboardData {
        return {
            totalProjects: 0,
            activeProjects: 0,
            completedProjects: 0,
            archivedProjects: 0,
            recentProjects: [],
            lastAccessedProjects: [],
            storageUsed: '0 GB',
            totalImages: 0,
            sharedProjectsCount: 0
        };
    }

    /**
     * Formats bytes into a human-readable string
     */
    private formatBytes(bytes: number): string {
        if (bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }
}

export const dashboardService = new DashboardService();
