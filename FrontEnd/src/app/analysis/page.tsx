'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { projectsService } from '@/services/projects.service';
import { imagesService } from '@/services/images.service';
import { Project } from '@/types/api';
import { SatelliteImage } from '@/types/image';
import { analysisService, AnalysisResult } from '@/services/analysis.service';

export default function AnalysisPage() {
  const router = useRouter();
  const [projects, setProjects] = useState<Project[]>([]);
  const [selectedProject, setSelectedProject] = useState<string>('');
  const [images, setImages] = useState<SatelliteImage[]>([]);
  const [selectedImage, setSelectedImage] = useState<string>('');
  const [analysisType, setAnalysisType] = useState<string>('');
  const [vegetationSubType, setVegetationSubType] = useState<string>('');
  const [description, setDescription] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [loadingImages, setLoadingImages] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState<AnalysisResult | null>(null);

  useEffect(() => {
    const fetchProjects = async () => {
      setLoading(true);
      try {
        const projectsResponse = await projectsService.getAllProjects();
        if (projectsResponse && Array.isArray(projectsResponse.content)) {
          setProjects(projectsResponse.content);
        } else {
          console.error('Invalid projects data received:', projectsResponse);
          setProjects([]);
          setError('Failed to load projects data.');
        }
      } catch (error) {
        setError('Failed to fetch projects.');
        setProjects([]);
      } finally {
        setLoading(false);
      }
    };

    fetchProjects();
  }, []);

  useEffect(() => {
    const fetchImages = async () => {
      if (!selectedProject) {
        setImages([]);
        setSelectedImage('');
        return;
      }
      setLoadingImages(true);
      setError('');
      try {
        console.log(`Fetching images for project ID: ${selectedProject}`);
        const projectImages = await imagesService.getImagesByProject(selectedProject);
        console.log('Fetched images:', projectImages);
        setImages(projectImages || []);
        setSelectedImage('');
      } catch (fetchError: any) {
        console.error('Error fetching images:', fetchError);
        setError(`Failed to fetch images for the selected project: ${fetchError.message}`);
        setImages([]);
      } finally {
        setLoadingImages(false);
      }
    };

    fetchImages();
  }, [selectedProject]);

  const handleProjectChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setSelectedProject(e.target.value);
    setImages([]);
    setSelectedImage('');
    setError('');
    setResult(null);
    if (analysisType === 'vegetation') {
      setVegetationSubType('');
    }
  };

  const handleAnalysisTypeChange = (type: string) => {
    setAnalysisType(type);
    if (type !== 'vegetation') {
      setVegetationSubType('');
    }
    setError('');
    setResult(null);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setResult(null);

    if (!selectedProject || !selectedImage || !analysisType || !description) {
      setError('Please select a project, image, analysis type, and provide a description.');
      setLoading(false);
      return;
    }
    if (analysisType === 'vegetation' && !vegetationSubType) {
      setError('Please select NDVI or EVI for Vegetation Analysis.');
      setLoading(false);
      return;
    }

    console.log('Submitting analysis with:', {
      projectId: selectedProject,
      imageId: selectedImage,
      analysisType,
      vegetationSubType: analysisType === 'vegetation' ? vegetationSubType : undefined,
      description,
    });

    if (analysisType !== 'vegetation') {
      setError(`Analysis type "${analysisType}" is not yet supported.`);
      setLoading(false);
      return;
    }

    try {
      const imageBlob = await imagesService.getImageData(selectedImage);
      const selectedImageData = images.find(img => img.id === selectedImage);
      const filename = selectedImageData?.filename || 'image.tif';

      const redBand = 1;
      const nirBand = 2;
      const blueBand = 3;

      let analysisResult;

      if (vegetationSubType === 'ndvi') {
        analysisResult = await analysisService.calculateNDVI({
          file: imageBlob,
          filename,
          redBand,
          nirBand,
          projectId: selectedProject,
          imageId: selectedImage,
          description
        });
      } else if (vegetationSubType === 'evi') {
        analysisResult = await analysisService.calculateEVI({
          file: imageBlob,
          filename,
          redBand,
          nirBand,
          blueBand,
          projectId: selectedProject,
          imageId: selectedImage,
          description
        });
      } else {
        throw new Error("Invalid vegetation analysis sub-type selected.");
      }

      setResult(analysisResult); // Keep setting local state for immediate feedback

      // Store result in sessionStorage and redirect immediately
      try {
        // Add projectId and imageId to the result before storing if they aren't already there
        // (Assuming the service might not return them, though the interface now includes them)
        const resultToStore = {
            ...analysisResult,
            projectId: selectedProject,
            imageId: selectedImage
        };
        sessionStorage.setItem('latestAnalysisResult', JSON.stringify(resultToStore));
        router.push(`/projects/${selectedProject}`); // Redirect immediately
      } catch (storageError) {
          console.error("Failed to save analysis result to sessionStorage:", storageError);
          // Handle potential storage errors (e.g., quota exceeded)
          setError("Analysis complete, but failed to prepare redirection. Please navigate manually.");
      }


    } catch (submitError: any) {
      console.error("Analysis submission error:", submitError);
      setError(`Analysis failed: ${submitError.message || 'Unknown error'}`);
    } finally {
      setLoading(false);
    }
  };

  // REMOVE DUPLICATE FUNCTION DEFINITION BELOW

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
        <div className="text-center mb-12">
          <h1 className="text-4xl tracking-tight font-extrabold text-gray-900 sm:text-5xl md:text-6xl">
            <span className="block">Image Analysis</span>
            <span className="block text-primary-600">Processing Tools</span>
          </h1>
          <p className="mt-3 max-w-md mx-auto text-base text-gray-500 sm:text-lg md:mt-5 md:text-xl md:max-w-3xl">
            Process and analyze satellite imagery with advanced algorithms
          </p>
        </div>

        <div className="max-w-4xl mx-auto">
          <form onSubmit={handleSubmit} className="space-y-8 bg-white shadow-lg rounded-lg p-8">
            {error && (
              <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded-md mb-4">
                {error}
              </div>
            )}
            {result && (
              <div className="bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded-md mb-4">
                Analysis completed successfully! Redirecting to project page...
              </div>
            )}

            <div>
              <label htmlFor="project-select" className="block text-sm font-medium text-gray-700 mb-2">
                Select Project
              </label>
              <select
                id="project-select"
                value={selectedProject}
                onChange={handleProjectChange}
                className="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-primary-600 focus:border-primary-600 rounded-md"
                disabled={loading}
              >
                <option value="">{loading ? 'Loading projects...' : 'Select a project'}</option>
                {projects.map((project) => (
                  <option key={project.id} value={project.id}>
                    {project.projectName || 'Unnamed Project'}
                  </option>
                ))}
              </select>
            </div>

            {selectedProject && (
              <div>
                <label htmlFor="image-select" className="block text-sm font-medium text-gray-700 mb-2">
                  Select Image
                </label>
                <select
                  id="image-select"
                  value={selectedImage}
                  onChange={(e) => setSelectedImage(e.target.value)}
                  className="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-primary-600 focus:border-primary-600 rounded-md"
                  disabled={loadingImages || images.length === 0}
                >
                  <option value="">
                    {loadingImages ? 'Loading images...' : (images.length === 0 ? 'No images found' : 'Select an image')}
                  </option>
                  {images.map((image) => (
                    <option key={image.id} value={image.id}>
                      {image.filename || `Image ID: ${image.id}`}
                    </option>
                  ))}
                </select>
                {loadingImages && <p className="text-sm text-gray-500 mt-1">Loading images...</p>}
              </div>
            )}

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-4">Analysis Type</label>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <button
                  type="button"
                  onClick={() => handleAnalysisTypeChange('vegetation')}
                  className={`p-6 border-2 rounded-lg text-center hover:border-primary-600 transition-colors ${
                    analysisType === 'vegetation' ? 'border-primary-600 bg-primary-50' : 'border-gray-200'
                  }`}
                >
                  <div className="font-semibold text-lg mb-2">Vegetation Analysis</div>
                  <div className="text-sm text-gray-500">NDVI, EVI</div>
                </button>
                <button
                  type="button"
                  onClick={() => handleAnalysisTypeChange('water')}
                  className={`p-6 border-2 rounded-lg text-center hover:border-primary-600 transition-colors ${
                    analysisType === 'water' ? 'border-primary-600 bg-primary-50' : 'border-gray-200'
                  } disabled:opacity-50 disabled:cursor-not-allowed`}
                  disabled
                >
                  <div className="font-semibold text-lg mb-2">Water Analysis</div>
                  <div className="text-sm text-gray-500">NDWI (Not Implemented)</div>
                </button>
                <button
                  type="button"
                  onClick={() => handleAnalysisTypeChange('land')}
                  className={`p-6 border-2 rounded-lg text-center hover:border-primary-600 transition-colors ${
                    analysisType === 'land' ? 'border-primary-600 bg-primary-50' : 'border-gray-200'
                  } disabled:opacity-50 disabled:cursor-not-allowed`}
                  disabled
                >
                  <div className="font-semibold text-lg mb-2">Land Analysis</div>
                  <div className="text-sm text-gray-500">Land Use (Not Implemented)</div>
                </button>
              </div>
            </div>

            {analysisType === 'vegetation' && (
              <div className="p-4 border border-gray-200 rounded-md bg-gray-50">
                <label className="block text-sm font-medium text-gray-700 mb-3">
                  Select Vegetation Index:
                </label>
                <div className="flex items-center space-x-6">
                  <label className="flex items-center space-x-2 cursor-pointer">
                    <input
                      type="radio"
                      name="vegetationSubType"
                      value="ndvi"
                      checked={vegetationSubType === 'ndvi'}
                      onChange={(e) => setVegetationSubType(e.target.value)}
                      className="focus:ring-primary-500 h-4 w-4 text-primary-600 border-gray-300"
                    />
                    <span className="text-sm font-medium text-gray-700">NDVI</span>
                  </label>
                  <label className="flex items-center space-x-2 cursor-pointer">
                    <input
                      type="radio"
                      name="vegetationSubType"
                      value="evi"
                      checked={vegetationSubType === 'evi'}
                      onChange={(e) => setVegetationSubType(e.target.value)}
                      className="focus:ring-primary-500 h-4 w-4 text-primary-600 border-gray-300"
                    />
                    <span className="text-sm font-medium text-gray-700">EVI</span>
                  </label>
                </div>
                <p className="text-xs text-gray-500 mt-2">Default bands will be used (Red: 1, NIR: 2, Blue: 3 for EVI).</p>
              </div>
            )}

            <div>
              <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-2">
                Analysis Description
              </label>
              <textarea
                id="description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={4}
                placeholder="Enter a description for this analysis..."
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-primary-600 focus:border-primary-600"
                required
              />
            </div>

            <div className="flex justify-end">
              <button
                type="submit"
                disabled={loading}
                className="inline-flex items-center px-8 py-3 border border-transparent text-base font-medium rounded-md text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-600 md:py-4 md:text-lg md:px-10"
              >
                {loading ? 'Processing...' : 'Start Analysis'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
