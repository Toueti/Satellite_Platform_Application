import { httpClient } from '@/utils/http-client';
import { RESOURCE_ENDPOINTS, PROJECT_ENDPOINTS } from '@/config/api';
import { SatelliteImage, ImageAnnotation, AnalysisResult } from '@/types/image';

export interface Image {
  id: string;
  name: string;
  url: string;
  thumbnailUrl: string;
  projectId: string;
  createdAt: Date;
  updatedAt: Date;
  metadata?: Record<string, any>;
}

export interface ImageFilter {
  tags?: string[];
  dateFrom?: string;
  dateTo?: string;
  location?: {
    latitude: number;
    longitude: number;
    radiusKm: number;
  };
  satellite?: string;
  cloudCoverageMax?: number;
  sortBy?: 'captureDate' | 'uploadDate' | 'size' | 'name';
  sortOrder?: 'asc' | 'desc';
}

class ImagesService {
  private retryCount = 0;
  private lastRequestTime = 0;
  private readonly minRequestInterval = 100; // Minimum 100ms between requests
  private readonly maxRetries = 3;
  private readonly retryDelays = [1000, 2000, 3000];

  async getAllImages(): Promise<Image[]> {
    const response = await httpClient.get(RESOURCE_ENDPOINTS.IMAGES.LIST);
    return response.data.map((image: any) => ({
      ...image,
      createdAt: new Date(image.createdAt),
      updatedAt: new Date(image.updatedAt)
    }));
  }

  async getSatelliteImage(id: string): Promise<SatelliteImage> {
    const response = await httpClient.get(RESOURCE_ENDPOINTS.IMAGES.GET(id));
    return {
      ...response.data,
      captureDate: response.data.captureDate || response.data.metadata?.captureDate,
      uploadDate: response.data.createdAt,
      annotations: response.data.annotations || []
    };
  }

  async getImage(id: string): Promise<Image> {
    const response = await httpClient.get(RESOURCE_ENDPOINTS.IMAGES.GET(id));
    return {
      ...response.data,
      createdAt: new Date(response.data.createdAt),
      updatedAt: new Date(response.data.updatedAt)
    };
  }

  async getImagesByProject(projectId: string | string[] | null | undefined, filter?: ImageFilter): Promise<SatelliteImage[]> {
    // Early return if no projectId
    if (!projectId) {
      console.error('No project ID provided');
      return [];
    }

    // Ensure we have a valid string ID
    let id: string;
    if (typeof projectId === 'string') {
      id = projectId;
    } else if (Array.isArray(projectId) && projectId.length > 0) {
      id = String(projectId[0]);
    } else if (typeof projectId === 'object') {
      console.error('Invalid project ID type (object):', projectId);
      return [];
    } else {
      console.error('Invalid project ID type:', typeof projectId);
      return [];
    }

    // Additional validation
    if (!id || id === 'undefined' || id === 'null' || id === '[object Object]') {
      console.error('Invalid project ID value:', id);
      return [];
    }

    try {
      const queryParams = new URLSearchParams();
      
      if (filter) {
        if (filter.tags?.length) queryParams.set('tags', filter.tags.join(','));
        if (filter.dateFrom) queryParams.set('dateFrom', filter.dateFrom);
        if (filter.dateTo) queryParams.set('dateTo', filter.dateTo);
        if (filter.cloudCoverageMax) queryParams.set('cloudCoverage', filter.cloudCoverageMax.toString());
        if (filter.satellite) queryParams.set('satellite', filter.satellite);
        if (filter.sortBy) queryParams.set('sortBy', filter.sortBy);
        if (filter.sortOrder) queryParams.set('sortOrder', filter.sortOrder);
        
        if (filter.location) {
          queryParams.set('lat', filter.location.latitude.toString());
          queryParams.set('lng', filter.location.longitude.toString());
          queryParams.set('radius', filter.location.radiusKm.toString());
        }
      }
      
      const url = `${PROJECT_ENDPOINTS.GET_IMAGES(id)}${queryParams.toString() ? `?${queryParams.toString()}` : ''}`;
      console.log('Making request to:', url);
      
      const response = await httpClient.get(url);
      const images = Array.isArray(response.data) ? response.data.map((image: any) => ({
        ...image,
        captureDate: image.captureDate || image.metadata?.captureDate,
        uploadDate: image.createdAt,
        annotations: image.annotations || []
      })) : [];

      if (images.length === 0) {
        console.log('No images found for project:', id);
      }

      return images;
    } catch (error: any) {
      console.error('Error fetching images for project:', error);
      throw error; // Let the component handle the error
    }
  }

  // Assuming an upload endpoint that takes FormData
  async uploadImage(formData: FormData): Promise<Image> {
    const response = await httpClient.post(RESOURCE_ENDPOINTS.IMAGES.UPLOAD, formData);
    return {
      ...response.data,
      createdAt: new Date(response.data.createdAt),
      updatedAt: new Date(response.data.updatedAt)
    };
  }
  
  // Tag management
  async addTag(imageId: string, tag: string): Promise<SatelliteImage> {
    const response = await httpClient.post(PROJECT_ENDPOINTS.ADD_TAG(imageId, tag), { tag });
    return {
      ...response.data,
      captureDate: response.data.captureDate || response.data.metadata?.captureDate,
      uploadDate: response.data.createdAt,
      annotations: response.data.annotations || []
    };
  }
  
  async removeTag(imageId: string, tag: string): Promise<SatelliteImage> {
    const response = await httpClient.delete(PROJECT_ENDPOINTS.REMOVE_TAG(imageId, tag));
    return {
      ...response.data,
      captureDate: response.data.captureDate || response.data.metadata?.captureDate,
      uploadDate: response.data.createdAt,
      annotations: response.data.annotations || []
    };
  }
  
  // Annotation management
  async addAnnotation(imageId: string, annotation: Omit<ImageAnnotation, 'id' | 'createdAt' | 'createdBy'>): Promise<ImageAnnotation> {
    const response = await httpClient.post(RESOURCE_ENDPOINTS.IMAGES.ANNOTATIONS.ADD(imageId), annotation);
    return response.data;
  }

  async updateAnnotation(imageId: string, annotationId: string, annotation: Partial<ImageAnnotation>): Promise<ImageAnnotation> {
    const response = await httpClient.put(RESOURCE_ENDPOINTS.IMAGES.ANNOTATIONS.UPDATE(imageId, annotationId), annotation);
    return response.data;
  }

  async deleteAnnotation(imageId: string, annotationId: string): Promise<void> {
    await httpClient.delete(RESOURCE_ENDPOINTS.IMAGES.ANNOTATIONS.DELETE(imageId, annotationId));
  }

  async getAnnotations(imageId: string): Promise<ImageAnnotation[]> {
    const response = await httpClient.get(RESOURCE_ENDPOINTS.IMAGES.ANNOTATIONS.LIST(imageId));
    return response.data;
  }
  
  // Analysis results
  async getAnalysisResults(imageId: string): Promise<AnalysisResult[]> {
    const response = await httpClient.get(RESOURCE_ENDPOINTS.IMAGES.ANALYSIS.GET(imageId));
    return response.data;
  }

  async getProjectAnalysisResults(projectId: string): Promise<AnalysisResult[]> {
    const response = await httpClient.get(RESOURCE_ENDPOINTS.IMAGES.ANALYSIS.GET_BY_PROJECT(projectId));
    return response.data;
  }
}

export const imagesService = new ImagesService();
