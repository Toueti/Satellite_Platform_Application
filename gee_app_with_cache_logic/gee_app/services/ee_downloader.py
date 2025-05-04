import ee
import os
import time
import requests
from datetime import datetime
from pathlib import Path
from typing import List, Dict, Optional, Union
import logging # Import logging

from gee_app.models.ee_request_model import GeneralEarthEngineRequest2

# Get a logger instance
logger = logging.getLogger(__name__)

def download_ee_images(request: GeneralEarthEngineRequest2, output_dir: str = "ee_downloads",
                       wait_time: int = 10, max_retries: int = 3) -> List[str]:
    """
    Download Earth Engine images based on the provided request parameters.
    
    Args:
        request: GeneralEarthEngineRequest object containing parameters
        output_dir: Directory to save downloaded images
        wait_time: Time to wait between download attempts in seconds
        max_retries: Maximum number of retry attempts for download
        
    Returns:
        List of paths to downloaded images
    """
    # Initialize Earth Engine if not already done
    try:
        ee.Initialize()
    except:
        # Skip if already initialized or handle authentication as needed
        pass
    
    # Create output directory if it doesn't exist
    Path(output_dir).mkdir(parents=True, exist_ok=True)
    
    # Determine the data source and get images
    images_list = _get_images(request)
    
    # Limit number of images if specified
    if request.images_number and len(images_list) > request.images_number:
        images_list = images_list[:request.images_number]
    
    # Process bands
    bands = request.bands if request.bands else None
    
    # Get region of interest
    roi = _get_region(request)
    
    # Handle visualization parameters
    vis_params = request.visualization_params if request.visualization_params else {}
    
    # Download each image
    download_paths = []
    total_images = len(images_list)
    logger.info(f"Starting download process for {total_images} images.")

    for idx, img_info in enumerate(images_list): # GEE returns info dicts, not Image objects directly from toList().getInfo()
        img = ee.Image(img_info['id']) # Reconstruct ee.Image object
        logger.info(f"Processing image {idx + 1}/{total_images} (ID: {img_info.get('id', 'N/A')})")

        # Generate filename
        timestamp = ""
        # Use properties from img_info if available
        img_properties = img_info.get('properties', {})
        time_start = img_properties.get('system:time_start')

        try:
            if time_start:
                # GEE timestamps are in milliseconds since epoch
                image_date = ee.Date(time_start).format('YYYY-MM-dd').getInfo()
                timestamp = f"_{image_date}"
            else:
                 # Use current time if no timestamp
                timestamp = f"_{datetime.now().strftime('%Y%m%d%H%M%S')}"
                logger.warning(f"Image {idx + 1} missing 'system:time_start'. Using current time for filename.")
        except Exception as e:
            # Use current time if error getting timestamp
            timestamp = f"_{datetime.now().strftime('%Y%m%d%H%M%S')}"
            logger.warning(f"Error getting timestamp for image {idx + 1}: {e}. Using current time for filename.")
        
        # Include place name if provided
        place_str = f"_{request.place_name}" if request.place_name else ""
        
        # Create filename
        filename = f"ee_image{idx}{timestamp}{place_str}.tif"
        filepath = os.path.join(output_dir, filename)
        
        # Apply band selection if specified
        if bands:
            img = img.select(bands)
        
        # Apply index calculation if requested
        if request.index:
            img = _calculate_index(img, request.index)
        
        # Apply band math if requested
        if request.band_math:
            for new_band, expression in request.band_math.items():
                img = img.addBand(ee.Image().expression(expression, {'RED': img.select('B4'), 'NIR': img.select('B8')}))
        # Apply band selection if specified
        if bands:
            logger.debug(f"Selecting bands: {bands}")
            img = img.select(bands)

        # Apply index calculation if requested
        if request.index:
            logger.debug(f"Calculating index: {request.index}")
            img = _calculate_index(img, request.index)

        # Apply band math if requested
        if request.band_math:
             logger.debug(f"Applying band math: {request.band_math}")
             for new_band, expression in request.band_math.items():
                # Ensure necessary bands exist for expression
                try:
                    img = img.addBand(ee.Image().expression(expression, {'RED': img.select('B4'), 'NIR': img.select('B8')}).rename(new_band))
                except Exception as e:
                    logger.error(f"Error applying band math for '{new_band}' on image {idx + 1}: {e}. Skipping band math.")
                    # Decide if you want to continue without this band or skip the image

        # Get the download URL
        logger.info(f"Generating download URL for image {idx + 1}...")
        try:
            params = {
                'name': filename,
                'scale': request.scale if request.scale else 30,
                'crs': request.crs if request.crs else 'EPSG:4326',
            }
            
            # Add region if specified
            if roi:
                params['region'] = roi
                
            # Add visualization parameters if provided
            if vis_params:
                for key, value in vis_params.items():
                    params[key] = value
            # Get download URL
            url = img.getDownloadURL(params)
            logger.info(f"Successfully generated download URL for image {idx + 1}.")
            logger.debug(f"Image {idx + 1} URL: {url}") # Log URL at debug level

            # Download the image
            logger.info(f"Attempting to download image {idx + 1} to {filepath}...")
            success = False
            for attempt in range(max_retries):
                logger.debug(f"Download attempt {attempt + 1}/{max_retries} for image {idx + 1}")
                try:
                    response = requests.get(url, stream=True, timeout=300) # Add timeout
                    response.raise_for_status() # Raise HTTPError for bad responses (4xx or 5xx)

                    with open(filepath, 'wb') as f:
                        for chunk in response.iter_content(chunk_size=8192): # Larger chunk size
                            # filter out keep-alive new chunks
                            if chunk:
                                f.write(chunk)
                    download_paths.append(filepath)
                    success = True
                    logger.info(f"Successfully downloaded image {idx + 1} to {filepath}")
                    break # Exit retry loop on success
                except requests.exceptions.RequestException as e:
                    logger.warning(f"Error downloading image {idx + 1}, attempt {attempt + 1}: {e}")
                except Exception as e: # Catch other potential errors
                     logger.error(f"Unexpected error downloading image {idx + 1}, attempt {attempt + 1}: {e}")

                # Wait before retrying if not the last attempt
                if attempt < max_retries - 1:
                    logger.info(f"Waiting {wait_time} seconds before retrying download for image {idx + 1}...")
                    time.sleep(wait_time)

            if not success:
                logger.error(f"Failed to download image {idx + 1} after {max_retries} attempts.")
                # Optionally: clean up partially downloaded file if it exists
                if os.path.exists(filepath):
                    try:
                        os.remove(filepath)
                        logger.info(f"Removed partially downloaded file: {filepath}")
                    except OSError as e:
                        logger.error(f"Error removing partial file {filepath}: {e}")

        except ee.EEException as e:
             logger.error(f"Earth Engine error generating download URL for image {idx + 1}: {e}")
        except Exception as e:
            logger.error(f"Unexpected error generating download URL for image {idx + 1}: {e}")

    logger.info(f"Download process finished. Successfully downloaded {len(download_paths)} out of {total_images} images.")
    return download_paths

