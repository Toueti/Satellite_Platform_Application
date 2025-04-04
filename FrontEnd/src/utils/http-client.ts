import { authService } from '../services/auth.service';

interface RequestOptions extends RequestInit {
    requiresAuth?: boolean;
}

class HttpClient {
    private rateLimitDelay = 0;
    private lastRequestTime = 0;
    private readonly minRequestInterval = 100; // Minimum 100ms between requests

    private async request(url: string, options: RequestOptions = {}, retryCount = 0): Promise<any> {
        // Implement request throttling
        const now = Date.now();
        const timeSinceLastRequest = now - this.lastRequestTime;
        
        if (timeSinceLastRequest < this.minRequestInterval) {
            await new Promise(resolve => setTimeout(resolve, this.minRequestInterval - timeSinceLastRequest));
        }
        
        // Add jitter to prevent thundering herd
        if (this.rateLimitDelay > 0) {
            const jitter = Math.random() * 500; // Random delay up to 500ms
            await new Promise(resolve => setTimeout(resolve, this.rateLimitDelay + jitter));
        }

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

        try {
            this.lastRequestTime = Date.now();
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
                } else if (response.status === 429) {
                    // Implement exponential backoff with max delay
                    this.rateLimitDelay = Math.min(
                        (this.rateLimitDelay || 1000) * 2,
                        30000 // Max 30 second delay
                    );

                    if (retryCount < 3) {
                        console.warn(`Rate limited. Retrying in ${this.rateLimitDelay / 1000} seconds...`);
                        await new Promise(resolve => setTimeout(resolve, this.rateLimitDelay));
                        return this.request(url, options, retryCount + 1);
                    } else {
                        throw new Error('Rate limit exceeded. Please try again in a few minutes.');
                    }
                }

                // Check for JSON response before attempting to parse
                const contentType = response.headers.get("content-type");
                if (contentType && contentType.includes("application/json")) {
                    const errorData = await response.json();
                    throw new Error(errorData.message || 'Request failed');
                }
                throw new Error(`Request failed with status ${response.status}`);
            }

            // Reset rate limit delay on successful request
            this.rateLimitDelay = 0;

            // Parse response
            const contentType = response.headers.get("content-type");
            if (contentType && contentType.includes("application/json")) {
                return await response.json();
            }
            return await response.text();
        } catch (error: any) {
            if (error.message.includes('Rate limit')) {
                throw error; // Re-throw rate limit errors
            }
            throw new Error(error.message || 'Request failed');
        }
    }

    async get(url: string, options: RequestOptions = {}) {
        return this.request(url, { ...options, method: 'GET' });
    }

    async post(url: string, body: any, options: RequestOptions = {}) {
        return this.request(url, {
            ...options,
            method: 'POST',
            body: JSON.stringify(body),
        });
    }

    async put(url: string, body: any, options: RequestOptions = {}) {
        return this.request(url, {
            ...options,
            method: 'PUT',
            body: JSON.stringify(body),
        });
    }

    async delete(url: string, options: RequestOptions = {}) {
        return this.request(url, { ...options, method: 'DELETE' });
    }
}

export const httpClient = new HttpClient();
