// Common API Response wrapper
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: string;
  timestamp: string;
}

// Project related types
export interface Project {
  id: string;
  name: string;
  description: string;
  createdAt: string;
  updatedAt: string;
  status: ProjectStatus;
  owner: string;
  collaborators: string[];
  metadata: ProjectMetadata;
}

export enum ProjectStatus {
  DRAFT = 'DRAFT',
  ACTIVE = 'ACTIVE',
  COMPLETED = 'COMPLETED',
  ARCHIVED = 'ARCHIVED'
}

export interface ProjectMetadata {
  location?: {
    lat: number;
    lng: number;
  };
  tags: string[];
  satelliteData?: {
    source: string;
    resolution: string;
    captureDate: string;
  };
}

// Dashboard related types
export interface DashboardData {
  totalProjects: number;
  mapCoverage: string;
  recentProjects: Project[];
  statistics: DashboardStatistics;
}

export interface DashboardStatistics {
  activeProjects: number;
  completedProjects: number;
  totalStorage: string;
  processedImages: number;
}

// Auth related types
export interface User {
  id: string;
  email: string;
  name: string;
  role: UserRole;
  createdAt: string;
  lastLogin: string;
}

export enum UserRole {
  ADMIN = 'ADMIN',
  USER = 'USER',
  GUEST = 'GUEST'
}

export interface AuthResponse {
  user: User;
  token: string;
  refreshToken: string;
}

// Error types
export interface ApiError {
  code: string;
  message: string;
  details?: Record<string, unknown>;
}
