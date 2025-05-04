'use client';

import React, { useState, useEffect } from 'react'; // Import useEffect
import { useSearchParams, useRouter, usePathname } from 'next/navigation'; // Import usePathname
import dynamic from 'next/dynamic';
import GeeDateModal from '@/components/GeeDateModal';
import dayjs, { Dayjs } from 'dayjs'; // Import dayjs for parsing dates

// Dynamically import the Map component with no SSR
const Map = dynamic(() => import('../../components/Map'), { ssr: false });

const MapPage: React.FC = () => {
  const searchParams = useSearchParams();
  const router = useRouter();
  const pathname = usePathname(); // Get the current path
  const projectId = searchParams.get('projectId');
  const [selectedRegion, setSelectedRegion] = useState<GeoJSON.Feature | null>(null);
  const [isDateModalOpen, setIsDateModalOpen] = useState(false);
  // State for initial dates from URL
  const [initialStartDate, setInitialStartDate] = useState<Dayjs | null>(null);
  const [initialEndDate, setInitialEndDate] = useState<Dayjs | null>(null);

  const handleShapeCreated = (e: any) => {
    const layer = e.layer;
    // Ensure it's a Feature, not just Geometry
    const geoJSON = layer.toGeoJSON();
    if (geoJSON.type !== 'Feature') {
      // Wrap geometry in a Feature if needed by the backend/hook
      const feature: GeoJSON.Feature = {
        type: 'Feature',
        properties: {},
        geometry: geoJSON
      };
      console.log('Selected region (converted to Feature):', feature);
      setSelectedRegion(feature);
    } else {
      console.log('Selected region:', geoJSON);
      setSelectedRegion(geoJSON as GeoJSON.Feature);
    }
    // Automatically open date modal after shape is created if projectId exists
    if (projectId) {
        handleOpenDateModal();
    } else {
        alert('Project ID is missing. Cannot proceed after selecting region.');
    }
  };

  // Effect to handle incoming URL parameters
  useEffect(() => {
    // Ensure this runs only client-side
    if (typeof window === 'undefined') return;

    const regionParam = searchParams.get('region');
    const startDateParam = searchParams.get('startDate');
    const endDateParam = searchParams.get('endDate');

    // Check if all required params are present
    if (regionParam && startDateParam && endDateParam) {
      console.log("Found GEE params in URL:", { regionParam, startDateParam, endDateParam });
      let shouldClearParams = true; // Flag to control clearing params
      try {
        // Parse region
        const parsedRegion = JSON.parse(decodeURIComponent(regionParam));
        // Basic validation (can be more robust)
        if (parsedRegion && (parsedRegion.type === 'Feature' || parsedRegion.type === 'Polygon' || parsedRegion.type === 'MultiPolygon')) {
           // If it's just geometry, wrap it in a feature for consistency
           const featureRegion = parsedRegion.type === 'Feature' ? parsedRegion : { type: 'Feature', properties: {}, geometry: parsedRegion };
           setSelectedRegion(featureRegion);
           console.log("Setting selected region from URL:", featureRegion);
        } else {
            throw new Error("Invalid region format in URL parameter.");
        }

        // Parse dates using dayjs
        const parsedStartDate = dayjs(startDateParam);
        const parsedEndDate = dayjs(endDateParam);

        if (parsedStartDate.isValid() && parsedEndDate.isValid()) {
          setInitialStartDate(parsedStartDate);
          setInitialEndDate(parsedEndDate);
          console.log("Setting initial dates from URL:", parsedStartDate.format('YYYY-MM-DD'), parsedEndDate.format('YYYY-MM-DD'));
          // Optionally open the date modal automatically
          setIsDateModalOpen(true);
        } else {
          throw new Error("Invalid date format in URL parameters.");
        }

      } catch (e) {
        console.error("Failed to process parameters from URL:", e);
        alert("Error processing parameters from URL. Please check the URL or select manually.");
        // Don't clear params if parsing failed, user might want to see them or retry
        shouldClearParams = false;
      } finally {
        // Clear the query params after attempting to process them
        // Use replace to avoid adding to browser history
        if (shouldClearParams) {
          console.log("Clearing GEE query parameters from URL");
          // Construct the base path, keeping projectId if present
          const basePath = projectId ? `${pathname}?projectId=${projectId}` : pathname;
          router.replace(basePath, { scroll: false }); // Use pathname and prevent scroll jump
        }
      }
    }
    // Run only once on mount or if searchParams change (though clearing them should prevent re-runs)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams, router, pathname, projectId]); // Add projectId dependency

  // Opens the date modal
  const handleOpenDateModal = () => {
    if (selectedRegion && projectId) {
      setIsDateModalOpen(true);
    } else if (!projectId) {
      alert('Project ID is missing. Cannot proceed.');
    } else {
      // This case should ideally not happen if modal is opened automatically after shape creation
      alert('Please select a region on the map first.');
    }
  };

  // Handler for when dates are submitted from the modal
  // Update signature to accept advancedParams
  const handleDateSubmit = (
      startDate: Dayjs | null,
      endDate: Dayjs | null,
      advancedParams: object // Use a generic object type for now
  ) => {
    console.log('Selected Dates:', startDate?.toISOString(), endDate?.toISOString());
    console.log('Advanced Params:', advancedParams); // Log received advanced params

    if (selectedRegion && projectId && startDate && endDate) {
      // Format dates as YYYY-MM-DD strings
      const formattedStartDate = startDate.format('YYYY-MM-DD');
      const formattedEndDate = endDate.format('YYYY-MM-DD');
      console.log('Formatted Dates:', formattedStartDate, formattedEndDate);

      // Construct query parameters
      const encodedRegion = encodeURIComponent(JSON.stringify(selectedRegion));
      const queryParams = new URLSearchParams({
        region: encodedRegion,
        startDate: formattedStartDate,
        endDate: formattedEndDate,
      });

      // Add advanced parameters if they exist and are not empty
      if (advancedParams && Object.keys(advancedParams).length > 0) {
          try {
              const encodedAdvancedParams = encodeURIComponent(JSON.stringify(advancedParams));
              queryParams.set('advancedParams', encodedAdvancedParams);
              console.log('Encoded Advanced Params:', encodedAdvancedParams);
          } catch (e) {
              console.error("Failed to stringify or encode advanced parameters:", e);
              // Decide how to handle: navigate without them or show error?
              // Navigating without them for now.
          }
      }

      // Navigate to project page with region, dates, and advanced params
      router.push(`/projects/${projectId}?${queryParams.toString()}`);
    } else {
      console.error('Missing data for navigation:', { selectedRegion, projectId, startDate, endDate, advancedParams });
      alert('An error occurred. Missing required data to proceed.');
    }
    // Close modal regardless of success/failure in this step
    setIsDateModalOpen(false);
  };

  return (
    // Wrap in a div with defined height
    <div className="h-screen flex flex-col relative">
      <Map
        onShapeCreated={handleShapeCreated}
        onClearShape={() => setSelectedRegion(null)}
        // Pass initial region to Map component (requires Map component update)
        initialRegion={selectedRegion}
      />

      {/* Button might become less necessary if modal opens automatically */}
      <button
        onClick={handleOpenDateModal}
        disabled={!selectedRegion || !projectId}
        className={`absolute bottom-4 left-1/2 transform -translate-x-1/2 z-[1000] py-2 px-6 rounded-md shadow-sm font-semibold transition-colors duration-200 ${
          selectedRegion && projectId
            ? 'bg-blue-600 text-white hover:bg-blue-700 border-2 border-blue-600'
            : 'bg-gray-300 text-gray-500 border-2 border-gray-300 cursor-not-allowed'
        }`}
        style={{ marginBottom: '1rem' }} // Add some margin if needed
      >
        {selectedRegion ? 'Confirm Region & Select Dates' : 'Draw Region'}
      </button>

      {/* Render the Date Modal */}
      <GeeDateModal
        open={isDateModalOpen}
        onClose={() => setIsDateModalOpen(false)}
        onSubmit={handleDateSubmit}
        // Pass initial dates to modal (requires GeeDateModal update)
        initialStartDate={initialStartDate}
        initialEndDate={initialEndDate}
      />
    </div> // Close the wrapper div
  );
};

export default MapPage;
