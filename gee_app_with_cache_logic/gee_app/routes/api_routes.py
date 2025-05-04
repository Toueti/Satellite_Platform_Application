import asyncio
import datetime
from flask import Blueprint, jsonify, request
from flask_cors import CORS
from pydantic import ValidationError
from gee_app.models.ee_request_model import GeneralEarthEngineRequest2
from gee_app.models.request_models_2 import GeneralEarthEngineRequest, BatchIngestionRequest,ChangeDetectionRequest
from gee_app.services.gee_operations import (
    calculate_ndvi, calculate_evi, analyze_agriculture, detect_water_bodies,
    detect_fire_hotspots, time_series_analysis, calculate_slope, calculate_aspect,
    generate_hillshade, batch_ingest_data
)
# Import the downloader function
from gee_app.services.ee_downloader import download_ee_images
from gee_app.services.gee_specialized import (
    detect_change_between_images, zonal_statistics, generate_histogram,
    land_cover_analysis, drought_analysis_vegetation, drought_analysis_precipitation
)
from gee_app.utils.sensor_utils import parse_region
from gee_app.utils.gee_utils import validate_gee_asset
from gee_app.utils.ee_utils import fetch_image_metadata, get_task_progress
from gee_app.utils.drive_utils import retrieve_image, list_images, retrieve_image, update_image, delete_image, create_folder, update_folder, delete_folder, list_folders_and_files, get_available_drive_storage
import logging
import os
import tempfile
from flask import send_file

# Blueprint setup
service_bp = Blueprint('service', __name__)
CORS(service_bp)

# Logger configuration
logger = logging.getLogger('gee_app')

# Supported analysis types mapped to their respective functions
ANALYSIS_TYPES = {
    'ndvi': calculate_ndvi,
    'evi': calculate_evi,
    'agriculture': analyze_agriculture,
    'water_bodies': detect_water_bodies,
    'fire_hotspots': detect_fire_hotspots,
    'time_series': time_series_analysis,
    'slope': calculate_slope,
    'aspect': calculate_aspect,
    'hillshade': generate_hillshade,
    'change_detection': detect_change_between_images,
    'zonal_stats': zonal_statistics,
    'histogram': generate_histogram,
    'land_cover': land_cover_analysis,
    'drought_vegetation': drought_analysis_vegetation,
    'drought_precipitation': drought_analysis_precipitation
}

