'use client';

import { useState, useEffect } from 'react';
import { imagesService } from '@/services/images.service'; // Removed Image import
import { SatelliteImage } from '@/types/image'; // Import SatelliteImage type
import ImageGrid from '@/components/ImageGrid/ImageGrid'; // Import ImageGrid
import Modal from '@/components/Modal'; // Import Modal
import { Box, CircularProgress, Typography, Button } from '@mui/material'; // Import MUI components

export default function StoragePage() {
  const [view, setView] = useState<'grid' | 'list'>('grid');
  const [filter, setFilter] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [items, setItems] = useState<SatelliteImage[]>([]); // Use SatelliteImage[]
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true); // Add loading state
  const [selectedImage, setSelectedImage] = useState<SatelliteImage | null>(null); // State for enlarged image
  const [favorites, setFavorites] = useState<string[]>([]); // State for favorites (basic implementation)

    useEffect(() => {
        const fetchImages = async () => {
            setLoading(true); // Set loading true
            try {
                const images = await imagesService.getAllImages();
                // TODO: Ensure imagesService.getAllImages() returns data compatible with SatelliteImage
                // For now, we assume it does or needs casting/mapping
                setItems(images as SatelliteImage[]); // Cast/assume type
            } catch (error: any) {
                setError(error.message || 'Failed to fetch images.');
            } finally {
                setLoading(false); // Set loading false
            }
        }
        fetchImages();
    }, []);

  // --- Handlers for ImageGrid ---
  const handleSelectImage = (image: SatelliteImage) => {
    setSelectedImage(image);
  };

  const handleDeleteImage = async (imageId: string) => {
    // Optional: Add a confirmation dialog
    // if (!window.confirm('Are you sure you want to delete this image?')) return;
    try {
      setLoading(true); // Indicate loading state
      await imagesService.deleteImage(imageId);
      setItems((prevItems) => prevItems.filter((item) => item.id !== imageId));
      setError(''); // Clear any previous error
    } catch (err: any) {
      setError(err.message || 'Failed to delete image.');
    } finally {
      setLoading(false);
    }
  };

  const handleAnnotateImage = (image: SatelliteImage) => {
    // Placeholder for annotation logic (e.g., navigate to an annotation page or open an annotation modal)
    console.log('Annotate image:', image.id);
    // Example: router.push(`/annotate/${image.id}`);
  };

  const handleToggleFavorite = (imageId: string) => {
    // Placeholder for favorite logic
    setFavorites((prevFavorites) =>
      prevFavorites.includes(imageId)
        ? prevFavorites.filter((id) => id !== imageId)
        : [...prevFavorites, imageId]
    );
    console.log('Toggle favorite for image:', imageId);
    // TODO: Persist favorite status (e.g., call an API)
  };
  // --- End Handlers ---

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files) return;

    setUploading(true);
    setError(''); // Clear previous errors
    try {
      const formData = new FormData();
      for (let i = 0; i < files.length; i++) {
        formData.append('image', files[i]); // Assuming 'image' is the field name expected by the backend
      }

      const uploadedImage = await imagesService.uploadImage(formData);

      // Update the items list with the new image
      // TODO: Ensure uploadedImage is compatible with SatelliteImage
      setItems((prevItems) => [...prevItems, uploadedImage as SatelliteImage]); // Cast to SatelliteImage

    } catch (error: any) {
      setError(error.message || 'Failed to upload image.');

    } finally {
      setUploading(false);
      // Reset the input field
      e.target.value = '';
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
        <div className="text-center mb-12">
          <h1 className="text-4xl tracking-tight font-extrabold text-gray-900 sm:text-5xl md:text-6xl">
            <span className="block">Storage</span>
            <span className="block text-primary-600">Management</span>
          </h1>
          <p className="mt-3 max-w-md mx-auto text-base text-gray-500 sm:text-lg md:mt-5 md:text-xl md:max-w-3xl">
            Manage and organize your satellite imagery and analysis results
          </p>
        </div>

        <div className="bg-white shadow-lg rounded-lg p-8">
          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 mb-8">
            <div className="flex-1">
              <input
                type="text"
                placeholder="Search files..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-md focus:ring-primary-600 focus:border-primary-600"
              />
            </div>
            <div className="flex items-center space-x-4">
              <select
                value={filter}
                onChange={(e) => setFilter(e.target.value)}
                className="px-4 py-2 border border-gray-300 rounded-md focus:ring-primary-600 focus:border-primary-600"
              >
                <option value="all">All Files</option>
                <option value="image">Images</option>
                <option value="data">Data</option>
                <option value="result">Results</option>
              </select>
              <label className="inline-flex items-center px-8 py-3 border border-transparent text-base font-medium rounded-md text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-600 cursor-pointer">
                <span>{uploading ? 'Uploading...' : 'Upload Files'}</span>
                <input
                  type="file"
                  multiple
                  className="hidden"
                  onChange={handleFileUpload}
                  disabled={uploading}
                />
              </label>
            </div>
          </div>
            {error && (
                <div className="mb-4 bg-red-100 border border-red-400 text-red-700 px-4 py-2 rounded-md">
                    {error}
                </div>
            )}

          <div className="flex justify-end mb-6">
            <div className="inline-flex rounded-md shadow-sm">
              <button
                onClick={() => setView('grid')}
                className={`px-4 py-2 text-sm font-medium rounded-l-md border ${
                  view === 'grid'
                    ? 'bg-primary-600 text-white border-primary-600'
                    : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'
                }`}
              >
                Grid View
              </button>
              <button
                onClick={() => setView('list')}
                className={`px-4 py-2 text-sm font-medium rounded-r-md border-t border-r border-b ${
                  view === 'list'
                    ? 'bg-primary-600 text-white border-primary-600'
                    : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'
                }`}
              >
                List View
              </button>
            </div>
          </div>

          {/* --- View Rendering --- */}
          {view === 'grid' ? (
             <ImageGrid
               images={items}
               loading={loading}
               onSelectImage={handleSelectImage}
               onDeleteImage={handleDeleteImage}
               onAnnotateImage={handleAnnotateImage} // Pass annotation handler
               onToggleFavorite={handleToggleFavorite} // Pass favorite handler
               favorites={favorites} // Pass favorites list
             />
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead>
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Name
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Type
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Size
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Uploaded Date {/* Changed from Created */}
                    </th>
                    {/* Removed Project column, add back if needed */}
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {items.length === 0 ? (
                    <tr>
                      <td colSpan={4} className="px-6 py-12 text-center"> {/* Adjusted colSpan */}
                        <p className="text-gray-500 text-lg">No files found</p>
                        <p className="text-gray-400 mt-2">Upload files to get started</p>
                      </td>
                    </tr>
                  ) : (
                    items.map((item) => (
                      <tr key={item.id} className="hover:bg-gray-50">
                        <td className="px-6 py-4 whitespace-nowrap">{item.filename}</td> {/* Use filename */}
                        <td className="px-6 py-4 whitespace-nowrap">Image</td> {/* Static type for now */}
                        <td className="px-6 py-4 whitespace-nowrap">
                          {item.size ? `${(item.size / 1024).toFixed(2)} MB` : 'N/A'} {/* Format size */}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          {new Date(item.uploadDate).toLocaleDateString()} {/* Use uploadDate */}
                        </td>
                        {/* Removed Project cell */}
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          )}
          {/* --- End View Rendering --- */}

        </div>
      </div>

      {/* --- Image Enlargement Modal --- */}
      {selectedImage && (
        <Modal
          open={!!selectedImage}
          onClose={() => setSelectedImage(null)}
          title={selectedImage.filename} // Pass filename as title
          content={ // Pass image element as content
            <Box sx={{ p: 0, maxWidth: '90vw', maxHeight: '80vh', overflow: 'hidden' }}> {/* Adjusted padding/height */}
              <img
                src={selectedImage.url}
                alt={selectedImage.filename}
                style={{ width: '100%', height: 'auto', display: 'block', maxHeight: '80vh' }} // Constrain image height
              />
            </Box>
          }
          actions={[ // Define actions array
            {
              label: 'Close',
              onClick: () => setSelectedImage(null),
              color: 'primary'
            }
          ]}
        />
      )}
      {/* --- End Modal --- */}

    </div>
  );
}
