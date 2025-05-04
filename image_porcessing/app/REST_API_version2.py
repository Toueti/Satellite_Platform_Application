import os
import time
from datetime import datetime
from typing import Optional, List
import numpy as np
import rasterio
from fastapi import FastAPI, File, UploadFile, Form, HTTPException
from starlette.responses import Response, FileResponse
import json
import matplotlib.pyplot as plt
from matplotlib.colors import LinearSegmentedColormap


app = FastAPI(title="Satellite Vegetation Indices Calculator API")

class VegetationIndicesCalculator:
    def __init__(self, filepath: str):
        self.filepath = filepath
        self.dataset = None
        self.index_image = None
        self.load_image()
    
    def load_image(self):
        try:
            self.dataset = rasterio.open(self.filepath)
        except Exception as e:
            raise HTTPException(status_code=400, detail=f"Failed to load image: {str(e)}")

    def calculate_ndvi(self, red_band: int = 1, nir_band: int = 2) -> np.ndarray:
        try:
            # Check if bands exist
            max_band = max(red_band, nir_band)
            if max_band > self.dataset.count:
                raise HTTPException(
                    status_code=400, 
                    detail=f"The selected file only has {self.dataset.count} bands, but band {max_band} was requested."
                )
            
            red = self.dataset.read(red_band).astype(np.float32)
            nir = self.dataset.read(nir_band).astype(np.float32)
            
            denominator = nir + red
            ndvi = np.where(denominator > 0, (nir - red) / denominator, 0)
            
            return ndvi
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Failed to calculate NDVI: {str(e)}")

    def calculate_evi(
        self, 
        red_band: int = 1, 
        nir_band: int = 2, 
        blue_band: int = 3,
        G: float = 2.5,
        C1: float = 6.0,
        C2: float = 7.5,
        L: float = 1.0
    ) -> np.ndarray:
        try:
            # Check if bands exist
            max_band = max(red_band, nir_band, blue_band)
            if max_band > self.dataset.count:
                raise HTTPException(
                    status_code=400,
                    detail=f"The selected file only has {self.dataset.count} bands, but band {max_band} was requested."
                )
            
            red = self.dataset.read(red_band).astype(np.float32)
            nir = self.dataset.read(nir_band).astype(np.float32)
            blue = self.dataset.read(blue_band).astype(np.float32)
            
            denominator = nir + C1 * red - C2 * blue + L
            evi = np.where(denominator > 0, G * (nir - red) / denominator, 0)
            
            return evi
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Failed to calculate EVI: {str(e)}")

    def calculate_savi(self, red_band: int = 1, nir_band: int = 2, L: float = 0.5) -> np.ndarray:
        try:
            red = self.dataset.read(red_band).astype(np.float32)
            nir = self.dataset.read(nir_band).astype(np.float32)
            
            return ((nir - red) / (nir + red + L)) * (1 + L)
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Failed to calculate SAVI: {str(e)}")

    def calculate_ndwi(self, green_band: int = 2, nir_band: int = 3) -> np.ndarray:
        try:
            green = self.dataset.read(green_band).astype(np.float32)
            nir = self.dataset.read(nir_band).astype(np.float32)
            
            return (green - nir) / (green + nir)
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Failed to calculate NDWI: {str(e)}")

    def save_result(self, index_image: np.ndarray, output_path: str, index_type: str):
        try:
            # Create a figure
            plt.figure(figsize=(10, 8))
            
            # Create custom colormap for vegetation indices
            colors = ['#d73027', '#f46d43', '#fdae61', '#fee08b', '#d9ef8b', '#a6d96a', '#66bd63', '#1a9850']
            cmap = LinearSegmentedColormap.from_list('vegetation', colors)
            
            # Plot the index
            plt.imshow(index_image, cmap=cmap)
            plt.colorbar(label=index_type)
            plt.title(f"{index_type} - {os.path.basename(self.filepath)}")
            plt.axis('off')
            
            # Save the plot
            plt.savefig(output_path, dpi=300, bbox_inches='tight')
            plt.close()
            
            return output_path
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Failed to save result: {str(e)}")

    def calculate_statistics(self, index_image: np.ndarray) -> dict:
        valid_pixels = index_image[~np.isnan(index_image) & ~np.isinf(index_image)]
        if len(valid_pixels) > 0:
            return {
                "min": float(np.min(valid_pixels)),
                "max": float(np.max(valid_pixels)),
                "mean": float(np.mean(valid_pixels)),
                "median": float(np.median(valid_pixels)),
                "std": float(np.std(valid_pixels))
            }
        return {
            "error": "No valid pixels found in the result"
        }
    
    def __del__(self):
        # Close the dataset to prevent file access issues
        if self.dataset:
            self.dataset.close()

