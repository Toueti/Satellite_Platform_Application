import ee
import logging
import asyncio
from typing import Any, Union, Dict, List, Optional
from gee_app.utils.sensor_utils import parse_region
from gee_app.utils.drive_utils import get_gee_images_folder_id, get_available_drive_storage
import datetime
import time
import re
import requests # Added for image download
import base64   # Added for image encoding

logger = logging.getLogger('gee_app')

class Config:
    DEFAULT_COLLECTION = "COPERNICUS/S2_HARMONIZED"

GLOBAL_DEM_COLLECTIONS = ['USGS/SRTMGL1_003', 'NASA/NASADEM']

# Store task statuses globally (use a DB in production)
task_statuses = {}

import datetime
import asyncio

async def get_image_urls(
    image: ee.Image,
    region: Optional[Union[ee.Geometry, str, Dict]],
    bands: List[str],
    scale: int,
    operation: str,
    place_name: Optional[str] = None,
    visualization_params: Optional[Dict] = None,
    crs: Optional[str] = None,
    format: str = "GEO_TIFF",
    max_retries: int = 3
) -> Dict:

    aoi = parse_region(region) if isinstance(region, (str, Dict)) else region or image.geometry()
    vis_bands = bands if isinstance(bands, list) else [bands]
    logger.debug(f"Using bands for visualization: {vis_bands}")

    # Check area and adjust scale if too large
    area = aoi.area(maxError=1000).getInfo() / 1e6  # sq km
    pixel_limit = 32768
    bounds = aoi.bounds().coordinates().getInfo()[0]
    width_m = abs(bounds[2][0] - bounds[0][0]) * 111320  # Rough meters (lon to m at equator)
    height_m = abs(bounds[2][1] - bounds[0][1]) * 111320  # Rough meters (lat to m)
    width_px = width_m / scale
    height_px = height_m / scale
    
    if width_px > pixel_limit or height_px > pixel_limit:
        new_scale = max(width_m / pixel_limit, height_m / pixel_limit)
        logger.warning(f"Pixel dimensions ({int(width_px)}x{int(height_px)}) exceed {pixel_limit}. Adjusting scale from {scale} to {int(new_scale)}")
        scale = int(new_scale)

    if not visualization_params:
        min_val, max_val = 0, 3000
        if any(index in operation for index in ['NDVI', 'EVI', 'NDMI']):
            min_val, max_val = -1, 1
        elif 'NDWI' in operation:
            min_val, max_val = -0.5, 0.5
        elif any(band in str(bands) for band in ['B8', 'NIR', 'B4', 'RED']):
            min_val, max_val = 0, 0.4
        
        visualization_params = {"bands": vis_bands, "min": min_val, "max": max_val, "region": aoi}
        if len(vis_bands) == 1:
            if 'NDVI' in operation:
                visualization_params["palette"] = ["#d73027", "#f46d43", "#fdae61", "#fee08b", "#d9ef8b", "#a6d96a", "#66bd63", "#1a9850"]
            elif 'Water' in operation or 'NDWI' in operation:
                visualization_params["palette"] = ["#ffffcc", "#a1dab4", "#41b6c4", "#2c7fb8", "#253494"]
            elif 'EVI' in operation:
                visualization_params["palette"] = ["#ff0000", "#ffffff", "#00ff00"]
    else:
        visualization_params["bands"] = vis_bands
        visualization_params["region"] = aoi
    logger.debug(f"Visualization params: {visualization_params}")

    thumb_url = image.getThumbURL(visualization_params)
    
    if area > 5000:
        logger.warning("AOI too large, reducing to 50km radius.")
        aoi = aoi.centroid().buffer(50000)
    
    full_res_info = {}
    timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
    safe_place_name = place_name.replace(" ", "_") if place_name else "region"
    export_name = f"{safe_place_name}_{operation}_{timestamp}"
    crs = crs or "EPSG:4326"
    
    if area < 150:
        full_res_params = {"bands": vis_bands, "region": aoi, "scale": scale, "format": format, "crs": crs}
        retry_count = 0
        while retry_count < max_retries:
            try:
                full_res_url = image.getDownloadURL(full_res_params)
                full_res_info = {"full_res_url": full_res_url, "format": format}
                logger.debug(f"Generated full-res URL for {operation}")
                break
            except ee.EEException as e:
                retry_count += 1
                if retry_count >= max_retries or "User memory limit exceeded" in str(e):
                    logger.warning(f"getDownloadURL failed for {operation}: {str(e)}. Falling back to export.")
                    full_res_info = await export_to_drive_async(image, vis_bands, scale, aoi, export_name, crs, format)
                    break
                wait_time = 2 ** retry_count
                logger.info(f"Retry {retry_count}/{max_retries} after {wait_time}s for {operation}")
                await asyncio.sleep(wait_time)
    else:
        logger.debug(f"Area {area} sq km exceeds threshold, exporting to Drive for {operation}")
        full_res_info = await export_to_drive_async(image, vis_bands, scale, aoi, export_name, crs, format)
    
    if not full_res_info:
        logger.error(f"full_res_info empty for {operation}, setting fallback")
        full_res_info = {"full_res_url": "Failed to generate URL", "message": "Unexpected error"}
    
    return {"thumb_url": thumb_url, "full_res_info": full_res_info}

