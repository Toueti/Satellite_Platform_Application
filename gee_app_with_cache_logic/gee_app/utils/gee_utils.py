import ee
import logging
from typing import Union, Dict, List, Optional
from gee_app.utils.sensor_utils import get_sensor_type,parse_region

from .ee_utils import  get_image_urls

logger = logging.getLogger('gee_app')

# --------------------------------------------
# General Utility Functions
# Suggested file: gee_utils.py
# These are reusable across multiple specific functions
# --------------------------------------------
def validate_gee_asset(asset_id: str) -> bool:
    """Check if a GEE asset (image or collection) exists."""
    try:
        ee.data.getInfo(asset_id)
        logger.debug(f"Validated GEE asset: {asset_id}")
        return True
    except ee.EEException as e:
        logger.warning(f"Invalid GEE asset {asset_id}: {str(e)}")
        return False

async def load_image_or_collection(id: str, is_collection: bool = False, region: Optional[Union[str, Dict]] = None, bands: Optional[List[str]] = None) -> Union[ee.Image, ee.ImageCollection]:
    """
    Loads an image or image collection, optionally clipping to a region and selecting specific bands.
    """
    if is_collection:
        data = ee.ImageCollection(id)
    else:
        data = ee.Image(id)
    if region:
        aoi = parse_region(region)
        if is_collection:
            data = data.filterBounds(aoi).map(lambda img: img.clip(aoi))
        else:
            data = data.clip(aoi)
    if bands:
        data = data.select(bands)
    return data

def compute_index(image: ee.Image, index: str) -> ee.Image:
    """
    Computes a specified index (NDVI, EVI, NDWI) based on the sensor type.
    """
    try:
        # Get the full image ID (e.g., 'COPERNICUS/S2/20210223T102929_20210223T103716_T32TMT')
        image_id = image.get('system:id').getInfo() or image.id().getInfo()
        sensor = get_sensor_type(image_id)
        logger.debug(f"Detected sensor: {sensor} for image {image_id}")
    except Exception as e:
        logger.warning(f"Failed to detect sensor: {str(e)}. Defaulting to Sentinel-2.")
        sensor = 'SENTINEL-2'  # Fallback for your case

    if index == 'NDVI':
        if sensor == 'LANDSAT_8':
            return image.normalizedDifference(['B5', 'B4']).rename('NDVI')
        elif sensor == 'LANDSAT_7_5':
            return image.normalizedDifference(['B4', 'B3']).rename('NDVI')
        elif sensor == 'SENTINEL-2':
            return image.normalizedDifference(['B8', 'B4']).rename('NDVI')
    elif index == 'EVI':
        if sensor == 'LANDSAT_8':
            return image.expression(
                '2.5 * ((NIR - RED) / (NIR + 6 * RED - 7.5 * BLUE + 1))',
                {'NIR': image.select('B5'), 'RED': image.select('B4'), 'BLUE': image.select('B2')}
            ).rename('EVI')
        elif sensor == 'LANDSAT_7_5':
            return image.expression(
                '2.5 * ((NIR - RED) / (NIR + 6 * RED - 7.5 * BLUE + 1))',
                {'NIR': image.select('B4'), 'RED': image.select('B3'), 'BLUE': image.select('B1')}
            ).rename('EVI')
        elif sensor == 'SENTINEL-2':
            return image.expression(
                '2.5 * ((NIR - RED) / (NIR + 6 * RED - 7.5 * BLUE + 1))',
                {'NIR': image.select('B8'), 'RED': image.select('B4'), 'BLUE': image.select('B2')}
            ).rename('EVI')
    elif index == 'NDWI':
        if sensor == 'LANDSAT_8':
            return image.normalizedDifference(['B3', 'B5']).rename('NDWI')
        elif sensor == 'LANDSAT_7_5':
            return image.normalizedDifference(['B2', 'B4']).rename('NDWI')
        elif sensor == 'SENTINEL-2':
            return image.normalizedDifference(['B3', 'B8']).rename('NDWI')
    else:
        raise ValueError(f"Index {index} not supported for sensor {sensor}")
    
    raise ValueError(f"Failed to compute {index} for sensor {sensor}")

