import { httpClient } from '@/utils/http-client';
import { PROJECT_ENDPOINTS } from '@/config/api';

export interface Project {
    id: string;
    name: string;
    description: string;
    createdAt: string;
    updatedAt: string;
    status: 'active' | 'completed' | 'archived';
    owner: string;
}

export interface CreateProjectDto {
    name: string;
    description: string;
}

class ProjectsService {
    async getAllProjects(): Promise<Project[]> {
        return httpClient.get(PROJECT_ENDPOINTS.LIST);
    }

    async getProject(id: string): Promise<Project> {
        return httpClient.get(PROJECT_ENDPOINTS.GET(id));
    }

    async createProject(project: CreateProjectDto): Promise<Project> {
        return httpClient.post(PROJECT_ENDPOINTS.CREATE, project);
    }

    async updateProject(id: string, project: Partial<CreateProjectDto>): Promise<Project> {
        return httpClient.put(PROJECT_ENDPOINTS.UPDATE(id), project);
    }

    async deleteProject(id: string): Promise<void> {
        return httpClient.delete(PROJECT_ENDPOINTS.DELETE(id));
    }
}

export const projectsService = new ProjectsService();
