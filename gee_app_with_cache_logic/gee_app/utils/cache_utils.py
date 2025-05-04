import redis
import os
import json
import logging
import shutil
import time
import hashlib
from typing import Optional, Dict, Any, Union, List, Tuple
from functools import wraps

logger = logging.getLogger('gee_app')

# Configuration
REDIS_URL = os.getenv("REDIS_URL")
CACHE_EXPIRATION_SECONDS = int(os.getenv("CACHE_EXPIRATION_SECONDS", "604800"))  # 7 days default
CACHE_DIR = os.getenv("CACHE_DIR", "cache")
ENABLE_CACHE = os.getenv("ENABLE_CACHE", "true").lower() == "true"
DEFAULT_COMPRESSION_LEVEL = int(os.getenv("REDIS_COMPRESSION_LEVEL", "1"))  # 0-9, where 0 is no compression

redis_client = None

class CacheConfig:
    """Centralized cache configuration management"""
    
    def __init__(self):
        self.enabled = ENABLE_CACHE
        self.expiration = CACHE_EXPIRATION_SECONDS
        self.compression_level = DEFAULT_COMPRESSION_LEVEL
    
    def disable(self):
        """Temporarily disable caching"""
        self.enabled = False
        
    def enable(self):
        """Enable caching"""
        self.enabled = True
    
    def set_expiration(self, seconds: int):
        """Set custom expiration time"""
        self.expiration = seconds
    
    def reset(self):
        """Reset to default settings"""
        self.enabled = ENABLE_CACHE
        self.expiration = CACHE_EXPIRATION_SECONDS
        self.compression_level = DEFAULT_COMPRESSION_LEVEL

# Global config instance
cache_config = CacheConfig()

def get_redis_client():
    """Initializes and returns the Redis client, handling connection errors with backoff retry."""
    global redis_client
    if redis_client is None:
        if not REDIS_URL:
            logger.error("❌ REDIS_URL environment variable not set.")
            raise ValueError("REDIS_URL environment variable not set.")
        
        max_retries = 3
        retry_delay = 1.0  # starting delay in seconds
        
        for attempt in range(max_retries):
            try:
                masked_url = REDIS_URL.split('@')[-1] if '@' in REDIS_URL else REDIS_URL
                logger.info(f"Attempting to connect to Redis at {masked_url}... (Attempt {attempt+1}/{max_retries})")
                
                # Connection pool with reasonable defaults
                redis_client = redis.from_url(
                    REDIS_URL, 
                    decode_responses=True,
                    socket_timeout=5.0,  # 5 seconds timeout for operations
                    socket_connect_timeout=5.0,  # 5 seconds timeout for connection
                    health_check_interval=30  # Check connection every 30 seconds
                )
                redis_client.ping()  # Test the connection
                logger.info("✅ Connected to Redis successfully")
                return redis_client
                
            except redis.exceptions.ConnectionError as e:
                logger.warning(f"⚠️ Redis connection attempt {attempt+1} failed: {str(e)}")
                if attempt < max_retries - 1:
                    wait_time = retry_delay * (2 ** attempt)  # Exponential backoff
                    logger.info(f"Retrying in {wait_time:.1f} seconds...")
                    time.sleep(wait_time)
                else:
                    logger.error(f"❌ Failed to connect to Redis after {max_retries} attempts")
                    redis_client = None
                    raise ConnectionError(f"Failed to connect to Redis after {max_retries} attempts: {str(e)}")
                    
            except Exception as e:
                logger.error(f"❌ An unexpected error occurred during Redis connection: {str(e)}")
                redis_client = None
                raise ConnectionError(f"An unexpected error occurred during Redis connection: {str(e)}")
    
    return redis_client