async def export_to_drive_async(
    image: ee.Image, 
    bands: Union[str, List[str]], 
    scale: int, 
    region: ee.Geometry, 
    export_name: str,
    crs: str = "EPSG:4326",
    format: str = "GEO_TIFF"
) -> Dict:
    """Async helper to export to Google Drive and return file info.
    
    Args:
        image: Earth Engine image to export
        bands: Bands to include in export
        scale: Resolution in meters
        region: Region to export
        export_name: Name for the exported file
        crs: Coordinate reference system (default: EPSG:4326)
        format: Export format (default: GEO_TIFF)
        
    Returns:
        Dictionary with export status information
    """
    from .drive_utils import get_drive_service  # Import here
    
    # Configure the export
    export_config = {
        'image': image.select(bands),
        'description': export_name,
        'folder': 'GEE_Images',
        'fileNamePrefix': export_name,
        'scale': scale,
        'region': region,
        'crs': crs,
        'maxPixels': 1e13,
        'fileFormat': format.lower() if format != "GEO_TIFF" else "GeoTIFF"
    }
    
    # Start the export task
    task = ee.batch.Export.image.toDrive(**export_config)
    task.start()
    task_id = task.id
    
    logger.info(f"Started export task {task_id} for {export_name}")
    
    # Create or retrieve task status dictionary if not already defined
    global task_statuses
    if 'task_statuses' not in globals():
        task_statuses = {}
    
    # Monitor the task status
    task_statuses[task_id] = {"state": "RUNNING", "message": "Export initiated", "url": None}
    
    # Maximum time to wait (30 minutes)
    max_wait_time = 30 * 60
    wait_start_time = time.time()
    
    while True:
        try:
            status = ee.data.getTaskStatus(task_id)[0]
            state = status.get('state', 'UNKNOWN')
            progress = status.get('progress', 'N/A')
            message = status.get('description', 'Processing...')
            
            # Update status in the global tracker
            task_statuses[task_id] = {"state": state, "message": message, "url": None}
            logger.info(f"Task {task_id} - State: {state}, Progress: {progress}, Message: {message}")
            
            # Check for completion states
            if state in ['COMPLETED', 'FAILED', 'CANCELLED']:
                break
                
            # Check for timeout
            if time.time() - wait_start_time > max_wait_time:
                task_statuses[task_id] = {"state": "TIMEOUT", "message": "Export timed out after 30 minutes", "url": None}
                logger.warning(f"Export task {task_id} timed out after 30 minutes")
                return {"status": "timeout", "message": "Export timed out after 30 minutes"}
                
            # Wait before checking again
            await asyncio.sleep(10)
                
        except Exception as e:
            logger.error(f"Error monitoring task {task_id}: {str(e)}")
            await asyncio.sleep(15)  # Wait longer on error
    
    # Handle non-successful completion
    if status['state'] != 'COMPLETED':
        error_msg = status.get('error_message', 'Unknown error')
        task_statuses[task_id] = {"state": "FAILED", "message": f"Export failed: {error_msg}", "url": None}
        logger.error(f"Export {task_id} failed: {error_msg}")
        return {"status": "failed", "message": error_msg}
    
    # Get Drive file information
    try:
        drive_service = get_drive_service()
        
        # First, find the GEE_Images folder or create it if it doesn't exist
        folder_query = "name='GEE_Images' and mimeType='application/vnd.google-apps.folder'"
        folder_response = drive_service.files().list(
            q=folder_query, 
            spaces='drive', 
            fields='files(id, name)'
        ).execute()
        
        folders = folder_response.get('files', [])
        if not folders:
            folder_metadata = {'name': 'GEE_Images', 'mimeType': 'application/vnd.google-apps.folder'}
            folder = drive_service.files().create(body=folder_metadata, fields='id').execute()
            folder_id = folder['id']
            logger.info(f"Created GEE_Images folder with ID: {folder_id}")
        else:
            folder_id = folders[0]['id']
            logger.info(f"Found existing GEE_Images folder with ID: {folder_id}")
        
        # Now find the exported file
        file_extension = ".tif" if format in ["GEO_TIFF", "GeoTIFF"] else f".{format.lower()}"
        file_query = f"name='{export_name}{file_extension}' and '{folder_id}' in parents"
        
        # Wait for the file to appear (up to 2 minutes)
        file_found = False
        file_wait_start = time.time()
        max_file_wait = 120  # 2 minutes
        
        while not file_found and time.time() - file_wait_start < max_file_wait:
            file_response = drive_service.files().list(
                q=file_query,
                spaces='drive',
                fields='files(id, name, webViewLink)'
            ).execute()
            
            files = file_response.get('files', [])
            if files:
                file_found = True
                file_id = files[0]['id']
                drive_url = files[0]['webViewLink']
                
                # Generate a direct download link if needed
                download_url = f"https://drive.google.com/uc?export=download&id={file_id}"
                
                result = {
                    "status": "completed", 
                    "message": f"Exported to Drive as '{export_name}{file_extension}'",
                    "drive_url": drive_url,
                    "download_url": download_url,
                    "file_id": file_id
                }
                
                # Store file_id and url in task status upon completion
                task_statuses[task_id] = {
                    "state": "COMPLETED",
                    "message": result["message"],
                    "url": drive_url, # Keep the web view link
                    "file_id": file_id, # Add the file ID
                    "filename": f"{export_name}{file_extension}" # Store the expected filename
                }

                logger.info(f"Export completed! File {export_name}{file_extension} (ID: {file_id}) at {drive_url}")
                return result
            
            await asyncio.sleep(5)
        
        # If we get here, the file wasn't found in Drive after waiting
        task_statuses[task_id] = {
            "state": "FILE_NOT_FOUND", # Use a more specific state
            "message": f"Export task completed, but file '{export_name}{file_extension}' not found in Drive after {max_file_wait}s.",
            "url": None,
            "file_id": None, # Explicitly set file_id to None
            "filename": f"{export_name}{file_extension}"
        }

        logger.warning(f"File '{export_name}{file_extension}' not found in GEE_Images folder after task completion.")
        return {
            "status": "file_not_found",
            "message": f"Export reported as complete, but file '{export_name}{file_extension}' not found in Drive"
        }
        
    except Exception as e:
        # Error during Drive API interaction after task completion
        task_statuses[task_id] = {
            "state": "DRIVE_API_ERROR", # Specific error state
            "message": f"Drive API error after task completion: {str(e)}",
            "url": None,
            "file_id": None,
            "filename": f"{export_name}{file_extension}"
        }
        logger.error(f"Drive API error after task completion for {task_id}: {str(e)}")
        return {"status": "error", "message": f"Drive API error: {str(e)}"} # Return error status