# --- Helper Function for Common Logic ---
async def _process_analysis(analysis_type: str, analysis_func, custom_args_func=None):
    """
    Handles common logic for processing a GEE analysis synchronously.
    Parses request, validates, and returns results immediately.
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': 'No JSON data provided'}), 400

        logger.debug(f"Received request for {analysis_type}: {data}")
        if 'analysis_type' not in data:
            data['analysis_type'] = analysis_type

        request_model = GeneralEarthEngineRequest(**data)
        region = request_model.region
        dataset = request_model.collection_id or request_model.image_id

        # Validate GEE assets
        if not dataset and analysis_type not in ['slope', 'aspect', 'hillshade']:  # DEM-based allow default
            return jsonify({'error': 'Either collection_id or image_id is required'}), 400
        if dataset and not validate_gee_asset(dataset):
            return jsonify({'error': f'Invalid GEE asset: {dataset}'}), 400

        # Basic validation
        if not region:
            return jsonify({'error': 'Region is required'}), 400

        aoi = parse_region(region)
        if not aoi:
            return jsonify({'error': 'Invalid region format'}), 400

        # Prepare default args
        args = {
            'region': aoi,
            'scale': request_model.scale or 30,
            'place_name': request_model.place_name or 'region'
        }
        if dataset:
            args['image_id' if request_model.image_id else 'collection_id'] = dataset

        # Apply custom args if provided
        if custom_args_func:
            args.update(custom_args_func(request_model, data))

        # Execute analysis immediately
        result = await analysis_func(**args)
        logger.info(f"Successfully processed {analysis_type} for {args['place_name']}")
        return jsonify({'status': 'completed', 'result': result}), 200

    except ValidationError as ve:
        logger.error(f"Validation error for {analysis_type}: {str(ve)}")
        return jsonify({'error': f"Validation error: {str(ve)}"}), 400
    except Exception as e:
        logger.error(f"Internal server error during {analysis_type}: {str(e)}")
        return jsonify({'error': f"Internal server error: {str(e)}"}), 500

# --- Individual Analysis Endpoints ---

@service_bp.route('/gee/ndvi', methods=['POST'])
async def process_ndvi():
    def custom_args(request_model, data):
        return {
            'bands': request_model.bands,
            'visualization_params': request_model.visualization_params
        }
    return await _process_analysis('ndvi', ANALYSIS_TYPES['ndvi'], custom_args)

@service_bp.route('/gee/evi', methods=['POST'])
async def process_evi():
    return await _process_analysis('evi', ANALYSIS_TYPES['evi'])

@service_bp.route('/gee/agriculture', methods=['POST'])
async def process_agriculture():
    return await _process_analysis('agriculture', ANALYSIS_TYPES['agriculture'])

@service_bp.route('/gee/water_bodies', methods=['POST'])
async def process_water_bodies():
    def custom_args(request_model, data):
        return {'threshold': request_model.threshold or 0.3}
    return await _process_analysis('water_bodies', ANALYSIS_TYPES['water_bodies'], custom_args)

@service_bp.route('/gee/fire_hotspots', methods=['POST'])
async def process_fire_hotspots():
    def custom_args(request_model, data):
        return {'threshold': request_model.threshold or 340}
    return await _process_analysis('fire_hotspots', ANALYSIS_TYPES['fire_hotspots'], custom_args)

@service_bp.route('/gee/time_series', methods=['POST'])
async def process_time_series():
    def custom_args(request_model, data):
        if not request_model.collection_id:
            raise ValueError('collection_id is required for time_series analysis')
        return {
            'index': request_model.index or 'NDVI',
            'interval': request_model.interval or 'month'
        }
    return await _process_analysis('time_series', ANALYSIS_TYPES['time_series'], custom_args)

@service_bp.route('/gee/slope', methods=['POST'])
async def process_slope():
    def custom_args(request_model, data):
        return {'dem_collection': request_model.collection_id or request_model.image_id or 'USGS/SRTMGL1_003'}
    return await _process_analysis('slope', ANALYSIS_TYPES['slope'], custom_args)

@service_bp.route('/gee/aspect', methods=['POST'])
async def process_aspect():
    def custom_args(request_model, data):
        return {'dem_collection': request_model.collection_id or request_model.image_id or 'USGS/SRTMGL1_003'}
    return await _process_analysis('aspect', ANALYSIS_TYPES['aspect'], custom_args)

@service_bp.route('/gee/hillshade', methods=['POST'])
async def process_hillshade():
    def custom_args(request_model, data):
        return {
            'dem_collection': request_model.collection_id or request_model.image_id or 'USGS/SRTMGL1_003',
            'azimuth': request_model.azimuth or 315,
            'altitude': request_model.altitude or 45
        }
    return await _process_analysis('hillshade', ANALYSIS_TYPES['hillshade'], custom_args)

@service_bp.route('/gee/change_detection', methods=['POST'])
async def process_change_detection():
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': 'No JSON data provided'}), 400

        logger.debug(f"Received request for change_detection: {data}")
        data['analysis_type'] = 'change_detection'
        request_model = ChangeDetectionRequest(**data)

        image_id_before = data.get('imageIdBefore')
        image_id_after = data.get('imageIdAfter')
        region = request_model.region

        if not (image_id_before and image_id_after):
            return jsonify({'error': 'Both image_id_before and image_id_after are required'}), 400
        if not region:
            return jsonify({'error': 'Region is required'}), 400
        if not validate_gee_asset(image_id_before):
            return jsonify({'error': f'Invalid GEE asset: {image_id_before}'}), 400
        if not validate_gee_asset(image_id_after):
            return jsonify({'error': f'Invalid GEE asset: {image_id_after}'}), 400

        aoi = parse_region(region)
        if not aoi:
            return jsonify({'error': 'Invalid region format'}), 400

        args = {
            'image_id_before': image_id_before,
            'image_id_after': image_id_after,
            'region': aoi,
            'bands': request_model.bands,
            'threshold': request_model.threshold or 0.2,
            'scale': request_model.scale or 30,
            'place_name': request_model.place_name or 'region'
        }

        result = await ANALYSIS_TYPES['change_detection'](**args)
        logger.info(f"Successfully processed change_detection for {args['place_name']}")
        return jsonify({'status': 'completed', 'result': result}), 200

    except ValidationError as ve:
        logger.error(f"Validation error for change_detection: {str(ve)}")
        return jsonify({'error': f"Validation error: {str(ve)}"}), 400
    except Exception as e:
        logger.error(f"Internal server error for change_detection: {str(e)}")
        return jsonify({'error': f"Internal server error: {str(e)}"}), 500

@service_bp.route('/gee/zonal_stats', methods=['POST'])
async def process_zonal_stats():
    return await _process_analysis('zonal_stats', ANALYSIS_TYPES['zonal_stats'])

@service_bp.route('/gee/histogram', methods=['POST'])
async def process_histogram():
    def custom_args(request_model, data):
        band = data.get('band')
        if not band:
            raise ValueError('Band is required for histogram')
        return {
            'band': band,
            'bins': request_model.bins or 50
        }
    return await _process_analysis('histogram', ANALYSIS_TYPES['histogram'], custom_args)

@service_bp.route('/gee/land_cover', methods=['POST'])
async def process_land_cover():
    def custom_args(request_model, data):
        return {
            'classification_band': request_model.classification_band,
            'temporal_analysis': request_model.temporal_analysis,
            'detailed_metrics': request_model.detailed_metrics
        }
    return await _process_analysis('land_cover', ANALYSIS_TYPES['land_cover'], custom_args)

@service_bp.route('/gee/drought_vegetation', methods=['POST'])
async def process_drought_vegetation():
    def custom_args(request_model, data):
        if not request_model.image_id and not (request_model.start_date and request_model.end_date):
            raise ValueError('Start and end dates are required for collection mode when image_id is not provided')
        args = {
            'index': request_model.index,
            'start_date': request_model.start_date,
            'end_date': request_model.end_date,
            'interval': request_model.interval
        }
        if request_model.image_id:
            args['image_id'] = request_model.image_id
        elif request_model.collection_id:
            args['collection_id'] = request_model.collection_id
        return args
    return await _process_analysis('drought_vegetation', ANALYSIS_TYPES['drought_vegetation'], custom_args)

@service_bp.route('/gee/drought_precipitation', methods=['POST'])
async def process_drought_precipitation():
    def custom_args(request_model, data):
        if not request_model.image_id and not (request_model.start_date and request_model.end_date):
            raise ValueError('Start and end dates are required for collection mode when image_id is not provided')
        args = {
            'start_date': request_model.start_date,
            'end_date': request_model.end_date,
            'time_scale': request_model.time_scale
        }
        if request_model.image_id:
            args['image_id'] = request_model.image_id
        elif request_model.collection_id:
            args['collection_id'] = request_model.collection_id
        return args
    return await _process_analysis('drought_precipitation', ANALYSIS_TYPES['drought_precipitation'], custom_args)

@service_bp.route('/gee/batch_ingest', methods=['POST'])
async def batch_ingest():
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': 'No JSON data provided'}), 400

        logger.debug(f"Received batch ingestion request: {data}")
        batch_request = BatchIngestionRequest(**data)

        if not batch_request.image_ids and not batch_request.collection_id:
            return jsonify({'error': 'At least one of image_ids or collection_id is required'}), 400

        aoi = parse_region(batch_request.region)
        if not aoi:
            return jsonify({'error': 'Invalid region format'}), 400

        result = await batch_ingest_data(
            region=batch_request.region,
            scale=batch_request.scale or 30,
            max_items=batch_request.max_items,
            image_ids=batch_request.image_ids if batch_request.image_ids else None,
            collection_id=batch_request.collection_id,
            start_date=batch_request.start_date,
            end_date=batch_request.end_date,
            cloud_cover_max=batch_request.cloud_cover_max or 100.0
        )
        logger.info(f"Successfully processed batch ingestion for {batch_request.place_name}")
        return jsonify({'status': 'completed', 'results': result}), 200

    except ValidationError as ve:
        logger.error(f"Validation error: {str(ve)}")
        return jsonify({'error': f"Validation error: {str(ve)}"}), 400
    except Exception as e:
        logger.error(f"Internal server error: {str(e)}")
        return jsonify({'error': f"Internal server error: {str(e)}"}), 500

# Endpoint to check GEE export task status (kept as-is since it’s not analysis-related)
@service_bp.route('/tasks/<task_id>/status', methods=['GET'])
async def check_export_status(task_id):
    """
    Check the status of a GEE export task by its ID.
    """
    logger.info(f"Checking status for GEE export task: {task_id}")
    task_info = await get_task_progress(task_id)
    return jsonify(task_info), 200

@service_bp.route('/gee/get_images', methods=['POST'])
async def process_metadata():
    """
    Fetches metadata for images in a collection based on criteria and returns immediately.
    Compatible with the enhanced GeneralEarthEngineRequest2 model.
    """
    analysis_type = 'metadata'
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': 'No JSON data provided'}), 400

        logger.debug(f"Received request for {analysis_type}: {data}")
        
        # Set analysis_type if not provided
        if 'analysis_type' not in data:
            data['analysis_type'] = analysis_type
            
        # Parse request model with validation
        try:
            request_model = GeneralEarthEngineRequest2(**data)
        except ValidationError as ve:
            logger.error(f"Validation error for {analysis_type}: {str(ve)}")
            return jsonify({'error': f"Validation error: {str(ve)}"}), 400

        # Core validation - need either collection_id or image_id
        if not request_model.collection_id and not request_model.image_id:
            return jsonify({'error': 'Either collection_id or image_id is required for metadata fetching'}), 400
        
        # Validate GEE assets
        if request_model.collection_id and not validate_gee_asset(request_model.collection_id):
            return jsonify({'error': f'Invalid GEE collection asset: {request_model.collection_id}'}), 400
            
        if request_model.image_id and not validate_gee_asset(request_model.image_id):
            return jsonify({'error': f'Invalid GEE image asset: {request_model.image_id}'}), 400
            
        if request_model.feature_collection_id and not validate_gee_asset(request_model.feature_collection_id):
            return jsonify({'error': f'Invalid GEE feature collection asset: {request_model.feature_collection_id}'}), 400
        
        # Date validation for collection requests
        if request_model.collection_id and not request_model.image_id:
            if not request_model.start_date or not request_model.end_date:
                return jsonify({'error': 'start_date and end_date are required when using collection_id'}), 400
        
        # Validate bands are provided for visualization
        if not request_model.bands and not request_model.visualization_params:
            return jsonify({'error': 'Either bands list or visualization_params is required for image rendering'}), 400
        
        # Convert Pydantic model to dict and pass to function
        args = request_model.model_dump()
        
        # Fetch metadata with all parameters
        metadata_result = await fetch_image_metadata(**args)
        
        # Log success
        if request_model.image_id:
            logger.info(f"Successfully fetched metadata for image: {request_model.image_id}")
        else:
            logger.info(f"Successfully fetched metadata for collection: {request_model.collection_id} " +
                        f"({metadata_result.get('totalResults', 0)} results)")
        
        # Add API version to response
        response = {
            "status": "completed",
            "date": datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "message": "Image fetched successfully",
            "data": metadata_result,
            "type": "Images",
            "image_id": metadata_result.get('images', [{}])[0].get('id') if metadata_result.get('images') else None
        }
        
        # Return results
        return jsonify(response), 200

    except ValidationError as ve:
        logger.error(f"Validation error for {analysis_type}: {str(ve)}")
        return jsonify({'error': f"Validation error: {str(ve)}"}), 400
    except ValueError as ve:
        logger.error(f"GEE error during metadata fetch: {str(ve)}")
        return jsonify({'error': str(ve)}), 400
    except Exception as e:
        logger.error(f"Internal server error during {analysis_type}: {str(e)}")
        return jsonify({'error': f"Internal server error: {str(e)}"}), 500

# --- Endpoint for Image Download ---
@service_bp.route('/gee/download', methods=['POST'])
async def process_download():
    """
    Handles requests to download GEE images based on provided parameters.
    """
    analysis_type = 'download'
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': 'No JSON data provided'}), 400

        logger.debug(f"Received request for {analysis_type}: {data}")
        data['analysis_type'] = analysis_type # Add for consistency if needed by model

        # Validate using the existing GeneralEarthEngineRequest model
        request_model = GeneralEarthEngineRequest(**data)

        # Basic validation (add more specific checks if needed for download)
        if not (request_model.image_id or request_model.collection_id):
             return jsonify({'error': 'Either image_id or collection_id is required for download'}), 400
        if request_model.collection_id and not validate_gee_asset(request_model.collection_id):
             return jsonify({'error': f'Invalid GEE asset: {request_model.collection_id}'}), 400
        if request_model.image_id and not validate_gee_asset(request_model.image_id):
             return jsonify({'error': f'Invalid GEE asset: {request_model.image_id}'}), 400
        # Region might be optional depending on whether the image/collection covers the whole globe
        # if not request_model.region:
        #     return jsonify({'error': 'Region is required'}), 400
        # aoi = parse_region(request_model.region) if request_model.region else None
        # if request_model.region and not aoi:
        #     return jsonify({'error': 'Invalid region format'}), 400

        # Define output directory (using a temporary directory for safety)
        # Or use a persistent one like 'downloads/' if needed
        # output_dir = tempfile.mkdtemp(prefix="ee_download_")
        output_dir = "downloads" # Use the existing downloads directory

        logger.info(f"Starting download process to directory: {output_dir}")

        # Call the download function (it's synchronous, so no await needed)
        # Note: download_ee_images is synchronous based on its definition
        downloaded_files = download_ee_images(request=request_model, output_dir=output_dir)

        if not downloaded_files:
            logger.warning(f"No files were downloaded for request: {data}")
            return jsonify({'status': 'completed', 'message': 'No files downloaded (check filters or collection status)', 'downloaded_files': []}), 200
        
        logger.info(f"Successfully downloaded {len(downloaded_files)} files for request.")
        # Return the list of downloaded file paths (relative to the server)
        return jsonify({'status': 'completed', 'downloaded_files': downloaded_files}), 200

    except ValidationError as ve:
        logger.error(f"Validation error for {analysis_type}: {str(ve)}")
        return jsonify({'error': f"Validation error: {str(ve)}"}), 400
    except ValueError as ve: # Catch specific errors like invalid assets if raised
        logger.error(f"Value error during {analysis_type}: {str(ve)}")
        return jsonify({'error': str(ve)}), 400
    except Exception as e:
        # Log the full traceback for better debugging
        logger.exception(f"Internal server error during {analysis_type}: {str(e)}")
        return jsonify({'error': f"Internal server error: {str(e)}"}), 500

# --- Drive Utilities Endpoints ---
@service_bp.route('/drive/list_images', methods=['GET'])
async def list_drive_images():
    """
    Endpoint to list images in Google Drive GEE_Images folder.
    """
    try:
        place_name = request.args.get('place_name')
        images = await asyncio.to_thread(list_images, place_name)
        return jsonify({'status': 'success', 'images': images}), 200
    except Exception as e:
        logger.error(f"Error listing drive images: {str(e)}")
        return jsonify({'error': f"Internal server error: {str(e)}"}), 500

@service_bp.route('/drive/retrieve_image/<file_id>', methods=['GET'])
async def download_drive_image(file_id):
    """
    Endpoint to retrieve a specific image from Google Drive.
    """
    try:
        target_directory = 'downloads'  # Or configure as needed
        file_path = await asyncio.to_thread(retrieve_image, file_id, target_directory)
        if file_path:
            return jsonify({'status': 'success', 'message': f'Image downloaded to {file_path}'}), 200
        else:
            return jsonify({'status': 'error', 'message': 'Image download failed'}), 500
    except Exception as e:
        logger.error(f"Error retrieving drive image {file_id}: {str(e)}")
        return jsonify({'error': f"Internal server error: {str(e)}"}), 500

@service_bp.route('/drive/update_image/<file_id>', methods=['POST'])
async def modify_drive_image(file_id):
    """
    Endpoint to update a specific image in Google Drive (name or content).
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': 'No JSON data provided'}), 400
        
        new_name = data.get('new_name')
        new_file_path = data.get('new_file_path') # Path to a file to replace content

        result = await asyncio.to_thread(update_image, file_id, new_name, new_file_path)
        return jsonify({'status': 'success', 'result': result}), 200
    except Exception as e:
        logger.error(f"Error updating drive image {file_id}: {str(e)}")
        return jsonify({'error': f"Internal server error: {str(e)}"}), 500

