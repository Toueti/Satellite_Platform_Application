from functools import lru_cache
import logging
import json
from typing import Union, Dict, Optional, List
import ee

logger = logging.getLogger(__name__)

@lru_cache(maxsize=128)
def get_sensor_type(image_id: str) -> str:
    """
    Determine sensor type using SATELLITE metadata or band fallback, with caching.

    Args:
        image_id (str): Earth Engine image asset ID.

    Returns:
        str: Sensor type ('LANDSAT_8', 'LANDSAT_7_5', 'SENTINEL-2', 'UNKNOWN').
    """
    try:
        image = ee.Image(image_id)
        satellite = image.get('SATELLITE').getInfo()

        # Check metadata first
        if satellite:
            if 'LANDSAT_8' in satellite or 'LANDSAT_9' in satellite:
                return 'LANDSAT_8'
            elif 'LANDSAT_7' in satellite or 'LANDSAT_5' in satellite:
                return 'LANDSAT_7_5'
            elif 'SENTINEL' in satellite:  # Fix: Detect Sentinel-2 correctly
                return 'SENTINEL-2'

        # If SATELLITE property is missing, fall back to band detection
        bands = image.bandNames().getInfo()
        if set(['B8', 'B4', 'B3', 'B2']).issubset(bands):  # Fix: Sentinel-2 correct bands
            return 'SENTINEL-2'
        elif set(['B5', 'B4', 'B3', 'B2']).issubset(bands):  # Landsat-8 bands
            return 'LANDSAT_8'
        elif set(['B4', 'B3', 'B2']).issubset(bands):  # Landsat-7/5 bands
            return 'LANDSAT_7_5'
        
        return 'UNKNOWN'
    
    except Exception as e:
        logger.error(f"Error detecting sensor type for {image_id}: {str(e)}")
        return 'UNKNOWN'
def select_bands_for_satellite(image: ee.Image) -> List[str]:
    """
    Select suitable bands for the specific satellite.
    :param image: ee.Image, the satellite image.
    :return: List[str], the selected bands.
    """
    sensor = get_sensor_type(image)  # This function should return the satellite name (e.g., 'SENTINEL-2', 'LANDSAT-8')

    if sensor == 'SENTINEL-2':
        # Sentinel-2 usually has these bands for analysis
        return ['B4', 'B3', 'B2']  # RGB bands for visual change detection
    elif sensor == 'LANDSAT_8':
        # Landsat 8 bands for analysis
        return ['B4', 'B3', 'B2']  # RGB bands for visual change detection
    elif sensor == 'LANDSAT_7':
        # Landsat 7 bands for analysis
        return ['B4', 'B3', 'B2']  # RGB bands for visual change detection
    else:
        # If the satellite is unknown or unsupported, return an empty list or default bands
        return []
    
def parse_region(region: Union[str, Dict, ee.Geometry, None]) -> Optional[ee.Geometry]:
    """
    Parse and validate GeoJSON region, returning an ee.Geometry object.

    Args:
        region (str | dict | ee.Geometry | None): GeoJSON as string, dict, or None.

    Returns:
        ee.Geometry | None: Converted GEE geometry or None if no region provided.

    Raises:
        ValueError: If GeoJSON is invalid or unsupported.
    """
    if not region:
        logger.error("parse_region received an empty region!")
        return None

    # ✅ If region is already an ee.Geometry, return it directly
    if isinstance(region, ee.Geometry):
        logger.debug("parse_region received an ee.Geometry object, returning it as-is.")
        return region
    
    logger.debug(f"parse_region received: {region}")

    # If region is a string, attempt to parse it as JSON
    if isinstance(region, str):
        try:
            region_dict = json.loads(region)
        except json.JSONDecodeError as e:
            logger.error(f"Invalid GeoJSON string provided: {region} | Error: {str(e)}")
            raise ValueError("Invalid GeoJSON string provided")
    else:
        region_dict = region

    # Validate GeoJSON format
    if not isinstance(region_dict, dict):
        logger.error(f"Region is not a valid dictionary: {region_dict}")
        raise ValueError("GeoJSON must be a dictionary")

    if 'type' not in region_dict or 'coordinates' not in region_dict:
        logger.error(f"GeoJSON missing 'type' or 'coordinates': {region_dict}")
        raise ValueError("GeoJSON must include 'type' and 'coordinates'")

    if region_dict['type'] not in ['Polygon', 'MultiPolygon']:
        logger.error(f"Unsupported GeoJSON type: {region_dict['type']}")
        raise ValueError("GeoJSON type must be 'Polygon' or 'MultiPolygon'")

    # Convert to ee.Geometry
    try:
        if region_dict['type'] == 'Polygon':
            return ee.Geometry.Polygon(region_dict['coordinates'])
        else:  # MultiPolygon
            return ee.Geometry.MultiPolygon(region_dict['coordinates'])
    except Exception as e:
        logger.error(f"Failed to create ee.Geometry: {str(e)}")
        raise ValueError("Failed to convert GeoJSON to Earth Engine Geometry")