async def export_to_drive(
    image: ee.Image,  # This will now be the *visualized* image
    scale: int,
    region: ee.Geometry,
    export_name: str,
    folder: str = "GEE_Images",
    fileFormat: str = "GeoTIFF", # Default format, align with getDownloadURL intent
    maxPixels: int = 1e13,
    crs: Optional[str] = None,
    **kwargs: Any # Accept other export arguments passed from fetch_image_metadata
) -> Dict:
    """
    Starts an asynchronous export of a (typically visualized) image to Google Drive.

    Args:
        image: The ee.Image object to export (should be the result of .visualize()
               if matching getDownloadURL output).
        scale: The scale (resolution) in meters.
        region: The ee.Geometry defining the export area.
        export_name: The base name for the exported file (without extension).
        folder: The Google Drive folder to export into.
        fileFormat: The format for the export (e.g., 'GeoTIFF', 'PNG').
        maxPixels: Maximum number of pixels allowed in the export.
        crs: Optional Coordinate Reference System for the export.
        **kwargs: Additional arguments for ee.batch.Export.image.toDrive
                  (e.g., specific fileFormatArguments).

    Returns:
        A dictionary containing the export task ID and status message.
    """
    try:
        # Prepare parameters for the export task
        # Note: We don't use .select(bands) because 'image' is already visualized
        export_params = {
            'image': image,
            'description': export_name,
            'folder': folder,
            'scale': scale,
            'region': region.getInfo()['coordinates'] if isinstance(region, ee.Geometry) else region, # Pass coordinates for robustness
            'fileFormat': fileFormat,
            'maxPixels': maxPixels,
            **kwargs # Include any other passed arguments
        }

        # Add CRS only if provided
        if crs:
            export_params['crs'] = crs

        # Log parameters being used for the export (excluding the potentially large image object)
        log_params = {k: v for k, v in export_params.items() if k != 'image'}
        logger.debug(f"Initiating Drive export '{export_name}' with params: {log_params}")

        # Start the export task
        export_task = ee.batch.Export.image.toDrive(**export_params)
        export_task.start()
        task_id = export_task.id
        logger.info(f"🚀 Started Drive export task {task_id} for '{export_name}'")

        # Determine file extension based on format for reporting
        file_extension = fileFormat.lower()
        if file_extension == "geotiff":
            file_extension = "tif"
        elif file_extension == "tfrecord":
            file_extension = "tfrecord.gz" # GEE default naming

        # Initialize task status tracking
        # Ensure the filename reflects the actual export format
        task_statuses[task_id] = {
            "state": "STARTED", # Consistent state naming
            "message": "Export task submitted to GEE.",
            "url": None, # URL will be potentially updated by monitoring task later
            "file_id": None, # File ID will be potentially updated by monitoring task later
            "filename": f"{export_name}.{file_extension}" # Store expected filename
        }

        # Start background monitoring task
        # Pass only task_id; monitor_export_task should handle fetching details if needed
        asyncio.create_task(monitor_export_task(task_id))

        return {
            "full_res_url": None, # No direct URL when exporting to Drive
            "message": f"Export started as '{export_name}.{file_extension}' in '{folder}'. Check progress with task ID.",
            "task_id": task_id,
            "status": "STARTED" # Return initial status
        }

    except ee.EEException as gee_error:
        logger.error(f"❌ GEE Error starting export task for {export_name}: {str(gee_error)}")
        raise RuntimeError(f"Failed to initiate GEE export task: {str(gee_error)}") from gee_error
    except Exception as e:
        logger.error(f"❌ Unexpected error starting export task for {export_name}: {str(e)}")
        # Re-raise or return an error status suitable for your application
        raise RuntimeError(f"Unexpected error initiating export task: {str(e)}") from e

