export interface SatelliteImage {
  id: string;
  filename: string;
  url: string;
  thumbnailUrl: string;
  captureDate: string;
  uploadDate: string;
  location: {
    latitude: number;
    longitude: number;
    locationName?: string;
  };
  size: number; 
  resolution: string;
  bands: string[];
  metadata: {
    cloudCoverage?: number;
    satellite?: string;
    sensor?: string;
    [key: string]: any;
  };
  tags: string[];
  annotations: ImageAnnotation[];
}

export interface ImageAnnotation {
  id: string;
  type: 'point' | 'polygon' | 'rectangle' | 'text';
  coordinates: number[][] | [number, number];
  color: string;
  label?: string;
  description?: string;
  createdAt: string;
  createdBy: string;
}

export interface AnalysisResult {
  id: string;
  type: string; // e.g., 'classification', 'change-detection', 'ndvi'
  name: string;
  description?: string;
  createdAt: string;
  parameters: {
    [key: string]: any;
  };
  resultImages: {
    id: string;
    url: string;
    thumbnailUrl: string;
    type: string;
    caption?: string;
  }[];
  metrics?: {
    [key: string]: number | string;
  };
  sourceImageIds: string[];
}
