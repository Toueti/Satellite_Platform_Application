'use client';

import { useState } from 'react';
import { useParams } from 'next/navigation';
import { Box, Typography, Button, CircularProgress } from '@mui/material';

// Import Hooks
import { useProjectData } from '@/hooks/useProjectData';
import { useProjectImages } from '@/hooks/useProjectImages';
import { useProjectAnalysis } from '@/hooks/useProjectAnalysis';
import { useProjectSharing } from '@/hooks/useProjectSharing';
import { useGeeImage } from '@/hooks/useGeeImage';

// Import Components
import ProjectHeader from '@/components/Project/ProjectHeader';
import ProjectTabs, { TabPanel } from '@/components/Project/ProjectTabs';
import ProjectDetailsTabPanel from '@/components/Project/ProjectDetailsTabPanel';
import ProjectImagesTabPanel from '@/components/Project/ProjectImagesTabPanel';
import ProjectAnalysisTabPanel from '@/components/Project/ProjectAnalysisTabPanel';

export default function ProjectDetailPage() {
    const params = useParams();
    const projectId = typeof params?.id === 'string' ? params.id : undefined;

    const [activeTab, setActiveTab] = useState(0);

    // Initialize Hooks
    const {
        project,
        setProject, // Needed for sharing hook
        loading: loadingProject,
        error: projectError,
        actionError: projectActionError,
        actionSuccess: projectActionSuccess,
        isLoadingAction: isLoadingProjectAction,
        handleArchiveProject,
        handleUnarchiveProject,
        handleDeleteProject,
        refetchProject, // Renamed from retryFetchProject
    } = useProjectData(projectId);

    const {
        satelliteImages,
        isLoadingImages,
        imageFetchError,
        imageActionError,
        imageActionSuccess,
        imageFilters,
        availableTags,
        // favoriteImages, // Not directly used in panels, handled within hook/ImageGrid
        selectedImage,
        isImageViewerOpen,
        isUploadModalOpen,
        isUploading,
        uploadError,
        fetchImages: refetchImages, // Renamed to avoid conflict
        handleFilterChange,
        handleImageView,
        handleCloseImageViewer,
        // handleToggleFavorite, // Not directly used in panels
        handleRemoveImage,
        handleSaveAnnotations,
        handleOpenUploadModal,
        handleCloseUploadModal,
        handleFileUploadWrapper,
    } = useProjectImages(projectId);

    const {
        analysisResults,
        isLoadingAnalysis,
        analysisError,
        latestAnalysisResult,
        isLoadingLatestResult,
        latestResultError,
        isSavingAnalysis,
        saveAnalysisError,
        saveAnalysisSuccess,
        fetchAnalysisResults: refetchAnalysis, // Renamed
        handleSaveAnalysis,
        // setLatestAnalysisResult, // Not directly set from here
    } = useProjectAnalysis(projectId, activeTab === 2); // Only fetch analysis when tab is active

    const {
        sharingEmail,
        setSharingEmail,
        sharingError,
        sharingSuccess,
        isSharing,
        handleShareProject,
        handleUnshareProject,
    } = useProjectSharing(projectId, setProject); // Pass setProject to update collaborators

    const {
        geeImageUrl,
        geeImageMetadata,
        isFetchingGeeImage,
        geeFetchError,
        // fetchAndDisplayGeeImage, // Not directly called from here, handled by hook's effect
    } = useGeeImage(projectId);

    const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
        setActiveTab(newValue);
    };

    // --- Render Logic ---

    // 1. Handle Invalid Project ID early
    if (projectId === undefined) {
        return (
            <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white flex items-center justify-center">
                <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded-md">
                    Invalid project ID provided in URL.
                </div>
            </div>
        );
    }

    // 2. Handle Initial Project Loading State
    if (loadingProject && !project) {
        return (
            <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white flex items-center justify-center">
                <CircularProgress size={40} />
            </div>
        );
    }

    // 3. Handle Project Fetch Error State (after loading attempt)
    if (projectError && !project) {
        return (
            <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white flex items-center justify-center">
                <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded-md text-center">
                    <Typography variant="h6" color="error" gutterBottom>Loading Error</Typography>
                    <p>{projectError}</p>
                    <Button
                        onClick={refetchProject} // Use refetch function from hook
                        variant="outlined"
                        color="error"
                        sx={{ mt: 2 }}
                    >
                        Try Again
                    </Button>
                </div>
            </div>
        );
    }

    // 4. Handle Case where project is null after loading without error (shouldn't happen ideally)
    if (!project) {
        return (
            <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white flex items-center justify-center">
                <div className="bg-yellow-100 border border-yellow-400 text-yellow-700 px-4 py-3 rounded-md">
                    Project data could not be loaded. Please try refreshing the page.
                </div>
            </div>
        );
    }

    // 5. Render Project Page Content
    return (
        <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                {/* Project Header */}
                <ProjectHeader
                    project={project}
                    projectId={projectId}
                    onArchive={handleArchiveProject}
                    onUnarchive={handleUnarchiveProject}
                    onDelete={handleDeleteProject}
                    actionError={projectActionError}
                    actionSuccess={projectActionSuccess}
                    isLoadingAction={isLoadingProjectAction}
                />

                {/* Project Tabs */}
                <ProjectTabs activeTab={activeTab} onTabChange={handleTabChange} />

                {/* Tab Panels */}
                <TabPanel value={activeTab} index={0}>
                    <ProjectDetailsTabPanel
                        project={project}
                        projectId={projectId}
                        sharingEmail={sharingEmail}
                        setSharingEmail={setSharingEmail}
                        sharingError={sharingError}
                        sharingSuccess={sharingSuccess}
                        isSharing={isSharing}
                        handleShareProject={handleShareProject}
                        handleUnshareProject={handleUnshareProject}
                    />
                </TabPanel>

                <TabPanel value={activeTab} index={1}>
                    <ProjectImagesTabPanel
                        projectId={projectId}
                        satelliteImages={satelliteImages}
                        isLoadingImages={isLoadingImages}
                        imageFetchError={imageFetchError}
                        imageActionError={imageActionError}
                        imageActionSuccess={imageActionSuccess}
                        imageFilters={imageFilters}
                        availableTags={availableTags}
                        selectedImage={selectedImage}
                        isImageViewerOpen={isImageViewerOpen}
                        isUploadModalOpen={isUploadModalOpen}
                        isUploading={isUploading}
                        uploadError={uploadError}
                        fetchImages={refetchImages}
                        handleFilterChange={handleFilterChange}
                        handleImageView={handleImageView}
                        handleCloseImageViewer={handleCloseImageViewer}
                        handleRemoveImage={handleRemoveImage}
                        handleSaveAnnotations={handleSaveAnnotations}
                        handleOpenUploadModal={handleOpenUploadModal}
                        handleCloseUploadModal={handleCloseUploadModal}
                        handleFileUploadWrapper={handleFileUploadWrapper}
                        geeImageUrl={geeImageUrl}
                        geeImageMetadata={geeImageMetadata}
                        isFetchingGeeImage={isFetchingGeeImage}
                        geeFetchError={geeFetchError}
                    />
                </TabPanel>

                <TabPanel value={activeTab} index={2}>
                    <ProjectAnalysisTabPanel
                        projectId={projectId}
                        analysisResults={analysisResults}
                        isLoadingAnalysis={isLoadingAnalysis}
                        analysisError={analysisError}
                        latestAnalysisResult={latestAnalysisResult}
                        isLoadingLatestResult={isLoadingLatestResult}
                        latestResultError={latestResultError}
                        isSavingAnalysis={isSavingAnalysis}
                        saveAnalysisError={saveAnalysisError}
                        saveAnalysisSuccess={saveAnalysisSuccess}
                        fetchAnalysisResults={refetchAnalysis}
                        handleSaveAnalysis={handleSaveAnalysis}
                        hasImages={satelliteImages.length > 0} // Pass boolean based on images state
                    />
                </TabPanel>
            </div>
        </div>
    );
}
