"use client";

import { useEffect, useRef, useState } from 'react';
import dynamic from 'next/dynamic';
import Modal from 'react-modal';

// Initialize react-modal
Modal.setAppElement('#modal-root');

// Dynamically import Leaflet components with no SSR
const Map = dynamic(
  () => import('../components/Map'),
  { ssr: false }
);

interface MapPageProps {}

const MapPage: React.FC<MapPageProps> = () => {
  const [selectedRegion, setSelectedRegion] = useState<GeoJSON.Feature | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [imageLayer, setImageLayer] = useState<string | undefined>(undefined);

  const handleShapeCreated = (e: any) => {
    const layer = e.layer;
    const geoJSON = layer.toGeoJSON();
    console.log('Selected region:', geoJSON);
    setSelectedRegion(geoJSON);
  };

  const handleOpenModal = () => {
    if (selectedRegion) {
      setIsModalOpen(true);
    }
  };

  const handleModalClose = () => {
    setIsModalOpen(false);
  };

  const handleSubmit = async () => {
    if (!selectedRegion || !startDate || !endDate) {
      alert('Please fill in all required fields');
      return;
    }

    try {
      console.log('Submitting request with:', {
        region: selectedRegion.geometry,
        start_date: startDate,
        end_date: endDate
      });

      const response = await fetch('/api/fetch-image', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          collection_id: 'COPERNICUS/S2_HARMONIZED',
          region: selectedRegion.geometry,
          start_date: startDate,
          end_date: endDate,
          visualization_params: {
            bands: ['B4', 'B3', 'B2'],
            min: 0,
            max: 3000
          }
        }),
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Failed to fetch image: ${errorText}`);
      }

      const data = await response.json();
      console.log('Received full response data:', data); // Log the entire response object

      if (!data.tile_url) {
        throw new Error('No tile URL in response');
      }
      console.log('Successfully received tile URL:', data.tile_url); // Log the specific tile URL

      setImageLayer(data.tile_url);
      setIsModalOpen(false);
    } catch (error) {
      console.error('Error fetching image:', error);
      alert('Failed to fetch image. Please try again.');
    }
  };

  return (
    <div className="bg-blue-600 rounded-lg shadow-lg p-1">
      <div className="h-[600px] w-full relative bg-white rounded-lg">
        <Map
          onShapeCreated={handleShapeCreated}
          imageUrl={imageLayer}
          onClearShape={() => setSelectedRegion(null)}
        />
      </div>

      {selectedRegion && (
        <div className="flex justify-center mt-4 mb-3">
          <button
            onClick={handleOpenModal}
            className="bg-white text-blue-600 hover:bg-blue-50 font-semibold py-2 px-6 rounded-md shadow-sm border-2 border-blue-600 transition-colors duration-200"
          >
            Set Time Frame
          </button>
        </div>
      )}

      <Modal
        isOpen={isModalOpen}
        onRequestClose={handleModalClose}
        className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 bg-white p-6 rounded-lg shadow-xl w-96 z-[1000]"
        overlayClassName="fixed inset-0 bg-black bg-opacity-50 z-[999]"
        contentLabel="Select Time Frame"
        style={{
          content: {
            position: 'absolute',
            top: '30%',  // Position it at 30% from the top
            left: '50%',
            transform: 'translate(-50%, -30%)',
            border: 'none',
            background: 'white',
            borderRadius: '0.5rem',
            padding: '1.5rem',
            maxWidth: '24rem',
            width: '100%',
            boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)'
          },
          overlay: {
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.5)',
            display: 'flex',
            alignItems: 'flex-start',
            justifyContent: 'center',
            padding: '2rem'
          }
        }}
      >
        <div className="space-y-4">
          <h2 className="text-xl font-semibold text-gray-800 mb-4">Select Time Frame</h2>
          <div className="space-y-3">
            <div>
              <label htmlFor="start-date" className="block text-sm font-medium text-gray-700 mb-1">
                Start Date
              </label>
              <input
                type="date"
                id="start-date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500"
              />
            </div>
            <div>
              <label htmlFor="end-date" className="block text-sm font-medium text-gray-700 mb-1">
                End Date
              </label>
              <input
                type="date"
                id="end-date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500"
              />
            </div>
          </div>
          <div className="flex justify-end space-x-3 mt-6">
            <button
              onClick={handleModalClose}
              className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              Cancel
            </button>
            <button
              onClick={handleSubmit}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 border border-transparent rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              Submit
            </button>
          </div>
        </div>
      </Modal>
    </div>
  );
};

export default MapPage;