def apply_threshold(image: ee.Image, band: str, threshold: float, comparison: str = 'gt') -> ee.Image:
    """
    Applies a threshold to a specific band for detection tasks.
    """
    if comparison == 'gt':
        mask = image.select(band).gt(threshold)
    elif comparison == 'lt':
        mask = image.select(band).lt(threshold)
    else:
        raise ValueError(f"Comparison {comparison} not supported")
    return mask.rename(f"{band}_mask")

async def terrain_analysis(dem_collection: str, analysis_type: str, region: Optional[Union[str, Dict]] = None, scale: int = 30, place_name: Optional[str] = None, **kwargs) -> Dict:
    """
    Performs terrain analysis (slope, aspect, hillshade) on a DEM.
    """
    dem = ee.Image(dem_collection)
    if region:
        aoi = parse_region(region)
        dem = dem.clip(aoi)
    else:
        aoi = dem.geometry()
    
    if analysis_type == 'slope':
        result = ee.Terrain.slope(dem)
    elif analysis_type == 'aspect':
        result = ee.Terrain.aspect(dem)
    elif analysis_type == 'hillshade':
        azimuth = kwargs.get('azimuth', 315)
        altitude = kwargs.get('altitude', 45)
        result = ee.Terrain.hillshade(dem, azimuth, altitude)
    else:
        raise ValueError(f"Unsupported terrain analysis type: {analysis_type}")
    
    return await get_image_urls(result, aoi, [analysis_type], scale, analysis_type.capitalize(), place_name)

async def detect_features(image_id: str, region: Optional[Union[str, Dict]], detection_type: str, threshold: float, scale: int = 30, place_name: Optional[str] = None) -> Dict:
    """
    General function for feature detection (e.g., fire hotspots, water bodies).
    """
    image = await load_image_or_collection(image_id, region=region)
    aoi = parse_region(region) if region else image.geometry()
    
    if detection_type == 'fire_hotspots':
        sensor = get_sensor_type(image_id)
        if sensor == 'LANDSAT_8':
            thermal = image.select('B10')
        elif sensor == 'LANDSAT_7_5':
            thermal_bands = ['B6_VCID_1', 'B6_VCID_2']
            thermal = image.select(thermal_bands).reduce(ee.Reducer.mean())
        elif sensor == 'SENTINEL-2':
            thermal = image.select('B11')
        else:
            logger.warning(f"⚠️ Unsupported sensor {sensor} for {image_id}, defaulting to Sentinel-2 B11")
            thermal = image.select('B11')
        hotspots = apply_threshold(thermal, 'B10' if sensor == 'LANDSAT_8' else 'B11', threshold, 'gt')
        final_image = image.addBands(hotspots)
    elif detection_type == 'water_bodies':
        ndwi = compute_index(image, 'NDWI')
        water_mask = apply_threshold(ndwi, 'NDWI', threshold, 'gt')
        final_image = image.addBands(water_mask)
    else:
        raise ValueError(f"Unsupported detection type: {detection_type}")
    
    return await get_image_urls(final_image, aoi, [f"{detection_type}_mask"], scale, detection_type.capitalize(), place_name)

async def time_series_analysis_util(collection_id: str, region: Optional[Union[str, Dict]], index: str, interval: str, place_name: Optional[str] = None) -> List[Dict]:
    """
    Performs time series analysis on an ImageCollection for a given index.
    """
    collection = ee.ImageCollection(collection_id)
    aoi = parse_region(region) if region else collection.first().geometry()
    collection = collection.filterBounds(aoi)
    
    def calculate_index(image):
        indexed = compute_index(image, index)
        return indexed.set('system:time_start', image.get('system:time_start'))
    
    indexed_collection = collection.map(calculate_index)
    
    if interval == 'month':
        months = ee.List.sequence(1, 12)
        def monthly_mean(m):
            monthly_col = indexed_collection.filter(ee.Filter.calendarRange(m, m, 'month'))
            mean_image = monthly_col.mean()
            stat = mean_image.reduceRegion(reducer=ee.Reducer.mean(), geometry=aoi, scale=30, maxPixels=1e10)
            return ee.Feature(None, {'date': ee.Date.fromYMD(2020, m, 1).format('YYYY-MM-dd'), 'index_value': stat.get(index)})
        features = months.map(monthly_mean).getInfo()
    else:
        raise NotImplementedError(f"Interval {interval} not yet implemented")
    
    return [f['properties'] for f in features['features'] if f is not None]