@service_bp.route('/drive/delete_image/<file_id>', methods=['DELETE'])
async def remove_drive_image(file_id):
    """
    Endpoint to delete a specific image from Google Drive.
    """
    try:
        result = await asyncio.to_thread(delete_image, file_id)
        return jsonify({'status': 'success', 'result': result}), 200
    except Exception as e:
        logger.error(f"Error deleting drive image {file_id}: {str(e)}")
        return jsonify({'error': f"Internal server error: {str(e)}"}), 500
    
@service_bp.route('/drive/delete_images', methods=['POST'])
async def remove_drive_images():
    """
    Endpoint to delete multiple images from Google Drive.
    """
    try:
        data = request.get_json()
        if not data or 'images' not in data:
            return jsonify({'error': 'No images provided in request'}), 400

        results = []
        for image in data['images']:
            file_id = image.get('id')
            if not file_id:
                return jsonify({'error': f'Missing id for image {image.get("name", "unknown")}'}, 400)
            
            try:
                result = await asyncio.to_thread(delete_image, file_id)
                results.append({
                    'id': file_id,
                    'name': image.get('name'),
                    'status': 'deleted' if result else 'failed'
                })
            except Exception as e:
                results.append({
                    'id': file_id,
                    'name': image.get('name'),
                    'status': 'failed',
                    'error': str(e)
                })

        return jsonify({
            'status': 'success',
            'results': results
        }), 200

    except Exception as e:
        logger.error(f"Error in batch image deletion: {str(e)}")
        return jsonify({'error': f"Internal server error: {str(e)}"}), 500

