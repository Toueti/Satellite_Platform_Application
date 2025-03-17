import { authService } from '../services/auth.service';

interface RequestOptions extends RequestInit {
    requiresAuth?: boolean;
}

class HttpClient {
    private async request(url: string, options: RequestOptions = {}) {
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
            }
            throw new Error('Request failed');
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
