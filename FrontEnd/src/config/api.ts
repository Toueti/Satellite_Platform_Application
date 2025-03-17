// API Configuration
export const API_BASE_URL = 'http://localhost:8081';

// Auth endpoints
export const AUTH_ENDPOINTS = {
    LOGIN: `${API_BASE_URL}/api/auth/signin`,
    REGISTER: `${API_BASE_URL}/api/auth/signup`,
    RESET_PASSWORD: `${API_BASE_URL}/api/auth/reset-password`,
};

// Project endpoints
export const PROJECT_ENDPOINTS = {
    LIST: `${API_BASE_URL}/api/projects`,
    CREATE: `${API_BASE_URL}/api/projects`,
    GET: (id: string) => `${API_BASE_URL}/api/projects/${id}`,
    UPDATE: (id: string) => `${API_BASE_URL}/api/projects/${id}`,
    DELETE: (id: string) => `${API_BASE_URL}/api/projects/${id}`,
};

// Resource endpoints
export const RESOURCE_ENDPOINTS = {
    IMAGES: {
        LIST: `${API_BASE_URL}/api/images`,
        UPLOAD: `${API_BASE_URL}/api/images/upload`,
        GET: (id: string) => `${API_BASE_URL}/api/images/${id}`,
    },
    SATELLITES: {
        LIST: `${API_BASE_URL}/api/satellites`,
        GET: (id: string) => `${API_BASE_URL}/api/satellites/${id}`,
    },
    GEE: {
        SEARCH: `${API_BASE_URL}/api/gee/search`,
        PROCESS: `${API_BASE_URL}/api/gee/process`,
    },
};

// Storage endpoints
export const STORAGE_ENDPOINTS = {
    UPLOAD: `${API_BASE_URL}/api/storage/upload`,
    DOWNLOAD: (filename: string) => `${API_BASE_URL}/api/storage/files/${filename}`,
};