def _generate_cache_key(data: Any) -> str:
    """
    Generates a consistent cache key for any input data structure.
    
    Args:
        data: Any JSON-serializable data that defines the cache entry uniqueness.
             Can be a dictionary, list, tuple, string, or any combination.
    
    Returns:
        A consistent hash-based cache key
    """
    if isinstance(data, dict):
        # Sort dictionary keys for consistent serialization
        sorted_data = sort_dict_recursive(data)
        serialized = json.dumps(sorted_data, sort_keys=True)
    elif isinstance(data, (list, tuple)):
        # For lists or tuples, serialize each item
        serialized = json.dumps(data, sort_keys=True if any(isinstance(x, dict) for x in data) else False)
    elif isinstance(data, str):
        # Use string directly if it's already a string
        serialized = data
    else:
        # For anything else, try to convert to string representation
        try:
            serialized = json.dumps(data, sort_keys=True if isinstance(data, dict) else False)
        except (TypeError, ValueError):
            serialized = str(data)
    
    # Create a consistent SHA256 hash
    hash_obj = hashlib.sha256(serialized.encode('utf-8'))
    return f"cache:{hash_obj.hexdigest()}"

def cache_decorator(prefix: str = "", ttl: Optional[int] = None):
    """
    Decorator for automatic function result caching.
    
    Args:
        prefix: Optional prefix for the cache key
        ttl: Optional custom TTL (Time To Live) in seconds
        
    Example usage:
        @cache_decorator(prefix="daily_stats", ttl=86400)
        def get_daily_statistics(date, region_id):
            # Heavy computation...
            return results
    """
    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            if not cache_config.enabled:
                return func(*args, **kwargs)
            
            # Create cache key based on function name, args and kwargs
            cache_data = {
                "func": func.__name__,
                "args": args,
                "kwargs": kwargs
            }
            
            # Add prefix if provided
            key = f"{prefix}:{_generate_cache_key(cache_data)}" if prefix else _generate_cache_key(cache_data)
            
            # Try to get from cache first
            cached_result = get_cached_data_by_key(key)
            if cached_result is not None:
                logger.debug(f"✓ Cache hit for function {func.__name__}")
                return cached_result
            
            # Execute function if not in cache
            result = func(*args, **kwargs)
            
            # Cache the result
            expiration = ttl if ttl is not None else cache_config.expiration
            store_data_with_key(key, result, expiration)
            
            return result
        return wrapper
    return decorator

def initialize_db(test_connection: bool = True) -> bool:
    """
    Initializes the Redis connection and returns success status.
    
    Args:
        test_connection: Whether to test the connection with a ping
        
    Returns:
        True if connection is successful, False otherwise
    """
    if not ENABLE_CACHE:
        logger.info("Caching is disabled via environment setting.")
        return False
        
    try:
        client = get_redis_client()
        if client and test_connection:
            client.ping()
            logger.info("✅ Redis connection test successful during initialization.")
            return True
        elif client:
            logger.info("Redis client initialized (connection not tested)")
            return True
        else:
            logger.error("❌ Redis client is None after initialization attempt.")
            return False
    except ConnectionError as e:
        logger.error(f"❌ Redis connection failed during initialization: {e}")
        return False
    except Exception as e:
        logger.error(f"❌ Unexpected error during Redis initialization: {e}")
        return False

def get_cached_data_by_key(cache_key: str) -> Optional[Any]:
    """
    Gets cached data directly using a key.
    
    Args:
        cache_key: The full cache key to retrieve
        
    Returns:
        The cached data if found, None otherwise
    """
    if not cache_config.enabled:
        return None
        
    try:
        client = get_redis_client()
        if not client:
            logger.warning("⚠️ Redis client not available, cannot get cached data.")
            return None

        logger.debug(f"Attempting to get data from Redis with key: {cache_key}")
        cached_result = client.get(cache_key)

        if cached_result:
            logger.info(f"✅ Cache hit for key: {cache_key}")
            try:
                data = json.loads(cached_result)
                # Increment request count atomically
                increment_request_count(cache_key)
                return data
            except json.JSONDecodeError as e:
                logger.error(f"❌ Error decoding JSON from Redis cache for key {cache_key}: {e}")
                return None
        else:
            logger.debug(f"Cache miss for key: {cache_key}")
            return None
            
    except redis.exceptions.ConnectionError as e:
        logger.error(f"❌ Redis connection error during get_cached_data: {e}")
        return None
    except Exception as e:
        logger.error(f"❌ Unexpected error during get_cached_data: {e}")
        return None