def _get_images(request: GeneralEarthEngineRequest2) -> List[ee.Image]:
    """Get images based on the request parameters."""
    # Case 1: Single image
    if request.image_id:
        return [ee.Image(request.image_id)]
    
    # Case 2: Image collection
    elif request.collection_id:
        logger.info(f"Fetching collection: {request.collection_id}")
        collection = ee.ImageCollection(request.collection_id)
        logger.debug(f"Initial collection size: {collection.size().getInfo()}") # Log initial size

        # Apply date filtering if dates are provided
        if request.start_date and request.end_date:
            logger.info(f"Applying date filter: {request.start_date} to {request.end_date}")
            collection = collection.filterDate(request.start_date, request.end_date)
            logger.debug(f"Size after date filter: {collection.size().getInfo()}")

        # Apply cloud cover filter if specified
        if request.max_cloud_cover is not None:
            logger.info(f"Applying cloud cover filter: < {request.max_cloud_cover}")
            collection = collection.filter(ee.Filter.lt('CLOUD_COVER', request.max_cloud_cover))
            logger.debug(f"Size after cloud cover filter: {collection.size().getInfo()}")

        # Apply additional filters if specified
        if request.filters:
            logger.info(f"Applying additional filters: {request.filters}")
            for filter_dict in request.filters:
                filter_type = filter_dict.get('type', 'eq') # Default to 'eq' if not specified
                prop = filter_dict.get('property')
                val = filter_dict.get('value')
                if not prop or val is None:
                    logger.warning(f"Skipping invalid filter: {filter_dict}")
                    continue

                logger.debug(f"Applying filter: {prop} {filter_type} {val}")
                if filter_type == 'eq':
                    collection = collection.filter(ee.Filter.eq(prop, val))
                elif filter_type == 'lt':
                    collection = collection.filter(ee.Filter.lt(prop, val))
                elif filter_type == 'gt':
                    collection = collection.filter(ee.Filter.gt(prop, val))
                # Add more filter types as needed
                else:
                     logger.warning(f"Unsupported filter type: {filter_type}")
            logger.debug(f"Size after additional filters: {collection.size().getInfo()}")

        # Get the size before attempting toList
        try:
            collection_size = collection.size().getInfo()
            logger.info(f"Final filtered collection size: {collection_size}")
        except Exception as e:
             logger.error(f"Error getting collection size: {e}")
             raise # Re-raise the exception as we can't proceed

        # Check if the collection is empty before calling toList
        if collection_size == 0:
            logger.warning("Filtered collection is empty. Returning empty list.")
            return [] # Return empty list directly

        # Convert collection to list
        logger.info(f"Converting collection of size {collection_size} to list...")
        try:
            image_list_info = collection.toList(collection_size).map(lambda img: ee.Image(img)).getInfo()
            logger.info(f"Successfully converted collection to list with {len(image_list_info)} images.")
            return image_list_info
        except Exception as e:
            logger.error(f"Error converting collection to list: {e}")
            raise # Re-raise the exception

    # Case 3: Feature collection (return empty list as no images to download)
    elif request.feature_collection_id:
        return []
    
    return []

def _get_region(request: GeneralEarthEngineRequest2) -> Optional[Dict]:
    """Extract region of interest from request."""
    if request.region:
        if isinstance(request.region, str):
            # Assume it's a JSON string
            import json
            return json.loads(request.region)
        else:
            return request.region
    return None

def _calculate_index(image: ee.Image, index_name: str) -> ee.Image:
    """Calculate common indices based on index name."""
    if index_name.upper() == 'NDVI':
        return image.normalizedDifference(['B8', 'B4']).rename('NDVI')
    elif index_name.upper() == 'NDWI':
        return image.normalizedDifference(['B3', 'B8']).rename('NDWI')
    elif index_name.upper() == 'EVI':
        return image.expression(
            '2.5 * ((NIR - RED) / (NIR + 6 * RED - 7.5 * BLUE + 1))',
            {
                'NIR': image.select('B8'),
                'RED': image.select('B4'),
                'BLUE': image.select('B2')
            }
        ).rename('EVI')
    else:
        # Return original image if index not recognized
        return image
