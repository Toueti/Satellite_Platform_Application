import asyncio
import os
import logging
import io
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload, MediaIoBaseDownload # Added MediaIoBaseDownload
from google.oauth2 import service_account
from google.auth.transport.requests import Request
from typing import Optional, List, Dict
from gee_app.utils.auth import Config

logger = logging.getLogger('gee_app')

GEE_KEY_FILE = Config.GEE_KEY_FILE
GEE_SERVICE_ACCOUNT = Config.GEE_SERVICE_ACCOUNT

def get_drive_service():
    """Helper to create a Google Drive API service instance."""
    SCOPES = ['https://www.googleapis.com/auth/drive']
    credentials = service_account.Credentials.from_service_account_file(
        GEE_KEY_FILE,
        scopes=SCOPES,
        subject=GEE_SERVICE_ACCOUNT
    )
    request = Request()
    credentials.refresh(request)
    return build('drive', 'v3', credentials=credentials)

def get_gee_images_folder_id(drive_service) -> str:
    """Get or create the GEE_Images folder ID."""
    folder_query = "name='GEE_Images' and mimeType='application/vnd.google-apps.folder'"
    logger.debug("Querying Drive with: %s", folder_query)
    
    try:
        response = drive_service.files().list(q=folder_query, spaces='drive').execute()
        folders = response.get('files', [])
        
        if not folders:
            logger.info("GEE_Images folder not found, creating it...")
            folder_metadata = {
                'name': 'GEE_Images',
                'mimeType': 'application/vnd.google-apps.folder'
            }
            folder = drive_service.files().create(body=folder_metadata, fields='id').execute()
            logger.info("Created GEE_Images folder with ID: %s", folder['id'])
            return folder['id']
        
        folder_id = folders[0]['id']
        logger.debug("Found GEE_Images folder with ID: %s", folder_id)
        return folder_id
    except Exception as e:
        logger.error("Error finding or creating GEE_Images folder: %s", str(e))
        raise

def list_images(place_name: Optional[str] = None) -> List[Dict]:
    """List all images in the GEE_Images folder (includes names)."""
    drive_service = get_drive_service()
    folder_id = get_gee_images_folder_id(drive_service)
    
    file_query = f"'{folder_id}' in parents"
    if place_name:
        file_query += f" and name contains '{place_name}'"
    
    logger.debug("Listing files with query: %s", file_query)
    
    try:
        response = drive_service.files().list(
            q=file_query,
            spaces='drive',
            fields='files(name, id, webViewLink)'
        ).execute()
        files = response.get('files', [])
        
        file_list = [
            {"name": file['name'], "id": file['id'], "url": file['webViewLink']}
            for file in files
        ]
        logger.debug("Listed %d images in GEE_Images: %s", len(file_list), file_list)
        return file_list
    except Exception as e:
        logger.error("Error listing images: %s", str(e))
        raise

def retrieve_image(file_id: str, target_directory: str, target_filename: Optional[str] = None) -> Optional[str]:
    """Download an image from Drive to a specific path. Returns the full path on success, None on failure."""
    drive_service = get_drive_service()
    full_path = None # Initialize full_path
    try:
        os.makedirs(target_directory, exist_ok=True)
        # Get file metadata including name
        file_metadata = drive_service.files().get(fileId=file_id, fields='name').execute()
        original_filename = file_metadata['name']
        filename = target_filename or original_filename # Use provided name or original name
        full_path = os.path.join(target_directory, filename)

        logger.debug("Retrieving image ID: %s (Original: %s) to %s", file_id, original_filename, full_path)
        request = drive_service.files().get_media(fileId=file_id)

        # Use io.FileIO for potentially large files
        fh = io.FileIO(full_path, 'wb')
        downloader = MediaIoBaseDownload(fh, request)
        done = False
        while done is False:
            status, done = downloader.next_chunk()
            if status:
                 logger.debug("Download %d%%.", int(status.progress() * 100))

        fh.close() # Ensure file handle is closed
        logger.info("Retrieved image ID: %s to %s", file_id, full_path)
        return full_path
    except Exception as e:
        logger.error("Error retrieving image ID: %s - %s", file_id, str(e))
        # Clean up partial download if exists and full_path was determined
        if full_path and os.path.exists(full_path):
            try:
                os.remove(full_path)
                logger.debug("Removed partially downloaded file: %s", full_path)
            except OSError as rm_err:
                 logger.error("Error removing partially downloaded file %s: %s", full_path, rm_err)
        return None # Return None on error