def get_cached_data(cache_params: Dict[str, Any], analysis_type: str = "") -> Optional[Any]:
    """
    Retrieves cached data based on input parameters.
    
    Args:
        cache_params: Dictionary of parameters that define the cache entry
        analysis_type: Optional type for grouping related cache entries
        
    Returns:
        The cached data if found, None otherwise
    """
    if not cache_config.enabled:
        return None
        
    # Add analysis_type to params if provided
    params_with_type = cache_params.copy()
        
    # Generate cache key from parameters
    cache_key = _generate_cache_key(params_with_type)
    return get_cached_data_by_key(cache_key)

def store_data_with_key(cache_key: str, data: Any, expiration: Optional[int] = None) -> bool:
    """
    Stores data in Redis with a specific key.
    
    Args:
        cache_key: The full cache key to store under
        data: The data to store (must be JSON serializable)
        expiration: Optional custom expiration time in seconds
        
    Returns:
        True if successful, False otherwise
    """
    if not cache_config.enabled:
        return False
        
    try:
        client = get_redis_client()
        if not client:
            logger.warning("⚠️ Redis client not available, cannot store data.")
            return False
            
        # Prepare data with metadata
        data_to_store = {
            "data": data,
            "cached_at": time.time(),
            "request_count": 1
        }
            
        try:
            serialized_data = json.dumps(data_to_store)
        except (TypeError, ValueError) as e:
            logger.error(f"❌ Error serializing data to JSON for key {cache_key}: {e}")
            return False
            
        # Use configured expiration if not specified
        exp_time = expiration if expiration is not None else cache_config.expiration
            
        # Store the data with expiration
        client.setex(cache_key, exp_time, serialized_data)
        logger.info(f"✅ Stored data in Redis with key: {cache_key}, expiration: {exp_time}s")
        return True
            
    except redis.exceptions.ConnectionError as e:
        logger.error(f"❌ Redis connection error during store_data: {e}")
        return False
    except Exception as e:
        logger.error(f"❌ Unexpected error during store_data: {e}")
        return False

def store_data(data: Dict[str, Any], analysis_type: str = "", expiration: Optional[int] = None) -> Optional[str]:
    """
    Stores data in Redis based on its content.
    
    Args:
        data: The data to store (must contain parameters for key generation)
        analysis_type: Optional type for grouping related cache entries
        expiration: Optional custom expiration time in seconds
        
    Returns:
        The cache key if successful, None otherwise
    """
    if not cache_config.enabled:
        return None
        
    # Extract params for key generation, but store the full data
    try:
        # Make a shallow copy to avoid modifying the original
        data_copy = data.copy()
        
        # Add type if provided
        if analysis_type:
            data_copy["_type"] = analysis_type
            
        # Generate key from the data itself
        cache_key = _generate_cache_key(data_copy)
        
        # Store the entire data
        success = store_data_with_key(cache_key, data, expiration)
        return cache_key if success else None
        
    except Exception as e:
        logger.error(f"❌ Error in store_data: {e}")
        return None

def increment_request_count(cache_key: str) -> Optional[int]:
    """
    Increments the request count for a cache entry and returns the new count.
    
    Args:
        cache_key: The cache key to increment counter for
        
    Returns:
        The new count if successful, None otherwise
    """
    try:
        client = get_redis_client()
        if not client:
            return None
            
        # Update count in a pipeline for atomicity
        pipe = client.pipeline()
        
        # Get the current data
        pipe.get(cache_key)
        
        # Execute pipeline
        results = pipe.execute()
        current_data = results[0]
        
        if not current_data:
            logger.warning(f"⚠️ Cache key not found for incrementing: {cache_key}")
            return None
            
        try:
            data_dict = json.loads(current_data)
            
            # Increment count
            current_count = data_dict.get("request_count", 0)
            data_dict["request_count"] = current_count + 1
            
            # Get TTL of original key to preserve it
            ttl = client.ttl(cache_key)
            if ttl > 0:
                # Store updated data with same TTL
                new_ttl = max(ttl, cache_config.expiration)
                client.expire(cache_key, new_ttl)
                client.setex(cache_key, ttl, json.dumps(data_dict))
                
            logger.debug(f"✅ Incremented request count for {cache_key} to {data_dict['request_count']}")
            return data_dict["request_count"]
            
        except json.JSONDecodeError as e:
            logger.error(f"❌ Error parsing JSON for request count increment: {e}")
            return None
            
    except redis.exceptions.ConnectionError as e:
        logger.error(f"❌ Redis connection error incrementing count: {e}")
        return None
    except Exception as e:
        logger.error(f"❌ Error incrementing request count: {e}")
        return None

