import axios from 'axios';
import { Project, ProjectStatus } from '@/types/api';

const API_BASE_URL = 'http://localhost:8080/api/thematician/projects';

export interface ProjectSharingRequest {
    projectId: string;
    otherEmail: string;
}

export const fetchStatistics = async (token: string) => {
    try {
        const response = await axios.get(`${API_BASE_URL}/statistics`, {
            headers: {
                Authorization: `Bearer ${token}`,
                Accept: 'application/json',
            },
        });
        return response.data.data as {
            totalProjects: number;
            imagesPerProject: Record<string, number>;
            lastAccessTime?: Record<string, string>;
        };
    } catch (error) {
        console.error('Error fetching statistics:', error);
        if (axios.isAxiosError(error)) {
            console.error('Axios error details:', error.response?.data);
        }
        throw new Error('Failed to fetch statistics');
    }
};

export interface PaginatedResponse<T> {
    content: T[];
    page: {
        size: number;
        number: number;
        totalElements: number;
        totalPages: number;
    };
}

export interface ProjectsResponse {
    timestamp: string;
    status: string;
    message: string;
    data: PaginatedResponse<Project>;
}

export const getAllProjects = async (page: number = 0, size: number = 10): Promise<PaginatedResponse<Project>> => {
    try {
        const token = localStorage.getItem('token');
        if (!token) {
            throw new Error('No authentication token found');
        }

        const response = await axios.get(`${API_BASE_URL}/all`, {
            params: {
                page,
                size
            },
            headers: {
                'Authorization': `Bearer ${token}`,
                'Accept': 'application/json'
            }
        });
        return response.data.data;
    } catch (error) {
        console.error('Error fetching projects:', error);
        if (axios.isAxiosError(error)) {
            if (error.response?.status === 403) {
                throw new Error('Not authorized to view projects');
            }
            if (error.response?.status === 401) {
                throw new Error('Authentication token expired. Please log in again.');
            }
            if (error.response?.data?.message) {
                throw new Error(error.response.data.message);
            }
        }
        throw new Error('Failed to fetch projects');
    }
};

export const getProject = async (id: string): Promise<Project> => {
    try {
        const token = localStorage.getItem('token');
        if (!token) {
            throw new Error('No authentication token found');
        }

        const response = await axios.get(`${API_BASE_URL}/${id}`, {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Accept': 'application/json'
            }
        });
        return response.data.data;
    } catch (error) {
        console.error('Error fetching project:', error);
        if (axios.isAxiosError(error)) {
            const status = error.response?.status;
            const message = error.response?.data?.message || error.message;
            if (status === 404) {
                throw new Error(`Project not found (404)`);
            } else if (status === 401) {
                 throw new Error('Authentication token expired. Please log in again.');
            } else if (status === 403) {
                 throw new Error('Not authorized to view this project');
            }
            // Include status in the generic message if available
            throw new Error(`Failed to fetch project: ${message}${status ? ` (Status: ${status})` : ''}`);
        }
        // Fallback for non-Axios errors
        throw new Error('Failed to fetch project');
    }
};

export const shareProject = async (request: ProjectSharingRequest): Promise<void> => {
    try {
        const token = localStorage.getItem('token');
        if (!token) {
            throw new Error('No authentication token found');
        }

        await axios.post(`${API_BASE_URL}/${request.projectId}/share`, 
            { email: request.otherEmail },
            {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Accept': 'application/json'
                }
            }
        );
    } catch (error) {
        console.error('Error sharing project:', error);
        throw new Error('Failed to share project');
    }
};

export const unshareProject = async (request: ProjectSharingRequest): Promise<void> => {
    try {
        const token = localStorage.getItem('token');
        if (!token) {
            throw new Error('No authentication token found');
        }

        await axios.post(
            `${API_BASE_URL}/${request.projectId}/unshare`,
            { email: request.otherEmail },
            {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Accept': 'application/json'
                }
            }
        );
    } catch (error) {
        console.error('Error unsharing project:', error);
        throw new Error('Failed to unshare project');
    }
};

export const archiveProject = async (id: string): Promise<void> => {
    try {
        const token = localStorage.getItem('token');
        if (!token) {
            throw new Error('No authentication token found');
        }

        await axios.post(
            `${API_BASE_URL}/${id}/archive`,
            {},
            {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Accept': 'application/json'
                }
            }
        );
    } catch (error) {
        console.error('Error archiving project:', error);
        throw new Error('Failed to archive project');
    }
};

export const unarchiveProject = async (id: string): Promise<void> => {
    try {
        const token = localStorage.getItem('token');
        if (!token) {
            throw new Error('No authentication token found');
        }

        await axios.post(
            `${API_BASE_URL}/${id}/unarchive`,
            {},
            {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Accept': 'application/json'
                }
            }
        );
    } catch (error) {
        console.error('Error unarchiving project:', error);
        throw new Error('Failed to unarchive project');
    }
};

export const deleteProject = async (id: string): Promise<void> => {
    try {
        const token = localStorage.getItem('token');
        if (!token) {
            throw new Error('No authentication token found');
        }

        await axios.delete(`${API_BASE_URL}/${id}`, {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Accept': 'application/json'
            }
        });
    } catch (error) {   
        console.error('Error deleting project:', error);
        throw new Error('Failed to delete project');
    }
};
export const createProject = async (projectData: {
    projectName: string;
    description?: string;
    tags?: string[];
    status?: string;
}): Promise<Project> => {
    try {
        const token = localStorage.getItem('token');
        if (!token) {
            throw new Error('No authentication token found');
        }

        const response = await axios.post(`${API_BASE_URL}/create`, {
            projectName: projectData.projectName,
            description: projectData.description || '',
            tags: projectData.tags || ['initial'], // Default tags if not provided
            status: projectData.status || 'CREATED' // Match backend expectation
        }, {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            }
        });

        return response.data.data; // Assuming response follows { data: Project }
    } catch (error) {
        console.error('Error creating project:', error);
        if (axios.isAxiosError(error)) {
            if (error.response?.status === 403) {
                throw new Error('Not authorized to create projects');
            }
            if (error.response?.status === 401) {
                throw new Error('Authentication token expired. Please log in again.');
            }
            if (error.response?.data?.message) {
                throw new Error(error.response.data.message);
            }
        }
        throw new Error('Failed to create project');
    }
};

// Ensure createProject is exported in projectsService
export const projectsService = {
    fetchStatistics,
    getAllProjects,
    getProject,
    createProject,
    shareProject,
    unshareProject,
    archiveProject,
    unarchiveProject,
    deleteProject
};
