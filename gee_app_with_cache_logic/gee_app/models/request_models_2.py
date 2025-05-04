from pydantic import BaseModel, Field, field_validator,model_validator
from typing import Union, Dict, List, Optional, Any

class GeneralEarthEngineRequest(BaseModel):
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
    
    # Miscellaneous
    custom_code: Optional[str] = Field(None, description="Custom Earth Engine JavaScript/Python code snippet for advanced operations")
    metadata: Optional[bool] = Field(False, description="If true, return metadata for the image/collection")
    
    #specify the analysis type
    analysis_type:str=Field(None,description="Analysis type")
    
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

    # Field-specific validators
    @classmethod
    def id_not_empty(cls, v: Optional[str], field_name: str) -> Optional[str]:
        if v is not None and not v.strip():
            raise ValueError(f"{field_name} cannot be empty")
        return v

    @classmethod
    def valid_date_format(cls, v: Optional[str], field_name: str) -> Optional[str]:
        if v is not None:
            try:
                from datetime import datetime
                datetime.strptime(v, '%Y-%m-%d')
            except ValueError:
                raise ValueError(f"{field_name} must be in YYYY-MM-DD format")
        return v
    
    image_id = Field(None, validator=id_not_empty)
    collection_id = Field(None, validator=id_not_empty)
    feature_collection_id = Field(None, validator=id_not_empty)
    start_date = Field(None, validator=valid_date_format)
    end_date = Field(None, validator=valid_date_format)

    # Existing field validators (unchanged)
    @field_validator('image_id', 'collection_id', 'feature_collection_id')
    def id_not_empty(cls, v):
        if v is not None and not v.strip():
            raise ValueError("ID fields cannot be empty")
        return v
    
    @field_validator('start_date', 'end_date')
    def valid_date_format(cls, v):
        if v is not None:
            try:
                from datetime import datetime
                datetime.strptime(v, '%Y-%m-%d')
            except ValueError:
                raise ValueError("Date must be in YYYY-MM-DD format")
        return v
    
    @field_validator('filters')
    def valid_filters(cls, v):
        if v is not None:
            for filt in v:
                if 'type' not in filt or 'property' not in filt or 'value' not in filt:
                    raise ValueError("Each filter must have 'type', 'property', and 'value' keys")
        return v

class ChangeDetectionRequest(BaseModel):
    imageIdBefore: str = Field(..., alias="imageIdBefore", description="Earth Engine image asset ID (before)")
    imageIdAfter: str = Field(..., alias="imageIdAfter", description="Earth Engine image asset ID (after)")
    region: Optional[Union[str, Dict]] = Field(None, description="GeoJSON region of interest (string or dict). If not provided, the full image geometry will be used.")
    bands: Optional[List[str]] = Field(None, description="List of bands for change detection")
    threshold: float = Field(0.2, ge=0, le=1, description="Threshold for change detection")
    scale: int = Field(30, ge=1, le=10000, description="Resolution in meters")
    place_name: Optional[str] = Field(None, description="Name of the place for export naming (e.g., 'France')")

#Drive API

# Request model for listing files
class ListImagesRequest(BaseModel):
    place_name: Optional[str] = Field(None, description="Filter files by place_name in filename")

# Response model for listing files
class ListImagesResponse(BaseModel):
    status: str = Field(..., description="Status of the request")
    images: List[Dict] = Field(..., description="List of image details (name, id, url)")
    message: Optional[str] = Field(None, description="Additional message")

# Request model for retrieving (downloading) a file
class RetrieveImageRequest(BaseModel):
    file_id: str = Field(..., description="Google Drive file ID to retrieve")

# Response model for retrieving a file
class RetrieveImageResponse(BaseModel):
    status: str = Field(..., description="Status of the request")
    name: Optional[str] = Field(None, description="Name of the retrieved image")
    file_path: Optional[str] = Field(None, description="Local path where file was downloaded")
    message: Optional[str] = Field(None, description="Additional message")

# Request model for updating a file
class UpdateImageRequest(BaseModel):
    file_id: str = Field(..., description="Google Drive file ID to update")
    new_name: Optional[str] = Field(None, description="New filename (without path)")
    new_file_path: Optional[str] = Field(None, description="Local path to new file content for replacement")

