import { authService } from '../services/auth.service';

interface RequestOptions extends RequestInit {
    requiresAuth?: boolean;
}

class HttpClient {
    private async request(url: string, options: RequestOptions = {}, retryCount = 0): Promise<any> {
        const { requiresAuth = true, headers = {}, ...rest } = options;

        const requestHeaders = new Headers({
            'Content-Type': 'application/json',
            ...headers as Record<string, string>
        });

        if (requiresAuth) {
            const token = authService.getToken();
            if (token) {
                requestHeaders.set('Authorization', `Bearer ${token}`);
            }
        }

        // Log headers correctly
        requestHeaders.forEach((value, key) => {
        });

        const response = await fetch(url, {
            headers: requestHeaders,
            ...rest,
        });

        if (!response.ok) {
            if (response.status === 401) {
                // Handle unauthorized access
                authService.logout();
                window.location.href = '/auth/login';
                throw new Error('Unauthorized access');
            } else if (response.status === 429 && retryCount < 3) {
                // Handle rate limiting with exponential backoff
                const delay = Math.pow(2, retryCount) * 1000; // 1, 2, 4 seconds
                console.warn(`Rate limited. Retrying in ${delay / 1000} seconds...`);
                await new Promise(resolve => setTimeout(resolve, delay));
                return this.request(url, options, retryCount + 1);
            }

            // Check for JSON response before attempting to parse
            const contentType = response.headers.get("content-type");
            if (contentType && contentType.includes("application/json")) {
              const errorData = await response.json();
              if(response.status === 429){
                throw new Error(errorData.message || 'You have been rate limited. Please try again later.');
              }
              throw new Error(errorData.message || 'Request failed');
            } else {
                throw new Error('Request failed');
            }

        }

        return response.json();
    }

    get(url: string, options: RequestOptions = {}) {
        return this.request(url, { ...options, method: 'GET' });
    }

    post(url: string, data: any, options: RequestOptions = {}) {
        return this.request(url, {
            ...options,
            method: 'POST',
            body: JSON.stringify(data),
        });
    }

    put(url: string, data: any, options: RequestOptions = {}) {
        return this.request(url, {
            ...options,
            method: 'PUT',
            body: JSON.stringify(data),
        });
    }

    delete(url: string, options: RequestOptions = {}) {
        return this.request(url, { ...options, method: 'DELETE' });
    }
}

export const httpClient = new HttpClient();