def invalidate_cache(cache_key: str) -> bool:
    """
    Invalidates (deletes) a specific cache entry.
    
    Args:
        cache_key: The cache key to invalidate
        
    Returns:
        True if deleted or not found, False on error
    """
    try:
        client = get_redis_client()
        if not client:
            logger.warning("⚠️ Redis client not available, cannot invalidate cache.")
            return False
            
        result = client.delete(cache_key)
        if result:
            logger.info(f"✅ Successfully invalidated cache key: {cache_key}")
        else:
            logger.info(f"Cache key not found for invalidation: {cache_key}")
            
        return True
        
    except redis.exceptions.ConnectionError as e:
        logger.error(f"❌ Redis connection error during cache invalidation: {e}")
        return False
    except Exception as e:
        logger.error(f"❌ Unexpected error during cache invalidation: {e}")
        return False

def invalidate_by_pattern(pattern: str) -> int:
    """
    Invalidates all cache entries matching a pattern.
    
    Args:
        pattern: Redis key pattern (e.g., "cache:user_*")
        
    Returns:
        Number of invalidated keys
    """
    try:
        client = get_redis_client()
        if not client:
            logger.warning("⚠️ Redis client not available, cannot invalidate by pattern.")
            return 0
            
        # Find all matching keys
        matching_keys = client.keys(pattern)
        
        if not matching_keys:
            logger.info(f"No keys found matching pattern: {pattern}")
            return 0
            
        # Delete all found keys
        deleted = client.delete(*matching_keys)
        logger.info(f"✅ Invalidated {deleted} cache entries matching pattern: {pattern}")
        return deleted
        
    except redis.exceptions.ConnectionError as e:
        logger.error(f"❌ Redis connection error during pattern invalidation: {e}")
        return 0
    except Exception as e:
        logger.error(f"❌ Unexpected error during pattern invalidation: {e}")
        return 0

def get_cache_stats() -> Dict[str, Any]:
    """
    Gets statistics about the current cache usage.
    
    Returns:
        Dictionary with cache statistics
    """
    try:
        client = get_redis_client()
        if not client:
            return {"error": "Redis client not available"}
            
        # Get all cache keys
        all_keys = client.keys("cache:*")
        
        # Get info about Redis
        info = client.info()
        
        stats = {
            "total_cache_keys": len(all_keys),
            "memory_used_bytes": info.get("used_memory", 0),
            "memory_used_readable": f"{info.get('used_memory_human', '0B')}",
            "uptime_seconds": info.get("uptime_in_seconds", 0),
            "connected_clients": info.get("connected_clients", 0),
            "cache_enabled": cache_config.enabled,
            "cache_expiration": cache_config.expiration
        }
        
        return stats
        
    except redis.exceptions.ConnectionError as e:
        logger.error(f"❌ Redis connection error getting cache stats: {e}")
        return {"error": f"Connection error: {str(e)}"}
    except Exception as e:
        logger.error(f"❌ Unexpected error getting cache stats: {e}")
        return {"error": f"Unexpected error: {str(e)}"}

# File caching functions
def ensure_cache_dir_exists():
    """Creates the cache directory if it doesn't exist."""
    try:
        os.makedirs(CACHE_DIR, exist_ok=True)
        logger.info(f"Cache directory '{CACHE_DIR}' ensured.")
    except OSError as e:
        logger.error(f"❌ Error creating cache directory '{CACHE_DIR}': {e}")
        raise

def generate_file_cache_path(identifier: str, extension: str = "") -> str:
    """
    Generates a consistent file path for a cache item.
    
    Args:
        identifier: Unique identifier for the file
        extension: Optional file extension (with or without dot)
        
    Returns:
        Full path to the cache file
    """
    # Generate a consistent filename
    filename = hashlib.sha256(identifier.encode('utf-8')).hexdigest()
    
    # Add extension if provided
    if extension:
        if not extension.startswith('.'):
            extension = f".{extension}"
        filename = f"{filename}{extension}"
        
    return os.path.join(CACHE_DIR, filename)