def update_image(file_id: str, new_name: Optional[str] = None, new_file_path: Optional[str] = None) -> Dict:
    """Update an image in GEE_Images, return old and new names in response."""
    drive_service = get_drive_service()
    
    try:
        # Get the current file name before updating
        old_metadata = drive_service.files().get(fileId=file_id, fields='name').execute()
        old_name = old_metadata['name']
        logger.debug("Updating image with ID: %s, current name: %s", file_id, old_name)
        
        file_metadata = {}
        if new_name:
            file_metadata['name'] = new_name
            logger.debug("New name specified: %s", new_name)
        
        if new_file_path:
            logger.debug("Replacing content with file from: %s", new_file_path)
            media = MediaFileUpload(new_file_path, mimetype='image/tiff')
            updated_file = drive_service.files().update(
                fileId=file_id,
                body=file_metadata,
                media_body=media,
                fields='id, name, webViewLink'
            ).execute()
        else:
            updated_file = drive_service.files().update(
                fileId=file_id,
                body=file_metadata,
                fields='id, name, webViewLink'
            ).execute()
        
        result = {
            "status": "success",
            "file_id": updated_file['id'],
            "old_name": old_name,
            "new_name": updated_file['name'],
            "url": updated_file['webViewLink'],
            "message": "Image updated successfully"
        }
        logger.info("Updated image with ID: %s from old name: %s to new name: %s", file_id, old_name, updated_file['name'])
        return result
    except Exception as e:
        logger.error("Error updating image with ID: %s - %s", file_id, str(e))
        raise

def delete_image(file_id: str) -> Dict:
    """Delete an image from GEE_Images, return deleted image name in response."""
    drive_service = get_drive_service()
    
    try:
        # Get the file name before deleting
        file_metadata = drive_service.files().get(fileId=file_id, fields='name').execute()
        filename = file_metadata['name']
        logger.debug("Deleting image with ID: %s, name: %s", file_id, filename)
        
        drive_service.files().delete(fileId=file_id).execute()
        logger.info("Deleted image with ID: %s, name: %s", file_id, filename)
        return {
            "status": "success",
            "name": filename,
            "message": "Image deleted successfully"
        }
    except Exception as e:
        logger.error("Error deleting image with ID: %s - %s", file_id, str(e))
        raise

def create_folder(folder_name: str, parent_id: Optional[str] = None) -> Dict:
    """Create a new folder under GEE_Images or root, return name and ID in response."""
    drive_service = get_drive_service()
    if not parent_id:
        parent_id = get_gee_images_folder_id(drive_service)
    
    try:
        logger.debug("Creating folder with name: %s under parent ID: %s", folder_name, parent_id)
        folder_metadata = {
            'name': folder_name,
            'mimeType': 'application/vnd.google-apps.folder',
            'parents': [parent_id]
        }
        folder = drive_service.files().create(body=folder_metadata, fields='id, name').execute()
        result = {
            "status": "success",
            "id": folder['id'],
            "name": folder['name'],
            "message": "Folder created successfully"
        }
        logger.info("Created folder with name: %s, ID: %s", folder['name'], folder['id'])
        return result
    except Exception as e:
        logger.error("Error creating folder with name: %s - %s", folder_name, str(e))
        raise

def update_folder(folder_id: str, new_name: Optional[str] = None, new_parent_id: Optional[str] = None) -> Dict:
    """Update a folder in Google Drive (rename or move)."""
    drive_service = get_drive_service()
    
    try:
        # Get current folder details
        old_metadata = drive_service.files().get(fileId=folder_id, fields='name, parents').execute()
        old_name = old_metadata['name']
        old_parents = old_metadata.get('parents', [])
        logger.debug("Updating folder with ID: %s, current name: %s, current parents: %s", folder_id, old_name, old_parents)
        
        file_metadata = {}
        if new_name:
            file_metadata['name'] = new_name
            logger.debug("New name specified: %s", new_name)
        
        if new_parent_id:
            # Move folder by updating parents (remove old, add new)
            file_metadata['parents'] = [new_parent_id]
            logger.debug("Moving folder to new parent ID: %s", new_parent_id)
        
        if not file_metadata:
            logger.debug("No changes specified for folder ID: %s", folder_id)
            return {
                "status": "success",
                "folder_id": folder_id,
                "old_name": old_name,
                "new_name": old_name,
                "message": "No updates specified, folder unchanged"
            }
        
        updated_file = drive_service.files().update(
            fileId=folder_id,
            body=file_metadata,
            fields='id, name',
            removeParents=','.join(old_parents) if new_parent_id else None  # Remove old parents if moving
        ).execute()
        
        result = {
            "status": "success",
            "folder_id": updated_file['id'],
            "old_name": old_name,
            "new_name": updated_file['name'],
            "message": "Folder updated successfully"
        }
        logger.info("Updated folder with ID: %s from old name: %s to new name: %s", folder_id, old_name, updated_file['name'])
        return result
    except Exception as e:
        logger.error("Error updating folder with ID: %s - %s", folder_id, str(e))
        raise

