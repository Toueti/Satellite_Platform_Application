from pydantic import BaseModel, Field, field_validator, model_validator
from typing import Union, Dict, List, Optional, Any

class GeneralEarthEngineRequest2(BaseModel):
    # Core Identification Fields
    image_id: Optional[str] = Field(None, description="Earth Engine image asset ID (optional if collection_id is provided)")
    collection_id: Optional[str] = Field(None, description="Earth Engine ImageCollection ID (e.g., 'COPERNICUS/S2_HARMONIZED')")
    feature_collection_id: Optional[str] = Field(None, description="Earth Engine FeatureCollection ID (e.g., 'FAO/GAUL/2015/level0')")
    
    # Spatial and Resolution Fields
    region: Optional[Union[str, Dict]] = Field(None, description="GeoJSON region of interest (string or dict). If not provided, the full image geometry will be used.")
    scale: Optional[int] = Field(30, ge=1, le=10000, description="Resolution in meters")
    crs: Optional[str] = Field(None, description="Coordinate Reference System (e.g., 'EPSG:4326')")
    
    # Temporal Fields
    start_date: Optional[str] = Field(None, description="Start date in YYYY-MM-DD format")
    end_date: Optional[str] = Field(None, description="End date in YYYY-MM-DD format")
    interval: Optional[str] = Field(None, description="Time interval: 'day', 'week', 'month', 'year' (for time series)")
    time_scale: Optional[int] = Field(None, ge=1, le=12, description="Time scale in units (e.g., months for SPI in drought analysis)")
    
    # Data Filtering
    filters: Optional[List[Dict]] = Field(None, description="List of filters for ImageCollection (e.g., [{'type': 'eq', 'property': 'CLOUD_COVER', 'value': 20}])")
    max_cloud_cover: Optional[float] = Field(None, ge=0, le=100, description="Maximum cloud cover percentage")
    images_number: Optional[int] = Field(None, ge=1, le=100, description="Number of images to retrieve")
    
    # Band Selection and Visualization
    bands: Optional[List[str]] = Field(None, description="List of bands for visualization or analysis")
    visualization_params: Optional[Dict] = Field(None, description="Visualization parameters (e.g., {'min': 0, 'max': 3000, 'palette': [...]})")
    band_math: Optional[Dict[str, str]] = Field(None, description="Custom band math expressions (e.g., {'ndvi': '(NIR - RED) / (NIR + RED)'})")
    
    # Analysis Parameters
    analysis_type: Optional[str] = Field(None, description="Analysis type")
    index: Optional[str] = Field(None, description="Predefined index to compute (e.g., 'NDVI', 'EVI', 'NDWI')")
    reducer: Optional[str] = Field(None, description="Reducer to apply (e.g., 'mean', 'median', 'sum', 'min', 'max')")
    threshold: Optional[float] = Field(None, description="Generic threshold value (e.g., for water detection, change detection)")
    classification_band: Optional[str] = Field(None, description="Band for classification (e.g., land cover)")
    detailed_metrics: Optional[bool] = Field(False, description="If true, return detailed metrics (e.g., for land cover)")
    temporal_analysis: Optional[bool] = Field(False, description="If true, perform temporal analysis")
    
    # Terrain Analysis
    dem_collection: Optional[str] = Field(None, description="DEM collection ID (e.g., 'USGS/SRTMGL1_003')")
    terrain_params: Optional[Dict] = Field(None, description="Terrain-specific parameters (e.g., {'azimuth': 315, 'altitude': 45} for hillshade)")
    
    # Machine Learning and Classification
    classifier: Optional[Dict] = Field(None, description="Classifier configuration (e.g., {'type': 'cart', 'training_data': 'path/to/training'})")
    training_data: Optional[str] = Field(None, description="Path to training FeatureCollection for supervised classification")
    
    # Export and Output
    place_name: Optional[str] = Field(None, description="Name of the place for export naming (e.g., 'France')")
    export_format: Optional[str] = Field(None, description="Export format (e.g., 'GeoTIFF', 'CSV', 'TFRecord')")
    export_destination: Optional[str] = Field(None, description="Export destination (e.g., 'Google Drive', 'Google Cloud Storage')")
    export_params: Optional[Dict] = Field(None, description="Additional export parameters (e.g., {'driveFolder': 'GEE_Exports'})")
    
    # Advanced Analysis
    histogram_params: Optional[Dict] = Field(None, description="Histogram parameters (e.g., {'band': 'B4', 'bins': 50})")
    zonal_stats: Optional[Dict] = Field(None, description="Zonal statistics parameters (e.g., {'reducer': 'mean', 'geometry': 'path/to/geom'})")
    join_params: Optional[Dict] = Field(None, description="Join parameters for combining collections (e.g., {'type': 'inner', 'condition': {...}})")
    
    dimensions: Optional[Dict] = Field(None, description="Dimensions for the output (e.g., {'width': 512, 'height': 512})")
    # Miscellaneous
    custom_code: Optional[str] = Field(None, description="Custom Earth Engine JavaScript/Python code snippet for advanced operations")
    metadata: Optional[bool] = Field(False, description="If true, return metadata for the image/collection")
    
    class Config:
        extra = "allow"
    
    # Validation
    @model_validator(mode='before')
    @classmethod
    def check_data_source(cls, data: Any) -> Any:
        if not isinstance(data, dict):
            raise ValueError("Input must be a dictionary")
        
        image_id = data.get('image_id')
        collection_id = data.get('collection_id')
        feature_collection_id = data.get('feature_collection_id')
        
        if image_id is None and collection_id is None and feature_collection_id is None:
            raise ValueError("At least one of 'image_id', 'collection_id', or 'feature_collection_id' must be provided")
        
        return data
    
    @field_validator('image_id', 'collection_id', 'feature_collection_id')
    def id_not_empty(cls, v: Optional[str]) -> Optional[str]:
        if v is not None and not v.strip():
            raise ValueError("ID fields cannot be empty")
        return v
    
    @field_validator('start_date', 'end_date')
    def valid_date_format(cls, v: Optional[str]) -> Optional[str]:
        if v is not None:
            try:
                from datetime import datetime
                datetime.strptime(v, '%Y-%m-%d')
            except ValueError:
                raise ValueError("Date must be in YYYY-MM-DD format")
        return v
    
    @field_validator('filters')
    def valid_filters(cls, v: Optional[List[Dict]]) -> Optional[List[Dict]]:
        if v is not None:
            for filt in v:
                if 'type' not in filt or 'property' not in filt or 'value' not in filt:
                    raise ValueError("Each filter must have 'type', 'property', and 'value' keys")
        return v