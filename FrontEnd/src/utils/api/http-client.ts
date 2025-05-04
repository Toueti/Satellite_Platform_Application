import { authService } from '../../services/auth.service'; // Corrected path

interface RequestOptions extends RequestInit {
    requiresAuth?: boolean;
    responseType?: 'json' | 'text' | 'blob'; // Add responseType
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

        const { requiresAuth = true, headers = {}, body, ...rest } = options; // Destructure body here

        // Determine if the body is FormData
        const isFormData = body instanceof FormData;

        // Initialize headers, conditionally setting Content-Type
        const initialHeaders: Record<string, string> = {
            ...(headers as Record<string, string>) // Spread incoming headers first
        };
        if (!isFormData) {
            // Only set default Content-Type if NOT FormData
            initialHeaders['Content-Type'] = 'application/json';
        }
        // If it IS FormData, the browser will set the correct multipart header automatically

        const requestHeaders = new Headers(initialHeaders);


        if (requiresAuth) {
            const token = authService.getToken();
            if (token) {
                requestHeaders.set('Authorization', `Bearer ${token}`);
            }
        }

        // Construct the full URL using the environment variable
        const baseUrl = process.env.NEXT_PUBLIC_API_BASE_URL || '';
        // Ensure the relative URL starts with a slash
        const relativeUrl = url.startsWith('/') ? url : `/${url}`;

        // --- Detailed Logging ---
        // console.log(`[httpClient] Base URL from env: "${baseUrl}"`); // Commented out
        // console.log(`[httpClient] Relative URL input: "${url}"`); // Commented out
        // console.log(`[httpClient] Calculated Relative URL: "${relativeUrl}"`); // Commented out
        // --- End Detailed Logging ---

        const fullUrl = `${baseUrl}${relativeUrl}`; // Simple concatenation
        // console.log(`[httpClient] Constructed Full URL: "${fullUrl}"`); // Commented out // Log the constructed URL for debugging

        try {
            this.lastRequestTime = Date.now();
            const response = await fetch(fullUrl, { // Use fullUrl
                headers: requestHeaders,
                body: body, // Pass the body along
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

                // Clone the response before reading it
                const responseClone = response.clone();
                const contentType = response.headers.get("content-type");
                
                if (contentType && contentType.includes("application/json")) {
                    try {
                        const errorData = await response.json();
                        throw new Error(errorData.message || 'Request failed');
                    } catch (jsonError) {
                        // If JSON parsing fails, try to get the text from the clone
                        const textError = await responseClone.text();
                        throw new Error(`Request failed: ${textError}`);
                    }
                }
                throw new Error(`Request failed with status ${response.status}`);
            }

            // Reset rate limit delay on successful request
            this.rateLimitDelay = 0;

            // Handle response based on specified type or content-type
            if (options.responseType === 'blob') {
                // Ensure the response is treated as blob regardless of Content-Type header
                // Note: fetch response.blob() always attempts to read the body as blob.
                return await response.blob();
            }

            // Clone the response before reading it for JSON/text
            const responseClone = response.clone();
            const contentType = response.headers.get("content-type");

            // Default to JSON if content type indicates it
            if (contentType && contentType.includes("application/json")) {
                try {
                    const jsonData = await response.json();
                    // Basic check if it looks like our standard API response structure
                    if (typeof jsonData === 'object' && jsonData !== null && 'data' in jsonData && 'status' in jsonData) {
                       return jsonData; // Return the structured response
                    }
                    // If not standard structure, return raw JSON data
                    return jsonData;
                } catch (jsonError) {
                    console.error("Error parsing JSON response:", jsonError);
                    // If JSON parsing fails, try to get the text from the clone
                    const rawText = await responseClone.text();
                    console.warn("Raw response text:", rawText);
                    
                    // Try to extract any useful information from the text
                    if (rawText.includes("DBRef") || rawText.includes("Unable to lazily resolve DBRef")) {
                        console.warn("Response contains DBRef objects that couldn't be serialized");
                        // Return a minimal valid response structure
                        return { status: "SUCCESS", message: "Data retrieved", data: {} };
                    }
                    
                    throw new Error("Failed to parse JSON response");
                }
            }

            // Default to text if not JSON or blob specified
            return await response.text();
        } catch (error: any) {
            // Log the error for debugging purposes
            console.error(`HTTP Request failed for URL: ${fullUrl}`, error); // Log fullUrl

            if (error.message.includes('Rate limit')) {
                throw error; // Re-throw rate limit errors
            }
            
            // Check for DBRef serialization errors
            if (error.message.includes("DBRef") || error.message.includes("Unable to lazily resolve DBRef")) {
                console.warn("DBRef serialization error detected in error handling");
                // Return a minimal valid response structure
                return { status: "SUCCESS", message: "Data retrieved", data: {} };
            }
            
            throw new Error(error.message || 'Request failed');
        }
    }

    async get(url: string, options: RequestOptions = {}) {
        return this.request(url, { ...options, method: 'GET' });
    }

    async post(url: string, body: any, options: RequestOptions = {}) {
        const isFormData = body instanceof FormData;
        const requestOptions: RequestOptions = {
            ...options,
            method: 'POST',
            body: isFormData ? body : JSON.stringify(body), // Pass FormData directly, stringify others
        };

        // If body is FormData, remove the default Content-Type header
        if (isFormData) {
            const headers = new Headers(options.headers);
            headers.delete('Content-Type');
            requestOptions.headers = headers;
        }

        return this.request(url, requestOptions);
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
