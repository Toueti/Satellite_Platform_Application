import ee
import logging
import math
import asyncio
from typing import Union, Dict, List, Optional
from gee_app.services.gee_helpers import (calculate_drought_trend, 
    get_diversity_interpretation, 
    get_fragmentation_interpretation, 
    get_landscape_integrity_score, 
    perform_temporal_analysis,
    classify_severity
)
from gee_app.utils.ee_utils import get_image_urls, parse_region
from gee_app.utils.gee_utils import (
    load_image_or_collection,
    compute_index,
    time_series_analysis_util
)
from gee_app.utils.sensor_utils import select_bands_for_satellite


logger = logging.getLogger('gee_app')

async def detect_change_between_images(image_id_before: str, image_id_after: str, region: Optional[Union[str, Dict]] = None, bands: Optional[List[str]] = None, threshold: float = 0.2, scale: int = 30, place_name: Optional[str] = None) -> Dict:
    logger.debug(f"Entering detect_change_between_images with image_id_before: {image_id_before}, image_id_after: {image_id_after}, region: {region}, bands: {bands}, threshold: {threshold}, scale: {scale}, place_name: {place_name}")
    try:
        # Load the images
        image_before = ee.Image(image_id_before)
        image_after = ee.Image(image_id_after)

        # Ensure valid bands (if not passed)
        if bands:
            image_before = image_before.select(bands)
            image_after = image_after.select(bands)
        else:
            bands = select_bands_for_satellite(image_before)
            logger.debug(f"Selected bands: {bands}")
            image_before = image_before.select(bands)
            image_after = image_after.select(bands)

        # Define the area of interest (AOI)
        aoi = parse_region(region) if region else image_before.geometry()
        
        # Compute the difference between the two images
        diff = image_after.subtract(image_before).abs()
        # Generate new band names for the difference image based on the selected bands
        diff_band_names = [f"difference_{b}" for b in bands]
        diff = diff.rename(diff_band_names)
        
        logger.debug(f"Difference image bands: {diff.bandNames().getInfo()}")
        

        # Calculate mean difference
        diff_mean = diff.reduce(ee.Reducer.mean())

        # Apply threshold to get the change mask
        change_mask = diff_mean.gt(threshold).rename('change_mask')

        # Clip the final image to the area of interest (if specified)
        final_image = change_mask.clip(aoi) if aoi else change_mask

        # Gather statistics about the change
        stats = {}
        if aoi:
            # Calculate change area in square meters
            change_area = change_mask.multiply(ee.Image.pixelArea()).reduceRegion(
                reducer=ee.Reducer.sum(),
                geometry=aoi,
                scale=scale,
                maxPixels=1e10
            ).get('change_mask')

            # Calculate total area in square meters
            total_area = ee.Image.pixelArea().reduceRegion(
                reducer=ee.Reducer.sum(),
                geometry=aoi,
                scale=scale,
                maxPixels=1e10
            ).get('area')

            # Calculate the percentage of change
            change_percentage = ee.Number(change_area).divide(total_area).multiply(100)

            # Mean, max, and min of the difference image (optional)
            diff_min = diff.reduce(ee.Reducer.min())
            diff_max = diff.reduce(ee.Reducer.max())
            diff_mean_value = diff_mean.getInfo()
            diff_min_value = diff_min.getInfo()
            diff_max_value = diff_max.getInfo()

            stats = {
                'change_area_sq_meters': ee.Number(change_area).getInfo(),
                'change_area_hectares': ee.Number(change_area).divide(10000).getInfo(),
                'total_area_sq_meters': ee.Number(total_area).getInfo(),
                'change_percentage': change_percentage.getInfo(),
                'diff_mean_value': diff_mean_value,
                'diff_min_value': diff_min_value,
                'diff_max_value': diff_max_value
            }

        # Generate URLs using the correct difference band names
        before_url = await get_image_urls(image_before, aoi, bands, scale, "BeforeImage", place_name)
        after_url = await get_image_urls(image_after, aoi, bands, scale, "AfterImage", place_name)
        diff_url = await get_image_urls(diff, aoi, diff_band_names, scale, "DifferenceImage", place_name)
        change_mask_url = await get_image_urls(final_image, aoi, ['change_mask'], scale, "ChangeMask", place_name)
            
        # Return all results
        return {
            **before_url,
            **after_url,
            **diff_url,
            **change_mask_url,
            'stats': stats
        }
    except Exception as e:
        logger.error(f"Change detection processing failed: {e}")
        raise ValueError(f"Error detecting change: {str(e)}")

