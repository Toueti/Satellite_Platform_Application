# import asyncio
# from flask import Blueprint, jsonify, request, current_app
# from flask_cors import CORS
# import datetime
# from pydantic import ValidationError
# from gee_app.models.request_models_2 import GeneralEarthEngineRequest, BatchIngestionRequest
# from gee_app.services.gee_operations import (
#     calculate_ndvi, calculate_evi, analyze_agriculture, detect_water_bodies,
#     detect_fire_hotspots, time_series_analysis, calculate_slope, calculate_aspect,
#     generate_hillshade, batch_ingest_data
# )
# from gee_app.services.gee_specialized import (
#     detect_change_between_images, zonal_statistics, generate_histogram,
#     land_cover_analysis, drought_analysis_vegetation, drought_analysis_precipitation
# )
# from gee_app.utils.sensor_utils import parse_region
# # Import caching functions from cache_utils instead of db_utils
# import os # Added for path operations
# import tempfile # Added for temporary download directory
# from gee_app.utils.cache_utils import store_data, get_cached_data, cache_downloaded_file
# from gee_app.utils.gee_utils import validate_gee_asset
# from gee_app.utils.ee_utils import fetch_image_metadata, get_task_progress # Added get_task_progress
# from gee_app.utils.drive_utils import retrieve_image # Added retrieve_image
# import logging

# # Removed import of dictionary 'cache' from cache_utils.py

# # Blueprint setup
# service_bp = Blueprint('service', __name__)
# CORS(service_bp)

# # Logger configuration
# logger = logging.getLogger('gee_app')

# # Supported analysis types mapped to their respective functions
# ANALYSIS_TYPES = {
#     'ndvi': calculate_ndvi,
#     'evi': calculate_evi,
#     'agriculture': analyze_agriculture,
#     'water_bodies': detect_water_bodies,
#     'fire_hotspots': detect_fire_hotspots,
#     'time_series': time_series_analysis,
#     'slope': calculate_slope,
#     'aspect': calculate_aspect,
#     'hillshade': generate_hillshade,
#     'change_detection': detect_change_between_images,
#     'zonal_stats': zonal_statistics,
#     'histogram': generate_histogram,
#     'land_cover': land_cover_analysis,
#     'drought_vegetation': drought_analysis_vegetation,
#     'drought_precipitation': drought_analysis_precipitation
# }

# # Dictionary to track analysis tasks (distinct from GEE export tasks)
# analysis_tasks = {} # Renamed from 'tasks' to avoid confusion with GEE export tasks

# # GEE export task statuses are managed in ee_utils.task_statuses (global dict)

# # --- Helper Function for Common Logic ---
# async def _initiate_analysis(analysis_type: str):
#     """
#     Handles common logic for initiating a GEE analysis task.
#     Parses request, validates, checks cache, and returns either cached data or task info.
#     """
#     try:
#         data = request.get_json()
#         if not data:
#             return jsonify({'error': 'No JSON data provided'}), 400

#         logger.debug(f"Received request for {analysis_type}: {data}")

#         # Add analysis_type to data if not present (needed for model validation)
#         if 'analysis_type' not in data:
#             data['analysis_type'] = analysis_type

#         request_model = GeneralEarthEngineRequest(**data)
#         region = request_model.region
#         dataset = request_model.collection_id or request_model.image_id

#         # Validate GEE assets
#         if not dataset:
#              return jsonify({'error': 'Either collection_id or image_id is required'}), 400
#         if not validate_gee_asset(dataset):
#             return jsonify({'error': f'Invalid GEE asset: {dataset}'}), 400

#         # Basic validation
#         if not region:
#             return jsonify({'error': 'Region is required'}), 400

#         aoi = parse_region(region)
#         if not aoi:
#             return jsonify({'error': 'Invalid region format'}), 400

#         # Check Redis cache using the new function
#         # get_cached_data now returns the full data dict if found and increments count internally
#         cached_result = get_cached_data(
#             request_model.image_id,
#             request_model.collection_id,
#             region, # Pass the original region dict
#             analysis_type
#         )
#         if cached_result: # Check if data was returned from Redis
#             logger.info(f"Returning cached result for {analysis_type}")
#             # increment_request_count is now called *inside* get_cached_data in cache_utils.py
#             response = jsonify({ # Create the response object
#                 'status': 'completed',
#                 # The function returns the full data dict, including the 'result' field
#                 'result': cached_result.get('result')
#             }), 200
#             return None, response # Return (None, response_tuple)

#         # If not cached, prepare for background analysis task
#         task_id = str(len(analysis_tasks) + 1) # Use analysis_tasks
#         analysis_tasks[task_id] = {'status': 'running', 'result': None, 'error': None} # Use analysis_tasks

#         context = { # Assign to context variable instead of returning directly
#             'task_id': task_id,
#             'request_model': request_model,
#             'aoi': aoi,
#             'dataset': dataset,
#             'scale': request_model.scale or 30,
#             'place_name': request_model.place_name or 'region',
#             'data': data # Pass original data for specific args
#         }
#         return context, None # Return (context_dict, None)