@app.post("/calculate/{index_type}")
async def calculate_index(
    index_type: str,
    file: UploadFile = File(...),
    metadata: str = Form(...)
) -> Response:
    # Parse metadata JSON
    try:
        params = json.loads(metadata)
        red_band = params.get("redBand", 1)
        nir_band = params.get("nirBand", 2)
        blue_band = params.get("blueBand", 3)
        G = params.get("G", 2.5)
        C1 = params.get("C1", 6.0)
        C2 = params.get("C2", 7.5)
        L = params.get("L", 1.0)
    except json.JSONDecodeError as e:
        raise HTTPException(status_code=400, detail=f"Invalid metadata JSON: {str(e)}")
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Error parsing metadata: {str(e)}")

    # Record start time
    start_time = datetime.now()
    start_timestamp = start_time.isoformat()

    # Create a unique filename with timestamp to avoid conflicts
    unique_prefix = f"upload_{int(time.time())}_{os.urandom(4).hex()}"

    try:
        # Create output directory if it doesn't exist
        output_dir = "output"
        if not os.path.exists(output_dir):
            os.makedirs(output_dir)
        
        # Create a temporary file directly in the output directory
        temp_file_path = os.path.join(output_dir, f"{unique_prefix}_input.tif")
        
        # Read the uploaded file content
        content = await file.read()
        
        # Write content to the temporary file
        with open(temp_file_path, "wb") as f:
            f.write(content)
        
        # Process the file
        try:
            # Initialize calculator
            calculator = VegetationIndicesCalculator(temp_file_path)
            
            # Calculate the requested index
            if index_type.upper() == "NDVI":
                result = calculator.calculate_ndvi(red_band, nir_band)
            elif index_type.upper() == "EVI":
                result = calculator.calculate_evi(red_band, nir_band, blue_band, G, C1, C2, L)
            elif index_type.upper() == "SAVI":
                result = calculator.calculate_savi(red_band, nir_band, L)
            elif index_type.upper() == "NDWI":
                result = calculator.calculate_ndwi(green_band=2, nir_band=nir_band)
            else:
                raise HTTPException(
                    status_code=400,
                    detail=f"Unsupported index type: {index_type}. Supported types are: NDVI, EVI, SAVI, NDWI"
                )
            
            # Save the result
            output_filename = f"{unique_prefix}_{index_type.lower()}.png"
            output_path = os.path.join(output_dir, output_filename)
            calculator.save_result(result, output_path, index_type)
            
            # Calculate statistics
            stats = calculator.calculate_statistics(result)
            
            # Explicitly close the dataset
            calculator.dataset.close()
            calculator.dataset = None
            
            # Record end time and calculate duration
            end_time = datetime.now()
            end_timestamp = end_time.isoformat()
            duration = (end_time - start_time).total_seconds()
            
            # Clean up the temporary input file
            try:
                os.remove(temp_file_path)
            except Exception:
                pass  # Ignore errors when cleaning up
            
            # Create metadata dictionary
            result_metadata = {
                "index_type": index_type,
                "start_time": start_timestamp,
                "end_time": end_timestamp,
                "processing_duration": duration,
                "statistics": stats
            }
            
            # Read the generated image file
            with open(output_path, "rb") as img_file:
                img_content = img_file.read()
            
            # Create multipart response with metadata and image
            boundary = "boundary"
            headers = {
                "Content-Type": f"multipart/mixed; boundary={boundary}"
            }
            
            # Format multipart content
            content = (
                f"--{boundary}\r\n"
                f"Content-Type: application/json\r\n\r\n"
                f"{json.dumps(result_metadata)}\r\n"
                f"--{boundary}\r\n"
                f"Content-Type: image/png\r\n"
                f"Content-Disposition: attachment; filename={output_filename}\r\n\r\n"
            ).encode()
            
            content += img_content
            content += f"\r\n--{boundary}--".encode()
            
            return Response(content=content, headers=headers)
            
        finally:
            # Make sure the calculator is closed
            if 'calculator' in locals() and calculator.dataset:
                try:
                    calculator.dataset.close()
                except:
                    pass
                    
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/download/{filename}")
async def download_result(filename: str):
    file_path = os.path.join("output", filename)
    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="File not found")
    return FileResponse(file_path)

@app.get("/")
async def root():
    return {
        "message": "Welcome to the Satellite Vegetation Indices Calculator API",
        "version": "2.0",
        "endpoints": {
            "/calculate/{index_type}": "Calculate vegetation indices (NDVI, EVI, SAVI, NDWI)",
            "/download/{filename}": "Download processed results"
        }
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