def delete_folder(folder_id: str, recursive: bool = False) -> Dict:
    """Delete a folder from Google Drive, optionally recursively."""
    drive_service = get_drive_service()
    
    try:
        # Get folder name before deletion
        folder_metadata = drive_service.files().get(fileId=folder_id, fields='name').execute()
        folder_name = folder_metadata['name']
        logger.debug("Deleting folder with ID: %s, name: %s, recursive: %s", folder_id, folder_name, recursive)
        
        if recursive:
            # List and delete all contents
            contents = fetch_contents(folder_id)  # Reuse helper from list_folders_and_files
            for file in contents['files']:
                drive_service.files().delete(fileId=file['id']).execute()
                logger.debug("Deleted file %s in folder %s", file['name'], folder_name)
            for subfolder in contents['subfolders']:
                delete_folder(subfolder['id'], recursive=True)  # Recursive call
                logger.debug("Recursively deleted subfolder %s in folder %s", subfolder['name'], folder_name)
        
        drive_service.files().delete(fileId=folder_id).execute()
        logger.info("Deleted folder with ID: %s, name: %s", folder_id, folder_name)
        return {
            "status": "success",
            "name": folder_name,
            "message": f"Folder {'and contents ' if recursive else ''}deleted successfully"
        }
    except Exception as e:
        logger.error("Error deleting folder with ID: %s - %s", folder_id, str(e))
        raise

# Ensure fetch_contents is available (from list_folders_and_files)
def fetch_contents(folder_id: str) -> Dict:
    """Helper to fetch files and folders for a given folder ID."""
    drive_service = get_drive_service()
    query = f"'{folder_id}' in parents"
    response = drive_service.files().list(
        q=query,
        spaces='drive',
        fields='files(name, id, webViewLink, mimeType)'
    ).execute()
    items = response.get('files', [])
    
    file_list = [
        {"name": f['name'], "id": f['id'], "url": f['webViewLink']}
        for f in items if f['mimeType'] != 'application/vnd.google-apps.folder'
    ]
    subfolders = [
        {"name": f['name'], "id": f['id']}
        for f in items if f['mimeType'] == 'application/vnd.google-apps.folder'
    ]
    
    subfolder_list = []
    for subfolder in subfolders:
        subfolder_contents = fetch_contents(subfolder['id'])
        subfolder_info = {
            "name": subfolder['name'],
            "id": subfolder['id'],
            "files": subfolder_contents['files'],
            "subfolders": subfolder_contents['subfolders']
        }
        subfolder_list.append(subfolder_info)
    
    return {"files": file_list, "subfolders": subfolder_list}

def list_folders_and_files(parent_id: Optional[str] = None) -> Dict:
    """List all folders and files recursively starting from the specified parent or GEE_Images."""
    drive_service = get_drive_service()
    if not parent_id:
        parent_id = get_gee_images_folder_id(drive_service)
    
    def fetch_contents(folder_id: str) -> Dict:
        """Helper to fetch files and folders for a given folder ID."""
        # Query for all items in the folder (no MIME type filter in query)
        query = f"'{folder_id}' in parents"
        response = drive_service.files().list(
            q=query,
            spaces='drive',
            fields='files(name, id, webViewLink, mimeType)'  # Add mimeType to filter in code
        ).execute()
        items = response.get('files', [])
        
        # Separate files and folders in code
        file_list = [
            {"name": f['name'], "id": f['id'], "url": f['webViewLink']}
            for f in items if f['mimeType'] != 'application/vnd.google-apps.folder'
        ]
        subfolders = [
            {"name": f['name'], "id": f['id']}
            for f in items if f['mimeType'] == 'application/vnd.google-apps.folder'
        ]
        
        # Recursively fetch contents of subfolders
        subfolder_list = []
        for subfolder in subfolders:
            subfolder_contents = fetch_contents(subfolder['id'])
            subfolder_info = {
                "name": subfolder['name'],
                "id": subfolder['id'],
                "files": subfolder_contents['files'],
                "subfolders": subfolder_contents['subfolders']
            }
            subfolder_list.append(subfolder_info)
        
        return {"files": file_list, "subfolders": subfolder_list}
    
    try:
        logger.debug("Listing folders and files starting from parent ID: %s", parent_id)
        contents = fetch_contents(parent_id)
        
        # Get the name of the root folder
        root_metadata = drive_service.files().get(fileId=parent_id, fields='name').execute()
        root_name = root_metadata['name']
        
        # Structure the response
        folder_info = {
            "name": root_name,
            "id": parent_id,
            "files": contents['files'],
            "subfolders": contents['subfolders']
        }
        result = {
            "status": "success",
            "folders": [folder_info],
            "message": "Folders and files listed successfully"
        }
        
        logger.debug("Listed folders and files: %s", result)
        return result
    except Exception as e:
        logger.error("Error listing folders and files: %s", str(e))
        raise
    
