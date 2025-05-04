import axios from 'axios';
import { authService } from './auth.service';

const VEGETATION_API_BASE_URL = 'http://localhost:8080/api/v1/vegetation-indices';
const PROCESSING_API_BASE_URL = 'http://localhost:8080/geospatial/processing';

export interface AnalysisResult {
    id?: string; // Added optional id field
    index_type: string;
    start_time: string;
    end_time: string;
    processing_duration: number;
    statistics: {
        min?: number; // Make optional
        max?: number; // Make optional
        mean?: number; // Make optional (already was effectively, but good practice)
        median?: number; // Make optional
        std?: number; // Make optional
    };
    processed_image: string;
    projectId: string; // Added projectId property
    imageId: string;   // Added imageId property
}

export interface ApiResponse<T> {
    timestamp: string;
    status: string;
    message: string;
    data: T;
}

interface CalculateNdviParams {
    file: Blob;
    filename: string;
    redBand: number;
    nirBand: number;
    projectId: string;
    imageId: string;
    description?: string;
}

interface CalculateEviParams extends CalculateNdviParams {
    blueBand: number;
}

const handleApiError = (error: any, operation: string): never => {
    console.error(`Error during ${operation}:`, error);
    if (axios.isAxiosError(error)) {
        if (error.response) {
            console.error('Error response data:', error.response.data);
            console.error('Error response status:', error.response.status);
            console.error('Error response headers:', error.response.headers);
            const message = error.response.data?.message || error.response.statusText || 'Server error';
            throw new Error(`Failed to ${operation}: ${message} (Status: ${error.response.status})`);
        }
    }
    throw new Error(`Failed to ${operation}: ${error.message || 'Unknown error'}`);
};

const calculateNDVI = async (params: CalculateNdviParams): Promise<AnalysisResult> => {
    const token = authService.getToken();
    if (!token) {
        throw new Error('No authentication token found');
    }

    const formData = new FormData();
    
    const metadata = {
        red_band: params.redBand,
        nir_band: params.nirBand
    };
    formData.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }));
    formData.append('file', params.file, params.filename);

    try {
        const response = await axios.post<ApiResponse<AnalysisResult>>(
            `${VEGETATION_API_BASE_URL}/ndvi`,
            formData,
            {
                headers: {
                    'Authorization': `Bearer ${token}`,
                }
            }
        );

        if (response.data.status !== 'SUCCESS' || !response.data.data) {
            throw new Error(response.data.message || 'Failed to calculate NDVI');
        }

        return response.data.data;
    } catch (error) {
        return handleApiError(error, 'calculate NDVI');
    }
};

const calculateEVI = async (params: CalculateEviParams): Promise<AnalysisResult> => {
    const token = authService.getToken();
    if (!token) {
        throw new Error('No authentication token found');
    }

    const formData = new FormData();
    
    const metadata = {
        red_band: params.redBand,
        nir_band: params.nirBand,
        blue_band: params.blueBand
    };
    formData.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }));
    formData.append('file', params.file, params.filename);

    try {
        const response = await axios.post<ApiResponse<AnalysisResult>>(
            `${VEGETATION_API_BASE_URL}/evi`,
            formData,
            {
                headers: {
                    'Authorization': `Bearer ${token}`,
                }
            }
        );

        if (response.data.status !== 'SUCCESS' || !response.data.data) {
            throw new Error(response.data.message || 'Failed to calculate EVI');
        }

        return response.data.data;
    } catch (error) {
        return handleApiError(error, 'calculate EVI');
    }
};