#     except ValidationError as ve:
#         logger.error(f"Validation error for {analysis_type}: {str(ve)}")
#         error_response = jsonify({'error': f"Validation error: {str(ve)}"}), 400
#         return None, error_response # Return (None, error_response_tuple)
#     except Exception as e:
#         logger.error(f"Internal server error during initiation for {analysis_type}: {str(e)}")
#         error_response = jsonify({'error': f"Internal server error: {str(e)}"}), 500
#         return None, error_response # Return (None, error_response_tuple)


# # --- Individual Analysis Endpoints ---

# @service_bp.route('/gee/ndvi', methods=['POST'])
# async def process_ndvi():
#     analysis_type = 'ndvi'
#     # _initiate_analysis now consistently returns (context, response)
#     context, response = await _initiate_analysis(analysis_type)

#     # If response is not None, it's either a cache hit or an error
#     if response:
#         return response # Return the response directly

#     # If response is None, it means cache miss and context is valid
#     task_id = context['task_id']
#     request_model = context['request_model']
#     args = {
#         'image_id': context['dataset'],
#         'region': context['aoi'],
#         'bands': request_model.bands,
#         'scale': context['scale'],
#         'place_name': context['place_name'],
#         'visualization_params': request_model.visualization_params
#     }
#     analysis_func = ANALYSIS_TYPES[analysis_type]
#     asyncio.create_task(run_analysis_with_storage(task_id, analysis_func, args, request_model))
#     logger.info(f"Task {task_id} started for {analysis_type} in {context['place_name']}")
#     return jsonify({'status': 'processing', 'task_id': task_id})

# @service_bp.route('/gee/evi', methods=['POST'])
# async def process_evi():
#     analysis_type = 'evi'
#     context, response = await _initiate_analysis(analysis_type)
#     if response:
#         return response

#     task_id = context['task_id']
#     request_model = context['request_model']
#     args = {
#         'image_id': context['dataset'],
#         'region': context['aoi'],
#         'scale': context['scale'],
#         'place_name': context['place_name']
#     }
#     analysis_func = ANALYSIS_TYPES[analysis_type]
#     asyncio.create_task(run_analysis_with_storage(task_id, analysis_func, args, request_model))
#     logger.info(f"Task {task_id} started for {analysis_type} in {context['place_name']}")
#     return jsonify({'status': 'processing', 'task_id': task_id})

# @service_bp.route('/gee/agriculture', methods=['POST'])
# async def process_agriculture():
#     analysis_type = 'agriculture'
#     context, response = await _initiate_analysis(analysis_type)
#     if response:
#         return response

#     task_id = context['task_id']
#     request_model = context['request_model']
#     args = {
#         'image_id': context['dataset'],
#         'region': context['aoi'],
#         'scale': context['scale'],
#         'place_name': context['place_name']
#     }
#     analysis_func = ANALYSIS_TYPES[analysis_type]
#     asyncio.create_task(run_analysis_with_storage(task_id, analysis_func, args, request_model))
#     logger.info(f"Task {task_id} started for {analysis_type} in {context['place_name']}")
#     return jsonify({'status': 'processing', 'task_id': task_id})

# @service_bp.route('/gee/water_bodies', methods=['POST'])
# async def process_water_bodies():
#     analysis_type = 'water_bodies'
#     context, response = await _initiate_analysis(analysis_type)
#     if response:
#         return response

#     task_id = context['task_id']
#     request_model = context['request_model']
#     args = {
#         'image_id': context['dataset'],
#         'region': context['aoi'],
#         'threshold': request_model.threshold or 0.3,
#         'scale': context['scale'],
#         'place_name': context['place_name']
#     }
#     analysis_func = ANALYSIS_TYPES[analysis_type]
#     asyncio.create_task(run_analysis_with_storage(task_id, analysis_func, args, request_model))
#     logger.info(f"Task {task_id} started for {analysis_type} in {context['place_name']}")
#     return jsonify({'status': 'processing', 'task_id': task_id})

# @service_bp.route('/gee/fire_hotspots', methods=['POST'])
# async def process_fire_hotspots():
#     analysis_type = 'fire_hotspots'
#     context, response = await _initiate_analysis(analysis_type)
#     if response:
#         return response

#     task_id = context['task_id']
#     request_model = context['request_model']
#     args = {
#         'image_id': context['dataset'],
#         'region': context['aoi'],
#         'threshold': request_model.threshold or 340,
#         'scale': context['scale'],
#         'place_name': context['place_name']
#     }
#     analysis_func = ANALYSIS_TYPES[analysis_type]
#     asyncio.create_task(run_analysis_with_storage(task_id, analysis_func, args, request_model))
#     logger.info(f"Task {task_id} started for {analysis_type} in {context['place_name']}")
#     return jsonify({'status': 'processing', 'task_id': task_id})

