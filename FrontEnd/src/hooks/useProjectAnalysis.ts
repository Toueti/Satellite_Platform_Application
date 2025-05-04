import { useState, useEffect, useCallback } from 'react';
import { analysisService, AnalysisResult, UpdateAnalysisData } from '@/services/analysis.service'; // Import UpdateAnalysisData

export function useProjectAnalysis(projectId: string | undefined | null, isActiveTab: boolean) {
    const [analysisResults, setAnalysisResults] = useState<AnalysisResult[]>([]);
    const [isLoadingAnalysis, setIsLoadingAnalysis] = useState(false);
    const [analysisError, setAnalysisError] = useState<string | null>(null);

    // State for handling the single latest result (e.g., from sessionStorage after redirect)
    const [latestAnalysisResult, setLatestAnalysisResult] = useState<AnalysisResult | null>(null);
    const [isLoadingLatestResult, setIsLoadingLatestResult] = useState(false);
    const [latestResultError, setLatestResultError] = useState<string | null>(null);

    // State for saving the latest result
    const [isSavingAnalysis, setIsSavingAnalysis] = useState(false);
    const [saveAnalysisError, setSaveAnalysisError] = useState<string | null>(null);
    const [saveAnalysisSuccess, setSaveAnalysisSuccess] = useState<string | null>(null);

    // State for deleting a result
    const [isDeletingAnalysis, setIsDeletingAnalysis] = useState<string | null>(null); // Store ID being deleted
    const [deleteAnalysisError, setDeleteAnalysisError] = useState<string | null>(null);
    const [deleteAnalysisSuccess, setDeleteAnalysisSuccess] = useState<string | null>(null);

    // State for updating a result
    const [editingResult, setEditingResult] = useState<AnalysisResult | null>(null); // Store the full result being edited
    const [isUpdatingAnalysis, setIsUpdatingAnalysis] = useState(false); // Changed to boolean
    const [updateAnalysisError, setUpdateAnalysisError] = useState<string | null>(null);
    const [updateAnalysisSuccess, setUpdateAnalysisSuccess] = useState<string | null>(null);


    // Function to fetch all analysis results for the project
    const fetchAnalysisResults = useCallback(async () => {
        if (!projectId) {
            setAnalysisResults([]);
            return;
        };

        setIsLoadingAnalysis(true);
        setAnalysisError(null);
        setSaveAnalysisError(null);
        setSaveAnalysisSuccess(null);
        setDeleteAnalysisError(null); // Clear delete errors on refetch
        setDeleteAnalysisSuccess(null); // Clear delete success on refetch
        setUpdateAnalysisError(null); // Clear update errors on refetch
        setUpdateAnalysisSuccess(null); // Clear update success on refetch
        try {
            const results = await analysisService.getAnalysisResults(projectId);
            // console.log('[useProjectAnalysis] Raw results from service:', JSON.stringify(results, null, 2)); // REMOVED Log
            setAnalysisResults(results || []); // Ensure results is an array
        } catch (error: any) {
            console.error('Error fetching analysis results:', error); // Keep this error log
            setAnalysisError(error.message || 'Failed to fetch analysis results');
            setAnalysisResults([]); // Clear results on error
        } finally {
            setIsLoadingAnalysis(false);
        }
    }, [projectId]);

    // Effect to fetch analysis results when the tab is active and projectId is valid
    useEffect(() => {
        if (isActiveTab && projectId) {
            fetchAnalysisResults();
        }
        // Clear results if tab becomes inactive or projectId is lost
        if (!isActiveTab || !projectId) {
            setAnalysisResults([]);
            setAnalysisError(null); // Clear error when not active
        }
    }, [projectId, isActiveTab, fetchAnalysisResults]);

    // Effect to load the latest result from sessionStorage on mount
    useEffect(() => {
        const storedResult = sessionStorage.getItem('latestAnalysisResult');
        if (storedResult) {
            try {
                const parsedResult: AnalysisResult = JSON.parse(storedResult);
                sessionStorage.removeItem('latestAnalysisResult'); // Remove after reading

                // Check if it's a simplified result needing a full refetch
                const isSimplifiedResult = (result: any) =>
                    result.status === 'success' && result.message?.includes('too large');

                if (isSimplifiedResult(parsedResult) && parsedResult.projectId && parsedResult.imageId) {
                    setIsLoadingLatestResult(true);
                    setLatestResultError(null);

                    analysisService.getLatestAnalysisResult(parsedResult.projectId, parsedResult.imageId)
                        .then(fullResult => {
                            if (fullResult) {
                                setLatestAnalysisResult(fullResult);
                                // Optionally trigger fetching all results if needed here
                                // fetchAnalysisResults();
                            } else {
                                throw new Error("Received empty analysis result on refetch.");
                            }
                        })
                        .catch(error => {
                            console.error("Failed to fetch full analysis result:", error);
                            setLatestResultError(
                                "Failed to load the complete analysis result. Please try running the analysis again."
                            );
                        })
                        .finally(() => {
                            setIsLoadingLatestResult(false);
                        });
                } else if (parsedResult.projectId === projectId) { // Ensure it belongs to the current project
                    setLatestAnalysisResult(parsedResult);
                    // Optionally trigger fetching all results if needed here
                    // fetchAnalysisResults();
                } else {
                     console.warn("Loaded analysis result from sessionStorage does not match current project ID.");
                }

            } catch (e) {
                console.error("Failed to parse analysis result from sessionStorage", e);
                sessionStorage.removeItem('latestAnalysisResult'); // Clear corrupted data
                setLatestResultError("Failed to load analysis result from previous session. The data may be corrupted.");
            }
        }
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [projectId]); // Run only when projectId is established

    // Function to save the currently displayed 'latestAnalysisResult'
    const handleSaveAnalysis = async () => {
        if (!latestAnalysisResult || !projectId) {
            setSaveAnalysisError("No analysis result available to save or project ID is missing.");
            return;
        }

        setIsSavingAnalysis(true);
        setSaveAnalysisError(null);
        setSaveAnalysisSuccess(null);

        try {
            // Ensure the result object has necessary identifiers if required by the backend
            const resultToSave = {
                ...latestAnalysisResult,
                projectId: projectId, // Ensure projectId is included
                imageId: latestAnalysisResult.imageId // Ensure imageId is included if needed
            };
            await analysisService.saveAnalysisResult(projectId, resultToSave);
            setSaveAnalysisSuccess("Analysis result saved successfully! Refreshing list...");

            // Refetch the list of all analysis results to include the newly saved one
            await fetchAnalysisResults();

            // Optionally clear the temporary latest result display after saving and refetching
            // setLatestAnalysisResult(null);
        } catch (error: any) {
            console.error("Error saving analysis result:", error);
            setSaveAnalysisError(error.message || "Failed to save analysis result. Please try again.");
        } finally {
            setIsSavingAnalysis(false);
        }
    };

    // Function to delete an analysis result
    const handleDeleteAnalysis = async (imageId: string, resultId: string) => {
        if (!imageId || !resultId) {
            setDeleteAnalysisError("Missing image ID or result ID for deletion.");
            return;
        }

        setIsDeletingAnalysis(resultId); // Set which ID is being deleted
        setDeleteAnalysisError(null);
        setDeleteAnalysisSuccess(null);

        try {
            await analysisService.deleteAnalysisResult(imageId, resultId);
            setDeleteAnalysisSuccess(`Result ${resultId} deleted successfully. Refreshing list...`);
            await fetchAnalysisResults(); // Refetch results after deletion
        } catch (error: any) {
            console.error("Error deleting analysis result:", error);
            setDeleteAnalysisError(error.message || "Failed to delete analysis result.");
        } finally {
            setIsDeletingAnalysis(null); // Clear deleting state
        }
    };

    // Function to initiate editing
    const startEditingAnalysis = (result: AnalysisResult) => {
        setEditingResult(result);
        setUpdateAnalysisError(null); // Clear previous errors
        setUpdateAnalysisSuccess(null);
    };

    // Function to cancel editing
    const cancelEditingAnalysis = () => {
        setEditingResult(null);
        setUpdateAnalysisError(null);
        setUpdateAnalysisSuccess(null);
    };

    // Function to perform the update API call - expects the *modified* data part
    const handleUpdateAnalysis = async (modifiedData: Partial<UpdateAnalysisData['data']>) => {
        if (!editingResult || !editingResult.id || !editingResult.imageId) {
            setUpdateAnalysisError("No result selected for editing or critical IDs are missing.");
            return;
        }
        if (!modifiedData) {
             setUpdateAnalysisError("Missing update data.");
             return;
        }

        setIsUpdatingAnalysis(true);
        setUpdateAnalysisError(null);
        setUpdateAnalysisSuccess(null);

        try {
            // Construct the full payload required by the backend PUT endpoint
            // We only allow editing 'notes' for now, but need to send the full structure
            const fullUpdateData: UpdateAnalysisData = {
                imageId: editingResult.imageId, // Required by backend
                data: {
                    // Include existing data, potentially overridden by modifiedData
                    indexType: editingResult.index_type,
                    meanValue: editingResult.statistics?.mean,
                    minValue: editingResult.statistics?.min,
                    maxValue: editingResult.statistics?.max,
                    processingTimeMs: editingResult.processing_duration * 1000,
                    ...modifiedData, // Apply changes (e.g., notes)
                },
                // Use existing date/type/status unless they are also editable
                date: editingResult.end_time, // Assuming end_time maps to the 'date' field
                type: 'VEGETATION_INDEX', // Assuming this is constant for now
                status: 'COMPLETED', // Assuming this is constant for now
            };

            await analysisService.updateAnalysisResult(editingResult.id, fullUpdateData);
            setUpdateAnalysisSuccess(`Result ${editingResult.id} updated successfully. Refreshing list...`);
            await fetchAnalysisResults(); // Refetch results after update
            cancelEditingAnalysis(); // Close modal on success
        } catch (error: any) {
            console.error("Error updating analysis result:", error);
            // Keep modal open on error to show message
            setUpdateAnalysisError(error.message || "Failed to update analysis result.");
        } finally {
            setIsUpdatingAnalysis(false); // Set to false when finished
        }
    };


    return {
        // State for list of results
        analysisResults,
        isLoadingAnalysis,
        analysisError,

        // State for single latest result (from session or direct run)
        latestAnalysisResult, // Keep this for the "Save Latest" feature
        isLoadingLatestResult,
        latestResultError,

        // State for saving action (for the latest result)
        isSavingAnalysis,
        saveAnalysisError,
        saveAnalysisSuccess,

        // State for deleting action
        isDeletingAnalysis,
        deleteAnalysisError,
        deleteAnalysisSuccess,

        // State for updating action
        editingResult, // Expose the result being edited for the modal
        isUpdatingAnalysis,
        updateAnalysisError,
        updateAnalysisSuccess,

        // Handlers
        fetchAnalysisResults, // Expose refetch function
        handleSaveAnalysis,   // Keep for saving the latest result
        handleDeleteAnalysis, // Add delete handler
        startEditingAnalysis, // Add handler to start editing
        cancelEditingAnalysis,// Add handler to cancel editing
        handleUpdateAnalysis, // Update handler now takes partial data
        setLatestAnalysisResult // Allow parent to set this if analysis is run directly
    };
}
