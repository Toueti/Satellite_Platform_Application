import { httpClient } from '@/utils/api/http-client';

// Define the structure for GEE parameters expected by the backend
// This should align with the 'parameters' object sent in the request body
export interface GeeParams {
    collection_id?: string; // e.g., 'COPERNICUS/S2_HARMONIZED'
    region: object; // GeoJSON geometry object
    start_date: string; // YYYY-MM-DD
    end_date: string;   // YYYY-MM-DD
    max_cloud_cover?: number; // percentage 0-100
    images_number?: number; // number of images to fetch
    bands?: string[]; // e.g., ['B4', 'B3', 'B2']
    visualization_params?: string | object; // JSON string or object for visualization
    dimensions?: string | object; // e.g., '768x768' or { width: 768, height: 768 }
    scale?: number; // resolution in meters
    crs?: string; // Coordinate Reference System, e.g., 'EPSG:4326'
    export_format?: string; // e.g., 'GeoTIFF'
    export_destination?: string; // e.g., 'Google Drive'
    metadata?: boolean; // Request metadata?
    // Add any other parameters the backend might accept
}

// Define the expected structure of the data within the successful response
interface GeeImageData {
    images: { previewUrl: string }[]; // Assuming previewUrl is in the first image
    // Add other expected fields from the 'data' object if needed
}

// Define the expected structure of the full successful API response
interface GeeApiResponse {
    status: string;
    message: string;
    data: GeeImageData;
    // Add other top-level fields if needed
}

// Parameters needed specifically for the fetch function call
// Includes auth token and potentially overrides for params stored elsewhere
export interface FetchGeeImageServiceParams {
    // Use Partial<GeeParams> as some might have defaults or come from context/hook state
    // The function will merge these with defaults. Region is mandatory here.
    params: Partial<GeeParams> & { region: object }; // Ensure region is present
    token: string; // Auth token
}

// Updated response type to reflect actual expected data
export interface GeeImageResult {
    previewUrl: string | null; // Can be null if no image found
    metadata: object | null; // Store the parameters used for this result
}

// Default GEE parameters (can be overridden)
const defaultGeeParams: Partial<GeeParams> = {
    collection_id: 'COPERNICUS/S2_HARMONIZED',
    start_date: '2021-05-15', // Updated default start date (less critical as it's overridden)
    end_date: '2021-07-15',   // Updated default end date (less critical as it's overridden)
    max_cloud_cover: 40, // Updated default
    images_number: 1,
    bands: ['B4', 'B3', 'B2'],
    visualization_params: { bands: ["B4", "B3", "B2"], min: 0, max: 3000 },
    dimensions: { width: 2048, height: 2048 }, // Updated default
    scale: 10, // Updated default
    crs: 'EPSG:4326',
    export_format: "GeoTIFF",
    export_destination: "Google Drive", // Usually not needed for preview
    metadata: true, // Request metadata
};

export const fetchGeeImage = async (serviceParams: FetchGeeImageServiceParams): Promise<GeeImageResult> => {
    const { params: inputParams, token } = serviceParams;
    const url = '/geospatial/gee/service'; // Use relative path if API_BASE_URL is set in httpClient

    // Ensure region is correctly extracted (it's mandatory in FetchGeeImageServiceParams)
    const geometry = inputParams.region; // Already an object due to type constraint

    // Merge input params with defaults, giving precedence to inputParams
    // Make sure region from input is used, not potentially undefined from defaults
    const mergedParams: Partial<GeeParams> = {
        ...defaultGeeParams,
        ...inputParams,
        region: geometry, // Explicitly set region from input
    };

    // Ensure visualization_params and dimensions are OBJECTS before sending
    // Parse them if they are provided as strings (e.g., from tweak modal)

    if (typeof mergedParams.visualization_params === 'string') {
        try {
            mergedParams.visualization_params = JSON.parse(mergedParams.visualization_params);
        } catch (e) {
            console.error("Error parsing visualization_params string:", e, "Using default object.");
            mergedParams.visualization_params = defaultGeeParams.visualization_params; // Fallback to default object
        }
    } else if (mergedParams.visualization_params === null || typeof mergedParams.visualization_params === 'undefined') {
        // Ensure it's at least the default object if not provided or explicitly null
        mergedParams.visualization_params = defaultGeeParams.visualization_params;
    }
    // If it's already an object, do nothing.

    if (typeof mergedParams.dimensions === 'string') {
        try {
            const parts = mergedParams.dimensions.split('x');
            if (parts.length === 2 && !isNaN(parseInt(parts[0])) && !isNaN(parseInt(parts[1]))) {
                mergedParams.dimensions = { width: parseInt(parts[0]), height: parseInt(parts[1]) };
            } else {
                throw new Error("Invalid dimensions string format. Expected 'widthxheight'.");
            }
        } catch (e) {
            console.error("Error parsing dimensions string:", e, "Using default object.");
            mergedParams.dimensions = defaultGeeParams.dimensions; // Fallback to default object
        }
    } else if (mergedParams.dimensions === null || typeof mergedParams.dimensions === 'undefined') {
         // Ensure it's at least the default object if not provided or explicitly null
        mergedParams.dimensions = defaultGeeParams.dimensions;
    }
     // If it's already an object, do nothing.

    // --- Adjust region structure ---
    // Check if the region is a GeoJSON Feature and extract the geometry if the backend expects only geometry
    // Add type assertion for safety, assuming region is an object with potential 'type' and 'geometry'
    const regionObject = mergedParams.region as any;
    if (regionObject && regionObject.type === 'Feature' && regionObject.geometry) {
        console.log("Region is a Feature, extracting geometry for backend request.");
        mergedParams.region = regionObject.geometry; // Replace the Feature with its geometry
    }
    // --- End region adjustment ---

    const body = {
        serviceType: 'get_images',
        parameters: mergedParams, // Send the potentially adjusted parameters
    };

    console.log("Sending GEE request with body:", JSON.stringify(body, null, 2));

    try {
        // Assuming httpClient handles the Authorization header if token is passed in options
        // And assuming httpClient returns the full response structure { status, message, data }
        const response: GeeApiResponse = await httpClient.post(url, body, {
            requiresAuth: true // Let httpClient handle the token
            // If httpClient doesn't handle auth automatically, add headers:
            // headers: { 'Authorization': Bearer ${token} }
        });

        console.log("Received GEE response:", response);

        // Extract the preview URL from the nested structure
        // Check if data and images exist and images array is not empty
        const previewUrl = response?.data?.images?.[0]?.previewUrl || null;

        if (!previewUrl) {
            console.warn("No preview URL found in GEE response.");
            // Decide how to handle this - throw error or return null?
            // Returning null for now, let the hook handle displaying feedback.
        }

        // Return the parameters that were *actually sent* in the metadata field
        const metadata = body.parameters;

        return { previewUrl, metadata };
    } catch (error: any) {
        console.error('Error in fetchGeeImage service:', error);
        // Try to extract a more specific message from the error if possible
        const errorMessage = error?.errorDetails?.message || error?.message || 'Unknown error fetching GEE image';
        throw new Error(`Failed to fetch GEE image: ${errorMessage}`);
    }
};