async def monitor_export_task(task_id: str, export_name: str, format: str = "GEO_TIFF"):
     """Monitors a GEE export task and updates its status, including finding the file on Drive."""
     # This function contains the logic previously in export_to_drive_async, but focused on monitoring an existing task_id
     # It's called via asyncio.create_task by export_to_drive

     global task_statuses
     logger.info(f"Monitoring started for task {task_id} ({export_name})")

     # Maximum time to wait (e.g., 1 hour, adjust as needed)
     max_wait_time = 60 * 60
     wait_start_time = time.time()
     check_interval = 30 # Check every 30 seconds

     while True:
        current_time = time.time()
        if current_time - wait_start_time > max_wait_time:
            task_statuses[task_id] = {
                "state": "TIMEOUT",
                "message": f"Monitoring timed out after {max_wait_time / 60} minutes.",
                "url": None, "file_id": None, "filename": task_statuses[task_id].get("filename")
            }
            logger.warning(f"Monitoring for task {task_id} timed out.")
            break # Exit monitoring loop

        try:
            # Get the latest status from GEE
            status_list = ee.data.getTaskStatus(task_id)
            if not status_list:
                 logger.warning(f"Task {task_id} status not found in GEE.")
                 # Keep previous status or mark as unknown? For now, keep polling.
                 await asyncio.sleep(check_interval)
                 continue

            status = status_list[0]
            state = status.get('state', 'UNKNOWN')
            message = status.get('description', 'Processing...')
            error_msg = status.get('error_message')

            # Update our global status tracker
            task_statuses[task_id].update({"state": state, "message": message or error_msg}) # Update existing entry

            logger.info(f"Task {task_id} - State: {state}") # Less verbose logging during monitoring

            if state == 'COMPLETED':
                logger.info(f"Task {task_id} completed. Attempting to find file in Drive...")
                # --- Find file in Drive ---
                try:
                    from .drive_utils import get_drive_service # Local import
                    drive_service = get_drive_service()
                    folder_id = get_gee_images_folder_id(drive_service) # Ensure folder exists

                    file_extension = ".tif" if format in ["GEO_TIFF", "GeoTIFF"] else f".{format.lower()}"
                    filename_to_find = f"{export_name}{file_extension}"
                    file_query = f"name='{filename_to_find}' and '{folder_id}' in parents and trashed = false"

                    file_found = False
                    file_wait_start = time.time()
                    max_file_wait = 180 # Wait up to 3 minutes for file to appear after completion

                    while not file_found and time.time() - file_wait_start < max_file_wait:
                        file_response = drive_service.files().list(
                            q=file_query, spaces='drive', fields='files(id, name, webViewLink)'
                        ).execute()
                        files = file_response.get('files', [])

                        if files:
                            file_found = True
                            file_info = files[0]
                            file_id = file_info['id']
                            drive_url = file_info['webViewLink']
                            final_message = f"Export completed and file found in Drive: {filename_to_find}"

                            task_statuses[task_id].update({
                                "state": "COMPLETED",
                                "message": final_message,
                                "url": drive_url,
                                "file_id": file_id,
                                "filename": filename_to_find # Confirm filename
                            })
                            logger.info(f"Task {task_id}: {final_message} (ID: {file_id})")
                            break # Exit file search loop

                        await asyncio.sleep(10) # Wait before checking Drive again

                    if not file_found:
                         # File not found after waiting
                         file_not_found_message = f"Export task completed, but file '{filename_to_find}' not found in Drive after {max_file_wait}s."
                         task_statuses[task_id].update({
                             "state": "FILE_NOT_FOUND",
                             "message": file_not_found_message,
                             "url": None, "file_id": None
                         })
                         logger.warning(f"Task {task_id}: {file_not_found_message}")

                except Exception as drive_err:
                    # Error interacting with Drive API
                    drive_error_message = f"Drive API error after task completion: {str(drive_err)}"
                    task_statuses[task_id].update({
                        "state": "DRIVE_API_ERROR",
                        "message": drive_error_message,
                        "url": None, "file_id": None
                    })
                    logger.error(f"Task {task_id}: {drive_error_message}")
                # --- End Find file in Drive ---
                break # Exit monitoring loop (Task completed)

            elif state in ['FAILED', 'CANCELLED']:
                final_message = f"Export {state.lower()}: {error_msg or message}"
                task_statuses[task_id].update({"state": state, "message": final_message})
                logger.error(f"Task {task_id} {state}: {error_msg or message}")
                break # Exit monitoring loop

            # If still RUNNING or READY, wait and check again
            await asyncio.sleep(check_interval)

        except ee.EEException as gee_err:
             logger.error(f"GEE error monitoring task {task_id}: {str(gee_err)}")
             # Decide whether to stop monitoring or keep trying
             await asyncio.sleep(check_interval * 2) # Wait longer after GEE error
        except Exception as e:
            logger.error(f"Unexpected error monitoring task {task_id}: {str(e)}")
            # Stop monitoring on unexpected errors?
            task_statuses[task_id].update({"state": "MONITORING_ERROR", "message": f"Unexpected monitoring error: {str(e)}"})
            break # Exit monitoring loop

     logger.info(f"Monitoring finished for task {task_id}")


async def get_task_progress(task_id: str) -> Dict:
    """Retrieves the current status of an export task from the global dictionary."""
    # Add filename and file_id to the returned status
    status = task_statuses.get(task_id)
    if not status:
        return {"task_id": task_id, "status": "UNKNOWN", "message": "Task ID not found in status tracker."}

    return {
        "task_id": task_id,
        "status": status.get("state", "UNKNOWN"),
        "message": status.get("message", "No message available."),
        "full_res_url": status.get("url"), # This is the Drive webViewLink
        "file_id": status.get("file_id"), # Needed for download
        "filename": status.get("filename") # Useful for caching
    }

