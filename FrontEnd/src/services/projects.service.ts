import { httpClient } from '@/utils/http-client'
import { PROJECT_ENDPOINTS } from '@/config/api'
import type { Image } from './images.service'
import { Project, ProjectStatus } from '@/types/api'

export interface CreateProjectDto {
  projectName: string;
  description?: string;
}

// Interface for the project sharing request
export interface ProjectSharingRequest {
  projectId: string
  otherEmail: string
  permission?: string
}

export interface ProjectFilter {
  status?: ProjectStatus;
  search?: string;
  tags?: string[];
  archived?: boolean;
  sortBy?: 'name' | 'createdAt' | 'updatedAt' | 'lastAccessedTime';
  sortOrder?: 'asc' | 'desc';
}

const RETRY_DELAYS = [1000, 2000, 3000]; // Retry delays in milliseconds

class ProjectsService {
  private retryCount = 0;
  private lastRequestTime = 0;
  private readonly minRequestInterval = 1000; // Minimum 1 second between requests

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
   * Safely converts any value to a string ID
   */
  private ensureStringId(id: any): string | null {
    if (!id) return null;
    
    // Handle string IDs
    if (typeof id === 'string') {
      return id;
    }
    
    // Handle number IDs
    if (typeof id === 'number') {
      return id.toString();
    }
    
    // Handle object IDs
    if (typeof id === 'object') {
      // Handle timestamp-based IDs
      if (id.timestamp) {
        return id.timestamp.toString();
      }
      
      // Try other common ID fields
      if (id.id) {
        return this.ensureStringId(id.id);
      }
      if (id.projectId) {
        return this.ensureStringId(id.projectId);
      }
    }
    
    console.error('Invalid project ID format:', id);
    return null;
  }

  /**
   * Maps raw project data from the API to our Project type
   */
  private mapProjectResponse(project: any): Project {
    const id = this.ensureStringId(project.id || project.projectId);
    if (!id) {
      console.error('Failed to extract valid ID from project:', project);
      throw new Error('Invalid project data: missing or invalid ID');
    }
    
    return {
      ...project,
      id,
      name: project.projectName || project.name || 'Unnamed Project',
      description: project.description || '',
      createdAt: project.createdAt ? new Date(project.createdAt).toISOString() : new Date().toISOString(),
      updatedAt: project.updatedAt ? new Date(project.updatedAt).toISOString() : new Date().toISOString(),
      status: project.status || 'DRAFT',
      owner: project.owner?.email || project.owner || '',
      collaborators: project.sharedUsers ? Object.keys(project.sharedUsers) : [],
      metadata: {
        tags: Array.isArray(project.tags) ? project.tags : [],
        location: project.metadata?.location || undefined,
        satelliteData: project.metadata?.satelliteData || undefined
      }
    };
  }

  /**
   * Safely extracts data from API responses
   */
  private safelyExtractData<T>(response: any, defaultValue: T = [] as any): T {
    try {
      if (!response) return defaultValue;
      if (typeof response === 'string') {
        try {
          return JSON.parse(response);
        } catch {
          return defaultValue;
        }
      }
      return response.data || defaultValue;
    } catch (error) {
      console.error('Error extracting data:', error);
      return defaultValue;
    }
  }

  /**
   * Gets all projects for the current user
   */
  async getAllProjects(filter?: ProjectFilter): Promise<Project[]> {
    try {
      let url = `${PROJECT_ENDPOINTS.LIST}`;
      
      if (filter) {
        const params = new URLSearchParams();
        
        if (filter.status) {
          params.append('status', filter.status);
        }
        
        if (filter.search) {
          params.append('search', filter.search);
        }
        
        if (filter.tags && filter.tags.length > 0) {
          params.append('tags', filter.tags.join(','));
        }
        
        if (filter.archived !== undefined) {
          params.append('archived', filter.archived.toString());
        }
        
        if (filter.sortBy) {
          params.append('sortBy', filter.sortBy);
        }
        
        if (filter.sortOrder) {
          params.append('sortOrder', filter.sortOrder);
        }
        
        const queryString = params.toString();
        if (queryString) {
          url += `?${queryString}`;
        }
      }
      
      const response = await this.retryRequest(() => httpClient.get(url));
      const projectsData = this.safelyExtractData<any>(response);
      
      // Check if we have a paginated response with content array
      if (projectsData && projectsData.content && Array.isArray(projectsData.content)) {
        return projectsData.content.map(this.mapProjectResponse.bind(this));
      }
      
      // Fallback for non-paginated response
      if (Array.isArray(projectsData)) {
        return projectsData.map(this.mapProjectResponse.bind(this));
      }
      
      // If we get here, something unexpected happened
      console.warn('Unexpected response format:', projectsData);
      return [];
    } catch (error: any) {
      console.error('Failed to load projects:', error);
      return [];
    }
  }

