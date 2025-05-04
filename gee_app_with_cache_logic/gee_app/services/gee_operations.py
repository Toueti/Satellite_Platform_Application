from typing import Any
import ee
import logging
import asyncio
from typing import Union, Dict, List, Optional
from gee_app.utils.gee_utils import (terrain_analysis,
    get_image_urls,
    load_image_or_collection ,
    compute_index,
    detect_features,
    time_series_analysis_util
)

from ..utils.ee_utils import parse_region, get_image_urls

logger = logging.getLogger('gee_app')

async def calculate_slope(dem_collection: str = 'USGS/SRTMGL1_003', region: Optional[Union[str, Dict]] = None, scale: int = 30, place_name: Optional[str] = None, visualization_params: Optional[Dict] = None, bands: Optional[List[str]] = None) -> Dict:
    logger.debug(f"Entering calculate_slope with dem_collection: {dem_collection}, region: {region}, scale: {scale}, place_name: {place_name}")
    return await terrain_analysis(dem_collection, 'slope', region, scale, place_name)

async def calculate_aspect(dem_collection: str = 'USGS/SRTMGL1_003', region: Optional[Union[str, Dict]] = None, scale: int = 30, place_name: Optional[str] = None) -> Dict:
    logger.debug(f"Entering calculate_aspect with dem_collection: {dem_collection}, region: {region}, scale: {scale}, place_name: {place_name}")
    return await terrain_analysis(dem_collection, 'aspect', region, scale, place_name)

async def generate_hillshade(dem_collection: str = 'USGS/SRTMGL1_003', region: Optional[Union[str, Dict]] = None, azimuth: float = 315, altitude: float = 45, scale: int = 30, place_name: Optional[str] = None) -> Dict:
    logger.debug(f"Entering generate_hillshade with dem_collection: {dem_collection}, region: {region}, azimuth: {azimuth}, altitude: {altitude}, scale: {scale}, place_name: {place_name}")
    return await terrain_analysis(dem_collection, 'hillshade', region, scale, place_name, azimuth=azimuth, altitude=altitude)

async def calculate_ndvi(
    image_id: str,
    region: Optional[Union[str, Dict]] = None,
    bands: Optional[List[str]] = None,
    scale: int = 30,
    place_name: Optional[str] = None,
    visualization_params: Optional[Dict] = None
) -> Dict:
    """
    Calculate NDVI from an image and return visualization URLs.
    """
    logger.debug(f"Entering calculate_ndvi with image_id: {image_id}, region: {region}, bands: {bands}, scale: {scale}, place_name: {place_name}")
    try:
        # Load the image
        image = await load_image_or_collection(image_id, region=region)
        aoi = parse_region(region) if region else image.geometry()

        # Compute NDVI
        if bands and len(bands) == 2:
            logger.debug(f"Computing NDVI with custom bands: {bands}")
            ndvi = image.normalizedDifference(bands).rename('NDVI')
        else:
            logger.debug("Using default NDVI computation (e.g., B8 and B4 for Sentinel-2)")
            ndvi = compute_index(image, 'NDVI')  # Assumes default NIR= B8, Red= B4 for S2

        # Add NDVI band to the original image
        final_image = image.addBands(ndvi)
        
        # Default visualization params if none provided
        vis_params = visualization_params or {'bands': ['NDVI'], 'min': -1, 'max': 1, 'palette': ['blue', 'white', 'green']}

        # Get URLs
        urls = await get_image_urls(
            image=final_image,
            region=aoi,
            bands=['NDVI'],  # Tell it to visualize the NDVI band
            scale=scale,
            operation="NDVI",
            place_name=place_name or "NDVI_region",
            visualization_params=vis_params
        )
        
        return urls

    except ee.EEException as e:
        logger.error(f"Error in NDVI calculation: {str(e)}")
        raise
    except Exception as e:
        logger.error(f"Unexpected error in NDVI: {str(e)}", exc_info=True)
        raise

