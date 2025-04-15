'use client';

import { useState, useEffect } from 'react'
import { useParams, useRouter } from 'next/navigation'
import Link from 'next/link'
import { projectsService, ProjectSharingRequest } from '@/services/projects.service'
import { Project, ProjectStatus } from '@/types/api'
import { imagesService, ImageFilter, Image } from '@/services/images.service'
import { SatelliteImage, ImageAnnotation } from '@/types/image'
import {
  ArrowLeftIcon,
  PhotoIcon,
  TagIcon,
  UserGroupIcon,
  ClockIcon,
  ArchiveBoxIcon,
  TrashIcon,
  PencilIcon,
} from '@heroicons/react/24/outline'
import Modal from '@/components/Modal' // Import the Modal component
import ImageGrid from '@/components/ImageGrid/ImageGrid'
import ImageFilterComponent from '@/components/ImageGrid/ImageFilter'
import ImageAnnotationDialog from '@/components/ImageGrid/ImageAnnotation'
import DragDropUpload from '@/components/ImageUpload/DragDropUpload'
import { 
  Box, 
  Button, 
  Typography, 
  Paper, 
  Tabs, 
  Tab, 
  Chip, 
  TextField, 
  Stack,
  Divider,
  ToggleButton,
  ToggleButtonGroup
} from '@mui/material'

