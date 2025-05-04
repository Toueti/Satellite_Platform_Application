"use client";

import { useEffect, useRef } from 'react';
import { MapContainer, TileLayer, FeatureGroup, useMap } from 'react-leaflet';
import { EditControl } from 'react-leaflet-draw';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import 'leaflet-draw/dist/leaflet.draw.css';
import { Feature as GeoJSONFeature } from 'geojson'; // Import GeoJSON Feature type

interface MapProps {
  onShapeCreated: (e: any) => void;
  imageUrl?: string;
  onClearShape: () => void;
  initialRegion?: GeoJSONFeature | null; // Add initialRegion prop
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

// Component to handle initial region drawing and map fitting
const InitialRegionHandler = ({ region }: { region?: GeoJSONFeature | null }) => {
    const map = useMap();
    const featureGroup = L.featureGroup().addTo(map); // Use a separate feature group or the main one? Let's use the main one via ref later.

    useEffect(() => {
        // This effect should ideally access the main FeatureGroup ref from the parent Map component
        // For now, let's assume we can access it or create a temporary one.
        // A better approach might be to pass the featureGroupRef down or handle this logic in the main Map component.

        // Let's try handling it directly in the main Map component's useEffect instead.
        // This component might not be necessary if we modify the main Map component's effect.

    }, [region, map]);

    return null;
}


const Map: React.FC<MapProps> = ({ onShapeCreated, imageUrl, onClearShape, initialRegion }) => {
  const featureGroupRef = useRef<L.FeatureGroup>(null); // Use L.FeatureGroup type
  const mapRef = useRef<L.Map | null>(null); // Ref to access map instance

  useEffect(() => {
    // Clear layers if image URL is removed (existing logic)
    if (!imageUrl && featureGroupRef.current) {
      featureGroupRef.current.clearLayers();
    }
  }, [imageUrl]);

  // Effect to handle drawing the initial region
  useEffect(() => {
    if (initialRegion && featureGroupRef.current && mapRef.current) {
        console.log("Drawing initial region:", initialRegion);
        const featureGroup = featureGroupRef.current;
        featureGroup.clearLayers(); // Clear any previous drawings

        try {
            const geoJsonLayer = L.geoJSON(initialRegion);
            geoJsonLayer.addTo(featureGroup);

            // Fit map bounds to the drawn layer
            mapRef.current.fitBounds(geoJsonLayer.getBounds());
        } catch (error) {
            console.error("Error adding initial region GeoJSON layer:", error);
        }
    }
    // Run when initialRegion changes
  }, [initialRegion]);


  const handleShapeCreated = (e: any) => {
    console.log('Shape created:', e.layer.toGeoJSON());
    onShapeCreated(e);
  };

  return (
    <MapContainer
      ref={mapRef} // Assign the ref here
      center={[0, 0]}
      zoom={2}
      className="h-full w-full rounded-lg"
      // whenReady prop doesn't take the instance, ref is used instead
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