async def zonal_statistics(image_id: str, region: Optional[Union[str, Dict]], scale: int = 30, place_name: Optional[str] = None) -> Dict:
    logger.debug(f"Entering zonal_statistics with image_id: {image_id}, region: {region}, scale: {scale}, place_name: {place_name}")
    image = ee.Image(image_id)
    aoi = parse_region(region) if region else image.geometry()
    clipped_image = image.clip(aoi)
    stats = clipped_image.reduceRegion(
        reducer=ee.Reducer.mean().combine(
            reducer2=ee.Reducer.stdDev(), sharedInputs=True
        ).combine(
            reducer2=ee.Reducer.minMax(), sharedInputs=True
        ),
        geometry=aoi,
        scale=scale,
        maxPixels=1e10
    )
    return stats.getInfo()

async def generate_histogram(image_id: str, region: Optional[Union[str, Dict]], band: str, bins: int = 50, scale: int = 30, place_name: Optional[str] = None) -> Dict:
    logger.debug(f"Entering generate_histogram with image_id: {image_id}, region: {region}, band: {band}, bins: {bins}, scale: {scale}, place_name: {place_name}")
    image = ee.Image(image_id)
    aoi = parse_region(region) if region else image.geometry()
    clipped_image = image.clip(aoi)
    histogram = clipped_image.select(band).reduceRegion(
        reducer=ee.Reducer.histogram(bins),
        geometry=aoi,
        scale=scale,
        maxPixels=1e10
    )
    return histogram.getInfo()

