import { useState, useEffect, useCallback } from 'react';
import { imagesService, ImageFilter } from '@/services/images.service';
import { SatelliteImage } from '@/types/image'; // Assuming ImageAnnotation is not used directly here

export function useProjectImages(projectId: string | undefined | null) {
    const [satelliteImages, setSatelliteImages] = useState<SatelliteImage[]>([]);
    const [isLoadingImages, setIsLoadingImages] = useState(false);
    const [imageFetchError, setImageFetchError] = useState<string | null>(null);
    const [imageActionError, setImageActionError] = useState<string | null>(null);
    const [imageActionSuccess, setImageActionSuccess] = useState<string | null>(null);

    const [imageFilters, setImageFilters] = useState<ImageFilter>({});
    const [availableTags, setAvailableTags] = useState<string[]>([]);
    const [favoriteImages, setFavoriteImages] = useState<string[]>([]);

    const [selectedImage, setSelectedImage] = useState<SatelliteImage | null>(null); // For viewer/annotation modal
    const [isImageViewerOpen, setIsImageViewerOpen] = useState(false); // Renamed from isAnnotationModalOpen

    const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
    const [isUploading, setIsUploading] = useState(false);
    const [uploadError, setUploadError] = useState<string | null>(null);

    // Fetch images function
    const fetchImages = useCallback(async () => {
        if (!projectId) {
            console.warn('No project ID available for fetching images');
            setSatelliteImages([]); // Clear images if no project ID
            setAvailableTags([]);
            return;
        }

        setIsLoadingImages(true);
        setImageFetchError(null);
        setImageActionError(null); // Clear action errors on refetch
        setImageActionSuccess(null); // Clear action success on refetch

        try {
            // console.log('Fetching images for project:', projectId); // Commented out
            // Apply filters if needed, currently fetching all and filtering client-side or in ImageGrid
            const projectImages = await imagesService.getImagesByProject(projectId, {
                sortBy: 'uploadDate',
                sortOrder: 'desc',
                // Potentially add filters from imageFilters here if API supports it
            });

            // console.log('Project images response:', projectImages); // Commented out

            if (projectImages && projectImages.length > 0) {
                setSatelliteImages(projectImages);
                const allTags = new Set<string>();
                projectImages.forEach(image => {
                    if (image.tags && Array.isArray(image.tags)) {
                        image.tags.forEach(tag => allTags.add(tag));
                    }
                });
                setAvailableTags(Array.from(allTags));
            } else {
                console.log('No images found for project:', projectId);
                setSatelliteImages([]);
                setAvailableTags([]);
            }
        } catch (error: any) {
            console.error('Error fetching images:', error);
            setImageFetchError('Failed to fetch images. Please try again later.');
            setSatelliteImages([]);
            setAvailableTags([]);
        } finally {
            setIsLoadingImages(false);
        }
    }, [projectId]); // Depend on projectId

    // Initial fetch and refetch on projectId change
    useEffect(() => {
        fetchImages();
    }, [fetchImages]);

    // Load/Save favorites from/to localStorage
    useEffect(() => {
        const storedFavorites = localStorage.getItem('favoriteImages');
        if (storedFavorites) {
            try {
                setFavoriteImages(JSON.parse(storedFavorites));
            } catch (e) {
                console.error("Failed to parse favorite images from localStorage", e);
                localStorage.removeItem('favoriteImages');
            }
        }
    }, []); // Run only on mount

    useEffect(() => {
        // Persist favorites whenever they change
        try {
            localStorage.setItem('favoriteImages', JSON.stringify(favoriteImages));
        } catch (e) {
            console.error("Failed to save favorite images to localStorage", e);
        }
    }, [favoriteImages]);

    // Cleanup blob URLs
    useEffect(() => {
        return () => {
            satelliteImages.forEach(image => {
                if (image.url && image.url.startsWith('blob:')) {
                    URL.revokeObjectURL(image.url);
                }
                if (image.thumbnailUrl && image.thumbnailUrl.startsWith('blob:')) {
                    URL.revokeObjectURL(image.thumbnailUrl);
                }
            });
        };
    }, [satelliteImages]);

    // --- Image Actions ---

    const handleFilterChange = (filters: ImageFilter) => {
        setImageFilters(filters);
        // Filtering logic might be applied here to update a derived `filteredImages` state,
        // or passed down to ImageGrid component to handle filtering.
        // console.log("Filters changed:", filters); // Commented out to reduce console noise
    };

    const handleImageView = (image: SatelliteImage) => {
        setSelectedImage(image);
        setIsImageViewerOpen(true);
    };

    const handleCloseImageViewer = () => {
        setIsImageViewerOpen(false);
        setSelectedImage(null);
    };

    const handleToggleFavorite = (imageId: string) => {
        setFavoriteImages((prevFavorites) =>
            prevFavorites.includes(imageId)
                ? prevFavorites.filter(id => id !== imageId)
                : [...prevFavorites, imageId]
        );
        // Persistence is handled by the useEffect watching favoriteImages
    };

    const handleRemoveImage = async (imageId: string) => {
        if (!projectId) {
            setImageActionError('Invalid project ID.');
            return;
        }
        if (!window.confirm('Are you sure you want to remove this image from the project? This might affect related analyses.')) {
            return;
        }

        setImageActionError(null);
        setImageActionSuccess(null);
        try {
            // TODO: Replace with actual API call
            // await imagesService.removeImageFromProject(projectId, imageId);
            console.log('Simulating: Remove image from project:', imageId);

            // Optimistically update UI
            setSatelliteImages((prevImages) => prevImages.filter((image) => image.id !== imageId));
            setImageActionSuccess('Image removed successfully (simulated).');
            // Optionally refetch images: await fetchImages();
        } catch (error: any) {
            console.error('Error removing image:', error);
            setImageActionError(error.message || 'Failed to remove image.');
        }
    };

    const handleSaveAnnotations = async (imageId: string, annotations: any[]) => {
        if (!projectId) {
            setImageActionError('Invalid project ID.');
            return;
        }
        setImageActionError(null);
        setImageActionSuccess(null);
        try {
            // TODO: Replace with actual API call to save annotations
            // await imagesService.saveImageAnnotations(projectId, imageId, annotations);
            console.log('Simulating: Save annotations for image:', imageId, annotations);

            // Optimistically update local state (if annotations are stored on the image object)
            // This depends on how annotations are structured and stored
            // const updatedImages = satelliteImages.map(img =>
            //     img.id === imageId ? { ...img, annotations } : img
            // );
            // setSatelliteImages(updatedImages);

            setIsImageViewerOpen(false); // Close modal on save
            setImageActionSuccess('Annotations saved successfully (simulated).');
        } catch (error: any) {
            console.error('Error saving annotations:', error);
            setImageActionError('Failed to save annotations.');
        }
    };

    // --- Upload Logic ---

    const handleOpenUploadModal = () => {
        setIsUploadModalOpen(true);
        setUploadError(null); // Clear errors when opening
    };

    const handleCloseUploadModal = () => {
        setIsUploadModalOpen(false);
        setUploadError(null);
    };

    const handleFileUpload = async (files: FileList | null) => {
        if (!files || files.length === 0) {
            setUploadError('Please select at least one file to upload.');
            return;
        }
        if (!projectId) {
            setUploadError('Project ID is missing. Cannot upload image.');
            return;
        }

        setIsUploading(true);
        setUploadError(null);
        setImageActionSuccess(null); // Use image action success for upload feedback

        // Assuming single file upload for simplicity based on original code
        const file = files[0];
        const formData = new FormData();
        formData.append('image', file);
        formData.append('projectId', projectId);
        formData.append('imageName', file.name);

        const metadata = {
            description: `Uploaded image: ${file.name}`,
            originalFilename: file.name,
            fileSize: file.size,
            mimeType: file.type,
            uploadTimestamp: new Date().toISOString()
        };
        formData.append('metadata', JSON.stringify(metadata));
        formData.append('storageType', 'filesystem'); // Or determine dynamically

        console.log('Uploading file:', file.name, 'size:', file.size, 'type:', file.type);

        try {
            const response = await imagesService.uploadImage(formData);
            console.log('Upload response:', response);
            setImageActionSuccess('Image uploaded successfully!');
            await fetchImages(); // Refresh the image list
            handleCloseUploadModal(); // Close modal on success
        } catch (error: any) {
            console.error('Error uploading file:', error);
            // More specific error handling
            if (error.response?.status === 413 || error.message.includes('413')) {
                setUploadError('File size too large. Please upload a smaller file.');
            } else if (error.response?.status === 415 || error.message.includes('415')) {
                setUploadError('Unsupported file type. Please upload a valid image file.');
            } else if (error.response?.status === 401 || error.message.includes('401')) {
                setUploadError('Unauthorized. Please log in again.');
            } else if (error.response?.status === 403 || error.message.includes('403')) {
                setUploadError('Access denied. You do not have permission to upload images.');
            } else {
                setUploadError(error.message || 'Failed to upload image. Please try again.');
            }
        } finally {
            setIsUploading(false);
        }
    };

    // Wrapper for DragDropUpload component if needed
    const handleFileUploadWrapper = async (uploadedFiles: File[]) => {
        if (!uploadedFiles || uploadedFiles.length === 0) {
            setUploadError('No files provided by the uploader component.');
            return;
        }
        const dataTransfer = new DataTransfer();
        uploadedFiles.forEach(file => dataTransfer.items.add(file));
        await handleFileUpload(dataTransfer.files);
    };


    return {
        // State
        satelliteImages,
        isLoadingImages,
        imageFetchError,
        imageActionError,
        imageActionSuccess,
        imageFilters,
        availableTags,
        favoriteImages,
        selectedImage,
        isImageViewerOpen,
        isUploadModalOpen,
        isUploading,
        uploadError,

        // Handlers
        fetchImages, // Expose refetch function
        handleFilterChange,
        handleImageView,
        handleCloseImageViewer,
        handleToggleFavorite,
        handleRemoveImage,
        handleSaveAnnotations,
        handleOpenUploadModal,
        handleCloseUploadModal,
        handleFileUpload, // Direct handler if needed
        handleFileUploadWrapper, // Wrapper for component
    };
}