  /**
   * Gets a specific project by ID
   */
  async getProject(id: any): Promise<Project | null> {
    const stringId = this.ensureStringId(id);
    if (!stringId) {
      console.error('Invalid project ID provided:', id);
      return null;
    }

    try {
      this.retryCount = 0; // Reset retry count
      const response = await this.retryRequest(() => httpClient.get(PROJECT_ENDPOINTS.GET(stringId)));
      
      if (!response || !response.data) {
        console.error('No project data received for ID:', stringId);
        return null;
      }

      const project = this.safelyExtractData<Project>(response, null as any);
      if (!project) {
        console.error('Failed to extract project data for ID:', stringId);
        return null;
      }

      return this.mapProjectResponse(project);
    } catch (error: any) {
      console.error('Failed to load project:', error);
      throw error; // Propagate the error to handle it in the component
    }
  }

  /**
   * Creates a new project
   */
  async createProject(project: CreateProjectDto): Promise<Project | null> {
    try {
      this.retryCount = 0; // Reset retry count
      const response = await this.retryRequest(() => httpClient.post(PROJECT_ENDPOINTS.CREATE, {
        projectName: project.projectName,
        description: project.description || '',
        status: 'DRAFT'
      }));
      
      const createdProject = this.safelyExtractData<Project>(response, {} as Project);
      return createdProject ? this.mapProjectResponse(createdProject) : null;
    } catch (error: any) {
      console.error('Failed to create project:', error);
      return null;
    }
  }

  /**
   * Updates an existing project
   */
  async updateProject(
    id: string,
    project: Partial<CreateProjectDto>
  ): Promise<Project | null> {
    try {
      this.retryCount = 0; // Reset retry count
      const response = await this.retryRequest(() => httpClient.put(PROJECT_ENDPOINTS.UPDATE(id), project));
      const updatedProject = this.safelyExtractData<Project>(response, {} as Project);
      return updatedProject ? this.mapProjectResponse(updatedProject) : null;
    } catch (error: any) {
      console.error('Failed to update project:', error);
      return null;
    }
  }

  /**
   * Deletes a project
   */
  async deleteProject(id: string): Promise<boolean> {
    try {
      this.retryCount = 0; // Reset retry count
      await this.retryRequest(() => httpClient.delete(PROJECT_ENDPOINTS.DELETE(id)));
      return true;
    } catch (error: any) {
      console.error('Failed to delete project:', error);
      return false;
    }
  }

  /**
   * Shares a project with another user
   */
  async shareProject(request: ProjectSharingRequest): Promise<Project | null> {
    try {
      this.retryCount = 0; // Reset retry count
      const response = await this.retryRequest(() => httpClient.post(PROJECT_ENDPOINTS.SHARE, request));
      const sharedProject = this.safelyExtractData<Project>(response, {} as Project);
      return sharedProject ? this.mapProjectResponse(sharedProject) : null;
    } catch (error: any) {
      console.error('Failed to share project:', error);
      return null;
    }
  }

  /**
   * Unshares a project with another user
   */
  async unshareProject(request: ProjectSharingRequest): Promise<Project | null> {
    try {
      this.retryCount = 0; // Reset retry count
      const response = await this.retryRequest(() => httpClient.post(PROJECT_ENDPOINTS.UNSHARE, request));
      const unsharedProject = this.safelyExtractData<Project>(response, {} as Project);
      return unsharedProject ? this.mapProjectResponse(unsharedProject) : null;
    } catch (error: any) {
      console.error('Failed to unshare project:', error);
      return null;
    }
  }

  /**
   * Archives a project
   */
  async archiveProject(projectId: string): Promise<boolean> {
    try {
      this.retryCount = 0; // Reset retry count
      await this.retryRequest(() => httpClient.post(PROJECT_ENDPOINTS.ARCHIVE(projectId), {}));
      return true;
    } catch (error: any) {
      console.error('Failed to archive project:', error);
      return false;
    }
  }

  /**
   * Unarchives a project
   */
  async unarchiveProject(projectId: string): Promise<boolean> {
    try {
      this.retryCount = 0; // Reset retry count
      await this.retryRequest(() => httpClient.post(PROJECT_ENDPOINTS.UNARCHIVE(projectId), {}));
      return true;
    } catch (error: any) {
      console.error('Failed to unarchive project:', error);
      return false;
    }
  }

  /**
   * Updates a project's status
   */
  async updateProjectStatus(projectId: string, status: ProjectStatus): Promise<Project | null> {
    try {
      this.retryCount = 0; // Reset retry count
      const response = await this.retryRequest(() => httpClient.put(`${PROJECT_ENDPOINTS.UPDATE(projectId)}/status?status=${status}`, {}));
      const updatedProject = this.safelyExtractData<Project>(response, {} as Project);
      return updatedProject ? this.mapProjectResponse(updatedProject) : null;
    } catch (error: any) {
      console.error('Failed to update project status:', error);
      return null;
    }
  }

  /**
   * Adds a tag to a project
   */
  async addTagToProject(projectId: string, tag: string): Promise<Project | null> {
    try {
      this.retryCount = 0; // Reset retry count
      const response = await this.retryRequest(() => httpClient.post(PROJECT_ENDPOINTS.ADD_TAG(projectId, tag), {}));
      const updatedProject = this.safelyExtractData<Project>(response, {} as Project);
      return updatedProject ? this.mapProjectResponse(updatedProject) : null;
    } catch (error: any) {
      console.error('Failed to add tag to project:', error);
      return null;
    }
  }
}

export const projectsService = new ProjectsService()