async def land_cover_analysis(
    image_id: str,  
    region: Optional[Union[str, Dict]] = None,
    scale: int = 10,
    place_name: Optional[str] = None,
    classification_band: str = None,  
    temporal_analysis: bool = False,
    detailed_metrics: bool = True
) -> Dict:
    """Perform comprehensive land cover analysis with ecological metrics."""
    logger.debug(f"Entering land_cover_analysis with image_id: {image_id}, region: {region}, scale: {scale}, place_name: {place_name}, classification_band: {classification_band}, temporal_analysis: {temporal_analysis}, detailed_metrics: {detailed_metrics}")
    try:
        # Validate inputs
        if not image_id:
            raise ValueError("Image ID is required for land cover analysis.")
        
        loop = asyncio.get_running_loop()

        # Load image and validate bands
        image = ee.Image(image_id)
        image_bands = await loop.run_in_executor(None, lambda: image.bandNames().getInfo())
        
        # Auto-detect classification band if not specified
        if not classification_band:
            # Check for common classification band names
            possible_bands = ['classification', 'Class', 'landcover', 'LC_Type1']
            for band in possible_bands:
                if band in image_bands:
                    classification_band = band
                    break
            else:
                # If no classification band found, use NDVI as example
                classification_band = 'NDVI'
                # Create a simple vegetation index if using raw image
                if 'B8' in image_bands and 'B4' in image_bands:
                    image = image.addBands(
                        image.normalizedDifference(['B8', 'B4']).rename('NDVI'))
                    image_bands.append('NDVI')
                elif 'NIR' in image_bands and 'Red' in image_bands:
                    image = image.addBands(
                        image.normalizedDifference(['NIR', 'Red']).rename('NDVI'))
                    image_bands.append('NDVI')

        # Validate the band exists
        if classification_band not in image_bands:
            available = ", ".join(image_bands)
            suggestions = {
                'Sentinel-2': 'Try B2 (Blue), B3 (Green), B4 (Red), B8 (NIR), or NDVI',
                'Landsat': 'Try SR_B2, SR_B3, SR_B4, or NDVI',
                'MODIS': 'Try NDVI, EVI, or Land_Cover_Type_1'
            }
            dataset_type = 'Sentinel-2' if 'B2' in image_bands else 'Unknown'
            raise ValueError(
                f"Classification band '{classification_band}' not found in {dataset_type} image. "
                f"Available bands: {available}. {suggestions.get(dataset_type, '')}"
            )
            
        # Define AOI (Area of Interest)
        if region is None:
            aoi = ee.Geometry.Rectangle([-180, -90, 180, 90], None, False)
            logger.info("Using global coverage with scale=1000m")
            scale, max_pixels = 1000, 1e13
        else:
            aoi = parse_region(region)
            max_pixels = 1e13
            
        # Detect if band contains continuous values
        band_values = await loop.run_in_executor(
            None,
            lambda: image.select(classification_band).reduceRegion(
                reducer=ee.Reducer.minMax(),
                geometry=aoi,
                scale=scale,
                maxPixels=max_pixels
            ).getInfo()
        )
        
        band_min = band_values.get(f"{classification_band}_min", 0)
        band_max = band_values.get(f"{classification_band}_max", 1)
        
        logger.info(f"Band value range: min={band_min}, max={band_max}")

        # Determine if we're dealing with NDVI or a classification
        is_ndvi = classification_band == 'NDVI'
        is_continuous = (
            isinstance(band_min, float) or 
            isinstance(band_max, float) or 
            (band_max - band_min) > 20
        )
        
        # Define the classification approach
        num_classes = 7  # Default number of bins
        
        if is_ndvi or is_continuous:
            logger.info(f"Continuous values detected in {classification_band}. Binning into {num_classes} classes.")
            
            # Create a proper classification from NDVI or other continuous values
            if is_ndvi:
                # NDVI typically ranges from -1 to 1
                # Create balanced classes across the NDVI range
                class_image = image.select(classification_band)
                
                # Properly bin NDVI values from -1 to 1 into classes 0 to (num_classes-1)
                classified = class_image.expression(
                    "value < -0.3 ? -1 : " +  # Water/negative NDVI
                    "value < 0.0 ? 0 : " +    # Bare/sparse
                    "value < 0.2 ? 1 : " +    # Grassland
                    "value < 0.4 ? 2 : " +    # Shrubland
                    "value < 0.6 ? 3 : " +    # Tree cover
                    "value < 0.8 ? 4 : 5",    # Dense and very dense vegetation
                    {'value': class_image}
                ).rename('classification')
            else:
                # For other continuous values, normalize to 0-1 range then bin
                normalized = (image.select(classification_band)
                            .subtract(band_min)
                            .divide(band_max - band_min))
                
                # Create num_classes-1 balanced classes from 0 to num_classes-1
                classified = (normalized
                            .multiply(num_classes - 1)
                            .floor()
                            .rename('classification'))
            
            # Add the classified band to the image
            image = image.addBands(classified)
            classification_band = 'classification'
            
            # Get updated frequency histogram after classification
            freq_hist = await loop.run_in_executor(
                None,
                lambda: image.select('classification').reduceRegion(
                    reducer=ee.Reducer.frequencyHistogram(),
                    geometry=aoi,
                    scale=scale,
                    maxPixels=max_pixels
                ).getInfo()
            )
            logger.info(f"Classification histogram: {freq_hist}")
            
            # Define land cover classes based on the classification approach
            land_cover_dict = {
                -1: {'name': 'Water/Negative NDVI', 'description': 'Water or low vegetation', 'ecological_value': 'Variable'},
                0: {'name': 'Bare/Sparse', 'description': 'Sparse vegetation', 'ecological_value': 'Low'},
                1: {'name': 'Grassland', 'description': 'Herbaceous vegetation', 'ecological_value': 'Moderate'},
                2: {'name': 'Shrubland', 'description': 'Shrubs <5m tall', 'ecological_value': 'Moderate'},
                3: {'name': 'Tree Cover', 'description': 'Areas dominated by trees', 'ecological_value': 'High'},
                4: {'name': 'Dense Vegetation', 'description': 'High NDVI areas', 'ecological_value': 'High'},
                5: {'name': 'Very Dense Vegetation', 'description': 'Very high NDVI', 'ecological_value': 'High'}
            }
        else:
            # For pre-classified images, use the original band
            logger.info(f"Using pre-classified data in band {classification_band}")
            image = image.select(classification_band).rename('classification')
            classification_band = 'classification'
            
            # Default land cover class definitions for pre-classified data
            land_cover_dict = {
                0: {'name': 'Water', 'description': 'Permanent water', 'ecological_value': 'High'},
                1: {'name': 'Bare/sparse', 'description': 'Sparse vegetation', 'ecological_value': 'Low'},
                2: {'name': 'Grassland', 'description': 'Herbaceous vegetation', 'ecological_value': 'Moderate'},
                3: {'name': 'Shrubland', 'description': 'Shrubs <5m tall', 'ecological_value': 'Moderate'},
                4: {'name': 'Cropland', 'description': 'Active agriculture', 'ecological_value': 'Variable'},
                5: {'name': 'Built-up', 'description': 'Urban areas', 'ecological_value': 'Low'},
                6: {'name': 'Tree cover', 'description': 'Areas dominated by trees', 'ecological_value': 'High'}
            }

        # Get total area
        total_area_result = await loop.run_in_executor(
            None,
            lambda: ee.Image.pixelArea().reduceRegion(
                reducer=ee.Reducer.sum(),
                geometry=aoi,
                scale=scale,
                maxPixels=max_pixels
            ).getInfo()
        )
        total_area = total_area_result.get('area', 0)

        if total_area <= 0:
            raise ValueError("AOI appears to be empty or invalid")

        # Initialize results structure
        stats = {
            'summary': {
                'total_area_sq_m': total_area,
                'unclassified_percentage': 0.0,
                'dominant_class': None,
                'shannon_index': 0.0,
                'fragmentation_index': 0.0
            },
            'classes': {},
            'visualization': {}
        }

        # Class processing
        class_proportions = []
        total_classified = 0
        max_percentage = 0

        for class_id, class_info in land_cover_dict.items():
            # Create a binary mask for this class
            class_mask = image.select('classification').eq(class_id)
            
            # Get class area
            area_result = await loop.run_in_executor(
                None,
                lambda: class_mask.multiply(ee.Image.pixelArea())
                .reduceRegion(
                    reducer=ee.Reducer.sum(),
                    geometry=aoi,
                    scale=scale,
                    maxPixels=max_pixels
                ).getInfo()
            )
            area = area_result.get('classification', 0)

            # Ensure area is valid
            area = max(0, min(area, total_area - total_classified))
            total_classified += area

            percentage = (area / total_area) * 100 if total_area else 0

            # Track proportions for diversity index
            if percentage > 0:
                class_proportions.append(percentage / 100)

            # Update dominant class
            if percentage > max_percentage:
                max_percentage = percentage
                stats['summary']['dominant_class'] = class_info['name']

            # Store class data
            stats['classes'][class_info['name']] = {
                'area_sq_m': area,
                'percentage': round(percentage, 2),
                'description': class_info['description'],
                'ecological_value': class_info.get('ecological_value', 'Unknown')
            }
            
            # Detailed metrics
            if detailed_metrics and percentage > 0.1:  # Only calculate for classes with significant presence
                max_error = max(1, scale / 10)
                
                # Calculate edge length with proper error handling
                logger.debug(f"Calculating edge length for class {class_id} with maxError={max_error}")
                try:
                    # Use reduceToVectors with proper parameters
                    edge_length = await loop.run_in_executor(
                        None,
                        lambda: class_mask.reduceToVectors(
                            geometry=aoi,
                            scale=scale,
                            geometryType='polygon',
                            eightConnected=False,
                            maxPixels=max_pixels
                        ).geometry().perimeter(maxError=max_error).getInfo()
                    )
                except ee.EEException as e:
                    logger.warning(f"Edge length calculation failed: {str(e)}")
                    edge_length = 0
                
                # Patch count - FIXED implementation
                try:
                    # Use connectedComponents properly
                    connected_components = class_mask.connectedComponents(
                        connectedness=ee.Kernel.plus(1),  # 4-connected
                        maxSize=int(1e9) 
                    )
                    
                    # Count distinct components
                    patch_count_result = await loop.run_in_executor(
                        None,
                        lambda: connected_components.select('labels').reduceRegion(
                            reducer=ee.Reducer.countDistinct(),
                            geometry=aoi,
                            scale=scale,
                            maxPixels=max_pixels
                        ).getInfo()
                    )
                    patch_count = patch_count_result.get('labels', 0)
                except ee.EEException as e:
                    logger.warning(f"Patch count calculation failed: {str(e)}")
                    patch_count = 0

                stats['classes'][class_info['name']].update({
                    'edge_length_m': edge_length,
                    'patch_count': patch_count
                })

        # Handle unclassified area
        unclassified = max(0, total_area - total_classified)
        unclassified_pct = (unclassified / total_area) * 100 if total_area else 0
        stats['summary']['unclassified_percentage'] = round(unclassified_pct, 2)
        
        if unclassified_pct > 0.1:  # Only add as a class if significant
            stats['classes']['Unclassified'] = {
                'area_sq_m': unclassified,
                'percentage': round(unclassified_pct, 2)
            }

        # Calculate Shannon diversity index
        if class_proportions:
            # Use proper handling for edge cases
            shannon = -sum(p * math.log(p) if p > 0 else 0 for p in class_proportions)
            max_shannon = math.log(len(class_proportions)) if len(class_proportions) > 0 else 1
            stats['summary']['shannon_index'] = round(shannon / max_shannon, 3)  # Normalized

        # Fragmentation index (edge density)
        if detailed_metrics:
            total_edge = sum(c.get('edge_length_m', 0) for c in stats['classes'].values())
            stats['summary']['fragmentation_index'] = round(total_edge / (total_area or 1), 4)

        # Generate visualization
        viz_params = {
            'min': min(land_cover_dict.keys()),
            'max': max(land_cover_dict.keys()),
            'palette': ['#0000FF', '#FFFF00', '#00FF00', '#228B22', '#A0522D', '#808080', '#008000', '#006400']
        }
        
        stats['visualization']['tile_url'] = await get_image_urls(
            image.select('classification').clip(aoi),
            aoi,
            ['classification'],
            scale,
            "LandCover",
            place_name,
            viz_params  # Pass visualization parameters
        )

        # Add interpretation data
        stats['interpretation'] = {
            'diversity': get_diversity_interpretation(stats['summary']['shannon_index']),
            'fragmentation': get_fragmentation_interpretation(stats['summary']['fragmentation_index']),
            'landscape_integrity': get_landscape_integrity_score(
                stats['summary']['shannon_index'], 
                stats['summary']['fragmentation_index'],
                max_percentage
            )
        }

        # Temporal analysis example
        if temporal_analysis:
            stats['temporal'] = await perform_temporal_analysis(image, aoi, scale)

        return stats

    except Exception as e:
        logger.error(f"Land cover analysis failed: {str(e)}", exc_info=True)
        raise RuntimeError(f"Analysis error: {str(e)}")