const getAnalysisResults = async (projectId: string): Promise<AnalysisResult[]> => {
    const token = authService.getToken();
    if (!token) {
        throw new Error('No authentication token found');
    }

    try {
        const response = await axios.get(`${PROCESSING_API_BASE_URL}/project/${projectId}`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        // Check if the actual results array is nested within response.data.data
        const backendResponseData = response.data; // The object { timestamp, status, message, data }
        const backendResultsArray = backendResponseData?.data; // Access the nested 'data' array

        // Ensure backendResultsArray is actually an array before mapping
        if (!Array.isArray(backendResultsArray)) {
             console.warn(`Expected 'data' property to be an array in the response, but received:`, backendResultsArray);
             // If it's not an array (and not a 404 handled below), return empty or throw
             // We'll rely on the catch block to handle actual errors like 404
             return [];
        }

        // Map backend structure to frontend AnalysisResult interface
        const mappedResults: AnalysisResult[] = backendResultsArray.map((backendResult: any) => {
            // Note: backendResult.id is missing from this endpoint's response.
            // The 'id' field will be undefined for items fetched via this list function.
            // Backend needs to be updated to include 'id' if delete/edit from list is needed.
            return {
                id: backendResult.id, // Will likely be undefined from this endpoint
                projectId: projectId, // Use the projectId passed to the function
                imageId: backendResult.imageId, // Correct: imageId is top-level
                index_type: backendResult.data?.indexType || 'N/A', // Correct: indexType is nested
                // Use 'date' field from backend for both start/end time for display consistency
                start_time: backendResult.date || new Date().toISOString(), // Correct: date is top-level
                end_time: backendResult.date || new Date().toISOString(), // Correct: date is top-level
                processing_duration: (backendResult.data?.processingTimeMs || 0) / 1000, // Correct: processingTimeMs is nested
                statistics: {
                    // These values are not provided by the list endpoint, set to undefined or null
                    min: backendResult.data?.minValue, // Map if available, else undefined
                    max: backendResult.data?.maxValue, // Map if available, else undefined
                    mean: backendResult.data?.meanValue, // Correct: meanValue is nested
                    median: backendResult.data?.medianValue, // Map if available, else undefined
                    std: backendResult.data?.stdDevValue, // Map if available, else undefined (adjust name if needed)
                },
                // processed_image is NOT available in the list endpoint based on backend example
                processed_image: '', // Explicitly set to empty string
            };
        });

        return mappedResults;
    } catch (error) {
        // Check specifically for 404
        if (axios.isAxiosError(error) && error.response?.status === 404) {
            console.warn(`No analysis results found for project ${projectId} (404). Returning empty array.`);
            return []; // Return empty array for 404
        }
        // For all other errors, use the existing handler
        return handleApiError(error, 'fetch analysis results');
    }
};

const getLatestAnalysisResult = async (projectId: string, imageId: string): Promise<AnalysisResult> => {
    const token = authService.getToken();
    if (!token) {
        throw new Error('No authentication token found');
    }

    try {
        const response = await axios.get(
            `${PROCESSING_API_BASE_URL}/latest/${projectId}/${imageId}`,
            {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            }
        );
        return response.data;
    } catch (error) {
        return handleApiError(error, 'fetch latest analysis result');
    }
};

const saveAnalysisResult = async (projectId: string, result: AnalysisResult): Promise<void> => {
    const token = authService.getToken();
    if (!token) {
        throw new Error('No authentication token found');
    }

    try {
        const formData = new FormData();
        
        // Add metadata
        formData.append('metadata', new Blob([JSON.stringify({
            projectId,
            imageId: result.imageId, // Added imageId here
            data: {
                indexType: result.index_type,
                meanValue: result.statistics.mean,
                processingTimeMs: result.processing_duration * 1000
            },
            date: result.end_time,
            type: 'VEGETATION_INDEX',
            status: 'COMPLETED'
        })], { type: 'application/json' }));

        // Convert base64 image to blob and add to form
        const imageData = result.processed_image;
        const imageBlob = await fetch(`data:image/png;base64,${imageData}`).then(r => r.blob());
        formData.append('file', imageBlob, 'result.tif');

        await axios.post(`${PROCESSING_API_BASE_URL}/save`, formData, {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'multipart/form-data'
            }
        });
    } catch (error) {
        handleApiError(error, 'save analysis result');
    }
};

// Interface for the data structure expected by the update endpoint
export interface UpdateAnalysisData {
    imageId: string;
    data: {
        indexType: string;
        meanValue?: number;
        minValue?: number;
        maxValue?: number;
        processingTimeMs?: number;
        notes?: string; // Added notes field
    };
    date: string;
    type: string; // e.g., 'VEGETATION_INDEX'
    status: string; // e.g., 'COMPLETED'
}


const deleteAnalysisResult = async (imageId: string, resultId: string): Promise<ApiResponse<null>> => {
    const token = authService.getToken();
    if (!token) {
        throw new Error('No authentication token found');
    }

    try {
        const response = await axios.delete<ApiResponse<null>>(
            `${PROCESSING_API_BASE_URL}/image/${imageId}/${resultId}`,
            {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            }
        );
        if (response.data.status !== 'SUCCESS') {
            throw new Error(response.data.message || 'Failed to delete analysis result');
        }
        return response.data; // Return the full API response
    } catch (error) {
        return handleApiError(error, 'delete analysis result');
    }
};

const updateAnalysisResult = async (resultId: string, updateData: UpdateAnalysisData): Promise<ApiResponse<AnalysisResult>> => {
    const token = authService.getToken();
    if (!token) {
        throw new Error('No authentication token found');
    }

    try {
        const response = await axios.put<ApiResponse<AnalysisResult>>(
            `${PROCESSING_API_BASE_URL}/${resultId}`,
            updateData,
            {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            }
        );
        if (response.data.status !== 'SUCCESS' || !response.data.data) {
            throw new Error(response.data.message || 'Failed to update analysis result');
        }
        // Map the updated result back to AnalysisResult if necessary, or assume backend returns compatible structure
        // For now, assume the returned data structure matches AnalysisResult or is close enough
        return response.data; // Return the full API response
    } catch (error) {
        return handleApiError(error, 'update analysis result');
    }
};


export const analysisService = {
    calculateNDVI,
    calculateEVI,
    getAnalysisResults,
    getLatestAnalysisResult,
    saveAnalysisResult,
    deleteAnalysisResult, // Added delete function
    updateAnalysisResult, // Added update function

    // Function to get the result image file as a blob
    async getAnalysisResultFile(resultId: string): Promise<Blob> {
        const token = authService.getToken();
        if (!token) {
            throw new Error('No authentication token found');
        }
        if (!resultId) {
            throw new Error('Result ID is required to fetch the file');
        }

        try {
            // Use axios directly for blob response
            const response = await axios.get(
                 `${PROCESSING_API_BASE_URL}/${resultId}/file`,
                 {
                     headers: { 'Authorization': `Bearer ${token}` },
                     responseType: 'blob', // Important: expect a blob response
                 }
            );
            // Check if the response is actually a blob
            if (!(response.data instanceof Blob)) {
                throw new Error('Received unexpected data format instead of a file blob.');
            }
            return response.data;
        } catch (error) {
            // Use handleApiError, but it might need adjustment if error response for blob is different
            return handleApiError(error, `fetch analysis result file for ID ${resultId}`);
        }
    }
};
