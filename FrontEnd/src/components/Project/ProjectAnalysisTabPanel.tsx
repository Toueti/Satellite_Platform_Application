import React from 'react';
import { useRouter } from 'next/navigation';
import {
    Box,
    Typography,
    Button,
    Paper,
    CircularProgress,
    Stack,
    Divider,
    IconButton, // Added for Edit/Delete buttons
    Tooltip,    // Added for button hints
    // Grid, // Grid is no longer used here, replaced by Stack
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import ImageIcon from '@mui/icons-material/Image'; // Import ImageIcon
import { analysisService, AnalysisResult, UpdateAnalysisData } from '@/services/analysis.service'; // Import analysisService
import EditAnalysisResultModal from './EditAnalysisResultModal'; // Import the modal

// Define props based on the hooks it will use
interface ProjectAnalysisTabPanelProps {
    projectId: string | undefined | null;

    // From useProjectAnalysis
    analysisResults: AnalysisResult[];
    isLoadingAnalysis: boolean;
    analysisError: string | null;
    latestAnalysisResult: AnalysisResult | null;
    isLoadingLatestResult: boolean; // Added for loading state of single result
    latestResultError: string | null; // Added for error state of single result
    isSavingAnalysis: boolean;
    saveAnalysisError: string | null;
    saveAnalysisSuccess: string | null;
    fetchAnalysisResults: () => void; // For retry button
    handleSaveAnalysis: () => void; // For saving latest result

    // Delete props
    isDeletingAnalysis: string | null; // ID of result being deleted
    deleteAnalysisError: string | null;
    deleteAnalysisSuccess: string | null;
    handleDeleteAnalysis: (imageId: string, resultId: string) => Promise<void>;

    // Update props
    isUpdatingAnalysis: boolean; // Corrected type to boolean
    updateAnalysisError: string | null;
    // updateAnalysisSuccess: string | null; // Success message handled by hook, maybe display globally later
    // Edit handlers from hook
    editingResult: AnalysisResult | null; // The result currently being edited
    startEditingAnalysis: (result: AnalysisResult) => void;
    cancelEditingAnalysis: () => void;
    handleUpdateAnalysis: (modifiedData: Partial<UpdateAnalysisData['data']>) => Promise<void>; // Updated signature

    // From useProjectImages (needed to enable/disable Run Analysis button)
    hasImages: boolean; // Pass a boolean indicating if project has images
}

const ProjectAnalysisTabPanel: React.FC<ProjectAnalysisTabPanelProps> = ({
    projectId,
    analysisResults,
    isLoadingAnalysis,
    analysisError,
    latestAnalysisResult,
    isLoadingLatestResult,
    latestResultError,
    isSavingAnalysis,
    saveAnalysisError,
    saveAnalysisSuccess,
    fetchAnalysisResults,
    handleSaveAnalysis,
    // Delete props
    isDeletingAnalysis,
    deleteAnalysisError,
    // deleteAnalysisSuccess, // Not explicitly displayed per-card for now
    handleDeleteAnalysis,
    // Update props
    isUpdatingAnalysis,
    updateAnalysisError,
    // Edit handlers
    editingResult,
    startEditingAnalysis,
    cancelEditingAnalysis,
    handleUpdateAnalysis,
    hasImages,
}) => {
    const router = useRouter();

    // console.log('[ProjectAnalysisTabPanel] Rendering with analysisResults:', JSON.stringify(analysisResults, null, 2)); // REMOVED Log

    // Function to trigger opening the edit modal
    const handleEditClick = (result: AnalysisResult) => {
        startEditingAnalysis(result); // Call hook function to set the editing state
    };

    const handleAnalysisClick = () => {
        if (projectId) {
            router.push(`/analysis?projectId=${String(projectId)}`);
        } else {
            console.warn("Project ID is undefined, cannot navigate to analysis.");
            // Optionally show an error message
        }
    };

    // Component to render a single analysis result card (reusable for list and latest)
    const AnalysisResultCard: React.FC<{ result: AnalysisResult, isLatest?: boolean }> = ({ result, isLatest = false }) => {
        const isBeingDeleted = isDeletingAnalysis === result.id;
        const currentError = (deleteAnalysisError && isBeingDeleted); // Only show delete error per card

        return (
        <Paper sx={{ p: 2, border: isLatest ? '1px dashed' : 'none', borderColor: isLatest ? 'primary.main' : 'transparent', opacity: isBeingDeleted ? 0.5 : 1, position: 'relative' }}>
            {/* Action Buttons - Show container even if ID is missing to show disabled buttons */}
            {!isLatest && (
                <Box sx={{ position: 'absolute', top: 8, right: 8, display: 'flex', gap: 0.5 }}>
                    {/* Edit Button */}
                    <Tooltip title={result.id ? "Edit Result Notes" : "Edit disabled: Result ID missing from backend list response"}>
                        <span> {/* Span needed for disabled Tooltip */}
                        <IconButton
                            aria-label="Edit analysis result"
                            size="small"
                            onClick={() => result.id && handleEditClick(result)}
                            disabled={!result.id || Boolean(isBeingDeleted || isUpdatingAnalysis)} // Disable if no ID or during other actions
                        >
                            <EditIcon fontSize="small" />
                        </IconButton>
                        </span>
                    </Tooltip>

                    {/* Delete Button */}
                    <Tooltip title={result.id ? "Delete Result" : "Delete disabled: Result ID missing from backend list response"}>
                         <span> {/* Span needed for disabled Tooltip */}
                        <IconButton
                            aria-label="Delete analysis result"
                            size="small"
                            color="error"
                            onClick={() => result.id && handleDeleteAnalysis(result.imageId, result.id)}
                            disabled={!result.id || Boolean(isBeingDeleted || isUpdatingAnalysis)} // Disable if no ID or during other actions
                        >
                            {isBeingDeleted ? <CircularProgress size={16} color="inherit" /> : <DeleteIcon fontSize="small" />}
                        </IconButton>
                        </span>
                    </Tooltip>

                    {/* Get Result Image Button */}
                    <Tooltip title={result.id ? "Get Result Image (Not Implemented)" : "Get Image disabled: Result ID missing from backend list response"}>
                         <span> {/* Span needed for disabled Tooltip */}
                        <IconButton
                            aria-label="Get result image"
                            size="small"
                            color="info" // Or another appropriate color
                            onClick={() => {
                                // TODO: Implement image fetching and adding to images tab
                                // Requires result.id and integration with useProjectImages/images.service
                                console.warn("Get Result Image functionality not implemented yet. Requires result ID and further integration.");
                            }}
                            disabled={!result.id || Boolean(isBeingDeleted || isUpdatingAnalysis)} // Disable if no ID or during other actions
                        >
                            <ImageIcon fontSize="small" />
                        </IconButton>
                        </span>
                    </Tooltip>
                </Box>
            )}

             <Typography variant="subtitle1" gutterBottom sx={{ pr: isLatest ? 0 : '60px' /* Make space for buttons */ }}>
                {result.index_type || 'Analysis Result'} {isLatest && "(Latest Run)"}
            </Typography>
            <Divider sx={{ mb: 2 }} />

            {/* Display error specific to this card */}
            {currentError && (
                <Typography color="error" variant="caption" sx={{ mb: 1, display: 'block' }}>
                    Error: {currentError}
                </Typography>
            )}

            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                {/* Display image if available, regardless of isLatest */}
                {result.processed_image && (
                    <Box sx={{ flex: 1, maxWidth: '100%', overflow: 'hidden' }}>
                        <Typography variant="subtitle2" gutterBottom>Processed Image</Typography>
                        <img
                             src={`data:image/png;base64,${result.processed_image}`}
                             alt={`${result.index_type || 'Analysis'} Result`}
                             style={{ width: '100%', height: 'auto', border: '1px solid #ccc', borderRadius: '4px' }}
                         />
                    </Box>
                )}
                {/* Removed the separate block that only showed image for isLatest */}
                <Box sx={{ flex: 1 }}>
                    <Typography variant="subtitle2" gutterBottom>Details & Statistics</Typography>
                    {/* Keep Type display for non-latest results for now, can remove later if needed */}
                    {!isLatest && <Typography variant="body2"><strong>Type:</strong> {result.index_type}</Typography>}
                    {/* Check if statistics object exists */}
                    {result.statistics ? (
                        <>
                            {/* Check each statistic individually */}
                            <Typography variant="body2">Min: {result.statistics.min?.toFixed(4) ?? 'N/A'}</Typography>
                            <Typography variant="body2">Max: {result.statistics.max?.toFixed(4) ?? 'N/A'}</Typography>
                            <Typography variant="body2">Mean: {result.statistics.mean?.toFixed(4) ?? 'N/A'}</Typography>
                            <Typography variant="body2">Median: {result.statistics.median?.toFixed(4) ?? 'N/A'}</Typography>
                            {/* Use the defined 'std' property */}
                            <Typography variant="body2">Std Dev: {result.statistics.std?.toFixed(4) ?? 'N/A'}</Typography>
                        </>
                    ) : (
                        <Typography variant="body2" color="text.secondary">Statistics not available.</Typography>
                    )}
                    <Divider sx={{ my: 1 }} />
                    <Typography variant="caption" display="block">
                        Duration: {result.processing_duration > 0 ? `${result.processing_duration.toFixed(2)}s` : 'N/A'}
                    </Typography>
                    <Typography variant="caption" display="block">
                        Date: {result.end_time ? new Date(result.end_time).toLocaleString() : 'N/A'} {/* Use end_time which maps to 'date' */}
                    </Typography>
                    {/* Show Image ID in the list view as well */}
                    {result.imageId && (
                         <Typography variant="caption" display="block">
                            Image ID: {result.imageId}
                        </Typography>
                    )}
                     {/* Show Result ID in the list view */}
                     {result.id && !isLatest && (
                         <Typography variant="caption" display="block">
                            Result ID: {result.id}
                        </Typography>
                    )}
                 </Box>
            </Stack>
        </Paper>
        );
    };


    return (
        <div className="space-y-4">
            {/* Header and Run Analysis Button */}
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
                <Typography variant="h6">
                    Analysis Results ({analysisResults.length})
                </Typography>
                <Button
                    variant="contained"
                    color="primary"
                    onClick={handleAnalysisClick}
                    disabled={!hasImages || !projectId} // Disable if no images or project ID
                >
                    Run New Analysis
                </Button>
            </Box>

            {/* Loading/Error states for analysis list */}
            {isLoadingAnalysis ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', py: 8 }}>
                    <CircularProgress size={40} sx={{ mr: 2 }}/>
                    <Typography>Loading Analysis Results...</Typography>
                </Box>
            ) : analysisError ? (
                <Paper sx={{ textAlign: 'center', py: 4, px: 2, bgcolor: 'error.light' }}>
                    <Typography color="error.contrastText" gutterBottom>
                        {analysisError}
                    </Typography>
                    <Button
                        onClick={fetchAnalysisResults} // Retry fetching results
                        variant="contained"
                        color="error"
                        sx={{ mt: 2 }}
                    >
                        Retry Loading Results
                    </Button>
                </Paper>
            ) : analysisResults.length === 0 && !latestAnalysisResult && !isLoadingLatestResult ? ( // Show only if list is empty AND no latest result loading/loaded
                <Paper sx={{ textAlign: 'center', py: 8, px: 4, bgcolor: 'background.default' }}>
                    <Typography variant="h6" gutterBottom>
                        No analysis results yet
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                        Run your first analysis on this project's images to see results here.
                    </Typography>
                    <Button
                        variant="contained"
                        color="primary"
                        onClick={handleAnalysisClick}
                        disabled={!hasImages || !projectId}
                    >
                        Start First Analysis
                    </Button>
                </Paper>
            ) : (
                 // Display Analysis Results List using Stack
                 <Stack spacing={3}> {/* Use Stack for vertical list */}
                    {analysisResults.map((result, index) => (
                        // Use index as key since result.id might not exist
                        <Box key={`analysis-${index}`}> {/* Wrap card in Box for key */}
                            <AnalysisResultCard result={result} />
                        </Box>
                    ))}
                </Stack>
            )}

            {/* Section to display and save the latest single result if available */}
            {isLoadingLatestResult && (
                <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', py: 4, mt: 4 }}>
                    <CircularProgress size={24} sx={{ mr: 2 }} />
                    <Typography>Loading Latest Result...</Typography>
                </Box>
            )}
            {latestResultError && (
                <Paper sx={{ p: 2, mt: 4, bgcolor: 'error.light' }}>
                    <Typography color="error.contrastText">{latestResultError}</Typography>
                </Paper>
            )}
            {latestAnalysisResult && (
                <Box mt={4}> {/* Add margin top to separate from the list */}
                    <AnalysisResultCard result={latestAnalysisResult} isLatest={true} />
                    <Box sx={{ mt: 2, display: 'flex', justifyContent: 'flex-end', alignItems: 'center' }}>
                        {saveAnalysisError && <Typography color="error" variant="caption" sx={{ mr: 2 }}>{saveAnalysisError}</Typography>}
                        {saveAnalysisSuccess && <Typography color="success.main" variant="caption" sx={{ mr: 2 }}>{saveAnalysisSuccess}</Typography>}
                        <Button
                            variant="contained"
                            onClick={handleSaveAnalysis}
                            disabled={isSavingAnalysis}
                            sx={{ minWidth: '140px' }} // Prevent width changes
                        >
                            {isSavingAnalysis ? <CircularProgress size={24} color="inherit" /> : 'Save This Result'}
                        </Button>
                    </Box>
                </Box>
            )}

            {/* Edit Modal */}
            <EditAnalysisResultModal
                open={!!editingResult} // Modal is open if editingResult is not null
                result={editingResult}
                isUpdating={isUpdatingAnalysis}
                error={updateAnalysisError}
                onClose={cancelEditingAnalysis} // Use cancel handler from hook
                onSave={handleUpdateAnalysis} // Use update handler from hook
            />
        </div>
    );
};

export default ProjectAnalysisTabPanel;