# @service_bp.route('/gee/time_series', methods=['POST'])
# async def process_time_series():
#     analysis_type = 'time_series'
#     context, response = await _initiate_analysis(analysis_type)
#     if response:
#         return response

#     task_id = context['task_id']
#     request_model = context['request_model']
#     if not request_model.collection_id: # Time series requires a collection
#          return jsonify({'error': 'collection_id is required for time_series analysis'}), 400

#     args = {
#         'collection_id': context['dataset'], # Use dataset which is collection_id here
#         'region': context['aoi'],
#         'index': request_model.index or 'NDVI',
#         'interval': request_model.interval or 'month',
#         'place_name': context['place_name']
#     }
#     analysis_func = ANALYSIS_TYPES[analysis_type]
#     asyncio.create_task(run_analysis_with_storage(task_id, analysis_func, args, request_model))
#     logger.info(f"Task {task_id} started for {analysis_type} in {context['place_name']}")
#     return jsonify({'status': 'processing', 'task_id': task_id})

# @service_bp.route('/gee/slope', methods=['POST'])
# async def process_slope():
#     analysis_type = 'slope'
#     context, response = await _initiate_analysis(analysis_type)
#     if response:
#         return response

#     task_id = context['task_id']
#     request_model = context['request_model']
#     args = {
#         'dem_collection': context['dataset'] or 'USGS/SRTMGL1_003',
#         'region': context['aoi'],
#         'scale': context['scale'],
#         'place_name': context['place_name']
#     }
#     analysis_func = ANALYSIS_TYPES[analysis_type]
#     asyncio.create_task(run_analysis_with_storage(task_id, analysis_func, args, request_model))
#     logger.info(f"Task {task_id} started for {analysis_type} in {context['place_name']}")
#     return jsonify({'status': 'processing', 'task_id': task_id})

# @service_bp.route('/gee/aspect', methods=['POST'])
# async def process_aspect():
#     analysis_type = 'aspect'
#     context, response = await _initiate_analysis(analysis_type)
#     if response:
#         return response

#     task_id = context['task_id']
#     request_model = context['request_model']
#     args = {
#         'dem_collection': context['dataset'] or 'USGS/SRTMGL1_003',
#         'region': context['aoi'],
#         'scale': context['scale'],
#         'place_name': context['place_name']
#     }
#     analysis_func = ANALYSIS_TYPES[analysis_type]
#     asyncio.create_task(run_analysis_with_storage(task_id, analysis_func, args, request_model))
#     logger.info(f"Task {task_id} started for {analysis_type} in {context['place_name']}")
#     return jsonify({'status': 'processing', 'task_id': task_id})

# @service_bp.route('/gee/hillshade', methods=['POST'])
# async def process_hillshade():
#     analysis_type = 'hillshade'
#     context, response = await _initiate_analysis(analysis_type)
#     if response:
#         return response

#     task_id = context['task_id']
#     request_model = context['request_model']
#     args = {
#         'dem_collection': context['dataset'] or 'USGS/SRTMGL1_003',
#         'region': context['aoi'],
#         'azimuth': request_model.azimuth or 315,
#         'altitude': request_model.altitude or 45,
#         'scale': context['scale'],
#         'place_name': context['place_name']
#     }
#     analysis_func = ANALYSIS_TYPES[analysis_type]
#     asyncio.create_task(run_analysis_with_storage(task_id, analysis_func, args, request_model))
#     logger.info(f"Task {task_id} started for {analysis_type} in {context['place_name']}")
#     return jsonify({'status': 'processing', 'task_id': task_id})

# @service_bp.route('/gee/change_detection', methods=['POST'])
# async def process_change_detection():
#     analysis_type = 'change_detection'
#     # Change detection needs special handling for multiple image IDs
#     try:
#         data = request.get_json()
#         if not data:
#             return jsonify({'error': 'No JSON data provided'}), 400

#         logger.debug(f"Received request for {analysis_type}: {data}")
#         data['analysis_type'] = analysis_type # Ensure analysis_type is present
#         request_model = GeneralEarthEngineRequest(**data)

#         image_id_before = data.get('image_id_before')
#         image_id_after = data.get('image_id_after')
#         region = request_model.region

#         if not (image_id_before and image_id_after):
#             return jsonify({'error': 'Both image_id_before and image_id_after are required'}), 400
#         if not region:
#             return jsonify({'error': 'Region is required'}), 400
#         if not validate_gee_asset(image_id_before):
#             return jsonify({'error': f'Invalid GEE asset: {image_id_before}'}), 400
#         if not validate_gee_asset(image_id_after):
#             return jsonify({'error': f'Invalid GEE asset: {image_id_after}'}), 400

#         aoi = parse_region(region)
#         if not aoi:
#             return jsonify({'error': 'Invalid region format'}), 400