export default function ProjectDetailPage() {
    const params = useParams()
    const projectId = params?.id;
    const router = useRouter()
    const [project, setProject] = useState<Project | null>(null)
    const [images, setImages] = useState<SatelliteImage[]>([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [retryCount, setRetryCount] = useState(0)
    const maxRetries = 3
    const retryDelays = [1000, 2000, 3000]
    const [sharingEmail, setSharingEmail] = useState('')
    const [sharingError, setSharingError] = useState('')
    const [sharingSuccess, setSharingSuccess] = useState('')
    const [isAddImageModalOpen, setIsAddImageModalOpen] = useState(false)
    const [isUploadModalOpen, setIsUploadModalOpen] = useState(false)
    const [allImages, setAllImages] = useState<SatelliteImage[]>([])
    const [selectedImages, setSelectedImages] = useState<string[]>([])
    const [satelliteImages, setSatelliteImages] = useState<SatelliteImage[]>([])
    const [imageFilters, setImageFilters] = useState<ImageFilter>({})
    const [availableTags, setAvailableTags] = useState<string[]>([])
    const [favoriteImages, setFavoriteImages] = useState<string[]>([])
    const [selectedImage, setSelectedImage] = useState<SatelliteImage | null>(null)
    const [isAnnotationModalOpen, setIsAnnotationModalOpen] = useState(false)
    const [activeTab, setActiveTab] = useState(0)
    const [isLoadingImages, setIsLoadingImages] = useState(false)
    const [isUploading, setIsUploading] = useState(false)
    const [uploadError, setUploadError] = useState<string | null>(null)
    const [addImageMode, setAddImageMode] = useState<'select' | 'upload'>('select')
    const [success, setSuccess] = useState<string | null>(null)

    // Define fetchImages outside of useEffect so it can be called from other functions
    const fetchImages = async () => {
        if (!projectId) {
            console.warn('No project ID available for fetching images');
            return;
        }

        setLoading(true);
        setError(null);

        try {
            console.log('Fetching images for project:', projectId);
            
            // First try to get project-specific images
            const projectImages = await imagesService.getImagesByProject(projectId, {
                sortBy: 'uploadDate',
                sortOrder: 'desc'
            });
            
            console.log('Project images response:', projectImages);
            
            if (projectImages && projectImages.length > 0) {
                setSatelliteImages(projectImages);
                
                // Extract unique tags from all images
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
            
            // Handle specific error cases
            if (error.message && error.message.includes('No static resource')) {
                console.warn('Images endpoint not implemented on backend or no images found');
                setSatelliteImages([]);
                setAvailableTags([]);
                setError('No images found for this project. You can upload images using the "Upload Image" button.');
            } else {
                setError('Failed to fetch images. Please try again later.');
                // Set empty arrays to prevent UI issues
                setSatelliteImages([]);
                setAvailableTags([]);
            }
        } finally {
            setLoading(false);
        }
    };

    if (typeof projectId !== 'string' || !projectId) {
        return (
            <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white flex items-center justify-center">
                <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded-md">
                    Invalid project ID
                </div>
            </div>
        );
    }

    useEffect(() => {
        const fetchProjectData = async () => {
            if (!projectId) {
                setError('Invalid project ID');
                setLoading(false);
                return;
            }

            setLoading(true);
            setError(null);

            try {
                const projectData = await projectsService.getProject(projectId);
                if (projectData) {
                    setProject(projectData);
                    setRetryCount(0);
                } else {
                    setError('Project not found');
                }
            } catch (err: any) {
                console.error('Error fetching project data:', err);
                const isRateLimitError = err.message?.includes('429');
                
                if (isRateLimitError && retryCount < maxRetries) {
                    const delay = retryDelays[retryCount];
                    console.log(`Rate limited. Retrying in ${delay}ms... (Attempt ${retryCount + 1}/${maxRetries})`);
                    setError(`Rate limit exceeded. Retrying in ${delay / 1000} seconds... (Attempt ${retryCount + 1}/${maxRetries})`);
                    setRetryCount(prev => prev + 1);
                    setTimeout(() => fetchProjectData(), delay);
                    return;
                }
                
                setError(isRateLimitError ? 'Rate limit exceeded. Please try again later.' : 
                    err.message || 'Failed to fetch project data');
                setRetryCount(0);
            } finally {
                if (!error?.includes('Retrying')) {
                    setLoading(false);
                }
            }
        };

        fetchProjectData();
    }, [projectId, retryCount]);

    // Only fetch images if we have project data and its ID
    useEffect(() => {
        if (!project?.id) return;

        let isMounted = true;

        const timeoutId = setTimeout(() => {
            fetchImages();
        }, 300);

        // Cleanup function to revoke object URLs when component unmounts
        return () => {
            isMounted = false;
            clearTimeout(timeoutId);
            // Cleanup object URLs
            satelliteImages.forEach(image => {
                if (image.url && image.url.startsWith('blob:')) {
                    URL.revokeObjectURL(image.url);
                }
                if (image.thumbnailUrl && image.thumbnailUrl.startsWith('blob:')) {
                    URL.revokeObjectURL(image.thumbnailUrl);
                }
            });
        };
    }, [project?.id, projectId]);

    const handleImageSelection = (imageId: string) => {
        setSelectedImages((prevSelectedImages) =>
            prevSelectedImages.includes(imageId)
                ? prevSelectedImages.filter((id) => id !== imageId)
                : [...prevSelectedImages, imageId]
        )
    }

    const handleConfirmAddImages = async () => {
        if (!projectId || typeof projectId !== 'string') {
            setError('Invalid project ID.')
            return
        }
        try {
            for (const imageId of selectedImages) {
                // We are not able to modify the backend, so we are simulating the adding of images
                // await projectsService.addImageToProject(projectId, imageId);
            }
            //Refetch the images
            const updatedImages = await imagesService.getImagesByProject(projectId);
            setSatelliteImages(updatedImages);
            setSelectedImages([]) // Clear selected images
            setIsAddImageModalOpen(false) // Close modal
        } catch (error: any) {
            setError(error.message || 'Failed to add images.')
        }
    }

    const handleRemoveImage = async (imageId: string) => {
        if (!projectId || typeof projectId !== 'string') {
            setError('Invalid project ID.')
            return
        }
        try {
            // await projectsService.removeImageFromProject(projectId, imageId); // No backend support
            // Update the images state to remove the deleted image
            setImages((prevImages) => prevImages.filter((image) => image.id !== imageId))
        } catch (error: any) {
            setError(error.message || 'Failed to remove image.')
        }
    }

    const handleShareProject = async () => {
        setSharingError('')
        setSharingSuccess('')
        if (!projectId) {
            setSharingError('Project ID is missing.')
            return
        }

        if (!sharingEmail) {
            setSharingError('Please enter an email address.')
            return
        }

        const request: ProjectSharingRequest = {
            projectId: projectId,
            otherEmail: sharingEmail,
        }

        try {
            await projectsService.shareProject(request)
            setSharingSuccess('Project shared successfully!')
            setSharingEmail('')

            // Refetch project to update sharedUsers
            const updatedProject = await projectsService.getProject(projectId)
            setProject(updatedProject)
        } catch (error: any) {
            setSharingError(error.message || 'Failed to share project.')
        }
    }

    const handleUnshareProject = async (emailToUnshare: string) => {
        setSharingError('')
        setSharingSuccess('')

        if (!projectId || typeof projectId !== 'string') {
            setSharingError('Invalid project ID.')
            return
        }

        const request: ProjectSharingRequest = {
            projectId: projectId,
            otherEmail: emailToUnshare,
        }

        try {
            await projectsService.unshareProject(request)
            setSharingSuccess(`Project unshared with ${emailToUnshare}.`)

            // Refetch project to update sharedUsers
            const updatedProject = await projectsService.getProject(projectId)
            setProject(updatedProject)
        } catch (error: any) {
            setSharingError(error.message || 'Failed to unshare project.')
        }
    }

    const handleArchiveProject = async () => {
        if (!projectId || typeof projectId !== 'string') {
            setError('Invalid project ID.')
            return
        }

        try {
            await projectsService.archiveProject(projectId)
            setProject((prevProject) => {
                if (!prevProject) return null
                return {
                    ...prevProject,
                    status: ProjectStatus.ARCHIVED,
                }
            })
        } catch (error: any) {
            setError(error.message || 'Failed to archive project.')
        }
    }

    const handleUnarchiveProject = async () => {
        if (!projectId || typeof projectId !== 'string') {
            setError('Invalid project ID.')
            return
        }

        try {
            await projectsService.unarchiveProject(projectId)
            setProject((prevProject) => {
                if (!prevProject) return null
                return {
                    ...prevProject,
                    status: ProjectStatus.ACTIVE,
                }
            })
        } catch (error: any) {
            setError(error.message || 'Failed to unarchive project.')
        }
    }

    const handleDeleteProject = async () => {
        if (!projectId || typeof projectId !== 'string') return;

        if (window.confirm('Are you sure you want to delete this project? This action cannot be undone.')) {
            try {
                await projectsService.deleteProject(projectId);
                router.push('/projects');
            } catch (err) {
                setError(err instanceof Error ? err.message : 'Failed to delete project');
            }
        }
    };

    const handleFilterChange = (filters: ImageFilter) => {
        setImageFilters(filters);
    };
    
    const handleImageSelect = (image: SatelliteImage) => {
        setSelectedImage(image);
    };
    
    const handleAnnotateImage = (image: SatelliteImage) => {
        setSelectedImage(image);
        setIsAnnotationModalOpen(true);
    };
    
    const handleToggleFavorite = (imageId: string) => {
        let newFavorites: string[];
        
        if (favoriteImages.includes(imageId)) {
            newFavorites = favoriteImages.filter(id => id !== imageId);
        } else {
            newFavorites = [...favoriteImages, imageId];
        }
        
        setFavoriteImages(newFavorites);
        localStorage.setItem('favoriteImages', JSON.stringify(newFavorites));
    };
    
    const handleSaveAnnotations = async (imageId: string, annotations: ImageAnnotation[]) => {
        try {
            const updatedImages = satelliteImages.map(img => 
                img.id === imageId ? { ...img, annotations } : img
            );
            setSatelliteImages(updatedImages);
            setIsAnnotationModalOpen(false);
        } catch (error) {
            console.error('Error saving annotations:', error);
        }
    };
    
    const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
        setActiveTab(newValue);
    };

    const handleProjectClick = (projectId: string | number) => {
        router.push(`/projects/${projectId}`);
    };

    const handleAnalysisClick = (projectId: string | number | undefined) => {
        if (projectId) {
        router.push(`/analysis?projectId=${String(projectId)}`);
        }
    };

    const handleFileUpload = async (files: FileList | null) => {
        if (!files || files.length === 0) {
            setError('Please select at least one file to upload');
            return;
        }

        setIsUploading(true);
        setError(null);
        setSuccess(null);

        try {
            const formData = new FormData();
            formData.append('image', files[0]);
            formData.append('projectId', projectId as string);
            
            // Add image name
            formData.append('imageName', files[0].name);
            
            // Add metadata
            const metadata = {
                description: `Uploaded image: ${files[0].name}`,
                originalFilename: files[0].name,
                fileSize: files[0].size,
                mimeType: files[0].type
            };
            formData.append('metadata', JSON.stringify(metadata));
            
            // Add storage type
            formData.append('storageType', 'filesystem');

            console.log('Uploading file:', files[0].name, 'size:', files[0].size, 'type:', files[0].type);
            
            // Log FormData entries for debugging
            for (const [key, value] of formData.entries()) {
                console.log(`FormData entry - ${key}:`, value instanceof File ? `File: ${value.name}, size: ${value.size} bytes` : value);
            }

            const response = await imagesService.uploadImage(formData);
            console.log('Upload response:', response);
            
            setSuccess('Image uploaded successfully');
            setIsUploading(false);
            
            // Refresh the images list
            fetchImages();
        } catch (error: any) {
            console.error('Error uploading file:', error);
            
            // Handle specific error cases
            if (error.message && error.message.includes('No files selected')) {
                setError('Please select a file to upload');
            } else if (error.message && error.message.includes('No project ID')) {
                setError('Project ID is missing. Please try again.');
            } else if (error.message && error.message.includes('413')) {
                setError('File size too large. Please try a smaller file.');
            } else if (error.message && error.message.includes('415')) {
                setError('Unsupported file type. Please upload an image file (JPG, PNG, GIF, TIFF).');
            } else if (error.message && error.message.includes('401')) {
                setError('Unauthorized. Please log in again.');
            } else if (error.message && error.message.includes('403')) {
                setError('Access denied. You do not have permission to upload images.');
            } else if (error.message && error.message.includes('Failed to parse multipart')) {
                setError('Server error during image upload. The server could not process the image. Please try again later or contact support.');
            } else {
                setError(error.message || 'Failed to upload image. Please try again.');
            }
            
            setIsUploading(false);
        }
    };

    // Convert FileList to File[] for DragDropUpload
    const handleFileUploadWrapper = async (files: File[]) => {
        if (!files || files.length === 0) {
            setError('Please select at least one file to upload');
            return;
        }
        // Create a FileList-like object
        const fileList = {
            0: files[0],
            length: 1,
            item: (index: number) => files[index]
        } as FileList;
        
        await handleFileUpload(fileList);
    };

    if (loading) {
        return (
            <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white flex items-center justify-center">
                <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary-600"></div>
            </div>
        );
    }

    if (error && !error.includes('Retrying')) {
        return (
            <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white flex items-center justify-center">
                <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded-md">
                    <p>{error}</p>
                    <button
                        onClick={() => window.location.reload()}
                        className="mt-4 bg-red-200 text-red-700 px-4 py-2 rounded hover:bg-red-300 transition-colors"
                    >
                        Try Again
                    </button>
                </div>
            </div>
        );
    }

    if (!project) {
        return (
            <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white flex items-center justify-center">
                <div className="bg-yellow-100 border border-yellow-400 text-yellow-700 px-4 py-3 rounded-md">
                    Project not found
                </div>
            </div>
        );
    }

    const statusColors = {
        [ProjectStatus.ACTIVE]: 'bg-green-100 text-green-800',
        [ProjectStatus.COMPLETED]: 'bg-blue-100 text-blue-800',
        [ProjectStatus.ARCHIVED]: 'bg-gray-100 text-gray-800',
        [ProjectStatus.DRAFT]: 'bg-yellow-100 text-yellow-800',
    };

    const ProjectCard = ({ project }: { project: Project }) => {
        // Make absolutely sure project has the required properties
        if (!project) return null;
        
        // Ensure we have a valid string ID
        const projectId = typeof project.id === 'object' 
            ? `temp-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`
            : String(project.id || `project-${project.name}-${Date.now()}`);
        
        return (
            <div 
                onClick={() => handleProjectClick(projectId)}
                className="cursor-pointer block bg-white rounded-lg shadow-sm p-6 hover:shadow-md transition-shadow"
            >
                <h3 className="font-medium text-gray-900">{project.name || 'Unnamed Project'}</h3>
                <p className="text-sm text-gray-500 mt-1">{project.description || 'No description'}</p>
                <div className="flex items-center mt-2 text-sm text-gray-500">
                    <ClockIcon className="h-4 w-4 mr-1" />
                    <span>Updated {new Date(project.updatedAt || Date.now()).toLocaleDateString()}</span>
                </div>
            </div>
        );
    };

    return (
        <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                <div className="mb-8">
                    <Link
                        href="/projects"
                        className="inline-flex items-center text-sm text-gray-500 hover:text-gray-700"
                    >
                        <ArrowLeftIcon className="h-4 w-4 mr-1" />
                        Back to Projects
                    </Link>
                    <div className="mt-4 flex items-center justify-between">
                        <div>
                            <h1 className="text-2xl font-bold text-gray-900">{project.name}</h1>
                            <p className="mt-1 text-sm text-gray-500">{project.description}</p>
                        </div>
                        <div className="flex items-center gap-2">
                            <Link
                                href={project && project.id && typeof project.id !== 'object' 
                                    ? `/projects/${String(project.id)}/edit` 
                                    : '/projects'}
                                className="inline-flex items-center px-3 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
                            >
                                <PencilIcon className="h-4 w-4 mr-1" />
                                Edit
                            </Link>
                            {project.status === ProjectStatus.ARCHIVED ? (
                                <button
                                    onClick={handleUnarchiveProject}
                                    className="inline-flex items-center px-3 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
                                >
                                    <ArchiveBoxIcon className="h-4 w-4 mr-1" />
                                    Unarchive
                                </button>
                            ) : (
                                <button
                                    onClick={handleArchiveProject}
                                    className="inline-flex items-center px-3 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
                                >
                                    <ArchiveBoxIcon className="h-4 w-4 mr-1" />
                                    Archive
                                </button>
                            )}
                            <button
                                onClick={handleDeleteProject}
                                className="inline-flex items-center px-3 py-2 border border-red-300 rounded-md text-sm font-medium text-red-700 bg-white hover:bg-red-50"
                            >
                                <TrashIcon className="h-4 w-4 mr-1" />
                                Delete
                            </button>
                        </div>
                    </div>
                </div>

                <Paper sx={{ width: '100%', mb: 4 }}>
                    <Tabs
                        value={activeTab}
                        onChange={handleTabChange}
                        indicatorColor="primary"
                        textColor="primary"
                        variant="fullWidth"
                    >
                        <Tab label="Project Details" />
                        <Tab label="Images" />
                        <Tab label="Analysis Results" />
                    </Tabs>
                </Paper>

                {/* Project Details Tab */}
                {activeTab === 0 && (
                    <Stack direction={{ xs: 'column', md: 'row' }} spacing={4}>
                        <Box sx={{ width: { xs: '100%', md: '50%' } }}>
                            <Paper sx={{ p: 3, height: '100%' }}>
                                <Typography variant="h6" gutterBottom>
                                    Project Information
                                </Typography>
                                <Box sx={{ mt: 2 }}>
                                    <Typography variant="body2" color="text.secondary">
                                        <strong>Created:</strong> {new Date(project.createdAt).toLocaleDateString()}
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary">
                                        <strong>Last Updated:</strong> {new Date(project.updatedAt).toLocaleDateString()}
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary">
                                        <strong>Status:</strong> {project.status}
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary">
                                        <strong>Owner:</strong> {project.owner}
                                    </Typography>
                                    {project.metadata?.location && (
                                        <Typography variant="body2" color="text.secondary">
                                            <strong>Location:</strong> {project.metadata.location.lat}, {project.metadata.location.lng}
                                        </Typography>
                                    )}
                                </Box>
                            </Paper>
                        </Box>

                        <Box sx={{ width: { xs: '100%', md: '50%' } }}>
                            <Paper sx={{ p: 3, height: '100%' }}>
                                <Typography variant="h6" gutterBottom>
                                    Sharing & Collaboration
                                </Typography>
                                <Box sx={{ mt: 2 }}>
                                    <Typography variant="body2" gutterBottom>
                                        Share this project with others:
                                    </Typography>
                                    <div className="flex space-x-2 mt-2">
                                        <input
                                            type="email"
                                            value={sharingEmail}
                                            onChange={(e) => setSharingEmail(e.target.value)}
                                            placeholder="Enter email address"
                                            className="flex-1 px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                                        />
                                        <button
                                            onClick={handleShareProject}
                                            className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-primary-600 hover:bg-primary-700"
                                        >
                                            Share
                                        </button>
                                    </div>
                                    {sharingError && (
                                        <p className="mt-2 text-sm text-red-600">{sharingError}</p>
                                    )}
                                    {sharingSuccess && (
                                        <p className="mt-2 text-sm text-green-600">{sharingSuccess}</p>
                                    )}

                                    <div className="mt-4">
                                        <Typography variant="body2" gutterBottom>
                                            Collaborators:
                                        </Typography>
                                        {project.collaborators && project.collaborators.length > 0 ? (
                                            <div className="space-y-2 mt-2">
                                                {project.collaborators.map((email) => (
                                                    <div
                                                        key={`collaborator-${email}-${Date.now()}`}
                                                        className="flex justify-between items-center p-2 bg-gray-50 rounded-md"
                                                    >
                                                        <span>{email}</span>
                                                        <button
                                                            onClick={(e) => {
                                                                e.stopPropagation();
                                                                handleUnshareProject(email);
                                                            }}
                                                            className="text-red-600 hover:text-red-800"
                                                        >
                                                            Remove
                                                        </button>
                                                    </div>
                                                ))}
                                            </div>
                                        ) : (
                                            <p className="text-sm text-gray-500">No collaborators yet</p>
                                        )}
                                    </div>
                                </Box>
                            </Paper>
                        </Box>
                    </Stack>
                )}

                {/* Images Tab */}
                {activeTab === 1 && (
                    <div>
                        <ImageFilterComponent 
                            availableTags={availableTags}
                            onFilterChange={handleFilterChange}
                            initialFilters={imageFilters}
                        />
                        
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
                            <Typography variant="h6">
                                Project Images ({satelliteImages.length})
                            </Typography>
                            <Button 
                                variant="contained" 
                                color="primary"
                                onClick={() => setIsAddImageModalOpen(true)}
                            >
                                Add Images
                            </Button>
                        </Box>

                        {isLoadingImages ? (
                            <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
                                <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary-600"></div>
                            </Box>
                        ) : error ? (
                            <Box sx={{ textAlign: 'center', py: 8 }}>
                                <Typography color="error" gutterBottom>
                                    {error}
                                </Typography>
                                <Button 
                                    onClick={() => window.location.reload()} 
                                    variant="outlined" 
                                    color="primary"
                                    sx={{ mt: 2 }}
                                >
                                    Retry
                                </Button>
                            </Box>
                        ) : satelliteImages.length === 0 ? (
                            <Box sx={{ 
                                textAlign: 'center', 
                                py: 8,
                                px: 4,
                                bgcolor: 'background.paper',
                                borderRadius: 1,
                                boxShadow: 1
                            }}>
                                <PhotoIcon className="h-12 w-12 mx-auto text-gray-400 mb-4" />
                                <Typography variant="h6" gutterBottom>
                                    No Images Yet
                                </Typography>
                                <Typography variant="body2" color="text.secondary" sx={{ mb: 4 }}>
                                    This project doesn't have any images yet. Click "Add Images" to get started.
                                </Typography>
                                <Button 
                                    variant="contained" 
                                    color="primary"
                                    onClick={() => setIsAddImageModalOpen(true)}
                                >
                                    Add Your First Image
                                </Button>
                            </Box>
                        ) : (
                            <ImageGrid 
                                images={satelliteImages}
                                loading={loading}
                                onSelectImage={handleImageSelect}
                                onAnnotateImage={handleAnnotateImage}
                                onToggleFavorite={handleToggleFavorite}
                                favorites={favoriteImages}
                                onDeleteImage={(imageId) => handleRemoveImage(imageId)}
                            />
                        )}
                    </div>
                )}

                {/* Analysis Results Tab */}
                {activeTab === 2 && (
                    <div>
                        <Box sx={{ textAlign: 'center', py: 8 }}>
                            <Typography variant="h6" gutterBottom>
                                No analysis results yet
                            </Typography>
                            <Typography variant="body2" color="text.secondary" sx={{ mb: 4 }}>
                                Run an analysis on this project to see results here
                            </Typography>
                            <button
                                onClick={() => project?.id ? handleAnalysisClick(project.id) : null}
                                className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-primary-600 hover:bg-primary-700"
                            >
                                Start New Analysis
                            </button>
                        </Box>
                    </div>
                )}

                {/* Add Image Modal */}
                <Modal
                    open={isAddImageModalOpen}
                    onClose={() => setIsAddImageModalOpen(false)}
                    title="Add Image"
                    content={
                        <div className="space-y-4">
                            {satelliteImages.length > 0 ? (
                                <>
                                    <div className="grid grid-cols-2 gap-4">
                                        {satelliteImages.map((image) => (
                                            <div
                                            key={image.id}
                                                className={`border rounded-lg p-2 cursor-pointer ${
                                                    selectedImage?.id === image.id ? 'border-blue-500 bg-blue-50' : ''
                                                }`}
                                                onClick={() => setSelectedImage(image)}
                                        >
                                            <img
                                                    src={image.thumbnailUrl || image.url}
                                                    alt={image.filename || 'Satellite image'}
                                                    className="w-full h-32 object-cover rounded"
                                                />
                                                <div className="mt-2 text-sm">
                                                    <p className="font-medium truncate">{image.filename || 'Unnamed image'}</p>
                                                    <p className="text-gray-500 text-xs">
                                                        {new Date(image.uploadDate).toLocaleDateString()}
                                                    </p>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </>
                            ) : (
                                <div className="text-center py-8">
                                    <p className="text-gray-500 mb-4">No images available to add to this project.</p>
                                    <p className="text-sm text-gray-400 mb-4">
                                        {error && error.includes('not available') 
                                            ? 'The image service is not available. The backend does not support image operations at this time.'
                                            : 'You can upload new images using the upload button above.'}
                                    </p>
                                </div>
                            )}
                        </div>
                    }
                    actions={[
                        {
                            label: "Cancel",
                            onClick: () => setIsAddImageModalOpen(false),
                            color: "inherit"
                        },
                        {
                            label: "Switch to Upload Mode",
                            onClick: () => {
                                setIsAddImageModalOpen(false);
                                setIsUploadModalOpen(true);
                            },
                            color: "primary",
                            disabled: false
                        },
                        {
                            label: "Add Selected Image",
                            onClick: handleConfirmAddImages,
                            color: "primary",
                            disabled: !selectedImage
                        }
                    ]}
                />

                {/* Upload Image Modal */}
                <Modal
                    open={isUploadModalOpen}
                    onClose={() => setIsUploadModalOpen(false)}
                    title="Upload Image"
                    content={
                        <div className="space-y-4">
                            <DragDropUpload
                                onUpload={handleFileUploadWrapper}
                                isUploading={isUploading}
                                acceptedFileTypes="image/*"
                                maxFiles={5}
                                maxFileSizeMB={10}
                            />
                            {uploadError && (
                                <div className="text-red-500 text-sm mt-2">
                                    {uploadError}
                                </div>
                            )}
                        </div>
                    }
                    actions={[
                        {
                            label: "Cancel",
                            onClick: () => setIsUploadModalOpen(false),
                            color: "inherit"
                        }
                    ]}
                />

                {/* Image Annotation Dialog */}
                <ImageAnnotationDialog
                    open={isAnnotationModalOpen}
                    onClose={() => setIsAnnotationModalOpen(false)}
                    image={selectedImage}
                    onSave={handleSaveAnnotations}
                />
            </div>
        </div>
    )
}