def check_file_cache(identifier: str, extension: str = "") -> Optional[str]:
    """
    Checks if a file exists in the cache.
    
    Args:
        identifier: Unique identifier for the file
        extension: Optional file extension
        
    Returns:
        Full path to the cached file if it exists, None otherwise
    """
    if not cache_config.enabled:
        return None
        
    cache_path = generate_file_cache_path(identifier, extension)
    
    if os.path.exists(cache_path):
        logger.info(f"✅ Cache hit for file: {identifier}")
        return cache_path
    else:
        logger.debug(f"Cache miss for file: {identifier}")
        return None

def cache_file(source_path: str, identifier: str, extension: str = "", move: bool = True) -> Optional[str]:
    """
    Stores a file in the cache.
    
    Args:
        source_path: Path to the source file
        identifier: Unique identifier for the file
        extension: Optional file extension
        move: Whether to move the file (True) or copy it (False)
        
    Returns:
        Path to the cached file if successful, None otherwise
    """
    if not cache_config.enabled:
        return None
        
    ensure_cache_dir_exists()
    
    if not os.path.exists(source_path):
        logger.error(f"❌ Source file not found for caching: {source_path}")
        return None
        
    cache_path = generate_file_cache_path(identifier, extension)
    
    try:
        if move:
            shutil.move(source_path, cache_path)
            logger.info(f"✅ Moved file to cache: {cache_path}")
        else:
            shutil.copy2(source_path, cache_path)
            logger.info(f"✅ Copied file to cache: {cache_path}")
            
        return cache_path
        
    except Exception as e:
        logger.error(f"❌ Error {'moving' if move else 'copying'} file to cache: {e}")
        return None

def clean_expired_files(max_age_days: int = 7) -> int:
    """
    Removes files from the cache directory that are older than max_age_days.
    
    Args:
        max_age_days: Maximum age in days before a file is removed
        
    Returns:
        Number of files removed
    """
    ensure_cache_dir_exists()
    
    now = time.time()
    max_age_seconds = max_age_days * 86400
    removed_count = 0
    
    try:
        for filename in os.listdir(CACHE_DIR):
            file_path = os.path.join(CACHE_DIR, filename)
            
            # Skip directories
            if os.path.isdir(file_path):
                continue
                
            # Check file age
            file_age = now - os.path.getmtime(file_path)
            
            if file_age > max_age_seconds:
                os.remove(file_path)
                removed_count += 1
                logger.debug(f"Removed expired cache file: {filename}")
                
        logger.info(f"✅ Removed {removed_count} expired files from cache directory")
        return removed_count
        
    except Exception as e:
        logger.error(f"❌ Error cleaning expired cache files: {e}")
        return removed_count

def cache_downloaded_file(source_path: str, cache_filename: str) -> Optional[str]:
    """
    Moves a downloaded file into the cache directory.

    Args:
        source_path: The current path of the downloaded file.
        cache_filename: The desired filename within the cache directory.

    Returns:
        The full path to the cached file if successful, otherwise None.
    """
    ensure_cache_dir_exists() # Make sure cache dir exists
    if not os.path.exists(source_path):
        logger.error(f"❌ Source file not found for caching: {source_path}")
        return None

    cache_filepath = os.path.join(CACHE_DIR, cache_filename)

    try:
        # Move the file to the cache directory
        shutil.move(source_path, cache_filepath)
        logger.info(f"✅ Moved downloaded file to cache: {cache_filepath}")
        return cache_filepath
    except Exception as e:
        logger.error(f"❌ Error moving file '{source_path}' to cache '{cache_filepath}': {e}")
        # Clean up source file if move failed? Depends on desired behavior.
        # If source_path still exists, maybe log that it wasn't moved.
        if os.path.exists(source_path):
             logger.warning(f"⚠️ Source file '{source_path}' still exists after failed move attempt.")
        return None

def sort_dict_recursive(obj):
    if isinstance(obj, dict):
        return {k: sort_dict_recursive(v) for k, v in sorted(obj.items())}
    if isinstance(obj, list):
        return [sort_dict_recursive(elem) for elem in obj]
    return obj

# Initialize when module is loaded
ensure_cache_dir_exists()