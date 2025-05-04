// Common API Response wrapper
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: string;
  timestamp: string;
}

export interface Project {
  id?: string;
  _id?: string;
  projectId?: string | { id?: string; _id?: string; $oid?: string; timestamp?: number; date?: string };
  name?: string;
  projectName?: string; // Added to match backend
  description?: string;
  createdAt?: string;
  updatedAt?: string;
  status?: string; // Changed to string to handle backend's "CREATED"
  owner?: string;
  ownerEmail?: string; // Added to match backend
  collaborators?: string[];
  images?: Array<{
      imageId: string;
      projectId: string;
      imageName: string;
      metadata: any;
      file: any;
      fileSize: number;
      storageType: string;
      storageIdentifier: string;
  }> | null; // Handle null from backend
  metadata?: ProjectMetadata;
  tags?: string[]; // Added to support backend's tags
}

export enum ProjectStatus {
  DRAFT = 'DRAFT',
  ACTIVE = 'ACTIVE',
  COMPLETED = 'COMPLETED',
  ARCHIVED = 'ARCHIVED',
  CREATED = 'CREATED' // Added to match backend
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

export interface PaginatedResponse<T> {
  content: T[];
  page: {
      size: number;
      number: number;
      totalElements: number;
      totalPages: number;
  };
}
// Error types
export interface ApiError {
  code: string;
  message: string;
  details?: Record<string, unknown>;
}