@service_bp.route('/drive/create_folder', methods=['POST'])
async def add_drive_folder():
    """
    Endpoint to create a new folder in Google Drive.
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': 'No JSON data provided'}), 400
        
        folder_name = data.get('folder_name')
        parent_id = data.get('parent_id') # Optional parent folder ID

        if not folder_name:
            return jsonify({'error': 'folder_name is required'}), 400

        result = await asyncio.to_thread(create_folder, folder_name, parent_id)
        return jsonify({'status': 'success', 'result': result}), 200
    except Exception as e:
        logger.error(f"Error creating drive folder: {str(e)}")
        return jsonify({'error': f"Internal server error: {str(e)}"}), 500

@service_bp.route('/drive/update_folder/<folder_id>', methods=['POST'])
async def modify_drive_folder(folder_id):
    """
    Endpoint to update a folder in Google Drive (rename or move).
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': 'No JSON data provided'}), 400
        
        new_name = data.get('new_name')
        new_parent_id = data.get('new_parent_id') # New parent folder ID for moving

        result = await asyncio.to_thread(update_folder, folder_id, new_name, new_parent_id)
        return jsonify({'status': 'success', 'result': result}), 200
    except Exception as e:
        logger.error(f"Error updating drive folder {folder_id}: {str(e)}")
        return jsonify({'error': f"Internal server error: {str(e)}"}), 500