def check_gee_images_folder(place_name: Optional[str] = None) -> List[Dict]:
    """
    Check the GEE_Images folder on Google Drive and return a list of files.

    Args:
        place_name (str, optional): Filter files by place_name in their filename.

    Returns:
        List[Dict]: List of dictionaries with file details (name, id, webViewLink).

    Raises:
        Exception: If Google Drive API access fails.
    """
    SCOPES = ['https://www.googleapis.com/auth/drive']
    credentials = service_account.Credentials.from_service_account_file(
        GEE_KEY_FILE,
        scopes=SCOPES,
        subject=GEE_SERVICE_ACCOUNT
    )
    
    # Refresh token
    request = Request()
    credentials.refresh(request)

    # Build Google Drive API service
    drive_service = build('drive', 'v3', credentials=credentials)

    try:
        # Query for GEE_Images folder
        folder_query = "name='GEE_Images' mimeType='application/vnd.google-apps.folder'"
        response = drive_service.files().list(q=folder_query, spaces='drive').execute()
        folders = response.get('files', [])

        if not folders:
            logger.info("GEE_Images folder not found.")
            return []

        folder_id = folders[0]['id']
        logger.debug("GEE_Images folder found. ID: %s", folder_id)

        # List files in the folder
        file_query = f"'{folder_id}' in parents"
        if place_name:
            file_query += f" {place_name}"  # Filter by place_name in filename
        files_response = drive_service.files().list(
            q=file_query,
            spaces='drive',
            fields='files(name, id, webViewLink)'
        ).execute()
        files = files_response.get('files', [])

        file_list = [
            {"name": file['name'], "id": file['id'], "url": file['webViewLink']}
            for file in files
        ]
        
        logger.debug("Found %d files in GEE_Images: %s", len(file_list), file_list)
        return file_list

    except Exception as e:
        logger.error("Error accessing GEE_Images: %s", str(e))
        return []

async def get_available_drive_storage() -> int:
    """
    Retrieves the available storage space in the user's Google Drive in bytes.

    Returns:
        Available storage in bytes. Returns 10TB for unlimited storage cases.
        
    Raises:
        Exception: If Drive service cannot be initialized or critical API failure occurs.
    """
    def sync_get_storage():
        # Synchronous inner function to run in thread
        drive_service = get_drive_service()
        logger.debug("Fetching Google Drive storage quota...")
        about = drive_service.about().get(fields='storageQuota').execute()
        storage_quota = about.get('storageQuota', {})
        limit = int(storage_quota.get('limit', 0))
        usage = int(storage_quota.get('usage', 0))

        if limit == 0:
            available = 10 * 1024 * 1024 * 1024 * 1024  # 10TB for unlimited
            logger.info(f"Drive storage appears unlimited. Assuming available: {available / (1024**3):.2f} GB")
            return available

        available = limit - usage
        logger.info(f"Drive Storage - Limit: {limit / (1024**3):.2f} GB, "
                   f"Usage: {usage / (1024**3):.2f} GB, "
                   f"Available: {available / (1024**3):.2f} GB ({available} bytes)")
        return max(0, available)  # Ensure non-negative

    try:
        return await asyncio.to_thread(sync_get_storage)
    except Exception as e:
        logger.error(f"Error fetching Google Drive storage quota: {str(e)}")
        raise Exception(f"Failed to retrieve Drive storage quota: {str(e)}")