async def drought_analysis_vegetation(
    image_id: str,
    collection_id: str,
    region: Optional[Union[str, Dict]],
    index: str,
    start_date: str,
    end_date: str,
    interval: str,
    scale:str,
    place_name: Optional[str] = None
) -> Dict:
    logger.debug(f"Entering drought_analysis_vegetation with image_id: {image_id}, collection_id: {collection_id}, region: {region}, index: {index}, start_date: {start_date}, end_date: {end_date}, interval: {interval}, scale: {scale}, place_name: {place_name}")
    default_aoi = ee.Geometry.Rectangle([-180, -90, 180, 90])
    aoi = parse_region(region) if region else default_aoi
    is_default_aoi = aoi.getInfo() == default_aoi.getInfo()
    vis_aoi = ee.Geometry.Rectangle([-122.75, 37.6, -122.35, 37.9])
    scale = 500 if region else 5000

    # Single image mode
    if image_id:
        image = await load_image_or_collection(image_id, region=region)
        indexed = compute_index(image, index) if index in ['NDVI', 'EVI', 'NDWI'] else image.select(index).rename(index)
        stat = indexed.reduceRegion(
            reducer=ee.Reducer.mean(),
            geometry=aoi,
            scale=scale,
            maxPixels=1e9,
            bestEffort=True
        ).getInfo()
        date = ee.Date(image.get('system:time_start')).format('YYYY-MM-dd').getInfo()
        result = [{'date': date, 'anomaly': stat.get(index) or None}]
        
        severity_counts = {'extreme': 0, 'severe': 0, 'moderate': 0, 'mild': 0}
        for item in result:
            if item['anomaly'] is not None:
                severity = classify_severity(item['anomaly'], is_spi=False)
                severity_counts[severity] += 1
        
        trend = calculate_drought_trend(result, value_key='anomaly')
        image_urls = await get_image_urls(indexed, vis_aoi, [index], scale, f"Drought_{index}", place_name)
        return {
            "time_series": result,
            "severity": severity_counts,
            "trend": trend,
            "resultImageUrl": image_urls["thumb_url"]
        }

    # Collection mode
    if not (start_date and end_date):
        raise ValueError("Start and end dates required for collection mode")
    
    collection = await load_image_or_collection(collection_id, is_collection=True, region=region)
    collection = collection.filterDate(start_date, end_date)
    
    # Use time_series_analysis_util with custom anomaly calculation
    def calculate_anomaly(image):
        indexed = compute_index(image, index) if index in ['NDVI', 'EVI', 'NDWI'] else image.select(index).rename(index)
        month = ee.Date(image.get('system:time_start')).get('month')
        monthly_mean = ee.ImageCollection.fromImages(
            ee.List.sequence(1, 12).map(
                lambda m: collection.filter(ee.Filter.calendarRange(m, m, 'month')).mean().set('month', m)
            )
        ).filter(ee.Filter.eq('month', month)).first()
        if not monthly_mean:
            logger.warning(f"No mean image for month {month.getInfo()} in {collection_id}")
            return None
        anomaly = indexed.subtract(monthly_mean)
        return anomaly.set('system:time_start', image.get('system:time_start'))

    anomaly_collection = collection.map(calculate_anomaly).filter(ee.Filter.notNull(['system:time_start']))
    result = await time_series_analysis_util(
        collection_id=collection_id,
        region=region,
        index=index,
        interval=interval,
        place_name=place_name
    )  # Simplified, assumes monthly for now
    
    # Adjust result for anomalies
    severity_counts = {'extreme': 0, 'severe': 0, 'moderate': 0, 'mild': 0}
    for item in result:
        severity = classify_severity(item['index_value'], is_spi=False)
        severity_counts[severity] += 1
    
    trend = calculate_drought_trend(result, value_key='index_value')
    mean_anomaly_image = anomaly_collection.mean()
    image_urls = await get_image_urls(mean_anomaly_image, vis_aoi, [index], scale, f"Drought_{index}", place_name)
    
    return {
        "time_series": result,
        "severity": severity_counts,
        "trend": trend,
        "resultImageUrl": image_urls["thumb_url"]
    }

