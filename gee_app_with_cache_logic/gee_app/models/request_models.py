from pydantic import BaseModel, Field, field_validator
from typing import Union, Dict, List, Optional

class NDVIRequest(BaseModel):
    image_id: str = Field(..., description="Earth Engine image asset ID")
    region: Optional[Union[str, Dict]] = Field(None, description="GeoJSON region of interest (string or dict). If not provided, the full image geometry will be used.")
    scale: int = Field(30, ge=1, le=10000, description="Resolution in meters")
    visualization_params: Optional[Dict] = Field({'min': -1,'max': 1,'palette': ['blue', 'white', 'green']}, description="Visualization parameters for the NDVI map")
    bands: Optional[List[str]] = Field(["ndvi"], description="Bands for visualization")
    place_name: Optional[str] = Field(None, description="Name of the place for export naming (e.g., 'France')")

    @field_validator('image_id')
    def image_id_not_empty(cls, v):
        if not v.strip():
            raise ValueError("image_id cannot be empty")
        return v

class EVIRequest(BaseModel):
    image_id: str = Field(..., description="Earth Engine image asset ID")
    region: Optional[Union[str, Dict]] = Field(None, description="GeoJSON region of interest (string or dict). If not provided, the full image geometry will be used.")
    scale: int = Field(30, ge=1, le=10000, description="Resolution in meters")
    place_name: Optional[str] = Field(None, description="Name of the place for export naming (e.g., 'France')")

class WaterBodiesRequest(BaseModel):
    image_id: str = Field(..., description="Earth Engine image asset ID")
    region: Optional[Union[str, Dict]] = Field(None, description="GeoJSON region of interest (string or dict). If not provided, the full image geometry will be used.")
    threshold: float = Field(0.1, ge=-1, le=1, description="NDWI threshold for water detection")
    scale: int = Field(30, ge=1, le=10000, description="Resolution in meters")
    place_name: Optional[str] = Field(None, description="Name of the place for export naming (e.g., 'France')")

class ChangeDetectionRequest(BaseModel):
    image_id_before: str = Field(..., alias="imageIdBefore", description="Earth Engine image asset ID (before)")
    image_id_after: str = Field(..., alias="imageIdAfter", description="Earth Engine image asset ID (after)")
    region: Optional[Union[str, Dict]] = Field(None, description="GeoJSON region of interest (string or dict). If not provided, the full image geometry will be used.")
    bands: Optional[List[str]] = Field(None, description="List of bands for change detection")
    threshold: float = Field(0.2, ge=0, le=1, description="Threshold for change detection")
    scale: int = Field(30, ge=1, le=10000, description="Resolution in meters")
    place_name: Optional[str] = Field(None, description="Name of the place for export naming (e.g., 'France')")

class LandCoverRequest(BaseModel):
    image_id: str = Field(..., description="Earth Engine image asset ID")
    classification_band: str = Field(..., description="Band containing the land cover classification")
    region: Optional[Union[str, Dict]] = Field(None, description="GeoJSON region of interest (string or dict). If not provided, the full image geometry will be used.")
    scale: int = Field(10, ge=1, le=10000, description="Resolution in meters")
    detailed_metrics: bool = Field(False, description="If true, return detailed land cover metrics")
    temporal_analysis: bool = Field(False, description="If true, perform temporal analysis for land cover change")
    place_name: Optional[str] = Field(None, description="Name of the place for export naming (e.g., 'France')")

class ImageMetadata(BaseModel):
    id: str = Field(..., description="Image asset ID")
    date: str = Field(..., description="Acquisition date in YYYY-MM-DD format")
    cloudCover: float = Field(..., description="Cloud cover percentage")
    bounds: Dict = Field(..., description="Image bounds as GeoJSON geometry")
    previewUrl: str = Field(..., description="URL of the image preview")

class ImageMetadataRequest(BaseModel):
    region: Optional[Union[str, Dict]] = Field(None, description="GeoJSON region of interest. If not provided, the full image geometry will be used.")
    startDate: str = Field(..., description="Start date in YYYY-MM-DD format")
    endDate: str = Field(..., description="End date in YYYY-MM-DD format")
    maxCloudCover: float = Field(20, ge=0, le=100, description="Maximum cloud cover percentage")
    imagesNumber: int = Field(5, ge=1, le=100, description="Number of images to retrieve")
    scale: int = Field(30, ge=1, le=10000, description="Resolution in meters")
    collectionId: str = Field("COPERNICUS/S2", description="Earth Engine ImageCollection ID")
    bands: List[str] = Field(["B4", "B3", "B2"], description="Bands for visualization")
    min_value: int = Field(0, description="Minimum value for visualization")
    max_value: int = Field(3000, description="Maximum value for visualization")
    