@service_bp.route('/drive/delete_folder/<folder_id>', methods=['DELETE'])
async def remove_drive_folder(folder_id):
    """
    Endpoint to delete a folder from Google Drive.
    """
    try:
        recursive = request.args.get('recursive', default=False, type=bool) # Optional recursive delete
        result = await asyncio.to_thread(delete_folder, folder_id, recursive)
        return jsonify({'status': 'success', 'result': result}), 200
    except Exception as e:
        logger.error(f"Error deleting drive folder {folder_id}: {str(e)}")
        return jsonify({'error': f"Internal server error: {str(e)}"}), 500

@service_bp.route('/drive/list_folders_files', methods=['GET'])
async def list_drive_folders_and_files():
    """
    Endpoint to list folders and files in Google Drive, starting from GEE_Images or a specified parent.
    """
    try:
        parent_id = request.args.get('parent_id') # Optional parent folder ID to start listing from
        result = await asyncio.to_thread(list_folders_and_files, parent_id)
        return jsonify({'status': 'success', 'folders': result['folders']}), 200
    except Exception as e:
        logger.error(f"Error listing drive folders and files: {str(e)}")
        return jsonify({'error': f"Internal server error: {str(e)}"}), 500

@service_bp.route('/drive/available_storage', methods=['GET'])
async def get_drive_storage():
    """
    Endpoint to get available Google Drive storage space.
    """
    try:
        storage_bytes = await get_available_drive_storage()
        storage_gb = storage_bytes / (1024**3)
        return jsonify({'status': 'success', 'available_storage_gb': f"{storage_gb:.2f} GB"}), 200
    except Exception as e:
        logger.error(f"Error fetching drive storage info: {str(e)}")
        return jsonify({'error': f"Internal server error: {str(e)}"}), 500

@service_bp.route('/downloads/<file_id>', methods=['GET'])
async def download_file(file_id):
    """
    Endpoint to download a file from the 'downloads/' directory by its filename.
    """
    try:
        file_path = os.path.join('downloads', file_id)
        if not os.path.exists(file_path):
            return jsonify({'error': 'File not found'}), 404
        
        return send_file(file_path, as_attachment=True)
    except Exception as e:
        logger.error(f"Error downloading file {file_id}: {str(e)}")
        return jsonify({'error': f"Internal server error: {str(e)}"}), 500


# Register the blueprint with the Flask app
def register_blueprints(app):
    """
    Registers the service blueprint with the Flask application.
    
    Args:
        app: The Flask application instance.
    """
    app.register_blueprint(service_bp)
