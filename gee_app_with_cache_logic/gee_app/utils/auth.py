import ee
import os
import logging
from dotenv import load_dotenv

load_dotenv()

logger = logging.getLogger(__name__)

class Config:
    # Google Earth Engine credentials
    GEE_SERVICE_ACCOUNT = os.getenv("GEE_SERVICE_ACCOUNT")
    GEE_KEY_FILE = os.getenv("GEE_KEY_PATH")
    # Default image collection
    DEFAULT_COLLECTION = os.getenv("DEFAULT_COLLECTION")  # Updated collection

    # Default image processing parameters
    DEFAULT_PROCESSING_PARAMS = {
        'calculate_ndvi': True,
        'max_cloud_cover': 20,
        'image_scale': 10
    }

def initialize_ee():
    """Initialize Google Earth Engine with service account credentials."""
    try:
        logger.info(f"GEE_SERVICE_ACCOUNT: {Config.GEE_SERVICE_ACCOUNT}")
        logger.info(f"GEE_KEY_FILE: {Config.GEE_KEY_FILE}")
        credentials = ee.ServiceAccountCredentials(
            email=Config.GEE_SERVICE_ACCOUNT,
            key_file=Config.GEE_KEY_FILE
        )
        logger.info("Credentials set. Initializing EE...")
        ee.Initialize(credentials)
        logger.info("✅ GEE initialized successfully")
    except Exception as e:
        logger.error(f"❌ GEE initialization failed: {str(e)}")
        raise
