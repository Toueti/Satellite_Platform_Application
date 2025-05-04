import requests
import json
import os

# API endpoint
url = "http://localhost:8000/calculate/NDVI"

# Path to the image file
file_path = "image.tif"

# Check if the file exists
if not os.path.exists(file_path):
    print(f"Error: File {file_path} not found!")
    exit(1)

# Prepare the metadata
metadata = {"redBand": 1, "nirBand": 2}

try:
    # Open the file in a way that ensures it's closed properly
    with open(file_path, 'rb') as file_object:
        # Read file content
        file_content = file_object.read()
        
        # Prepare the files and data for the request
        files = {
            'file': (os.path.basename(file_path), file_content, 'image/tiff')
        }
        data = {
            'metadata': json.dumps(metadata)  # Convert metadata to JSON string
        }
        
        # Make the request
        print("Sending request to calculate NDVI...")
        response = requests.post(url, files=files, data=data)
        
        # Check response status
        print(f"Response Status Code: {response.status_code}")
        
        if response.status_code == 200:
            print("Success! NDVI calculated successfully.")
            print(json.dumps(response.json(), indent=2))
        else:
            print("Error in API response!")
            print(f"Response: {response.text}")
            
except Exception as e:
    print(f"Exception occurred: {str(e)}")