async def fetch_image_metadata(
    # Core identification fields
    collection_id: str,
    image_id: Optional[str] = None,
    feature_collection_id: Optional[str] = None,
    
    # Temporal fields
    start_date: Optional[str] = None,
    end_date: Optional[str] = None,
    interval: Optional[str] = None,
    time_scale: Optional[int] = None,
    
    # Spatial and resolution fields
    region: Optional[Union[Dict, str]] = None,
    scale: Optional[int] = 10,
    crs: Optional[str] = None,
    
    # Band selection and visualization
    bands: Optional[List[str]] = None,
    visualization_params: Optional[Dict] = None,
    band_math: Optional[Dict[str, str]] = None,
    min_value: float = 0,
    max_value: float = 3000,
    
    # Data filtering
    filters: Optional[List[Dict]] = None,
    max_cloud_cover: Optional[float] = 20.0,
    images_number: Optional[int] = 10,
    
    # Analysis parameters
    analysis_type: Optional[str] = None,
    index: Optional[str] = None,
    reducer: Optional[str] = None,
    threshold: Optional[float] = None,
    classification_band: Optional[str] = None,
    detailed_metrics: Optional[bool] = False,
    temporal_analysis: Optional[bool] = False,
    
    # Terrain analysis
    dem_collection: Optional[str] = None,
    terrain_params: Optional[Dict] = None,
    
    # Machine learning and classification
    classifier: Optional[Dict] = None,
    training_data: Optional[str] = None,
    
    # Export and output
    place_name: Optional[str] = None,
    export_format: Optional[str] = None,
    export_destination: Optional[str] = None,
    export_params: Optional[Dict] = None,
    dimensions: Optional[Union[int, Dict]] = 3840,  # Can be int (width) or Dict with width and height
    
    # Advanced analysis
    histogram_params: Optional[Dict] = None,
    zonal_stats: Optional[Dict] = None,
    join_params: Optional[Dict] = None,
    
    # Miscellaneous
    custom_code: Optional[str] = None,
    metadata: Optional[bool] = False
) -> Dict:
    """
    Fetches metadata for images in a collection with storage-aware export handling.
    Returns thumbnails and full-res (visualized) images in high resolution.
    Compatible with GeneralEarthEngineRequest2 model.
    """
    try:
        # Get available Google Drive storage (in bytes), fallback to 50MB
        available_storage = 50 * 1024 * 1024
        try:
            available_storage = await get_available_drive_storage()
            logger.debug(f"Available Drive storage: {available_storage / 1024 / 1024:.2f} MB")
        except Exception as e:
            logger.warning(f"Could not determine Drive storage: {e}. Proceeding with caution (assuming 50MB).")

        min_storage_threshold = 50 * 1024 * 1024
        can_export = available_storage >= min_storage_threshold
        if not can_export:
            logger.warning(f"Available storage ({available_storage} bytes) below 50MB threshold. "
                           "Proceeding with thumbnails only, no exports.")

        # Prepare region
        ee_region = None
        if region:
            try:
                ee_region = ee.Geometry(region) if isinstance(region, (dict, str)) else None
                if ee_region:
                    logger.debug(f"🔍 GEE Geometry created: {ee_region.getInfo()}")
            except Exception as parse_error:
                logger.warning(f"⚠️ Could not parse region: {region}. Error: {parse_error}")
                ee_region = None
        
        # Handle core identification - prioritize image_id if provided
        if image_id:
            # If specific image ID is provided, use it directly
            try:
                ee_image = ee.Image(image_id)
                images = [{'id': image_id, 'properties': ee_image.getInfo()['properties']}]
                logger.debug(f"🔍 Using single image with ID: {image_id}")
            except Exception as img_error:
                logger.error(f"❌ Error loading image {image_id}: {str(img_error)}")
                raise ValueError(f"Invalid image ID: {image_id}")
        else:
            # Filter image collection
            collection = ee.ImageCollection(collection_id)
            
            # Apply temporal filters if specified
            if start_date and end_date:
                collection = collection.filterDate(start_date, end_date)
            
            # Apply spatial filters if specified
            if ee_region:
                collection = collection.filterBounds(ee_region)
            
            # Apply cloud cover filter if specified
            if max_cloud_cover is not None:
                collection = collection.filter(ee.Filter.lt("CLOUDY_PIXEL_PERCENTAGE", max_cloud_cover))
            
            # Apply additional custom filters if specified
            if filters:
                for filter_item in filters:
                    filter_type = filter_item.get('type')
                    property_name = filter_item.get('property')
                    value = filter_item.get('value')
                    
                    if not all([filter_type, property_name, value is not None]):
                        logger.warning(f"Skipping invalid filter: {filter_item}")
                        continue
                    
                    if filter_type == 'eq':
                        collection = collection.filter(ee.Filter.eq(property_name, value))
                    elif filter_type == 'gt':
                        collection = collection.filter(ee.Filter.gt(property_name, value))
                    elif filter_type == 'lt':
                        collection = collection.filter(ee.Filter.lt(property_name, value))
                    elif filter_type == 'gte':
                        collection = collection.filter(ee.Filter.gte(property_name, value))
                    elif filter_type == 'lte':
                        collection = collection.filter(ee.Filter.lte(property_name, value))
                    else:
                        logger.warning(f"Unsupported filter type: {filter_type}")
            
            # Get images list
            images_count = images_number if images_number is not None else 10
            images = collection.toList(images_count).getInfo()
            logger.debug(f"🔍 Images retrieved: {len(images)} images found")
        
        # Process images
        processed_images = []
        images_to_export = []
        total_export_size = 0
        
        # Handle visualization parameters
        vis_params = visualization_params.copy() if visualization_params else {}
        # Only set default bands if not specified, but respect user input
        if "bands" not in vis_params and selected_bands:
            vis_params["bands"] = selected_bands[:min(3, len(selected_bands))]
        if "min" not in vis_params:
            vis_params["min"] = min_value
        if "max" not in vis_params:
            vis_params["max"] = max_value
        logger.debug(f"Visualization params: {vis_params}")


        
        # Handle dimensions for export
        export_dimensions = dimensions
        if isinstance(dimensions, dict):
            # If dimensions is a dict, extract width for exports
            export_dimensions = dimensions.get('width', 3840)
        
        # Process each image
        Image_Number = 0
        for image_info in images:
            logger.info(f"🔍 Processing image number: {Image_Number + 1}")
            Image_Number += 1

            ee_image = ee.Image(image_info['id'])
            image_geometry = ee_image.geometry()
            image_region = ee_region if ee_region else image_geometry

            try:
                bounds_info = image_geometry.bounds().getInfo()
            except Exception as geom_error:
                logger.warning(f"Could not get geometry info: {geom_error}")
                bounds_info = None

            # Initialize response variables
            full_res_url = None
            preview_url = None
            export_info = None
            image_size = 0
            analysis_results = {}
            histogram_results = None
            zonal_stats_results = None

            try:
                # Select bands if specified
                selected_bands = bands if bands else ee_image.bandNames().getInfo()
                ee_image_selected = ee_image.select(selected_bands)
                logger.debug(f"Selected bands: {selected_bands}")

                # Apply band math if specified
                available_bands = ee_image_selected.bandNames().getInfo()
                for band in selected_bands:
                    if band not in available_bands:
                        logger.warning(f"Band '{band}' not found in image {image_info['id']}")
                if band_math:
                    for new_band_name, expression in band_math.items():
                        try:
                            # Explicitly map bands to variables in the expression
                            band_map = {band: ee_image_selected.select(band) for band in selected_bands if band in expression}
                            logger.debug(f"Band map for '{new_band_name}': {list(band_map.keys())}")
                            computed_band = ee_image_selected.expression(expression, band_map).rename(new_band_name)
                            ee_image_selected = ee_image_selected.addBands(computed_band)
                            selected_bands.append(new_band_name)
                            logger.debug(f"Added '{new_band_name}' to image. Bands now: {ee_image_selected.bandNames().getInfo()}")
                        except Exception as band_math_error:
                            logger.warning(f"Failed to compute band math '{new_band_name}': {str(band_math_error)}")
                # Compute index if specified
                if index:
                    index_band = None
                    try:
                        if index.upper() == 'NDVI':
                            index_band = ee_image_selected.normalizedDifference(['NIR', 'RED']).rename('NDVI')
                        elif index.upper() == 'EVI':
                            index_band = ee_image_selected.expression(
                                '2.5 * ((NIR - RED) / (NIR + 6 * RED - 7.5 * BLUE + 1))',
                                {
                                    'NIR': ee_image_selected.select('NIR'),
                                    'RED': ee_image_selected.select('RED'),
                                    'BLUE': ee_image_selected.select('BLUE')
                                }
                            ).rename('EVI')
                        elif index.upper() == 'NDWI':
                            index_band = ee_image_selected.normalizedDifference(['GREEN', 'NIR']).rename('NDWI')
                        
                        if index_band:
                            ee_image_selected = ee_image_selected.addBands(index_band)
                            # Add index to selected bands and visualization if it doesn't include bands
                            selected_bands.append(index)
                            if "bands" not in visualization_params:
                                visualization_params["bands"] = [index]
                            analysis_results[index] = True
                    except Exception as index_error:
                        logger.warning(f"Failed to compute index {index}: {index_error}")
                        analysis_results[index] = False
                
                # Apply terrain analysis if dem_collection is specified
                if dem_collection and terrain_params:
                    try:
                        dem = ee.Image(dem_collection)
                        if 'slope' in terrain_params and terrain_params['slope']:
                            slope = ee.Terrain.slope(dem)
                            ee_image_selected = ee_image_selected.addBands(slope.rename('slope'))
                            selected_bands.append('slope')
                        if 'aspect' in terrain_params and terrain_params['aspect']:
                            aspect = ee.Terrain.aspect(dem)
                            ee_image_selected = ee_image_selected.addBands(aspect.rename('aspect'))
                            selected_bands.append('aspect')
                        if 'hillshade' in terrain_params and terrain_params['hillshade']:
                            azimuth = terrain_params.get('azimuth', 315)
                            altitude = terrain_params.get('altitude', 45)
                            hillshade = ee.Terrain.hillshade(dem, azimuth, altitude)
                            ee_image_selected = ee_image_selected.addBands(hillshade.rename('hillshade'))
                            selected_bands.append('hillshade')
                    except Exception as terrain_error:
                        logger.warning(f"Failed to apply terrain analysis: {terrain_error}")
                
                # Apply histogram analysis if specified
                if histogram_params:
                    try:
                        hist_band = histogram_params.get('band', selected_bands[0])
                        hist_bins = histogram_params.get('bins', 50)
                        hist_min = histogram_params.get('min', min_value)
                        hist_max = histogram_params.get('max', max_value)
                        
                        histogram = ee_image_selected.select(hist_band).reduceRegion(
                            reducer=ee.Reducer.histogram(hist_bins, hist_min, hist_max),
                            geometry=image_region,
                            scale=scale,
                            maxPixels=1e9
                        ).getInfo()
                        
                        histogram_results = histogram
                    except Exception as hist_error:
                        logger.warning(f"Failed to compute histogram: {hist_error}")
                
                # Apply zonal statistics if specified
                if zonal_stats and feature_collection_id:
                    try:
                        feature_collection = ee.FeatureCollection(feature_collection_id)
                        stat_reducer = ee.Reducer.mean()
                        if zonal_stats.get('reducer') == 'mean':
                            stat_reducer = ee.Reducer.mean()
                        elif zonal_stats.get('reducer') == 'median':
                            stat_reducer = ee.Reducer.median()
                        elif zonal_stats.get('reducer') == 'sum':
                            stat_reducer = ee.Reducer.sum()
                        elif zonal_stats.get('reducer') == 'min':
                            stat_reducer = ee.Reducer.min()
                        elif zonal_stats.get('reducer') == 'max':
                            stat_reducer = ee.Reducer.max()
                        
                        zonal_stats_results = ee_image_selected.select(selected_bands).reduceRegions(
                            collection=feature_collection,
                            reducer=stat_reducer,
                            scale=scale
                        ).getInfo()
                    except Exception as zonal_error:
                        logger.warning(f"Failed to compute zonal statistics: {zonal_error}")
                
                # Prepare visualization parameters
                vis_params = visualization_params.copy()
                if "bands" not in vis_params and selected_bands:
                    # Use first three bands or fewer if less available
                    vis_params["bands"] = selected_bands[:min(3, len(selected_bands))]
                
                # Visualize image for high-res rendering
                try:
                    vis_image = ee_image_selected.visualize(**vis_params)
                except Exception as vis_error:
                    logger.error(f"Visualization failed: {str(vis_error)}")
                    raise ValueError(f"Visualization error: {str(vis_error)}")

                # ✅ High-res full image 
                download_params = {
                    "format": export_format if export_format else "GEO_TIFF",
                    "region": image_region,
                    "dimensions": export_dimensions
                }
                
                if crs:
                    if not isinstance(crs, str) or not crs.startswith("EPSG:"):
                        raise ValueError(f"Invalid CRS format: {crs}")
                    download_params["crs"] = crs

                full_res_url = vis_image.getDownloadURL(download_params)

                # ✅ Thumbnail preview
                preview_dimensions = export_dimensions // 4 if isinstance(export_dimensions, int) else 960
                preview_url = ee_image_selected.getThumbURL({
                    **vis_params,
                    "region": image_region,
                    "dimensions": preview_dimensions
                })

            except ee.EEException as img_error:
                error_str = str(img_error)
                logger.warning(f"Could not generate direct download URL for {image_info['id']}: {error_str}")

                size_match = re.search(r'Total request size \((\d+) bytes\)', error_str)
                # Check if it's the size limit error AND we have a visualized image
                if size_match and "must be less than or equal to" in error_str and 'vis_image' in locals():
                    image_size = int(size_match.group(1)) # Store the estimated size
                    logger.info(f"Image {image_info['id']} estimated size {image_size / 1024 / 1024:.2f} MB exceeds direct download limit.")

                    logger.info("Checking if exporting is allowed based on storage...")
                    if can_export: # Check if exporting is allowed based on storage
                        total_export_size += image_size # Add to total potential export size
                        logger.info(f"test2 {total_export_size}")

                            # --->>> ADD THIS LOGGING <<<---
                        logger.debug(f"Checking state BEFORE 'if can_export'. Type(total_export_size): {type(total_export_size)}, Value: {total_export_size}")
                        logger.debug(f"Checking state BEFORE 'if can_export'. Type(image_size): {type(image_size)}, Value: {image_size}")
                        logger.debug(f"Checking state BEFORE 'if can_export'. Value(can_export): {can_export}")
                        # --->>> END OF ADDED LOGGING <<<---

                        # Prepare item for export using the VISUALIZED image
                        timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
                        safe_image_id = image_info['id'].replace('/', '_')
                        export_name = f"{place_name}_{safe_image_id}_{timestamp}" if place_name else f"metadata_export_{safe_image_id}_{timestamp}"

                        # --- Prepare export_item dictionary ---
                        export_item = {
                            "image": vis_image,          # <--- KEY CHANGE: Use the visualized image
                            "scale": scale,              # Use the requested scale
                            "region": image_region,      # Use the relevant region
                            "export_name": export_name,
                            "size": image_size,          # Store estimated size for checks
                            # --- Pass export configurations ---
                            "fileFormat": export_format if export_format else "GeoTIFF", # Use requested or default
                            "folder": export_destination if export_destination else "GEE_Images", # Specify folder
                            "maxPixels": 1e13, # Default, consider making configurable
                            # Add CRS if specified for the export
                            "crs": crs if crs else "EPSG:4326"
                        }

                        # Add custom export parameters if provided, potentially overriding defaults
                        if export_params:
                           export_item.update(export_params) # Merge/override with user-defined export params

                        images_to_export.append(export_item)
                        logger.info(f"Queued {image_info['id']} for Drive export as '{export_name}.{export_item['fileFormat']}'")
                        full_res_url = None # Ensure URL is None as we are exporting instead
                    else:
                        logger.warning(f"Image {image_info['id']} too large for download, but export skipped (insufficient storage or export disabled).")
                        full_res_url = None # No URL if too large and not exporting
                else:
                     # Handle other GEE errors during URL generation if necessary
                     logger.warning(f"Non-size related EEException during URL generation for {image_info['id']}: {error_str}")
                     full_res_url = None # No URL on other errors either

            except Exception as url_gen_error:
                 logger.error(f"❌ Unexpected error generating download URL for {image_info['id']}: {url_gen_error}")
                 full_res_url = None # Ensure URL is None on unexpected errors


            # ✅ Thumbnail preview (should still work even if full-res is too large)
            preview_url = None
            try:
                preview_dimensions = export_dimensions // 4 if isinstance(export_dimensions, int) else 960 # Smaller dimensions for preview
                preview_params = {
                    **vis_params, # Use the same visualization parameters
                    "region": image_region,
                    "dimensions": preview_dimensions
                }
                # Use the ORIGINAL ee_image_selected for getThumbURL, as it's generally more reliable
                # and doesn't require the potentially large vis_image to be computed twice if URL fails.
                preview_url = ee_image_selected.getThumbURL(preview_params)
            except Exception as thumb_err:
                logger.warning(f"⚠️ Could not generate preview thumbnail for {image_info['id']}: {thumb_err}")

            # Download and encode preview image if URL was generated
            if preview_url:
                try:
                    response = requests.get(preview_url, timeout=30) # Added timeout
                    response.raise_for_status() # Raise HTTPError for bad responses (4xx or 5xx)
                    if response.status_code == 200:
                        logger.debug(f"Successfully downloaded and encoded preview for {image_info['id']}")
                    else:
                        logger.warning(f"Failed to download preview image for {image_info['id']}. Status code: {response.status_code}")
                except requests.exceptions.RequestException as req_err:
                    logger.warning(f"⚠️ Request failed for preview thumbnail {image_info['id']}: {req_err}")
                except Exception as enc_err:
                     logger.warning(f"⚠️ Error encoding preview image for {image_info['id']}: {enc_err}")

                # Correctly indented except block for the outer try/except handling general image processing errors
                except Exception as other_error:
                    logger.error(f"Unexpected error during processing for {image_info['id']}: {other_error}")
                    # Ensure essential metadata fields are still populated even on error
                    bounds_info = bounds_info if 'bounds_info' in locals() else None # Keep bounds if already fetched

            # Build metadata object (this was already correctly placed outside the inner try/except)
            metadata_obj = {
                "id": image_info["id"],
                "date": ee.Date(image_info["properties"]["system:time_start"]).format("YYYY-MM-dd").getInfo(),
                "cloudCover": image_info["properties"].get("CLOUDY_PIXEL_PERCENTAGE", None),
                "bounds": bounds_info,
                "fullResUrl": full_res_url,
                "previewUrl": preview_url, # Keep the URL for reference if needed
                "exportInfo": export_info,
                "size": image_size
            }
            
            # Add detailed metrics if requested
            if detailed_metrics:
                metadata_obj["properties"] = image_info["properties"]
                if analysis_results:
                    metadata_obj["analysisResults"] = analysis_results
                if histogram_results:
                    metadata_obj["histogram"] = histogram_results
                if zonal_stats_results:
                    metadata_obj["zonalStats"] = zonal_stats_results
            
            processed_images.append(metadata_obj)

        # Handle exports
        if can_export and total_export_size > available_storage:
            logger.warning(f"Total estimated export size ({total_export_size / 1024 / 1024:.2f} MB) exceeds available storage "
                        f"({available_storage / 1024 / 1024:.2f} MB). Skipping exports.")
            can_export = False # Prevent export initiation

        export_results = [] # To store results from export_to_drive calls
        if can_export:
                        timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
                        safe_image_id = image_info['id'].replace('/', '_')
                        base_export_name = place_name if place_name else "metadata_export"
                        export_name = f"{base_export_name}_{safe_image_id}_{timestamp}"
                        try:
                            # Step 1: Create dict with only standard types (NO float, NO GEE objects yet)
                            export_item = {
                                "export_name": export_name,
                                "size": image_size,
                                "scale": scale,
                                "fileFormat": export_format if export_format else "GeoTIFF",
                                "folder": export_destination if export_destination else "GEE_Images",
                            }

                            # Step 2: Add the float
                            export_item["maxPixels"] = 1e13

                            # Step 3: Add CRS (standard type)
                            if crs:
                                export_item["crs"] = crs

                            # Step 4: Add GEE Geometry object
                            export_item["region"] = image_region

                            # Step 5: Add GEE Image object
                            export_item["image"] = vis_image

                            # Step 6: Handle export_params (Dictionary update)
                            if export_params:
                                if isinstance(export_params, dict):
                                    export_item.update(export_params)
                                else:
                                    logger.warning(f"Ignoring non-dictionary 'export_params': {type(export_params)}")

                            images_to_export.append(export_item)
                        except Exception as diag_error:
                            raise # Re-raise to see traceback

                        full_res_url = None # Set outside block
                        
        else:
            logger.warning(f"Non-size related EEException during URL generation for {image_info['id']}: {error_str}")
            full_res_url = None
    
        
        # Prepare result
        result = {
        "images": processed_images,
        "totalResults": len(processed_images),
        # Include overall warnings or status messages
        }
        # Add warnings based on export status
        if not can_export and len(images_to_export) > 0:
            result["warning"] = f"Insufficient storage ({available_storage / 1024 / 1024:.2f}MB) for estimated export size ({total_export_size / 1024 / 1024:.2f}MB). Exports skipped."
        elif len(images_to_export) > 0 and len(export_results) == len(images_to_export):
            result["export_status"] = f"Initiated {len(export_results)} exports to Google Drive."
        elif len(images_to_export) > 0 and len(export_results) < len(images_to_export):
            result["export_status"] = f"Attempted {len(images_to_export)} exports, {len(export_results)} started. Some failed during initiation."
            
        # Add custom analysis results if applicable
        if analysis_type:
            result["analysisType"] = analysis_type
            
        # Add temporal analysis results if requested
        if temporal_analysis and start_date and end_date:
            try:
                # Include some statistics about the temporal range
                date_range = {
                    "startDate": start_date,
                    "endDate": end_date,
                    "totalDays": (datetime.datetime.strptime(end_date, "%Y-%m-%d") - 
                                 datetime.datetime.strptime(start_date, "%Y-%m-%d")).days,
                    "imageFrequency": (datetime.datetime.strptime(end_date, "%Y-%m-%d") - 
                                      datetime.datetime.strptime(start_date, "%Y-%m-%d")).days / 
                                      max(len(processed_images), 1)
                }
                result["temporalAnalysis"] = date_range
            except Exception as date_error:
                logger.warning(f"Could not compute temporal analysis: {date_error}")
        
        return result

    except Exception as e:
        logger.error(f"❌ Error in fetch_image_metadata: {str(e)}")
        raise ValueError(f"GEE Error: {str(e)}")
