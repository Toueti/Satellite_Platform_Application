from googleapiclient.discovery import build
from google.oauth2 import service_account
from google.auth.transport.requests import Request

def check_gee_images_folder():
    SCOPES = ['https://www.googleapis.com/auth/drive']
    
    GEE_KEY_FILE = r'C:\Users\HP\Desktop\login-sigin spring boot backend\JWT\src\main\resources\satellite-platform-application-f7154aa5ce46.json'
    SCOPES = ['https://www.googleapis.com/auth/drive', 'https://www.googleapis.com/auth/earthengine']

    # # Load credentials
    credentials = service_account.Credentials.from_service_account_file(GEE_KEY_FILE, scopes=SCOPES)
    
    # Refresh token
    request = Request()
    credentials.refresh(request)

    # Build Google Drive API service
    drive_service = build('drive', 'v3', credentials=credentials)

    try:
        # Query for GEE_Images folder
        folder_query = "name='GEE_Images' and mimeType='application/vnd.google-apps.folder'"
        response = drive_service.files().list(q=folder_query, spaces='drive').execute()
        folders = response.get('files', [])

        if not folders:
            print("GEE_Images folder not found.")
            return False

        folder_id = folders[0]['id']
        print(f"GEE_Images folder found. ID: {folder_id}")

        # List files in the folder
        file_query = f"'{folder_id}' in parents"
        files_response = drive_service.files().list(q=file_query, spaces='drive', fields='files(name, id)').execute()
        files = files_response.get('files', [])

        if files:
            print("Files in GEE_Images:")
            for file in files:
                print(f"- {file['name']} (ID: {file['id']})")
        else:
            print("GEE_Images folder is empty.")

        return True

    except Exception as e:
        print(f"Error accessing GEE_Images: {e}")
        return False

# Run the test
check_gee_images_folder()


# from google.oauth2 import service_account
# from google.auth.transport.requests import Request

# GEE_KEY_FILE = r'C:\Users\HP\Desktop\login-sigin spring boot backend\JWT\src\main\resources\satellite-platform-application-f7154aa5ce46.json'
# SCOPES = ['https://www.googleapis.com/auth/drive', 'https://www.googleapis.com/auth/earthengine']

# # Load credentials
# credentials = service_account.Credentials.from_service_account_file(GEE_KEY_FILE, scopes=SCOPES)

# # Create a request object for token refresh
# request = Request()

# # Refresh to get a token
# credentials.refresh(request)

# # Print the token
# access_token = credentials.token
# print(f"Access Token: {access_token}")