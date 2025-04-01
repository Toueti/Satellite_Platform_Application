import { httpClient } from '@/utils/http-client';
import { RESOURCE_ENDPOINTS } from '@/config/api';

export interface GeeParams {
    startDate: string;
    endDate: string;
    region: string; // Assuming GeoJSON string
    analysisType: string; // e.g., 'vegetation', 'water', 'land'
    // Add other relevant parameters
}

export interface GeeResult {
  // Define the structure of GEE results based on the backend
  id: string;
  imageUrl: string; // Assuming a URL for the result image
  // Add other result properties
}
class GeeService {
  async search(query: string): Promise<any> { // Replace 'any' with a more specific type if possible
    const url = `${RESOURCE_ENDPOINTS.GEE.SEARCH}?query=${encodeURIComponent(query)}`;
    return httpClient.get(url);
  }

  async process(params: GeeParams): Promise<GeeResult> {
    return httpClient.post(RESOURCE_ENDPOINTS.GEE.PROCESS, params);
  }
}

export const geeService = new GeeService();
