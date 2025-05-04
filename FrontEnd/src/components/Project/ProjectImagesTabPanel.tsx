import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import {
    Box,
    Typography,
    Button,
    Paper,
    CircularProgress,
    Snackbar, // Added for success/error messages
    Alert,    // Added for success/error messages
    Stack,    // Added for button layout
} from '@mui/material';
import { PhotoIcon, ArrowDownTrayIcon, ArrowUturnLeftIcon, XMarkIcon } from '@heroicons/react/24/outline'; // Added save, back, and clear icons
import ImageFilterComponent from '@/components/ImageGrid/ImageFilter'; // Assuming path is correct
import ImageGrid from '@/components/ImageGrid/ImageGrid'; // Assuming path is correct
import Modal from '@/components/Modal';
import DragDropUpload from '@/components/ImageUpload/DragDropUpload';
import FullscreenImageViewer from '@/components/ImageGrid/FullscreenImageViewer';
import GeeTweakParamsModal from './GeeTweakParamsModal'; // Import the new modal
import { SatelliteImage } from '@/types/image';
import { ImageFilter } from '@/services/images.service';
import { GeeParams } from '@/services/gee.service'; // Import GeeParams

// Define props based on the hooks it will use
interface ProjectImagesTabPanelProps {
    projectId: string | undefined | null; // Needed for navigation

    // From useProjectImages
    satelliteImages: SatelliteImage[];
    isLoadingImages: boolean;
    imageFetchError: string | null;
    imageActionError: string | null;
    imageActionSuccess: string | null;
    imageFilters: ImageFilter;
    availableTags: string[];
    selectedImage: SatelliteImage | null;
    isImageViewerOpen: boolean;
    isUploadModalOpen: boolean;
    isUploading: boolean;
    uploadError: string | null;
    fetchImages: () => void; // For retry button
    handleFilterChange: (filters: ImageFilter) => void;
    handleImageView: (image: SatelliteImage) => void;
    handleCloseImageViewer: () => void;
    handleRemoveImage: (imageId: string) => void;
    handleSaveAnnotations: (imageId: string, annotations: any[]) => void; // Keep 'any' if type removed
    handleOpenUploadModal: () => void;
    handleCloseUploadModal: () => void;
    handleFileUploadWrapper: (files: File[]) => Promise<void>; // Changed return type to Promise<void>

    // From useGeeImage (Updated)
    geeImageUrl: string | null;
    geeImageMetadata: object | null; // Could be GeeParams | null
    lastUsedParams: Partial<GeeParams> | null; // Params used for the current image
    isFetchingGeeImage: boolean;
    geeFetchError: string | null;
    refetchWithTweakedParams: (tweakedParams: Partial<GeeParams>) => Promise<void>; // Function to refetch
    clearGeeImage: () => void; // Function to clear image/state
}

// Define state for the intermediate "Add Image" choice modal
type AddImageModalState = 'closed' | 'selectType';