#         # Simplified cache check for change detection (can be enhanced)
#         # For now, we assume change detection is less frequently cached due to two image IDs
#         # A more robust cache key would involve both image IDs.

#         task_id = str(len(analysis_tasks) + 1) # Use analysis_tasks
#         analysis_tasks[task_id] = {'status': 'running', 'result': None, 'error': None} # Use analysis_tasks
#         scale = request_model.scale or 30
#         place_name = request_model.place_name or 'region'

#         args = {
#             'image_id_before': image_id_before,
#             'image_id_after': image_id_after,
#             'region': aoi,
#             'bands': request_model.bands,
#             'threshold': request_model.threshold or 0.2,
#             'scale': scale,
#             'place_name': place_name
#         }
#         analysis_func = ANALYSIS_TYPES[analysis_type]
#         # Pass a modified request_model or relevant parts for storage
#         storage_model_data = request_model.model_dump() # Use model_dump() for Pydantic v2+
#         storage_model_data['image_id'] = f"{image_id_before}_vs_{image_id_after}" # Combine IDs for storage pseudo-ID
#         storage_model_data['collection_id'] = None # Not applicable
#         storage_request_model = GeneralEarthEngineRequest(**storage_model_data)

#         asyncio.create_task(run_analysis_with_storage(task_id, analysis_func, args, storage_request_model)) # Use analysis_tasks
#         logger.info(f"Task {task_id} started for {analysis_type} in {place_name}")
#         return jsonify({'status': 'processing', 'task_id': task_id})

#     except ValidationError as ve:
#         logger.error(f"Validation error for {analysis_type}: {str(ve)}")
#         return jsonify({'error': f"Validation error: {str(ve)}"}), 400
#     except Exception as e:
#         logger.error(f"Internal server error for {analysis_type}: {str(e)}")
#         return jsonify({'error': f"Internal server error: {str(e)}"}), 500


# @service_bp.route('/gee/zonal_stats', methods=['POST'])
# async def process_zonal_stats():
#     analysis_type = 'zonal_stats'
#     context, response = await _initiate_analysis(analysis_type)
#     if response:
#         return response

#     task_id = context['task_id']
#     request_model = context['request_model']
#     args = {
#         'image_id': context['dataset'],
#         'region': context['aoi'],
#         'scale': context['scale'],
#         'place_name': context['place_name']
#         # Add other specific params if needed for zonal_stats function
#     }
#     analysis_func = ANALYSIS_TYPES[analysis_type]
#     asyncio.create_task(run_analysis_with_storage(task_id, analysis_func, args, request_model))
#     logger.info(f"Task {task_id} started for {analysis_type} in {context['place_name']}")
#     return jsonify({'status': 'processing', 'task_id': task_id})

# @service_bp.route('/gee/histogram', methods=['POST'])
# async def process_histogram():
#     analysis_type = 'histogram'
#     context, response = await _initiate_analysis(analysis_type)
#     if response:
#         return response

#     task_id = context['task_id']
#     request_model = context['request_model']
#     data = context['data']
#     band = data.get('band')
#     if not band:
#         return jsonify({'error': 'Band is required for histogram'}), 400

#     args = {
#         'image_id': context['dataset'],
#         'region': context['aoi'],
#         'band': band,
#         'bins': request_model.bins or 50,
#         'scale': context['scale'],
#         'place_name': context['place_name']
#     }
#     analysis_func = ANALYSIS_TYPES[analysis_type]
#     asyncio.create_task(run_analysis_with_storage(task_id, analysis_func, args, request_model))
#     logger.info(f"Task {task_id} started for {analysis_type} in {context['place_name']}")
#     return jsonify({'status': 'processing', 'task_id': task_id})

# @service_bp.route('/gee/land_cover', methods=['POST'])
# async def process_land_cover():
#     analysis_type = 'land_cover'
#     context, response = await _initiate_analysis(analysis_type)
#     if response:
#         return response

#     task_id = context['task_id']
#     request_model = context['request_model']
#     args = {
#         'image_id': context['dataset'],
#         'region': context['aoi'],
#         'scale': context['scale'],
#         'place_name': context['place_name'],
#         'classification_band': request_model.classification_band,
#         'temporal_analysis': request_model.temporal_analysis,
#         'detailed_metrics': request_model.detailed_metrics
#     }
#     analysis_func = ANALYSIS_TYPES[analysis_type]
#     asyncio.create_task(run_analysis_with_storage(task_id, analysis_func, args, request_model))
#     logger.info(f"Task {task_id} started for {analysis_type} in {context['place_name']}")
#     return jsonify({'status': 'processing', 'task_id': task_id})

# @service_bp.route('/gee/drought_vegetation', methods=['POST'])
# async def process_drought_vegetation():
#     analysis_type = 'drought_vegetation'
#     context, response = await _initiate_analysis(analysis_type)
#     if response:
#         return response

