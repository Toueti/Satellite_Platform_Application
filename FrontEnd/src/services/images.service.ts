import { httpClient } from '@/utils/api/http-client';
import { RESOURCE_ENDPOINTS, PROJECT_ENDPOINTS } from '@/config/api';
import { SatelliteImage, ImageAnnotation, AnalysisResult } from '@/types/image';
import { authService } from '@/services/auth.service';
// Removed API_BASE_URL import

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
  searchTerm?: string; // Added searchTerm property
  sortBy?: 'captureDate' | 'uploadDate' | 'size' | 'name';
  sortOrder?: 'asc' | 'desc';
  dateRange?: {
    start: Date;
    end: Date;
  };
  favorites?: boolean;
}

export class ImagesService {
  private static instance: ImagesService;
  // Removed baseUrl property, rely on httpClient and relative paths

  private constructor() {}

  public static getInstance(): ImagesService {
    if (!ImagesService.instance) {
      ImagesService.instance = new ImagesService();
    }
    return ImagesService.instance;
  }

  // Helper function to map raw image data (from backend) to SatelliteImage (frontend type)
  private mapToSatelliteImage(imageData: any): SatelliteImage {
    const imageId = imageData.imageId || imageData.id; // Use imageId primarily
    const filename = imageData.imageName || imageData.filename || 'Unknown Filename';
    // Construct URL assuming a backend endpoint exists to serve the image by ID
    // Use relative path; httpClient in getImageData will handle the base URL
    const imageUrl = imageId ? `/geospatial/images/${imageId}/data` : '/placeholder-image.png';
    // Note: Using the full data URL for thumbnail might be inefficient.
    // Consider a dedicated thumbnail endpoint if performance is an issue.
    const thumbnailUrl = imageUrl; 

    // Extract dates if available. The getImagesByProject response item lacks date fields.
    // Set to null if not provided by the specific backend response for this item.
    const captureDate = imageData.captureDate || imageData.metadata?.captureDate || null; 
    const uploadDate = imageData.uploadDate || imageData.createdAt || null; 

    return {
      id: imageId,
      filename: filename,
      url: imageUrl,
      thumbnailUrl: thumbnailUrl, // Use the same URL for now
      captureDate: captureDate, // Will be null if not provided
      uploadDate: uploadDate,   // Will be null if not provided
      location: imageData.location || { latitude: 0, longitude: 0 }, // Default location
      // Assuming fileSize is in bytes, store it as is. Formatting happens in the component.
      size: imageData.fileSize || imageData.size || 0,
      resolution: imageData.resolution || 'Unknown',
      bands: imageData.bands || [],
      metadata: imageData.metadata || {},
      tags: imageData.tags || [],
      annotations: imageData.annotations || [],
    };
  }

  async getAllImages(): Promise<SatelliteImage[]> { // Return SatelliteImage[]
    try {
      // Use relative path
      const response = await httpClient.get('/api/images');
      // Map the raw data to SatelliteImage[]
      const images = Array.isArray(response.data) ? response.data.map(this.mapToSatelliteImage) : [];
      return images;
    } catch (error: any) {
      console.error('Error fetching all images:', error);
      if (error.message && error.message.includes('No static resource api/images')) {
        console.warn('Images endpoint not implemented on backend. Returning empty array.');
        return [];
      }
      throw error;
    }
  }

  async getImageById(id: string): Promise<Image> {
    try {
      // Use relative path
      const response = await httpClient.get(`/api/images/${id}`);
      return response.data; // Adjusted to return response.data
    } catch (error: any) {
      console.error(`Error fetching image with ID ${id}:`, error);
      if (error.message && error.message.includes('No static resource api/images')) {
        console.warn('Image endpoint not implemented on backend.');
        throw new Error('Image service not available');
      }
      throw error;
    }
  }