# TERRAIN MODELS
class SlopeRequest(BaseModel):
    dem_collection: str = Field('USGS/SRTMGL1_003', description="DEM collection ID")
    region: Optional[Union[str, Dict]] = Field(None, description="GeoJSON region of interest. If not provided, the full image geometry will be used.")
    scale: int = Field(30, ge=1, le=10000, description="Resolution in meters")
    place_name: Optional[str] = Field(None, description="Name of the place for export naming (e.g., 'France')")
    visualization_params: Optional[Dict] = Field({'min': 0,'max': 60,'palette': ['blue', 'yellow', 'red']}, description="Visualization parameters for the slope map")
    bands: Optional[List[str]] = Field(["slope"], description="Bands for visualization")

class HillshadeRequest(BaseModel):
    dem_collection: str = Field('USGS/SRTMGL1_003', description="DEM collection ID")
    region: Optional[Union[str, Dict]] = Field(None, description="GeoJSON region of interest. If not provided, the full image geometry will be used.")
    azimuth: float = Field(315, ge=0, le=360, description="Sun azimuth angle")
    altitude: float = Field(45, ge=0, le=90, description="Sun altitude angle")
    scale: int = Field(30, ge=1, le=10000, description="Resolution in meters")
    place_name: Optional[str] = Field(None, description="Name of the place for export naming (e.g., 'France')")

# FIRE DETECTION
class FireRequest(BaseModel):
    image_id: str = Field(..., description="Earth Engine image asset ID")
    region: Optional[Union[str, Dict]] = Field(None, description="GeoJSON region of interest. If not provided, the full image geometry will be used.")
    threshold: float = Field(340, ge=0, le=500, description="Thermal threshold in Kelvin")
    scale: int = Field(30, ge=1, le=10000, description="Resolution in meters")
    place_name: Optional[str] = Field(None, description="Name of the place for export naming (e.g., 'France')")

# TIME SERIES
class TimeSeriesRequest(BaseModel):
    collection_id: str = Field(..., description="Earth Engine ImageCollection ID (e.g., 'COPERNICUS/S2_HARMONIZED')")
    region: Optional[Union[str, Dict]] = Field(None, description="GeoJSON region of interest. If not provided, the full image geometry will be used.")
    index: str = Field(..., description="Index to analyze (e.g., 'NDVI')")
    interval: str = Field('month', description="Time interval: 'day', 'week', 'month', 'year'")
    place_name: Optional[str] = Field(None, description="Name of the place for export naming (e.g., 'France')")

# AGRICULTURE
class AgricultureRequest(BaseModel):
    image_id: str = Field(..., description="Earth Engine image asset ID")
    region: Optional[Union[str, Dict]] = Field(None, description="GeoJSON region of interest. If not provided, the full image geometry will be used.")
    scale: int = Field(30, ge=1, le=10000, description="Resolution in meters")
    place_name: Optional[str] = Field(None, description="Name of the place for export naming (e.g., 'France')")

# ZONAL STATS
class ZonalStatsRequest(BaseModel):
    image_id: str = Field(..., description="Earth Engine image asset ID")
    region: Optional[Union[str, Dict]] = Field(None, description="GeoJSON region of interest. If not provided, the full image geometry will be used.")
    scale: int = Field(30, ge=1, le=10000, description="Resolution in meters")
    place_name: Optional[str] = Field(None, description="Name of the place for export naming (e.g., 'France')")

# HISTOGRAM
class HistogramRequest(BaseModel):
    image_id: str = Field(..., description="Earth Engine image asset ID")
    region: Optional[Union[str, Dict]] = Field(None, description="GeoJSON region of interest. If not provided, the full image geometry will be used.")
    band: str = Field(..., description="Band to analyze")
    bins: int = Field(50, ge=2, le=1000, description="Number of histogram bins")
    scale: int = Field(30, ge=1, le=10000, description="Resolution in meters")
    place_name: Optional[str] = Field(None, description="Name of the place for export naming (e.g., 'France')")

