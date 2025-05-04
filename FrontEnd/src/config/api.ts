// API Configuration
// Base URL is handled by httpClient using NEXT_PUBLIC_API_BASE_URL
// Ensure all endpoint definitions below use RELATIVE paths starting with '/'
// export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080'; // Removed

// Auth endpoints
export const AUTH_ENDPOINTS = {
  LOGIN: '/api/auth/signin',
  REGISTER: '/api/auth/signup',
  RESET_PASSWORD: '/api/auth/reset-password',
};

// Project endpoints
export const PROJECT_ENDPOINTS = {
    LIST: '/api/thematician/projects/all',
    CREATE: '/api/thematician/projects/create',
    GET: (id: string) => `/api/thematician/projects/${id}`,
    UPDATE: (id: string) => `/api/thematician/projects/${id}`,
    DELETE: (id: string) => `/api/thematician/projects/${id}`,
    SHARE: '/api/thematician/projects/share',
    UNSHARE: '/api/thematician/projects/unshare',
    SHARED_USERS: (id: string) => `/api/thematician/projects/${id}/shared-users`,
    ARCHIVE: (id: string) => `/api/thematician/projects/${id}/archive`,
    UNARCHIVE: (id: string) => `/api/thematician/projects/${id}/unarchive`,
    GET_IMAGES: (id: string) => `/api/thematician/projects/${id}/images`,
    ADD_IMAGE: (projectId: string, imageId: string) => `/api/thematician/projects/${projectId}/images/${imageId}`,
    REMOVE_IMAGE: (projectId: string, imageId: string) => `/api/thematician/projects/${projectId}/images/${imageId}`,
    GET_AI_MODEL: (id: string) => `/api/thematician/projects/${id}/ai-model`,
    ADD_TAG: (projectId: string, tag: string) => `/api/thematician/projects/${projectId}/tags/${tag}`,
    REMOVE_TAG: (projectId: string, tag: string) => `/api/thematician/projects/${projectId}/tags/${tag}`,
};

// Resource endpoints
export const RESOURCE_ENDPOINTS = {
    IMAGES: {
        LIST: '/api/images',
        UPLOAD: '/geospatial/images/add', // Used by images.service
        GET: (id: string) => `/api/images/${id}`,
        ANNOTATIONS: {
            ADD: (imageId: string) => `/api/images/${imageId}/annotations`,
            UPDATE: (imageId: string, annotationId: string) => `/api/images/${imageId}/annotations/${annotationId}`,
            DELETE: (imageId: string, annotationId: string) => `/api/images/${imageId}/annotations/${annotationId}`,
            LIST: (imageId: string) => `/api/images/${imageId}/annotations`
        },
        ANALYSIS: {
            GET: (imageId: string) => `/api/images/${imageId}/analysis`,
            GET_BY_PROJECT: (projectId: string) => `/api/projects/${projectId}/analysis`
        }
    },
    SATELLITES: {
        LIST: '/api/satellites',
        GET: (id: string) => `/api/satellites/${id}`,
    },
    GEE: {
        SEARCH: '/api/gee/search',
        PROCESS: '/api/gee/process', // Note: GEE service uses '/geospatial/gee/service' - check consistency
    },
};

// Storage endpoints
export const STORAGE_ENDPOINTS = {
    UPLOAD: '/api/storage/upload',
    DOWNLOAD: (filename: string) => `/api/storage/files/${filename}`,
    USAGE: '/admin/storage/usage',
};