async def calculate_evi(
    image_id: str,
    region: Optional[Union[str, Dict]] = None,
    scale: int = 30,
    place_name: Optional[str] = None
) -> Dict:
    """
    Calculate EVI from an image and return visualization URLs.
    """
    logger.debug(f"Entering calculate_evi with image_id: {image_id}, region: {region}, scale: {scale}, place_name: {place_name}")
    try:
        # Load the image
        image = await load_image_or_collection(image_id, region=region)
        aoi = parse_region(region) if region else image.geometry()

        # Compute EVI
        logger.debug(f"Computing EVI for image {image_id}")
        evi = compute_index(image, 'EVI')
        
        # Debug bands
        evi_bands = evi.bandNames().getInfo()
        logger.debug(f"EVI bands after compute_index: {evi_bands}")
        if 'EVI' not in evi_bands:
            raise ValueError("EVI band not created by compute_index")

        # Add EVI band
        final_image = image.addBands(evi)
        final_bands = final_image.bandNames().getInfo()
        logger.debug(f"Final image bands: {final_bands}")

        # Visualization params
        vis_params = {'bands': ['EVI'], 'min': 0, 'max': 1, 'palette': ['red', 'white', 'green']}

        # Get URLs
        logger.debug(f"Calling get_image_urls with bands=['EVI']")
        urls = await get_image_urls(
            image=final_image,
            region=aoi,
            bands=['EVI'],
            scale=scale,
            operation="EVI",
            place_name=place_name or "EVI_region",
            visualization_params=vis_params
        )
        
        return urls

    except ee.EEException as e:
        logger.error(f"Error in EVI calculation: {str(e)}")
        raise
    except Exception as e:
        logger.error(f"Unexpected error in EVI: {str(e)}", exc_info=True)
        raise

async def analyze_agriculture(image_id: str, region: Optional[Union[str, Dict]] = None, scale: int = 30, place_name: Optional[str] = None) -> Dict:
    logger.debug(f"Entering analyze_agriculture with image_id: {image_id}, region: {region}, scale: {scale}, place_name: {place_name}")
    image = await load_image_or_collection(image_id, region=region)
    aoi = parse_region(region) if region else image.geometry()
    ndvi = compute_index(image, 'NDVI')
    evi = compute_index(image, 'EVI')
    combined = image.addBands([ndvi, evi])
    
    loop = asyncio.get_running_loop()
    stats = await loop.run_in_executor(
        None,
        lambda: combined.reduceRegion(
            reducer=ee.Reducer.mean().combine(reducer2=ee.Reducer.stdDev(), sharedInputs=True),
            geometry=aoi,
            scale=scale,
            maxPixels=1e10
        ).getInfo()
    )
    
    ndvi_urls = await get_image_urls(combined.select('NDVI'), aoi, ['NDVI'], scale, "NDVI", place_name)
    evi_urls = await get_image_urls(combined.select('EVI'), aoi, ['EVI'], scale, "EVI", place_name)
    
    return {
        "stats": stats,
        "ndvi_thumb_url": ndvi_urls["thumb_url"],
        "evi_thumb_url": evi_urls["thumb_url"]
    }

async def detect_water_bodies(image_id: str, region: Optional[Union[str, Dict]] = None, threshold: float = 0.3, scale: int = 30, place_name: Optional[str] = None) -> Dict:
    logger.debug(f"Entering detect_water_bodies with image_id: {image_id}, region: {region}, threshold: {threshold}, scale: {scale}, place_name: {place_name}")
    return await detect_features(image_id, region, 'water_bodies', threshold, scale, place_name)

async def detect_fire_hotspots(image_id: str, region: Optional[Union[str, Dict]], threshold: float = 340, scale: int = 30, place_name: Optional[str] = None) -> Dict:
    logger.debug(f"Entering detect_fire_hotspots with image_id: {image_id}, region: {region}, threshold: {threshold}, scale: {scale}, place_name: {place_name}")
    return await detect_features(image_id, region, 'fire_hotspots', threshold, scale, place_name)

async def time_series_analysis(collection_id: str, region: Optional[Union[str, Dict]], index: str, interval: str, place_name: Optional[str] = None) -> List[Dict]:
    logger.debug(f"Entering time_series_analysis with collection_id: {collection_id}, region: {region}, index: {index}, interval: {interval}, place_name: {place_name}")
    return await time_series_analysis_util(collection_id, region, index, interval, place_name)

