export enum ProjectStatus {
  ACTIVE = 'ACTIVE',
  COMPLETED = 'COMPLETED',
  ARCHIVED = 'ARCHIVED',
  DRAFT = 'DRAFT'
}

export interface Project {
  id: string;
  name: string;
  description?: string;
  status: ProjectStatus;
  createdAt: string;
  updatedAt: string;
  owner: string;
  collaborators: string[];
  metadata?: {
    location?: {
      latitude: number;
      longitude: number;
    };
    tags: string[];
    imageCount?: number;
    satelliteData?: {
      satellite: string;
      date: string;
      resolution: string;
    };
  };
}
