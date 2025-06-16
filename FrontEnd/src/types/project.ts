import { User } from './user'; // Assuming User type exists for assignments

// Define possible project statuses
export type ProjectStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'ARCHIVED';

export interface Project {
  id: string;
  name: string;
  description: string;
  status: ProjectStatus;
  // Optional: Add fields for assigned users if needed later
  // assignedUsers?: User[];
  // assignedThematicians?: User[];
  // createdAt?: string; // Or Date
  // updatedAt?: string; // Or Date
}

// Interface for creating a new project (might omit id, status, etc.)
export interface CreateProjectData {
  name: string;
  description: string;
  // status might be set automatically on the backend
}

// Interface for updating a project (all fields optional)
export interface UpdateProjectData {
  name?: string;
  description?: string;
  status?: ProjectStatus;
}