async def process_image(image: ee.Image, image_id: str, aoi: ee.Geometry, scale: float) -> tuple[str, Dict]:
    """Process a single image asynchronously."""
    try:
        image = image.clip(aoi)
        thumb_url = get_image_urls(image, aoi, scale, None, None, None, None)[0]
        return image_id, {"thumb_url": thumb_url}
    except Exception as e:
        return image_id, {"error": str(e)}

async def batch_ingest_data(
    region: Dict,
    scale: float,
    max_items: int,
    image_ids: List[str] = None,
    collection_id: str = None,
    start_date: str = None,
    end_date: str = None,
    cloud_cover_max: float = 100.0
) -> Dict[str, Any]:
    """Handle batch ingestion of images and collections with raw parameters."""
    results = {}
    aoi = ee.Geometry(region)

    # Process individual image_ids
    if image_ids:
        tasks = []
        for image_id in image_ids[:max_items]:
            image = load_image_or_collection(image_id, None, None, None, 100.0)
            tasks.append(process_image(image, image_id, aoi, scale))

        processed = await asyncio.gather(*tasks, return_exceptions=True)
        for image_id, result in processed:
            results[image_id] = result

    # Process collection_id with filters
    if collection_id:
        collection = load_image_or_collection(
            None, collection_id, start_date, end_date, cloud_cover_max
        )
        collection = collection.filterBounds(aoi).map(lambda img: img.clip(aoi))
        image_list = collection.toList(max_items)
        image_count = image_list.length().getInfo()

        tasks = []
        for i in range(min(image_count, max_items)):
            image = ee.Image(image_list.get(i))
            image_id = image.get('system:index').getInfo()
            tasks.append(process_image(image, image_id, aoi, scale))

        processed = await asyncio.gather(*tasks, return_exceptions=True)
        for image_id, result in processed:
            results[image_id] = result

    return results
    """Handle batch ingestion of images and collections."""
    results = {}
    scale = batch_request.scale or 30
    max_items = batch_request.max_items

    # Process individual image_ids
    if image_ids:
        logger.info(f"Processing {len(image_ids)} image_ids")
        tasks = []
        for image_id in image_ids[:max_items]:
            image = load_image_or_collection(image_id, None, None, None, 100.0)
            tasks.append(process_image(image, image_id, aoi, scale))

        processed = await asyncio.gather(*tasks, return_exceptions=True)
        for image_id, result in processed:
            results[image_id] = result
            if "error" not in result:
                storage_data = {
                    'image_id': image_id,
                    'collection_id': None,
                    'region': batch_request.region,
                    'scale': scale,
                    'analysis_type': 'raw',
                    'timestamp': datetime.datetime.utcnow().isoformat(),
                    'result': result,
                    'status': 'completed'
                }
                logger.info(f"Ingested {image_id}")
            else:
                logger.warning(f"Skipped storing {image_id} due to error")

    # Process collection_id with filters
    if collection_id:
        logger.info(f"Processing collection {collection_id}")
        collection = load_image_or_collection(
            None, collection_id,
            batch_request.start_date, batch_request.end_date,
            batch_request.cloud_cover_max
        )
        collection = collection.filterBounds(aoi).map(lambda img: img.clip(aoi))
        image_list = collection.toList(max_items)
        image_count = image_list.length().getInfo()
        logger.info(f"Found {image_count} images in collection")

        tasks = []
        for i in range(min(image_count, max_items)):
            image = ee.Image(image_list.get(i))
            image_id = image.get('system:index').getInfo()
            tasks.append(process_image(image, image_id, aoi, scale))

        processed = await asyncio.gather(*tasks, return_exceptions=True)
        for image_id, result in processed:
            results[image_id] = result
            if "error" not in result:
                storage_data = {
                    'image_id': image_id,
                    'collection_id': collection_id,
                    'region': batch_request.region,
                    'scale': scale,
                    'analysis_type': 'raw',
                    'timestamp': datetime.datetime.utcnow().isoformat(),
                    'result': result,
                    'status': 'completed'
                }
                logger.info(f"Ingested {image_id} from {collection_id}")
            else:
                logger.warning(f"Skipped storing {image_id} due to error")

    return results