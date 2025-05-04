import { authService } from '@/services/auth.service';

interface FetchOptions extends RequestInit {
  token?: string;
}

interface ApiResponse<T = any> {
  success: boolean;
  data: T;
  error?: string;
}

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

// Retry configuration
const MAX_RETRIES = 3;
const INITIAL_RETRY_DELAY = 1000; // 1 second

const sleep = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

// Add retry logic to handle rate limiting
const handleRequest = async (requestFn: () => Promise<any>, retries = 0): Promise<any> => {
  try {
    return await requestFn();
  } catch (error: any) {
    if (error.response?.status === 429 && retries < MAX_RETRIES) {
      const delay = INITIAL_RETRY_DELAY * Math.pow(2, retries);
      console.log(`Rate limited. Retrying in ${delay}ms...`);
      await sleep(delay);
      return handleRequest(requestFn, retries + 1);
    }
    throw error;
  }
};

export async function fetchWithAuth<T>(
  endpoint: string,
  options: FetchOptions = {}
): Promise<ApiResponse<T>> {
  const token = authService.getToken();
  
  if (!token) {
    throw new Error('No authentication token found');
  }

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
    ...options.headers,
  };

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      const errorData = await response.json();
      throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
    }
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  const data = await response.json();
  return {
    success: true,
    data,
  };
}

export const api = {
  async get<T = any>(endpoint: string): Promise<T> {
    return handleRequest(async () => {
      const response = await fetchWithAuth<T>(endpoint);
      return response.data;
    });
  },

  async post<T = any>(endpoint: string, body: any): Promise<T> {
    return handleRequest(async () => {
      const response = await fetchWithAuth<T>(endpoint, {
        method: 'POST',
        body: JSON.stringify(body),
      });
      return response.data;
    });
  },

  async put<T = any>(endpoint: string, body: any): Promise<T> {
    return handleRequest(async () => {
      const response = await fetchWithAuth<T>(endpoint, {
        method: 'PUT',
        body: JSON.stringify(body),
      });
      return response.data;
    });
  },

  async delete<T = any>(endpoint: string): Promise<T> {
    return handleRequest(async () => {
      const response = await fetchWithAuth<T>(endpoint, {
        method: 'DELETE',
      });
      return response.data;
    });
  },
};
