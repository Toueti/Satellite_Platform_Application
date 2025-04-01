import { httpClient } from '@/utils/http-client'
import { PROJECT_ENDPOINTS } from '@/config/api'
import type { Image } from './images.service'
import type { ProjectStatus } from '@/types/api'

export interface Project {
  id: string
  name: string
  description: string
  owner: string
  createdAt: Date
  updatedAt: Date
  lastAccessedTime?: Date
  archived: boolean
  tags?: string[]
  sharedUsers?: string[]
  status: ProjectStatus
  collaborators: string[]
  metadata: {
    location?: {
      lat: number
      lng: number
    }
    tags: string[]
    satelliteData?: {
      source: string
      resolution: string
      captureDate: string
    }
  }
}

export interface CreateProjectDto {
  name: string
  description: string
}

// Interface for the project sharing request
export interface ProjectSharingRequest {
  projectId: string
  otherEmail: string
}

class ProjectsService {
  // Helper function to map project response data
  private mapProjectResponse(project: any): Project {
    return {
      ...project,
      createdAt: new Date(project.createdAt),
      updatedAt: new Date(project.updatedAt),
      lastAccessedTime: project.lastAccessedTime
        ? new Date(project.lastAccessedTime)
        : undefined,
    }
  }

  async getAllProjects(): Promise<Project[]> {
    try {
      const response = await httpClient.get(PROJECT_ENDPOINTS.LIST)
      // Map the response data to the correct types
      return response.data.map(this.mapProjectResponse)
    } catch (error: any) {
      throw new Error(error.message || 'Failed to load projects')
    }
  }

  async getProject(id: string): Promise<Project> {
    try {
      const response = await httpClient.get(PROJECT_ENDPOINTS.GET(id))
      return this.mapProjectResponse(response.data)
    } catch (error: any) {
      throw new Error(error.message || 'Failed to load project')
    }
  }

  async createProject(project: CreateProjectDto): Promise<Project> {
    try {
      const response = await httpClient.post(PROJECT_ENDPOINTS.CREATE, project)
      return this.mapProjectResponse(response.data)
    } catch (error: any) {
      throw new Error(error.message || 'Failed to create project')
    }
  }

  async updateProject(
    id: string,
    project: Partial<CreateProjectDto>
  ): Promise<Project> {
    try {
      const response = await httpClient.put(PROJECT_ENDPOINTS.UPDATE(id), project)
      return this.mapProjectResponse(response.data)
    } catch (error: any) {
      throw new Error(error.message || 'Failed to update project')
    }
  }

  async deleteProject(id: string): Promise<void> {
    try {
      await httpClient.delete(PROJECT_ENDPOINTS.DELETE(id))
    } catch (error: any) {
      throw new Error(error.message || 'Failed to delete project')
    }
  }

  // New methods for project sharing
  async shareProject(request: ProjectSharingRequest): Promise<Project> {
    try {
      const response = await httpClient.post(PROJECT_ENDPOINTS.SHARE, request)
      return this.mapProjectResponse(response.data)
    } catch (error: any) {
      throw new Error(error.message || 'Failed to share project')
    }
  }

  async unshareProject(request: ProjectSharingRequest): Promise<Project> {
    try {
      const response = await httpClient.post(PROJECT_ENDPOINTS.UNSHARE, request)
      return this.mapProjectResponse(response.data)
    } catch (error: any) {
      throw new Error(error.message || 'Failed to unshare project')
    }
  }

  async getSharedUsers(projectId: string): Promise<string[]> {
    // Assuming the backend returns an array of user emails
    try {
      const response = await httpClient.get(
        PROJECT_ENDPOINTS.SHARED_USERS(projectId)
      )
      return response.data
    } catch (error: any) {
      throw new Error(error.message || 'Failed to get shared users')
    }
  }

  // New methods for archiving
  async archiveProject(projectId: string): Promise<void> {
    try {
      await httpClient.post(PROJECT_ENDPOINTS.ARCHIVE(projectId), {})
    } catch (error: any) {
      throw new Error(error.message || 'Failed to archive project')
    }
  }

  async unarchiveProject(projectId: string): Promise<void> {
    try {
      await httpClient.post(PROJECT_ENDPOINTS.UNARCHIVE(projectId), {})
    } catch (error: any) {
      throw new Error(error.message || 'Failed to unarchive project')
    }
  }

  // New methods for image association
  async getImagesByProject(projectId: string): Promise<Image[]> {
    try {
      const response = await httpClient.get(PROJECT_ENDPOINTS.GET_IMAGES(projectId))
      return response.data.map((image: any) => ({
        ...image,
        createdAt: new Date(image.createdAt),
        updatedAt: new Date(image.updatedAt),
      }))
    } catch (error: any) {
      throw new Error(error.message || 'Failed to get project images')
    }
  }

  async addImageToProject(projectId: string, imageId: string): Promise<void> {
    try {
      await httpClient.post(PROJECT_ENDPOINTS.ADD_IMAGE(projectId, imageId), {})
    } catch (error: any) {
      throw new Error(error.message || 'Failed to add image to project')
    }
  }

  async removeImageFromProject(projectId: string, imageId: string): Promise<void> {
    try {
      await httpClient.delete(PROJECT_ENDPOINTS.REMOVE_IMAGE(projectId, imageId))
    } catch (error: any) {
      throw new Error(error.message || 'Failed to remove image from project')
    }
  }
}

export const projectsService = new ProjectsService()