# Response model for updating a file
class UpdateImageResponse(BaseModel):
    status: str = Field(..., description="Status of the request")
    file_id: str = Field(..., description="Updated file ID")
    old_name: Optional[str] = Field(None, description="Original name of the image")
    new_name: str = Field(..., description="Updated name of the image")
    url: str = Field(..., description="Updated file URL")
    message: Optional[str] = Field(None, description="Additional message")

# Request model for deleting a file
class DeleteImageRequest(BaseModel):
    file_id: str = Field(..., description="Google Drive file ID to delete")

# Response model for deleting a file
class DeleteImageResponse(BaseModel):
    status: str = Field(..., description="Status of the request")
    name: Optional[str] = Field(None, description="Name of the deleted image")
    message: Optional[str] = Field(None, description="Additional message")
  
# Request model for creating a folder  
class CreateFolderRequest(BaseModel):
    folder_name: str = Field(..., description="Name of the folder to create")
    parent_id: Optional[str] = Field(None, description="Parent folder ID (defaults to GEE_Images if not provided)")

# Response model for creating a folder (already matches create_folder output, but formalized here)
class CreateFolderResponse(BaseModel):
    status: str = Field(..., description="Status of the request")
    id: str = Field(..., description="ID of the created folder")
    name: str = Field(..., description="Name of the created folder")
    message: Optional[str] = Field(None, description="Additional message")
    
# Request model (optional parent_id to start from a specific folder)
class ListFoldersRequest(BaseModel):
    parent_id: Optional[str] = Field(None, description="Parent folder ID to start from (defaults to GEE_Images)")

# Response model for folders and files
class FileInfo(BaseModel):
    name: str = Field(..., description="Name of the file")
    id: str = Field(..., description="File ID")
    url: str = Field(..., description="Web view link to the file")

class FolderInfo(BaseModel):
    name: str = Field(..., description="Name of the folder")
    id: str = Field(..., description="Folder ID")
    files: List[FileInfo] = Field(..., description="List of files in this folder")
    subfolders: List['FolderInfo'] = Field(..., description="List of subfolders in this folder")

class ListFoldersResponse(BaseModel):
    status: str = Field(..., description="Status of the request")
    folders: List[FolderInfo] = Field(..., description="List of top-level folders and their contents")
    message: Optional[str] = Field(None, description="Additional message")
    
# Resolve forward reference for recursive FolderInfo
FolderInfo.model_rebuild()

# Request model for updating a folder
class UpdateFolderRequest(BaseModel):
    folder_id: str = Field(..., description="ID of the folder to update")
    new_name: Optional[str] = Field(None, description="New name for the folder")
    new_parent_id: Optional[str] = Field(None, description="New parent folder ID (moves the folder)")

# Response model for updating a folder
class UpdateFolderResponse(BaseModel):
    status: str = Field(..., description="Status of the request")
    folder_id: str = Field(..., description="ID of the updated folder")
    old_name: Optional[str] = Field(None, description="Original name of the folder")
    new_name: str = Field(..., description="Updated name of the folder")
    message: Optional[str] = Field(None, description="Additional message")

# Request model for deleting a folder
class DeleteFolderRequest(BaseModel):
    folder_id: str = Field(..., description="ID of the folder to delete")
    recursive: Optional[bool] = Field(False, description="If true, delete folder and all contents")

# Response model for deleting a folder
class DeleteFolderResponse(BaseModel):
    status: str = Field(..., description="Status of the request")
    name: Optional[str] = Field(None, description="Name of the deleted folder")
    message: Optional[str] = Field(None, description="Additional message")

class BatchIngestionRequest(BaseModel):
    image_ids: Optional[List[str]] = Field(default=None, description="List of GEE image IDs to ingest")
    collection_id: Optional[str] = Field(default=None, description="GEE collection ID to ingest from")
    region: Dict = Field(..., description="GeoJSON region to clip data to")
    start_date: Optional[str] = Field(default=None, description="Start date for collection filter (YYYY-MM-DD)")
    end_date: Optional[str] = Field(default=None, description="End date for collection filter (YYYY-MM-DD)")
    cloud_cover_max: Optional[float] = Field(default=20.0, description="Max cloud cover percentage")
    scale: Optional[float] = Field(default=30, description="Resolution scale in meters")
    place_name: Optional[str] = Field(default="batch_region", description="Name for the region")
    max_items: Optional[int] = Field(default=100, description="Max number of items to ingest", ge=1, le=500)