const ProjectImagesTabPanel: React.FC<ProjectImagesTabPanelProps> = ({ // Add React.FC back
    projectId,
    satelliteImages,
    isLoadingImages,
    imageFetchError,
    imageActionError, // Display these errors
    imageActionSuccess, // Display these success messages
    imageFilters,
    availableTags,
    selectedImage,
    isImageViewerOpen,
    isUploadModalOpen,
    isUploading,
    uploadError,
    fetchImages,
    handleFilterChange,
    handleImageView,
    handleCloseImageViewer,
    handleRemoveImage,
    handleSaveAnnotations,
    handleOpenUploadModal,
    handleCloseUploadModal,
    handleFileUploadWrapper,
    // Destructure new props from useGeeImage
    geeImageUrl,
    geeImageMetadata,
    lastUsedParams,
    isFetchingGeeImage,
    geeFetchError,
    refetchWithTweakedParams,
    clearGeeImage,
}) => {
    const router = useRouter();
    const [addImageModalState, setAddImageModalState] = useState<AddImageModalState>('closed');
    const [isTweakModalOpen, setIsTweakModalOpen] = useState(false); // State for tweak modal
    const [isSavingGee, setIsSavingGee] = useState(false);
    const [saveGeeError, setSaveGeeError] = useState<string | null>(null);
    const [saveGeeSuccess, setSaveGeeSuccess] = useState<string | null>(null);

    // Handler for saving GEE image
    const handleSaveGeeImage = async () => {
        if (!geeImageUrl || !projectId) {
            setSaveGeeError("Missing image URL or Project ID.");
            return;
        }
        setIsSavingGee(true);
        setSaveGeeError(null);
        setSaveGeeSuccess(null); // Clear previous success message
        try {
            // Fetch the image data
            const response = await fetch(geeImageUrl); // Potential CORS issue here
            if (!response.ok) {
                throw new Error(`Failed to fetch image data: ${response.status} ${response.statusText}`);
            }
            const blob = await response.blob();

            // Determine filename (try to get from URL or default)
            let filename = `gee_image_${projectId}_${Date.now()}.tiff`; // Default
            try {
                const urlParts = new URL(geeImageUrl).pathname.split('/');
                const potentialFilename = urlParts[urlParts.length - 1];
                if (potentialFilename.includes('.')) { // Basic check for extension
                    filename = potentialFilename;
                }
            } catch (e) { /* Ignore URL parsing errors, use default */ }

            // Create File object
            const imageFile = new File([blob], filename, { type: blob.type || 'image/tiff' }); // Use blob type or default

            // Use the existing upload wrapper
            await handleFileUploadWrapper([imageFile]);

            // Assuming handleFileUploadWrapper doesn't throw on success but might set its own success/error states
            // We'll set a specific success message here for the GEE save action
            setSaveGeeSuccess('GEE image successfully sent for saving!');
            // Optionally clear the preview after successful save initiation?
            // setGeeImageUrl(null); // Decide if this is desired UX

        } catch (err: any) {
            console.error('Failed to save GEE image:', err);
            let errMsg = 'Failed to save GEE image. Check console for details.';
            if (err instanceof TypeError && err.message.includes('Failed to fetch')) {
                errMsg += ' (This might be a CORS issue if the URL is external).';
            } else if (err.message) {
                errMsg = `Failed to save GEE image: ${err.message}`;
            }
            setSaveGeeError(errMsg);
        } finally {
            setIsSavingGee(false);
        }
    };

    // Handler for opening the tweak modal
    const handleOpenTweakModal = () => {
        setIsTweakModalOpen(true);
    };

    // Handler for closing the tweak modal
    const handleCloseTweakModal = () => {
        setIsTweakModalOpen(false);
    };

    // Handler for submitting tweaked parameters
    const handleTweakSubmit = async (tweakedParams: Partial<GeeParams>) => {
        // The hook's function already handles setting loading/error states
        await refetchWithTweakedParams(tweakedParams);
        // Close the modal after submission attempt (success or fail, error shown in snackbar)
        setIsTweakModalOpen(false);
    };

    // Handler for navigating back to the map with parameters
    const handleGoBackToMap = () => {
        if (!lastUsedParams || !lastUsedParams.region || !lastUsedParams.start_date || !lastUsedParams.end_date) {
            console.error("Cannot go back to map: Missing required parameters (region, dates).");
            // Optionally navigate back without params or show error
            router.push('/map'); // Navigate back without params as fallback
            clearGeeImage(); // Still clear the image
            return;
        }

        try {
            const queryParams = new URLSearchParams({
                region: encodeURIComponent(JSON.stringify(lastUsedParams.region)),
                startDate: lastUsedParams.start_date,
                endDate: lastUsedParams.end_date,
                // Add other params if the map page should use them (e.g., cloud cover?)
                // maxCloudCover: String(lastUsedParams.max_cloud_cover || defaultCloudCover),
            });

            // Navigate to map page with parameters
            router.push(`/map?${queryParams.toString()}`);

            // Clear the GEE image state on the project page after navigating
            clearGeeImage();

        } catch (error) {
            console.error("Error constructing URL for map navigation:", error);
            // Fallback navigation
            router.push('/map');
            clearGeeImage();
        }
    };

    // Handler for FullscreenImageViewer navigation
    const handleNavigate = (direction: 'prev' | 'next') => {
        if (!selectedImage || !satelliteImages || satelliteImages.length < 2) return;

        const currentIndex = satelliteImages.findIndex(img => img.id === selectedImage.id);
        if (currentIndex === -1) return;

        let nextIndex;
        if (direction === 'prev') {
            nextIndex = currentIndex > 0 ? currentIndex - 1 : satelliteImages.length - 1; // Wrap around (optional) or stop at ends
            // nextIndex = Math.max(0, currentIndex - 1); // Stop at ends
        } else {
            nextIndex = currentIndex < satelliteImages.length - 1 ? currentIndex + 1 : 0; // Wrap around (optional) or stop at ends
            // nextIndex = Math.min(satelliteImages.length - 1, currentIndex + 1); // Stop at ends
        }

        const nextImage = satelliteImages[nextIndex];
        if (nextImage) {
            handleImageView(nextImage); // Use the prop function to update the selected image
        }
    };

    const handleNavigateToMap = () => {
        if (projectId) {
            router.push(`/map?projectId=${projectId}`);
            setAddImageModalState('closed');
        } else {
            console.error("Cannot navigate to map without Project ID");
            // Optionally show an error to the user
        }
    };

    const openSelectTypeModal = () => {
        setAddImageModalState('selectType');
    };

    const closeSelectTypeModal = () => {
        setAddImageModalState('closed');
    };

    const openUploadModalFromChoice = () => {
        setAddImageModalState('closed');
        handleOpenUploadModal(); // Call the hook's function to open the actual upload modal
    };

    // Determine which error/success message to show (prioritize GEE save messages, then action messages)
    const displayError = saveGeeError || imageActionError || imageFetchError || geeFetchError;
    const displaySuccess = saveGeeSuccess || imageActionSuccess; // Prioritize GEE save success message

    // Snackbar close handlers
    const handleCloseSnackbar = (event?: React.SyntheticEvent | Event, reason?: string) => {
        if (reason === 'clickaway') {
            return;
        }
        // Clear local component errors/success messages
        setSaveGeeError(null);
        setSaveGeeSuccess(null);
        // Note: We don't clear imageActionError/Success here as they come from props and should be managed by the parent hook
    };

    return (
        <div>
            {/* Image Filters */}
            <ImageFilterComponent
                availableTags={availableTags}
                onFilterChange={handleFilterChange}
                initialFilters={imageFilters} // Pass current filters
            />

            {/* Add Images Button */}
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', my: 3 }}>
                <Typography variant="h6">
                    Project Images ({satelliteImages.length})
                </Typography>
                <Button
                    variant="contained"
                    color="primary"
                    onClick={openSelectTypeModal} // Open the choice modal first
                    startIcon={<PhotoIcon className="h-5 w-5" />}
                >
                    Add Images
                </Button>
            </Box>

            {/* Combined Snackbar for all feedback */}
            <Snackbar
                open={!!displayError || !!displaySuccess}
                autoHideDuration={6000}
                onClose={handleCloseSnackbar}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
            >
                <Alert
                    onClose={handleCloseSnackbar}
                    severity={displayError ? 'error' : 'success'}
                    sx={{ width: '100%' }}
                >
                    {displayError || displaySuccess}
                </Alert>
            </Snackbar>

            {/* GEE Image Section */}
            {isFetchingGeeImage && (
                <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', py: 4, mb: 4 }}>
                    <CircularProgress size={24} sx={{ mr: 2 }} />
                    <Typography>Fetching GEE Image...</Typography>
                </Box>
            )}
            {/* Note: Removed separate geeFetchError display, handled by Snackbar */}
            {geeImageUrl && (
                <Paper sx={{ mb: 4, p: 2, border: '1px solid #ccc' }}>
                    <Typography variant="subtitle1" gutterBottom>
                        Fetched GEE Image Preview
                    </Typography>
                    <img src={geeImageUrl} alt="GEE Image Preview" style={{ maxWidth: '100%', height: 'auto', display: 'block', margin: 'auto' }} />
                    {geeImageMetadata && (
                        <Box sx={{ mt: 2, maxHeight: '150px', overflowY: 'auto', bgcolor: 'grey.100', p: 1 }}>
                            <Typography variant="caption">Metadata:</Typography>
                            <pre className="text-xs">
                                {JSON.stringify(geeImageMetadata, null, 2)}
                            </pre>
                        </Box>
                    )}
                    {/* Action Buttons for GEE Image */}
                    <Stack direction="row" spacing={2} sx={{ mt: 2, justifyContent: 'center' }}>
                        <Button
                            variant="contained"
                            color="primary"
                            startIcon={isSavingGee ? <CircularProgress size={20} color="inherit" /> : <ArrowDownTrayIcon className="h-5 w-5" />}
                            onClick={handleSaveGeeImage}
                            disabled={isSavingGee || !projectId}
                        >
                            {isSavingGee ? 'Saving...' : 'Save to Project'}
                        </Button>
                        {/* Tweak Parameters Button */}
                        <Button
                            variant="outlined"
                            color="secondary"
                            onClick={handleOpenTweakModal}
                            disabled={isFetchingGeeImage || !lastUsedParams} // Disable if fetching or no params to tweak
                        >
                            Tweak Parameters
                        </Button>
                        {/* Go Back to Map Button */}
                        <Button
                            variant="outlined"
                            color="inherit"
                            onClick={handleGoBackToMap}
                            startIcon={<ArrowUturnLeftIcon className="h-5 w-5" />}
                            disabled={isFetchingGeeImage || !lastUsedParams} // Disable if fetching or no params to restore
                        >
                            Go Back to Map
                        </Button>
                        {/* Clear Preview Button */}
                        <Button
                            variant="outlined"
                            color="error" // Use error color for clear action
                            onClick={clearGeeImage} // Call the clear function from props
                            startIcon={<XMarkIcon className="h-5 w-5" />}
                            disabled={isFetchingGeeImage} // Disable while fetching
                        >
                            Clear Preview
                        </Button>
                    </Stack>
                </Paper>
            )}

            {/* Image Grid Section */}
            {isLoadingImages ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', py: 8 }}>
                    <CircularProgress size={40} sx={{ mr: 2 }} />
                    <Typography>Loading Images...</Typography>
                </Box>
            ) : imageFetchError && !imageActionError ? ( // Show fetch error only if no action error
                <Paper sx={{ textAlign: 'center', py: 4, px: 2, bgcolor: 'error.light' }}>
                    <Typography color="error.contrastText" gutterBottom>
                        {imageFetchError}
                    </Typography>
                    <Button
                        onClick={fetchImages} // Retry fetching images
                        variant="contained"
                        color="error" // Use error color for button too
                        sx={{ mt: 2 }}
                    >
                        Retry Loading Images
                    </Button>
                </Paper>
            ) : satelliteImages.length === 0 && !geeImageUrl && !isFetchingGeeImage ? (
                <Paper sx={{ textAlign: 'center', py: 8, px: 4, bgcolor: 'background.default' }}>
                    <PhotoIcon className="h-12 w-12 mx-auto text-gray-400 mb-4" />
                    <Typography variant="h6" gutterBottom>
                        No Images Yet
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                        This project doesn't have any satellite images yet. Use the "Add Images" button to fetch from GEE via the map or upload your own.
                    </Typography>
                    <Button
                        variant="contained"
                        color="primary"
                        onClick={openSelectTypeModal}
                    >
                        Add Your First Image
                    </Button>
                </Paper>
            ) : (
                <ImageGrid
                    images={satelliteImages} // Pass the full list; filtering might happen inside ImageGrid or via props
                    loading={isLoadingImages}
                    onSelectImage={handleImageView} // Use for click action
                    onAnnotateImage={handleImageView} // Can be the same handler
                    onDeleteImage={handleRemoveImage}
                    // Pass other props if ImageGrid supports them, e.g., onToggleFavorite
                />
            )}

            {/* Modals */}

            {/* 1. Add Image Choice Modal */}
            <Modal
                open={addImageModalState === 'selectType'}
                onClose={closeSelectTypeModal}
                title="Add Image to Project"
                content={(
                    <div className="space-y-4">
                        <Typography variant="body1">
                            Choose how to add an image:
                        </Typography>
                        <Button
                            fullWidth
                            variant="outlined"
                            onClick={handleNavigateToMap}
                            sx={{ mb: 1 }}
                            disabled={!projectId} // Disable if no project ID
                        >
                            Select Area on Map (Fetch GEE Image)
                        </Button>
                        <Button
                            fullWidth
                            variant="outlined"
                            onClick={openUploadModalFromChoice}
                            disabled={!projectId} // Disable if no project ID
                        >
                            Upload Image File
                        </Button>
                    </div>
                )}
                actions={[
                    {
                        label: "Cancel",
                        onClick: closeSelectTypeModal,
                        color: "inherit"
                    }
                ]}
            />

            {/* 2. Upload Image Modal (controlled by useProjectImages hook) */}
            <Modal
                open={isUploadModalOpen}
                onClose={handleCloseUploadModal} // Use hook's close function
                title="Upload Image File"
                content={(
                    <div className="space-y-4">
                        <DragDropUpload
                            onUpload={handleFileUploadWrapper} // Use hook's upload wrapper
                            isUploading={isUploading} // Use hook's uploading state
                            maxFiles={1}
                            maxFileSizeMB={25} // Example size limit
                        />
                        {uploadError && (
                            <Typography color="error" variant="body2" sx={{ mt: 2 }}>
                                {uploadError}
                            </Typography>
                        )}
                        {isUploading && (
                             <Box sx={{ display: 'flex', alignItems: 'center', mt: 2 }}>
                                <CircularProgress size={20} sx={{ mr: 1 }} />
                                <Typography variant="body2">Uploading...</Typography>
                             </Box>
                        )}
                    </div>
                )}
                actions={[
                    {
                        label: "Cancel",
                        onClick: handleCloseUploadModal,
                        color: "inherit",
                        disabled: isUploading // Disable cancel during upload
                    }
                ]}
            />

            {/* 3. Fullscreen Viewer/Annotator (controlled by useProjectImages hook) */}
            <FullscreenImageViewer
                open={isImageViewerOpen}
                onClose={handleCloseImageViewer} // Use hook's close function
                image={selectedImage} // Use hook's selected image
                images={satelliteImages} // Pass the full list for navigation
                onNavigate={handleNavigate} // Pass the navigation handler
                // Pass save handler if annotation is enabled
                // onSaveAnnotations={handleSaveAnnotations}
            />

            {/* 4. GEE Tweak Parameters Modal */}
            <GeeTweakParamsModal
                open={isTweakModalOpen}
                onClose={handleCloseTweakModal}
                initialParams={lastUsedParams || {}} // Pass last used params or empty object
                onSubmit={handleTweakSubmit}
                isSubmitting={isFetchingGeeImage} // Use hook's fetching state
            />
        </div>
    );
};

export default ProjectImagesTabPanel;