  async getImageData(imageId: string): Promise<Blob> {
    try {
      // Use relative path
      const url = `/geospatial/images/${imageId}/data`;
      // console.log(`Fetching image data from: ${url}`); // httpClient logs the full URL now
      // Use the updated httpClient with responseType: 'blob'
      const blobResponse = await httpClient.get(url, {
        responseType: 'blob', // Specify blob response type
      });

      // httpClient now throws for non-ok responses, so if we reach here, it's a success.
      // The response should be a Blob directly.
      if (blobResponse instanceof Blob) {
        // console.log(`Successfully fetched image data for ID ${imageId}, size: ${blobResponse.size} bytes`); // Commented out
        return blobResponse;
      } else {
        // This case should ideally not happen if httpClient works as expected
        console.error(`Error fetching image data for ID ${imageId}: Expected Blob but received`, blobResponse);
        throw new Error('Failed to fetch image data: Invalid response format received from httpClient.');
      }
    } catch (error: any) {
      // Log the error caught from httpClient
      console.error(`Error fetching image data for ID ${imageId}:`, error);
      // Re-throw a more specific error message for the service layer
      throw new Error(`Failed to fetch image data for ID ${imageId}: ${error.message || 'Unknown error'}`);
    }
  }

  async getImagesByProject(projectId: string | string[] | null | undefined, filter?: ImageFilter): Promise<SatelliteImage[]> {
    try {
      if (!projectId) {
        console.warn('No project ID provided for getImagesByProject');
        return [];
      }

      const id = Array.isArray(projectId) ? projectId[0] : projectId;
      const queryParams = new URLSearchParams();
      if (filter) {
        if (filter.dateFrom) queryParams.append('dateFrom', filter.dateFrom);
        if (filter.dateTo) queryParams.append('dateTo', filter.dateTo);
        if (filter.tags) queryParams.append('tags', filter.tags.join(','));
        // Add other filter properties if the backend supports them
        if (filter.sortBy) queryParams.append('sortBy', filter.sortBy);
        if (filter.sortOrder) queryParams.append('sortOrder', filter.sortOrder);
        if (filter.cloudCoverageMax !== undefined) queryParams.append('cloudCoverageMax', filter.cloudCoverageMax.toString());
        if (filter.satellite) queryParams.append('satellite', filter.satellite);
        if (filter.favorites !== undefined) queryParams.append('favorites', filter.favorites.toString());
        if (filter.location) {
          queryParams.append('latitude', filter.location.latitude.toString());
          queryParams.append('longitude', filter.location.longitude.toString());
          queryParams.append('radiusKm', filter.location.radiusKm.toString());
        }
        if (filter.dateRange) {
          queryParams.append('dateRangeStart', filter.dateRange.start.toISOString());
          queryParams.append('dateRangeEnd', filter.dateRange.end.toISOString());
        }
      }

      // Use relative path
      const url = `/geospatial/images/by-project/${id}${queryParams.toString() ? `?${queryParams.toString()}` : ''}`;
      // console.log('Making request to:', url); // httpClient logs the full URL now

      const response = await httpClient.get(url);
      // The httpClient seems to automatically extract the backend's 'data' field.
      // So, response.data should contain { content: [...], page: {...} }
      const imageContent = response?.data?.content; 
      // console.log('Extracted imageContent (final attempt):', imageContent); // Commented out // Keep one log for confirmation

      const images = Array.isArray(imageContent) ? await Promise.all(imageContent.map(async (image: any) => {
        const normalizedImage = {
          ...image,
          id: image.imageId, // Use imageId from the backend response item
        };

        let imageUrl = '/placeholder-image.png'; // Default fallback
        let thumbnailUrl = '/placeholder-image.png';

        if (normalizedImage && normalizedImage.id) {
          try {
            const imageBlob = await this.getImageData(normalizedImage.id);
            imageUrl = URL.createObjectURL(imageBlob);
            thumbnailUrl = imageUrl; // Same URL for thumbnail
          } catch (error) {
            console.warn(`Failed to fetch image data for image ${normalizedImage.id}:`, error);
          }
        }
        
        // Explicitly construct the SatelliteImage object with correct property names
        const result: SatelliteImage = {
          id: normalizedImage.id, // Already correctly mapped from imageId
          filename: normalizedImage.imageName || 'Unknown Filename', // Map imageName to filename
          url: imageUrl,
          thumbnailUrl: thumbnailUrl,
          captureDate: normalizedImage.captureDate || normalizedImage.metadata?.captureDate || null, // Set to null if missing
          uploadDate: normalizedImage.createdAt || null, // Set to null if missing (as createdAt is not in the response item)
          location: normalizedImage.location || { latitude: 0, longitude: 0 },
          size: normalizedImage.fileSize || 0, // Map fileSize to size
          resolution: normalizedImage.resolution || 'Unknown',
          bands: normalizedImage.bands || [],
          metadata: normalizedImage.metadata || {},
          tags: normalizedImage.tags || [],
          annotations: normalizedImage.annotations || [],
        };
        return result;
      })) : [];

      if (images.length === 0) {
        console.log('No images found for project:', id);
      } else {
        // console.log('Project images response:', images); // Commented out
      }

      return images;
    } catch (error: any) {
      // Added more detailed logging here
      console.error('!!! Outer catch block in getImagesByProject hit:', error); 
      return [];
    }
  }

