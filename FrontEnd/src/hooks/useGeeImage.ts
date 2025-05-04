import { useState, useEffect, useCallback } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import {
    fetchGeeImage,
    GeeImageResult,
    GeeParams, // Import GeeParams
    FetchGeeImageServiceParams // Import service params type
} from '@/services/gee.service';
import { authService } from '@/services/auth.service';

// Define the hook's return type
interface UseGeeImageReturn {
    geeImageUrl: string | null;
    geeImageMetadata: object | null; // Consider making this GeeParams | null for better typing
    lastUsedParams: Partial<GeeParams> | null; // Store params used for the current image
    isFetchingGeeImage: boolean;
    geeFetchError: string | null;
    fetchAndDisplayGeeImage: (params: Partial<GeeParams> & { region: object }) => Promise<void>; // Updated signature
    refetchWithTweakedParams: (tweakedParams: Partial<GeeParams>) => Promise<void>; // New function
    clearGeeImage: () => void; // Function to clear the image and error
}


export function useGeeImage(projectId: string | undefined | null): UseGeeImageReturn {
    const router = useRouter();
    const pathname = usePathname();
    const [geeImageUrl, setGeeImageUrl] = useState<string | null>(null);
    const [geeImageMetadata, setGeeImageMetadata] = useState<object | null>(null); // Could be more specific
    const [lastUsedParams, setLastUsedParams] = useState<Partial<GeeParams> | null>(null); // Store params
    const [isFetchingGeeImage, setIsFetchingGeeImage] = useState(false);
    const [geeFetchError, setGeeFetchError] = useState<string | null>(null);

    // Function to fetch GEE image based on parameters
    const fetchAndDisplayGeeImage = useCallback(async (
        // Expects parameters including the region object
        paramsToFetch: Partial<GeeParams> & { region: object }
    ) => {
        setIsFetchingGeeImage(true);
        setGeeFetchError(null);
        setGeeImageUrl(null);
        setGeeImageMetadata(null);
        // Don't clear lastUsedParams here, only on success
        console.log('Fetching GEE image with params:', paramsToFetch);

        try {
            const token = authService.getToken();
            if (!token) {
                throw new Error("Authentication token not found.");
            }

            // Prepare params for the service call
            const serviceCallParams: FetchGeeImageServiceParams = {
                params: paramsToFetch,
                token: token,
            };

            // Call service with the full parameter object
            const response: GeeImageResult = await fetchGeeImage(serviceCallParams);

            if (response.previewUrl) {
                setGeeImageUrl(response.previewUrl);
                // Store the actual parameters used for this successful fetch
                // The service merges defaults, so response.metadata might hold the final params?
                // For now, store the input params + region. Adjust if service returns used params.
                setLastUsedParams(paramsToFetch);
                setGeeImageMetadata(response.metadata); // Store whatever metadata the service returns
                console.log('GEE Image fetched successfully:', response.previewUrl);
            } else {
                // Handle case where fetch was successful but no image URL was returned
                setLastUsedParams(null); // Clear params if fetch didn't yield an image
                console.warn('GEE fetch successful, but no image preview URL found.');
                setGeeFetchError('No suitable image found for the selected criteria.');
            }

        } catch (error: any) {
            console.error("Error fetching GEE image in hook:", error);
            setLastUsedParams(null); // Clear params on error
            // Use the specific error message thrown by the service
            setGeeFetchError(error.message || 'Failed to fetch GEE image');
        } finally {
            setIsFetchingGeeImage(false);
        }
    }, []); // Dependencies: authService and fetchGeeImage are stable imports

    // Function to refetch using last used region but with tweaked parameters
    const refetchWithTweakedParams = useCallback(async (tweakedParams: Partial<GeeParams>) => {
        if (!lastUsedParams || !lastUsedParams.region) {
            setGeeFetchError("Cannot refetch: Previous parameters (including region) not available.");
            return;
        }

        // Merge tweaked params with the last used params, ensuring region is preserved
        const newParams: Partial<GeeParams> & { region: object } = {
            ...lastUsedParams, // Start with last used params
            ...tweakedParams, // Override with tweaked values
            region: lastUsedParams.region, // Ensure region is explicitly kept
        };

        // Call the main fetch function with the new combined parameters
        await fetchAndDisplayGeeImage(newParams);

    }, [lastUsedParams, fetchAndDisplayGeeImage]);

    // Effect to check for region and date parameters in URL on mount or projectId change
    useEffect(() => {
        // Ensure this runs only client-side and projectId is available
        if (typeof window === 'undefined' || !projectId) return;

        const searchParams = new URLSearchParams(window.location.search);
        const regionParam = searchParams.get('region');
        const startDateParam = searchParams.get('startDate');
        const endDateParam = searchParams.get('endDate');
        const advancedParamsParam = searchParams.get('advancedParams'); // Get advanced params string

        // Only fetch if all required core parameters are present
        if (regionParam && startDateParam && endDateParam) {
            let shouldClearParams = true; // Flag to control clearing params
            try {
                const parsedRegion = JSON.parse(decodeURIComponent(regionParam));
                let parsedAdvancedParams: Partial<GeeParams> = {};

                // Parse advanced parameters if present
                if (advancedParamsParam) {
                    try {
                        parsedAdvancedParams = JSON.parse(decodeURIComponent(advancedParamsParam));
                        console.log("Parsed Advanced Params from URL:", parsedAdvancedParams);
                    } catch (advErr) {
                        console.error("Failed to parse advancedParams from URL:", advErr);
                        // Decide how to handle: ignore, show error, use defaults?
                        // Ignoring for now, defaults will be applied by the service.
                    }
                }

                // Construct initial params object for fetch, merging core and advanced params
                const initialFetchParams: Partial<GeeParams> & { region: object } = {
                    region: parsedRegion,
                    start_date: startDateParam,
                    end_date: endDateParam,
                    ...parsedAdvancedParams, // Spread the parsed advanced params
                };

                // Trigger fetch with the constructed params object
                fetchAndDisplayGeeImage(initialFetchParams);

            } catch (e) {
                console.error("Failed to parse region from URL or fetch GEE image:", e);
                setGeeFetchError("Failed to process parameters from URL.");
                // Don't clear params if parsing failed, user might want to see them
                shouldClearParams = false;
            } finally {
                 // Clear the query params after attempting fetch (if successful parse)
                 // Use replace to avoid adding to browser history
                 if (shouldClearParams) {
                    console.log("Clearing GEE query parameters from URL");
                    router.replace(pathname, { scroll: false }); // Use pathname and prevent scroll jump
                 }
            }
        }
    // Only re-run if projectId changes. fetchAndDisplayGeeImage is stable due to useCallback.
    // pathname is included in case the component somehow persists across different paths (unlikely here).
    }, [projectId, fetchAndDisplayGeeImage, router, pathname]);

    // Function to clear the GEE image state
    const clearGeeImage = useCallback(() => {
        setGeeImageUrl(null);
        setGeeImageMetadata(null);
        setLastUsedParams(null);
        setGeeFetchError(null);
        // Optionally clear URL params again if they somehow persisted
        // router.replace(pathname, { scroll: false });
    }, [/*pathname, router*/]); // router/pathname likely stable, but include if needed

    return {
        geeImageUrl,
        geeImageMetadata,
        lastUsedParams, // Expose last used params
        isFetchingGeeImage,
        geeFetchError,
        fetchAndDisplayGeeImage, // Expose if needed externally
        refetchWithTweakedParams, // Expose the new refetch function
        clearGeeImage, // Expose clear function
    };
}