#     task_id = context['task_id']
#     request_model = context['request_model']
#     # Validate required fields for collection mode if image_id is not provided
#     if not request_model.image_id and not (request_model.start_date and request_model.end_date):
#         return jsonify({'error': 'Start and end dates are required for collection mode when image_id is not provided'}), 400

#     args = {
#         'image_id': request_model.image_id, # Can be None
#         'collection_id': request_model.collection_id, # Can be None (dataset covers this)
#         'region': context['aoi'],
#         'index': request_model.index,
#         'start_date': request_model.start_date,
#         'end_date': request_model.end_date,
#         'interval': request_model.interval,
#         'scale': context['scale'],
#         'place_name': context['place_name']
#     }
#     # Ensure dataset is passed correctly based on whether it's image or collection
#     if request_model.image_id:
#         args['image_id'] = context['dataset']
#         args.pop('collection_id', None) # Remove collection_id if image_id is primary
#     elif request_model.collection_id:
#         args['collection_id'] = context['dataset']
#         args.pop('image_id', None) # Remove image_id if collection_id is primary

#     analysis_func = ANALYSIS_TYPES[analysis_type]
#     asyncio.create_task(run_analysis_with_storage(task_id, analysis_func, args, request_model))
#     logger.info(f"Task {task_id} started for {analysis_type} in {context['place_name']}")
#     return jsonify({'status': 'processing', 'task_id': task_id})

# @service_bp.route('/gee/drought_precipitation', methods=['POST'])
# async def process_drought_precipitation():
#     analysis_type = 'drought_precipitation'
#     context, response = await _initiate_analysis(analysis_type)
#     if response:
#         return response

#     task_id = context['task_id']
#     request_model = context['request_model']
#     # Validate required fields
#     if not request_model.image_id and not (request_model.start_date and request_model.end_date):
#         return jsonify({'error': 'Start and end dates are required for collection mode when image_id is not provided'}), 400

#     args = {
#         'image_id': request_model.image_id, # Can be None
#         'collection_id': request_model.collection_id, # Can be None
#         'region': context['aoi'],
#         'start_date': request_model.start_date,
#         'end_date': request_model.end_date,
#         'time_scale': request_model.time_scale,
#         'place_name': context['place_name']
#     }
#      # Ensure dataset is passed correctly based on whether it's image or collection
#     if request_model.image_id:
#         args['image_id'] = context['dataset']
#         args.pop('collection_id', None)
#     elif request_model.collection_id:
#         args['collection_id'] = context['dataset']
#         args.pop('image_id', None)

#     analysis_func = ANALYSIS_TYPES[analysis_type]
#     asyncio.create_task(run_analysis_with_storage(task_id, analysis_func, args, request_model))
#     logger.info(f"Task {task_id} started for {analysis_type} in {context['place_name']}")
#     return jsonify({'status': 'processing', 'task_id': task_id})

# @service_bp.route('/gee/batch_ingest', methods=['POST'])
# async def batch_ingest():
#     try:
#         data = request.get_json()
#         if not data:
#             return jsonify({'error': 'No JSON data provided'}), 400

#         logger.debug(f"Received batch ingestion request: {data}")
#         batch_request = BatchIngestionRequest(**data)

#         if not batch_request.image_ids and not batch_request.collection_id:
#             return jsonify({'error': 'At least one of image_ids or collection_id is required'}), 400

#         aoi = parse_region(batch_request.region)
#         if not aoi:
#             return jsonify({'error': 'Invalid region format'}), 400

#         task_id = f"batch_{len(analysis_tasks) + 1}" # Use analysis_tasks
#         analysis_tasks[task_id] = {'status': 'running', 'results': {}, 'error': None} # Use analysis_tasks

#         asyncio.create_task(run_batch_task(task_id, batch_request, aoi)) # Use analysis_tasks
#         logger.info(f"Batch ingestion task {task_id} started for {batch_request.place_name}")
#         return jsonify({'status': 'processing', 'task_id': task_id})

#     except ValidationError as ve:
#         logger.error(f"Validation error: {str(ve)}")
#         return jsonify({'error': f"Validation error: {str(ve)}"}), 400
#     except Exception as e:
#         logger.error(f"Internal server error: {str(e)}")
#         return jsonify({'error': f"Internal server error: {str(e)}"}), 500

# # Endpoint to check task status
# @service_bp.route('/gee/status/<task_id>', methods=['GET'])
# async def check_status(task_id):
#     """
#     Check the status of a background task by its ID.
    
#     Args:
#         task_id (str): The ID of the analysis task to check.

#     Returns:
#         JSON response with task status, result (if completed), or error (if failed).
#     """
#     if task_id in analysis_tasks: # Use analysis_tasks
#         return jsonify(analysis_tasks[task_id]) # Use analysis_tasks
#     return jsonify({'error': 'Analysis task not found'}), 404

# # --- New Endpoint for GEE Export Task Status ---
# @service_bp.route('/tasks/<task_id>/status', methods=['GET'])
# async def check_export_status(task_id):
#     """
#     Check the status of a GEE export task by its ID.
#     If completed, triggers download from Drive and caching.