  async uploadImage(formData: FormData): Promise<SatelliteImage> { // Return SatelliteImage
    try {
      console.log('Uploading image with formData:', formData);

      if (!formData || formData.entries().next().done) {
        console.warn('Empty FormData provided to uploadImage');
        throw new Error('No files selected for upload. Please select at least one image file.');
      }

      const formDataEntries = Array.from(formData.entries());
      formDataEntries.forEach(([key, value]) => {
        console.log(`FormData entry - ${key}:`, value instanceof File ? `File: ${value.name}, size: ${value.size} bytes` : value);
      });

      const file = formData.get('image') as File;
      const projectId = formData.get('projectId') as string;

      if (!file) {
        throw new Error('No image file provided');
      }

      if (!projectId) {
        throw new Error('No project ID provided');
      }

      const updatedFormData = new FormData();
      updatedFormData.append('file', file);
      updatedFormData.append('projectId', projectId);
      updatedFormData.append('imageName', file.name);

      const metadata = {
        description: `Uploaded image: ${file.name}`,
        originalFilename: file.name,
        fileSize: file.size,
        mimeType: file.type
      };
      updatedFormData.append('metadata', JSON.stringify(metadata));
      updatedFormData.append('storageType', 'filesystem');

      // Use httpClient.post - it now handles FormData correctly
      const response = await httpClient.post(
        RESOURCE_ENDPOINTS.IMAGES.UPLOAD, // Pass relative path
        updatedFormData, // Pass FormData directly
        { requiresAuth: true } // Ensure auth is handled
      );

      // httpClient throws on non-ok responses, so we just need to process the successful response
      const data = response; // httpClient returns the parsed JSON data directly
      console.log('Image uploaded successfully:', data);
      // Map the nested 'data' property from the response to SatelliteImage
      if (data && data.data) {
        return this.mapToSatelliteImage(data.data); 
      } else {
        // Handle cases where the expected 'data.data' structure is missing
        console.error('Unexpected response structure from uploadImage:', data);
        // Return a placeholder or throw an error, depending on desired handling
        // For now, let's return a placeholder similar to what was happening before,
        // but log the error clearly.
        return this.mapToSatelliteImage({}); // Pass empty object to get placeholder
      }
    } catch (error: any) {
      console.error('Error uploading image:', error);
      throw error;
    }
  }

  async deleteImage(id: string): Promise<void> {
    try {
      // Use relative path
      await httpClient.delete(`/geospatial/images${id}`);
    } catch (error: any) {
      console.error(`Error deleting image with ID ${id}:`, error);
      if (error.message && error.message.includes('No static resource api/images')) {
        console.warn('Image delete endpoint not implemented on backend.');
        throw new Error('Image delete service not available');
      }
      throw error;
    }
  }

  async updateImageAnnotations(id: string, annotations: ImageAnnotation[]): Promise<Image> {
    try {
      // Use relative path
      const response = await httpClient.put(`/api/images/${id}/annotations`, { annotations });
      return response.data; // Adjusted to return response.data
    } catch (error: any) {
      console.error(`Error updating annotations for image with ID ${id}:`, error);
      if (error.message && error.message.includes('No static resource api/images')) {
        console.warn('Image annotations endpoint not implemented on backend.');
        throw new Error('Image annotations service not available');
      }
      throw error;
    }
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

  async getAnalysisResults(imageId: string): Promise<AnalysisResult[]> {
    const response = await httpClient.get(RESOURCE_ENDPOINTS.IMAGES.ANALYSIS.GET(imageId));
    return response.data;
  }

  async getProjectAnalysisResults(projectId: string): Promise<AnalysisResult[]> {
    const response = await httpClient.get(RESOURCE_ENDPOINTS.IMAGES.ANALYSIS.GET_BY_PROJECT(projectId));
    return response.data;
  }
}

export const imagesService = ImagesService.getInstance();