class ImageRequest(BaseModel):
    featureCollection: str = Field("FAO/GAUL/2015/level0", description="Dataset for place boundaries")
    place_name: str = Field(..., description="Name of the place (e.g., 'France')")
    image_collection: str = Field("COPERNICUS/S2_HARMONIZED", description="Satellite image dataset")
    start_date: str = Field(..., description="Start date in YYYY-MM-DD format")
    end_date: str = Field(..., description="End date in YYYY-MM-DD format")
    vis_params_bands: List[str] = Field(["B4", "B3", "B2"], description="Bands for visualization")
    vis_params_min: int = Field(0, description="Minimum value")
    vis_params_max: int = Field(3000, description="Maximum value")
    scale: int = Field(30, ge=1, le=10000, description="Resolution in meters for full-res image")
# Request model for fetching image URLs based on configuration

class ImageUrlRequest(BaseModel):
    region: Optional[Union[str, Dict]] = Field(None, description="GeoJSON region of interest")
    start_date: str = Field(..., description="Start date in YYYY-MM-DD format")
    end_date: str = Field(..., description="End date in YYYY-MM-DD format")
    max_cloud_cover: float = Field(20, ge=0, le=100, description="Maximum cloud cover percentage")
    bands: List[str] = Field(default=["B4", "B3", "B2"], description="Bands for visualization")
    scale: int = Field(30, ge=1, le=10000, description="Resolution in meters")
    thumbnail_dimensions: str = Field("512x512", description="Thumbnail dimensions")
    min_value: int = Field(0, description="Minimum value for visualization")
    max_value: int = Field(3000, description="Maximum value for visualization")
    place_name: Optional[str] = Field(None, description="Name of the place for export naming")
    collection_id: str = Field("COPERNICUS/S2_HARMONIZED", description="Earth Engine ImageCollection ID")
    check_existing: bool = Field(False, description="If true, return existing images from GEE_Images instead of fetching a new one")

class ImageUrlResponse(BaseModel):
    status: str = Field(..., description="Status of the request")
    image_id: Optional[str] = Field(None, description="Selected Earth Engine image asset ID")
    thumb_url: Optional[str] = Field(None, description="URL for the thumbnail image")
    full_res_url: Optional[str] = Field(None, description="URL for the full-resolution image")
    message: Optional[str] = Field(None, description="Additional message")
    task_id: Optional[str] = Field(None, description="Task ID if full-res image is being exported")
    existing_images: Optional[List[Dict]] = Field(None, description="List of existing images in GEE_Images if check_existing is true")
    
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
    
# Define Drought Request Models
class DroughtVegetationRequest(BaseModel):
    image_id: str = Field(None, description="Earth Engine image asset ID for vegetation data")
    collection_id: str = Field('MODIS/061/MOD13A1', description="Earth Engine ImageCollection ID for vegetation data")
    region: Optional[Union[str, Dict]] = Field(None, description="GeoJSON region of interest. If not provided, the full image geometry will be used.")
    index: str = Field('EVI', description="Vegetation index to analyze (e.g., 'NDVI')")
    start_date: str = Field('2021-01-01', description="Start date in YYYY-MM-DD format")
    end_date: str = Field('2021-12-31', description="End date in YYYY-MM-DD format")
    interval: str = Field('month', description="Time interval for analysis: 'day', 'week', 'month', 'year'")
    place_name: Optional[str] = Field(None, description="Name of the place for export naming (e.g., 'France')")

class DroughtPrecipitationRequest(BaseModel):
    image_id: str = Field(None, description="Earth Engine image asset ID for precipitation data")
    collection_id: str = Field('UCSB-CHG/CHIRPS/PENTAD', description="Earth Engine ImageCollection ID for precipitation data")
    region: Optional[Union[str, Dict]] = Field(None, description="GeoJSON region of interest. If not provided, the full image geometry will be used.")
    start_date: str = Field('2015-01-01', description="Start date in YYYY-MM-DD format")
    end_date: str = Field('2020-12-31', description="End date in YYYY-MM-DD format")
    time_scale: int = Field(3, ge=1, le=12, description="Time scale in months for SPI calculation")
    place_name: Optional[str] = Field(None, description="Name of the place for export naming (e.g., 'France')")
