from pymongo import MongoClient
from typing import Optional, Dict, Any
from bson.objectid import ObjectId
import os
import logging

logger = logging.getLogger('gee_app')

class MongoDBConfig:
    MONGO_URI = os.getenv("MONGO_URI")
    DB_NAME = os.getenv("MONGO_DB_NAME")

def get_mongo_client():
    try:
        client = MongoClient(MongoDBConfig.MONGO_URI)
        db = client[MongoDBConfig.DB_NAME]
        logger.info("✅ Connected to MongoDB")
        return db
    except Exception as e:
        logger.error(f"❌ Failed to connect to MongoDB: {str(e)}")
        raise


mongo_db = get_mongo_client()

def initialize_db():
    gee_data_collection = mongo_db["gee_data"]  # Corrected collection name
    gee_data_collection.create_index("timestamp", expireAfterSeconds=604800)
    gee_data_collection.create_index("image_id")
    gee_data_collection.create_index("collection_id")
    gee_data_collection.create_index("analysis_type")
    gee_data_collection.create_index([("region", "2dsphere")])
    gee_data_collection.create_index("request_count")  # Add index for request_count
    logger.info("✅ Geospatial index created on gee_data collection")

def get_cached_data(image_id: Optional[str], collection_id: Optional[str], region: Dict, analysis_type: str) -> Optional[Dict]:
    query = {
        "analysis_type": analysis_type,
        "region": { "$geoIntersects": { "$geometry": region } }
    }
    if image_id:
        query["image_id"] = image_id
    if collection_id:
        query["collection_id"] = collection_id
    
    result = mongo_db['gee_data'].find_one(query)  # Corrected collection name
    return result

def store_data(data: Dict[str, Any]) -> str:
    logger.debug(f"Storing data in MongoDB: {data}")
    data['request_count'] = 1  # Initialize request_count
    result = mongo_db['gee_data'].insert_one(data)  # Corrected collection name
    logger.info(f"Stored data with ID: {result.inserted_id}")
    return str(result.inserted_id)

def increment_request_count(object_id: str):
    """Increments the request count for a given cached item."""
    logger.debug(f"Incrementing request count for ID: {object_id}")
    try:
        oid = ObjectId(object_id)
        logger.debug(f"Converted ObjectId: {oid}")
    except Exception as e:
        logger.error(f"Error converting to ObjectId: {e}")
        return

    result = mongo_db['gee_data'].update_one(
        {'_id': oid},
        {'$inc': {'request_count': 1}}
    )
    logger.info(f"Incremented request count. Matched: {result.matched_count}, Modified: {result.modified_count}")