async def drought_analysis_precipitation(image_id: str, collection_id: str, region: Optional[Union[str, Dict]], start_date: str, end_date: str, time_scale: int, place_name: Optional[str]) -> Dict:
    logger.debug(f"Entering drought_analysis_precipitation with image_id: {image_id}, collection_id: {collection_id}, region: {region}, start_date: {start_date}, end_date: {end_date}, time_scale: {time_scale}, place_name: {place_name}")
    if  region:
        aoi = parse_region( region) if isinstance( region, (str, dict)) else  region
    elif  place_name:
        if  place_name.lower() == 'san francisco':
            aoi = ee.Geometry.Rectangle([-122.75, 37.6, -122.35, 37.9])
        else:
            aoi = ee.Geometry.Point(0, 0).buffer(50000)
    else:
        aoi = ee.Geometry.Rectangle([-180, -90, 180, 90])

    # Single image mode
    if image_id:
        image = ee.Image( image_id).clip(aoi)
        stat = image.reduceRegion(
            reducer=ee.Reducer.mean(),
            geometry=aoi,
            scale=30,
            maxPixels=1e9,
            bestEffort=True
        )
        date = ee.Date(image.get('system:time_start')).format('YYYY-MM-dd').getInfo()
        return [{
            'date': date,
            'precipitation': stat.get('precipitation').getInfo() if stat.get('precipitation') is not None else None
        }]

    # Time-series SPI mode
    ee_collection = ee.ImageCollection( collection_id).filterDate( start_date,  end_date)
    ee_collection = ee_collection.filterBounds(aoi)

    start = ee.Date( start_date)
    end = ee.Date( end_date)
    n_months = ee.Number(end.difference(start, 'month')).round().subtract(1)

    def get_monthly_image(i):
        i = ee.Number(i)
        month_start = start.advance(i, 'month')
        month_end = month_start.advance(1, 'month')
        monthly_sum = ee_collection.filterDate(month_start, month_end).sum() \
                                   .set('system:time_start', month_start)
        return monthly_sum

    monthly_collection = ee.ImageCollection(
        ee.List.sequence(0, n_months).map(get_monthly_image)
    )

    monthly_collection = monthly_collection.filter(
        ee.Filter.gte('system:time_start', start.advance( time_scale - 1, 'month'))
    )

    def compute_rolling_sum(image):
        current_date = ee.Date(image.get('system:time_start'))
        window_start = current_date.advance(1 -  time_scale, 'month')
        window = monthly_collection.filterDate(window_start, current_date.advance(1, 'day'))
        roll_sum = window.sum().rename('accumulated_precipitation')
        return roll_sum.set('system:time_start', current_date) \
                       .set('month', current_date.get('month'))

    rolling_collection = monthly_collection.map(compute_rolling_sum)

    def compute_spi(image):
        month = ee.Number(image.get('month'))
        month_collection = rolling_collection.filter(ee.Filter.eq('month', month))
        mean_val = month_collection.mean().select('accumulated_precipitation')
        std_val = month_collection.reduce(ee.Reducer.stdDev()).select('accumulated_precipitation_stdDev')
        spi = image.select('accumulated_precipitation') \
                   .subtract(mean_val) \
                   .divide(std_val) \
                   .rename('spi')
        return image.addBands(spi)

    spi_collection = rolling_collection.map(compute_spi)

    def reduce_region(image):
        stat = image.reduceRegion(
            reducer=ee.Reducer.mean(),
            geometry=aoi,
            scale=5000,
            maxPixels=1e9,
            bestEffort=True
        )
        date = ee.Date(image.get('system:time_start')).format('YYYY-MM-dd').getInfo()
        spi_value = stat.get('spi')
        if spi_value is None:
            return None
