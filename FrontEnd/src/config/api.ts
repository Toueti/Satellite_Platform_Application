// API Configuration
export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080';

// Auth endpoints
export const AUTH_ENDPOINTS = {
  LOGIN: `${API_BASE_URL}/api/auth/signin`,
  REGISTER: `${API_BASE_URL}/api/auth/signup`,
  RESET_PASSWORD: `${API_BASE_URL}/api/auth/reset-password`,
};

// Project endpoints
export const PROJECT_ENDPOINTS = {
    LIST: `${API_BASE_URL}/api/thematician/projects/all`,
    CREATE: `${API_BASE_URL}/api/thematician/projects/create`,
    GET: (id: string) => `${API_BASE_URL}/api/thematician/projects/${id}`,
    UPDATE: (id: string) => `${API_BASE_URL}/api/thematician/projects/${id}`,
    DELETE: (id: string) => `${API_BASE_URL}/api/thematician/projects/${id}`,
    SHARE: `${API_BASE_URL}/api/thematician/projects/share`,
    UNSHARE: `${API_BASE_URL}/api/thematician/projects/unshare`,
    SHARED_USERS: (id: string) => `${API_BASE_URL}/api/thematician/projects/${id}/shared-users`,
    ARCHIVE: (id: string) => `${API_BASE_URL}/api/thematician/projects/${id}/archive`,
    UNARCHIVE: (id: string) => `${API_BASE_URL}/api/thematician/projects/${id}/unarchive`,
    GET_IMAGES: (id: string) => `${API_BASE_URL}/api/thematician/projects/${id}/images`,
    ADD_IMAGE: (projectId: string, imageId: string) => `${API_BASE_URL}/api/thematician/projects/${projectId}/images/${imageId}`,
    REMOVE_IMAGE: (projectId: string, imageId: string) => `${API_BASE_URL}/api/thematician/projects/${projectId}/images/${imageId}`,
    GET_AI_MODEL: (id: string) => `${API_BASE_URL}/api/thematician/projects/${id}/ai-model`,
    ADD_TAG: (projectId: string, tag: string) => `${API_BASE_URL}/api/thematician/projects/${projectId}/tags/${tag}`,
    REMOVE_TAG: (projectId: string, tag: string) => `${API_BASE_URL}/api/thematician/projects/${projectId}/tags/${tag}`,
};

// Resource endpoints
export const RESOURCE_ENDPOINTS = {
    IMAGES: {
        LIST: `${API_BASE_URL}/api/images`,
        UPLOAD: `${API_BASE_URL}/api/images/upload`,
        GET: (id: string) => `${API_BASE_URL}/api/images/${id}`,
        ANNOTATIONS: {
            ADD: (imageId: string) => `${API_BASE_URL}/api/images/${imageId}/annotations`,
            UPDATE: (imageId: string, annotationId: string) => `${API_BASE_URL}/api/images/${imageId}/annotations/${annotationId}`,
            DELETE: (imageId: string, annotationId: string) => `${API_BASE_URL}/api/images/${imageId}/annotations/${annotationId}`,
            LIST: (imageId: string) => `${API_BASE_URL}/api/images/${imageId}/annotations`
        },
        ANALYSIS: {
            GET: (imageId: string) => `${API_BASE_URL}/api/images/${imageId}/analysis`,
            GET_BY_PROJECT: (projectId: string) => `${API_BASE_URL}/api/projects/${projectId}/analysis`
        }
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
    USAGE: `${API_BASE_URL}/admin/storage/usage`,
};