#     Args:
#         task_id (str): The ID of the GEE export task.

#     Returns:
#         JSON response with task status, potentially including cache path.
#     """
#     logger.info(f"Checking status for GEE export task: {task_id}")
#     # Get status from the global dict in ee_utils
#     task_info = await get_task_progress(task_id)
#     status = task_info.get("status")
#     file_id = task_info.get("file_id")
#     filename = task_info.get("filename")
#     cache_path = task_info.get("cache_path") # Check if already cached

#     # If already cached, just return the info
#     if status == "CACHED" and cache_path:
#         logger.info(f"Task {task_id} already cached at {cache_path}")
#         return jsonify(task_info), 200

#     # If completed successfully by GEE and Drive, and has file_id, but not yet cached
#     if status == "COMPLETED" and file_id and filename and not cache_path:
#         logger.info(f"Task {task_id} completed, attempting download (File ID: {file_id}) and cache.")

#         # Create a temporary directory for download
#         with tempfile.TemporaryDirectory() as temp_dir:
#             logger.debug(f"Created temporary directory for download: {temp_dir}")
#             downloaded_path = None
#             try:
#                 # Attempt download using drive_utils function
#                 downloaded_path = retrieve_image(file_id, temp_dir, filename)

#                 if downloaded_path and os.path.exists(downloaded_path):
#                     logger.info(f"Successfully downloaded file to temporary path: {downloaded_path}")
#                     # Attempt to move to cache
#                     final_cache_path = cache_downloaded_file(downloaded_path, filename)

#                     if final_cache_path:
#                         logger.info(f"Successfully cached file: {final_cache_path}")
#                         # Update the global status dict (this is not ideal for concurrency)
#                         from gee_app.utils.ee_utils import task_statuses # Re-import for update
#                         if task_id in task_statuses:
#                             task_statuses[task_id]['state'] = "CACHED"
#                             task_statuses[task_id]['cache_path'] = final_cache_path
#                             task_statuses[task_id]['message'] = f"File downloaded and cached at {final_cache_path}"
#                             task_info = task_statuses[task_id] # Update local task_info for response
#                         else:
#                              logger.warning(f"Task {task_id} disappeared from status dict during caching.")
#                              # Fallback: return current info but log inconsistency
#                     else:
#                         logger.error(f"Failed to move downloaded file {downloaded_path} to cache.")
#                         # Update status to reflect caching failure?
#                         from gee_app.utils.ee_utils import task_statuses
#                         if task_id in task_statuses:
#                              task_statuses[task_id]['state'] = "CACHE_FAILED"
#                              task_statuses[task_id]['message'] = "Download succeeded but failed to move file to cache."
#                              task_info = task_statuses[task_id]
#                 else:
#                     logger.error(f"Download failed for file ID {file_id} (Task {task_id}).")
#                     # Update status to reflect download failure?
#                     from gee_app.utils.ee_utils import task_statuses
#                     if task_id in task_statuses:
#                          task_statuses[task_id]['state'] = "DOWNLOAD_FAILED"
#                          task_statuses[task_id]['message'] = "GEE export completed, but download from Drive failed."
#                          task_info = task_statuses[task_id]

#             except Exception as e:
#                 logger.exception(f"Error during download/cache process for task {task_id}: {e}")
#                 # Update status to reflect generic error during this phase
#                 from gee_app.utils.ee_utils import task_statuses
#                 if task_id in task_statuses:
#                      task_statuses[task_id]['state'] = "PROCESSING_ERROR"
#                      task_statuses[task_id]['message'] = f"Error processing completed task: {e}"
#                      task_info = task_statuses[task_id]
#             finally:
#                 # The temporary directory is automatically cleaned up by the 'with' statement
#                 logger.debug(f"Temporary directory {temp_dir} cleaned up.")

#     # Return the latest status (potentially updated by download/cache process)
#     return jsonify(task_info), 200

# @service_bp.route('/gee/metadata', methods=['POST'])  # Single route definition
# async def process_metadata():
#     """
#     Fetches metadata for images in a collection based on criteria.
#     Uses caching to avoid redundant fetches and returns cached data if available.
#     Does not use the background task system as it's expected to be relatively fast.
#     """
#     analysis_type = 'metadata'
#     try:
#         data = request.get_json()
#         if not data:
#             return jsonify({'error': 'No JSON data provided'}), 400

#         logger.debug(f"Received request for {analysis_type}: {data}")
#         data['analysis_type'] = analysis_type  # Ensure analysis_type is present for model

#         # Validate request using the general model
#         request_model = GeneralEarthEngineRequest(**data)

