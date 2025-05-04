import requests
import json
from pathlib import Path

def test_calculate_ndvi():
    # API endpoint
    url = "http://localhost:8000/calculate/ndvi"
    
    # Test image path
    image_path = "image.tif"
    
    # Prepare the metadata
    metadata = {
        "redBand": 1,
        "nirBand": 2
    }
    
    # Prepare the files
    files = {
        'file': ('image.tif', open(image_path, 'rb'), 'image/tiff'),
        'metadata': (None, json.dumps(metadata), 'application/json')
    }
    
    # Send request
    response = requests.post(url, files=files)
    
    # Check status code
    print(f"Status Code: {response.status_code}")
    
    if response.status_code == 200:
        # Get content type and boundary
        content_type = response.headers.get('Content-Type', '')
        if 'multipart/mixed' in content_type:
            boundary = content_type.split('boundary=')[1]
            
            # Split response into parts using boundary
            parts = response.content.split(f'--{boundary}'.encode())
            
            # Process each part
            for part in parts:
                if not part.strip():
                    continue
                    
                # Split headers from content
                try:
                    headers_raw, content = part.split(b'\r\n\r\n', 1)
                    headers = headers_raw.decode()
                    
                    if 'Content-Type: application/json' in headers:
                        # Process JSON metadata
                        try:
                            metadata = json.loads(content.strip().decode())
                            print("\nMetadata:")
                            print(json.dumps(metadata, indent=2))
                        except json.JSONDecodeError:
                            print("Error decoding JSON metadata")
                            
                    elif 'Content-Type: image/png' in headers:
                        # Save image
                        output_file = "test_output.png"
                        with open(output_file, 'wb') as f:
                            # Remove the final boundary marker if present
                            if b'--\r\n' in content:
                                content = content.split(b'--\r\n')[0]
                            f.write(content.strip())
                        print(f"\nImage saved as: {output_file}")
                        
                except ValueError:
                    continue
    else:
        print(f"Error: {response.text}")

if __name__ == "__main__":
    test_calculate_ndvi()
