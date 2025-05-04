import { httpClient } from '@/utils/http-client';
import { RESOURCE_ENDPOINTS } from '@/config/api';

export interface Satellite {
  id: string;
  name: string;
  noradId: number; // Assuming NORAD ID is used
  // Add other relevant satellite properties
}

class SatellitesService {
  async getAllSatellites(): Promise<Satellite[]> {
    const response = await httpClient.get(RESOURCE_ENDPOINTS.SATELLITES.LIST);
    return response.data;
  }

  async getSatellite(id: string): Promise<Satellite> {
    return httpClient.get(RESOURCE_ENDPOINTS.SATELLITES.GET(id));
  }
}

export const satellitesService = new SatellitesService();
