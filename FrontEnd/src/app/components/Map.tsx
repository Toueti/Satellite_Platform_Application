"use client";

import { useEffect, useRef } from 'react';
import { MapContainer, TileLayer, FeatureGroup, useMap } from 'react-leaflet';
import { EditControl } from 'react-leaflet-draw';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import 'leaflet-draw/dist/leaflet.draw.css';

interface MapProps {
  onShapeCreated: (e: any) => void;
  imageUrl?: string;
  onClearShape: () => void;
}

// Component to handle image layer updates
const ImageLayerComponent = ({ url }: { url?: string }) => {
  const map = useMap();
  const layerRef = useRef<L.TileLayer | null>(null);

  useEffect(() => {
    if (layerRef.current) {
      map.removeLayer(layerRef.current);
      layerRef.current = null;
    }

    if (url) {
      console.log('Adding new tile layer with URL:', url);
      layerRef.current = L.tileLayer(url);
      layerRef.current.addTo(map);
    }

    return () => {
      if (layerRef.current) {
        map.removeLayer(layerRef.current);
      }
    };
  }, [url, map]);

  return null;
};

const Map: React.FC<MapProps> = ({ onShapeCreated, imageUrl, onClearShape }) => {
  const featureGroupRef = useRef<any>(null);

  useEffect(() => {
    if (!imageUrl && featureGroupRef.current) {
      featureGroupRef.current.clearLayers();
    }
  }, [imageUrl]);

  const handleShapeCreated = (e: any) => {
    console.log('Shape created:', e.layer.toGeoJSON());
    onShapeCreated(e);
  };

  return (
    <MapContainer
      center={[0, 0]}
      zoom={2}
      className="h-full w-full rounded-lg"
    >
      <TileLayer
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
      />
      <FeatureGroup ref={featureGroupRef}>
        <EditControl
          position="topright"
          onCreated={handleShapeCreated}
          onDeleted={onClearShape}
          draw={{
            rectangle: true,
            polygon: true,
            circle: false,
            circlemarker: false,
            marker: false,
            polyline: false,
          }}
        />
      </FeatureGroup>
      <ImageLayerComponent url={imageUrl} />
    </MapContainer>
  );
};

export default Map;