#         # Basic validation
#         if not request_model.collection_id:
#             return jsonify({'error': 'collection_id is required for metadata fetching'}), 400
#         if not validate_gee_asset(request_model.collection_id):
#             return jsonify({'error': f'Invalid GEE asset: {request_model.collection_id}'}), 400
#         if not request_model.start_date or not request_model.end_date:
#             return jsonify({'error': 'start_date and end_date are required'}), 400
#         if not request_model.bands:
#             return jsonify({'error': 'bands list is required for thumbnail generation'}), 400

#         # Prepare cache parameters with all relevant fields for key generation
#         # In process_metadata route:
#         cache_params = {
#             'collection_id': request_model.collection_id,
#             'region': request_model.region,
#             'scale': request_model.scale,
#             '_type': analysis_type,  # Changed key to match retrieval
#             'start_date': request_model.start_date,
#             'end_date': request_model.end_date,
#             'bands': request_model.bands,
#             'max_cloud_cover': request_model.max_cloud_cover,
#             'images_number': request_model.images_number,
#             'min_value': data.get('min_value', 0),
#             'max_value': data.get('max_value', 3000),
#             'dimensions': data.get('dimensions', 1080)
#         }

#         # Check Redis cache
#         cached_result = get_cached_data(cache_params, analysis_type)
#         if cached_result is not None:  # Explicit check for None
#             logger.info(f"Cache hit for collection: {request_model.collection_id}, returning cached metadata")
#             return jsonify({
#                 'status': 'completed',
#                 'result': cached_result  # cache_utils returns the 'data' field directly
#             }), 200
#         else:
#             logger.debug(f"Cache miss for collection: {request_model.collection_id}, fetching new metadata")

#         # If not cached, fetch metadata
#         args = {
#             "collection_id": request_model.collection_id,
#             "start_date": request_model.start_date,
#             "end_date": request_model.end_date,
#             "bands": request_model.bands,
#             "region": request_model.region,
#             "max_cloud_cover": request_model.max_cloud_cover,
#             "images_number": request_model.images_number,
#             "min_value": cache_params['min_value'],
#             "max_value": cache_params['max_value'],
#             "dimensions": cache_params['dimensions'],
#             "scale": request_model.scale
#         }

#         # Fetch metadata with timing
#         start_time = datetime.datetime.now()
#         metadata_result = await fetch_image_metadata(**args)
#         execution_time = (datetime.datetime.now() - start_time).total_seconds()

#         # Store the result in Redis cache
#         cache_warning = None
#         try:
#             from gee_app.utils.cache_utils import store_data_with_key, _generate_cache_key
#             # Generate the exact same key as get_cached_data
#             cache_key = _generate_cache_key(cache_params)
#             # In process_metadata route:
#             logger.debug(f"Generated cache key: {cache_key}")
#             # Store metadata_result directly as the 'data' field content
#             success = store_data_with_key(cache_key, metadata_result)
#             if success:
#                 logger.info(f"Metadata stored in cache with key: {cache_key} for collection: {request_model.collection_id}")
#             else:
#                 logger.warning(f"Failed to store metadata in cache for key: {cache_key}")
#                 cache_warning = "Failed to store metadata in cache"
#         except Exception as cache_err:
#             logger.error(f"Failed to store metadata in cache: {cache_err}")
#             cache_warning = f"Caching failed: {str(cache_err)}"

#         logger.info(f"Successfully fetched and processed metadata for collection: {request_model.collection_id} in {execution_time:.2f}s")
#         response = {
#             'status': 'completed',
#             'result': metadata_result
#         }
#         if cache_warning:
#             response['warning'] = cache_warning
#         return jsonify(response), 200

#     except ValidationError as ve:
#         logger.error(f"Validation error for {analysis_type}: {str(ve)}")
#         return jsonify({'error': f"Validation error: {str(ve)}"}), 400
#     except ValueError as ve:  # Catch specific GEE errors raised in fetch_image_metadata
#         logger.error(f"GEE error during metadata fetch: {str(ve)}")
#         return jsonify({'error': str(ve)}), 400
#     except Exception as e:
#         logger.error(f"Internal server error during {analysis_type}: {str(e)}")
#         return jsonify({'error': f"Internal server error: {str(e)}"}), 500


# # --- Background Task Runner ---

# async def run_analysis_with_storage(task_id, analysis_func, args, request_model: GeneralEarthEngineRequest):
#     """ Runs analysis, stores result in DB and cache, updates task status. """
#     logger.debug(f"Entering run_analysis_with_storage for task_id: {task_id}")
#     try:
#         start_time = datetime.datetime.now()  # Start timing
#         result = await analysis_func(**args)
#         end_time = datetime.datetime.now()  # End timing
#         execution_time = (end_time - start_time).total_seconds()  # Calculate execution time

#         logger.debug(f"Analysis completed for task_id: {task_id}, result: {result}")
#         logger.debug(f"Analysis completed for task_id: {task_id}, result: {result}")
#         analysis_tasks[task_id]['status'] = 'completed' # Use analysis_tasks
#         analysis_tasks[task_id]['result'] = result # Use analysis_tasks

