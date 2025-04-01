'use client';

import { useState, useEffect } from 'react'
import { useParams } from 'next/navigation'
import Link from 'next/link'
import {
  projectsService,
  Project,
  ProjectSharingRequest,
} from '@/services/projects.service'
import { Image, ImageFilter } from '@/services/images.service'
import { imagesService } from '@/services/images.service'
import { SatelliteImage, ImageAnnotation } from '@/types/image'
import Modal from '@/components/Modal' // Import the Modal component
import ImageGrid from '@/components/ImageGrid/ImageGrid'
import ImageFilterComponent from '@/components/ImageGrid/ImageFilter'
import ImageAnnotationDialog from '@/components/ImageGrid/ImageAnnotation'
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
  Divider
} from '@mui/material'

export default function ProjectDetailPage() {
  const { id } = useParams()
  const [project, setProject] = useState<Project | null>(null)
  const [images, setImages] = useState<Image[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [sharingEmail, setSharingEmail] = useState('')
  const [sharingError, setSharingError] = useState('')
  const [sharingSuccess, setSharingSuccess] = useState('')
  const [isAddImageModalOpen, setIsAddImageModalOpen] = useState(false)
  const [allImages, setAllImages] = useState<Image[]>([]) // To store all available images
  const [selectedImages, setSelectedImages] = useState<string[]>([]) // Image IDs to be added
  const [satelliteImages, setSatelliteImages] = useState<SatelliteImage[]>([])
  const [imageFilters, setImageFilters] = useState<ImageFilter>({})
  const [availableTags, setAvailableTags] = useState<string[]>([])
  const [favoriteImages, setFavoriteImages] = useState<string[]>([])
  const [selectedImage, setSelectedImage] = useState<SatelliteImage | null>(null)
  const [isAnnotationModalOpen, setIsAnnotationModalOpen] = useState(false)
  const [activeTab, setActiveTab] = useState(0)

  useEffect(() => {
    const fetchProjectData = async () => {
      if (id && typeof id === 'string') {
        setLoading(true)
        try {
          const [projectData, imagesData] = await Promise.all([
            projectsService.getProject(id),
            projectsService.getImagesByProject(id),
          ])
          setProject(projectData)
          setImages(imagesData)
        } catch (error: any) {
          setError(error.message || 'Failed to fetch project data.')
        } finally {
          setLoading(false)
        }
      }
    }

    fetchProjectData()
  }, [id])

  // Fetch all images for the "Add Image" modal
  useEffect(() => {
    const fetchAllImages = async () => {
      if (isAddImageModalOpen) {
        // Only fetch when modal is open
        try {
          const imagesData = await imagesService.getAllImages()
          setAllImages(imagesData)
        } catch (error: any) {
          setError(error.message || 'Failed to fetch all images.')
        }
      }
    }
    fetchAllImages()
  }, [isAddImageModalOpen])

  // Fetch satellite images with filters
  useEffect(() => {
    const fetchSatelliteImages = async () => {
      if (id && typeof id === 'string') {
        try {
          const satelliteData = await imagesService.getImagesByProject(id, imageFilters);
          setSatelliteImages(satelliteData);
          
          // Extract all unique tags from images
          const tags = new Set<string>();
          satelliteData.forEach(img => {
            img.tags.forEach(tag => tags.add(tag));
          });
          setAvailableTags(Array.from(tags));
          
          // Load favorite images from local storage
          const savedFavorites = localStorage.getItem('favoriteImages');
          if (savedFavorites) {
            setFavoriteImages(JSON.parse(savedFavorites));
          }
        } catch (error: any) {
          console.error('Error fetching satellite images:', error);
        }
      }
    };
    
    fetchSatelliteImages();
  }, [id, imageFilters]);

  const handleImageSelection = (imageId: string) => {
    setSelectedImages((prevSelectedImages) =>
      prevSelectedImages.includes(imageId)
        ? prevSelectedImages.filter((id) => id !== imageId)
        : [...prevSelectedImages, imageId]
    )
  }

  const handleConfirmAddImages = async () => {
    if (!id || typeof id !== 'string') {
      setError('Invalid project ID.')
      return
    }
    try {
      for (const imageId of selectedImages) {
        // We are not able to modify the backend, so we are simulating the adding of images
        // await projectsService.addImageToProject(id, imageId);
      }
      //Refetch the images
      const updatedImages = await projectsService.getImagesByProject(id)
      setImages(updatedImages)
      setSelectedImages([]) // Clear selected images
      setIsAddImageModalOpen(false) // Close modal
    } catch (error: any) {
      setError(error.message || 'Failed to add images.')
    }
  }

  const handleRemoveImage = async (imageId: string) => {
    if (!id || typeof id !== 'string') {
      setError('Invalid project ID.')
      return
    }
    try {
      // await projectsService.removeImageFromProject(id, imageId); // No backend support
      // Update the images state to remove the deleted image
      setImages((prevImages) => prevImages.filter((image) => image.id !== imageId))
    } catch (error: any) {
      setError(error.message || 'Failed to remove image.')
    }
  }

  const handleShareProject = async () => {
    setSharingError('')
    setSharingSuccess('')
    if (!id || typeof id !== 'string') {
      setSharingError('Invalid project ID.')
      return
    }
    if (!sharingEmail) {
      setSharingError('Please enter an email address.')
      return
    }

    const request: ProjectSharingRequest = {
      projectId: id,
      otherEmail: sharingEmail,
    }

    try {
      await projectsService.shareProject(request)
      setSharingSuccess('Project shared successfully!')
      setSharingEmail('')

      // Refetch project to update sharedUsers
      const updatedProject = await projectsService.getProject(id)
      setProject(updatedProject)
    } catch (error: any) {
      setSharingError(error.message || 'Failed to share project.')
    }
  }

  const handleUnshareProject = async (emailToUnshare: string) => {
    setSharingError('')
    setSharingSuccess('')

    if (!id || typeof id !== 'string') {
      setSharingError('Invalid project ID.')
      return
    }

    const request: ProjectSharingRequest = {
      projectId: id,
      otherEmail: emailToUnshare,
    }

    try {
      await projectsService.unshareProject(request)
      setSharingSuccess(`Project unshared with ${emailToUnshare}.`)

      // Refetch project to update sharedUsers
      const updatedProject = await projectsService.getProject(id)
      setProject(updatedProject)
    } catch (error: any) {
      setSharingError(error.message || 'Failed to unshare project.')
    }
  }

  const handleArchiveProject = async () => {
    if (!id || typeof id !== 'string') {
      setError('Invalid project ID.')
      return
    }

    try {
      await projectsService.archiveProject(id)
      setProject((prevProject) => {
        if (!prevProject) return null
        return {
          ...prevProject,
          archived: true,
        }
      })
    } catch (error: any) {
      setError(error.message || 'Failed to archive project.')
    }
  }

  const handleUnarchiveProject = async () => {
    if (!id || typeof id !== 'string') {
      setError('Invalid project ID.')
      return
    }

    try {
      await projectsService.unarchiveProject(id)
      setProject((prevProject) => {
        if (!prevProject) return null
        return {
          ...prevProject,
          archived: false,
        }
      })
    } catch (error: any) {
      setError(error.message || 'Failed to unarchive project.')
    }
  }

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
      // In a real implementation, this would call an API to save the annotations
      // For now, we'll just update the local state
      const updatedImages = satelliteImages.map(img => 
        img.id === imageId ? { ...img, annotations } : img
      );
      setSatelliteImages(updatedImages);
    } catch (error) {
      console.error('Error saving annotations:', error);
    }
  };
  
  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setActiveTab(newValue);
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary-600"></div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white flex items-center justify-center">
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded-md">
          {error}
        </div>
      </div>
    )
  }

  if (!project) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white flex items-center justify-center">
        <div className="bg-yellow-100 border border-yellow-400 text-yellow-700 px-4 py-3 rounded-md">
          Project not found
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="mb-6">
          <div className="flex justify-between items-center">
            <h1 className="text-3xl font-bold text-gray-900">{project.name}</h1>
            <div className="flex space-x-2">
              <Link href="/projects" className="inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50">
                Back to Projects
              </Link>
              <Link href={`/analysis?projectId=${project.id}`} className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-primary-600 hover:bg-primary-700">
                Run Analysis
              </Link>
            </div>
          </div>
          <p className="mt-2 text-sm text-gray-500">
            {project.description || 'No description provided'}
          </p>
          <div className="mt-2 flex flex-wrap gap-2">
            {project.metadata?.tags?.map((tag) => (
              <span key={tag} className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                {tag}
              </span>
            ))}
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
                            key={email}
                            className="flex justify-between items-center p-2 bg-gray-50 rounded-md"
                          >
                            <span>{email}</span>
                            <button
                              onClick={() => handleUnshareProject(email)}
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
            
            <ImageGrid 
              images={satelliteImages}
              loading={loading}
              onSelectImage={handleImageSelect}
              onAnnotateImage={handleAnnotateImage}
              onToggleFavorite={handleToggleFavorite}
              favorites={favoriteImages}
              onDeleteImage={(imageId) => handleRemoveImage(imageId)}
            />
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
              <Link href={`/analysis?projectId=${project.id}`} className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-primary-600 hover:bg-primary-700">
                Start New Analysis
              </Link>
            </Box>
          </div>
        )}

        {/* Add Image Modal */}
        <Modal
          open={isAddImageModalOpen}
          onClose={() => setIsAddImageModalOpen(false)}
          title="Add Images to Project"
          content={
            <Box sx={{ p: 2 }}>
              {allImages.length > 0 ? (
                <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr 1fr', md: '1fr 1fr 1fr' }, gap: 2 }}>
                  {allImages.map((image) => (
                    <Box
                      key={image.id}
                      sx={{
                        position: 'relative',
                        border: 1,
                        borderRadius: 1,
                        overflow: 'hidden',
                        cursor: 'pointer',
                        borderColor: selectedImages.includes(image.id) ? 'primary.main' : 'grey.300',
                        boxShadow: selectedImages.includes(image.id) ? 2 : 0
                      }}
                      onClick={() => handleImageSelection(image.id)}
                    >
                      <img
                        src={image.url}
                        alt={image.name}
                        style={{
                          width: '100%',
                          height: '160px',
                          objectFit: 'cover'
                        }}
                      />
                      <Box sx={{ p: 1 }}>
                        <Typography variant="subtitle2" noWrap>
                          {image.name}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {new Date(image.createdAt).toLocaleDateString()}
                        </Typography>
                      </Box>
                      {selectedImages.includes(image.id) && (
                        <Box
                          sx={{
                            position: 'absolute',
                            top: 8,
                            right: 8,
                            bgcolor: 'primary.main',
                            color: 'white',
                            borderRadius: '50%',
                            p: 0.5
                          }}
                        >
                          <svg
                            xmlns="http://www.w3.org/2000/svg"
                            className="h-4 w-4"
                            viewBox="0 0 20 20"
                            fill="currentColor"
                          >
                            <path
                              fillRule="evenodd"
                              d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                              clipRule="evenodd"
                            />
                          </svg>
                        </Box>
                      )}
                    </Box>
                  ))}
                </Box>
              ) : (
                <Typography color="text.secondary" align="center">
                  No images available
                </Typography>
              )}
            </Box>
          }
          actions={[
            {
              label: 'Cancel',
              onClick: () => {
                setIsAddImageModalOpen(false);
                setSelectedImages([]);
              }
            },
            {
              label: 'Add Selected',
              onClick: handleConfirmAddImages,
              color: 'primary',
              disabled: selectedImages.length === 0
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