#         # Store analysis result in Redis cache
#         storage_data = {
#             'image_id': request_model.image_id,
#             'collection_id': request_model.collection_id,
#             'region': request_model.region, # Store the original region dict
#             'scale': request_model.scale,
#             'analysis_type': request_model.analysis_type,
#             'timestamp': datetime.datetime.utcnow().isoformat(),
#             'result': result,
#             'status': 'completed', # Assuming only completed results are stored here
#             'execution_time': execution_time
#             # 'request_count' is initialized inside store_data
#         }
#         logger.debug(f"Attempting to store data in Redis for task_id: {task_id}")
#         try:
#             # This now calls cache_utils.store_data
#             cache_key_returned = store_data(storage_data)
#             logger.info(f"Data stored successfully in Redis for task_id: {task_id} with key: {cache_key_returned}")
#         except (ConnectionError, ValueError, RuntimeError) as store_err:
#              logger.error(f"Failed to store analysis data in Redis for task {task_id}: {store_err}")
#              # Decide if this should mark the analysis task as failed or just log

#         # Removed the old dictionary cache logic

#     except Exception as e:
#         logger.error(f"Error in run_analysis_with_storage for task_id: {task_id}: {str(e)}")
#         analysis_tasks[task_id]['status'] = 'failed' # Use analysis_tasks
#         analysis_tasks[task_id]['error'] = str(e) # Use analysis_tasks
#         # Removed storing failed tasks in cache



# async def run_batch_task(task_id: str, batch_request: BatchIngestionRequest, aoi):
#     try:
#         results = {}

#         # Check cache for image_ids
#         uncached_image_ids = []
#         if batch_request.image_ids:
#             logger.info(f"Validating {len(batch_request.image_ids)} image_ids")
#             valid_image_ids = [img_id for img_id in batch_request.image_ids if validate_gee_asset(img_id)]
#             if not valid_image_ids:
#                 logger.warning("No valid image_ids provided after validation")
#             else:
#                 logger.info(f"Checking cache for {len(valid_image_ids)} valid image_ids")
#                 for image_id in valid_image_ids:
#                     cached = get_cached_data(image_id, None, batch_request.region, "raw")
#                     if cached and cached.get('status') == 'completed':
#                         results[image_id] = cached['result']
#                         logger.info(f"Using cached data for {image_id}")
#                     else:
#                         uncached_image_ids.append(image_id)

#         # Check cache for collection_id (simplified: process all if uncached)
#         uncached_collection = None
#         if batch_request.collection_id:
#             if validate_gee_asset(batch_request.collection_id):
#                 logger.info(f"Collection {batch_request.collection_id} is valid")
#                 uncached_collection = batch_request.collection_id
#             else:
#                 logger.warning(f"Invalid collection_id: {batch_request.collection_id}")
#                 uncached_collection = None

#         # Process uncached data
#         if uncached_image_ids or uncached_collection:
#             logger.info(f"Processing {len(uncached_image_ids)} uncached image_ids and collection {uncached_collection}")
#             batch_results = await batch_ingest_data(
#                 region=batch_request.region,
#                 scale=batch_request.scale or 30,
#                 max_items=batch_request.max_items,
#                 image_ids=uncached_image_ids if uncached_image_ids else None,
#                 collection_id=uncached_collection,
#                 start_date=batch_request.start_date,
#                 end_date=batch_request.end_date,
#                 cloud_cover_max=batch_request.cloud_cover_max or 100.0
#             )

#             # Store successful results
#             for image_id, result in batch_results.items():
#                 results[image_id] = result
#                 if "error" not in result:
#                     storage_data = {
#                         'image_id': image_id,
#                         'collection_id': batch_request.collection_id if image_id in batch_results and uncached_collection else None,
#                         'region': batch_request.region,
#                         'scale': batch_request.scale or 30,
#                         'analysis_type': 'raw',
#                         'timestamp': datetime.datetime.utcnow().isoformat(),
#                         'result': result,
#                         'status': 'completed'
#                     }
#                     store_data(storage_data) # Store raw data cache
#                     logger.info(f"Ingested and stored {image_id}")
#                 else:
#                     logger.warning(f"Skipped storing {image_id} due to error: {result['error']}")

#         analysis_tasks[task_id]['status'] = 'completed' # Use analysis_tasks
#         analysis_tasks[task_id]['results'] = results # Use analysis_tasks
#     except Exception as e:
#         logger.error(f"Batch ingestion task {task_id} failed: {str(e)}")
#         analysis_tasks[task_id]['status'] = 'failed' # Use analysis_tasks
#         analysis_tasks[task_id]['error'] = str(e) # Use analysis_tasks
#         # Removed storing failed batch tasks in cache

# # Register the blueprint with the Flask app
# def register_blueprints(app):
#     """
#     Registers the service blueprint with the Flask application.
    
#     Args:
#         app: The Flask application instance.
#     """
#     app.register_blueprint(service_bp)
