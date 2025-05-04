import os
import numpy as np
import rasterio
from fastapi import FastAPI, UploadFile, File, HTTPException, Form
from fastapi.responses import FileResponse
from typing import Optional
import tempfile
from pathlib import Path
from matplotlib.colors import LinearSegmentedColormap

app = FastAPI(title="Vegetation Indices Calculator API")

# Define a custom NDVI colormap (low NDVI = red/brown, high NDVI = green)
def get_vegetation_colormap():
    colors = [
        '#d73027',  # Red (very low NDVI, unhealthy/dead vegetation)
        '#f46d43',  # Orange
        '#fdae61',  # Light orange
        '#fee08b',  # Yellow (sparse vegetation)
        '#d9ef8b',  # Light green
        '#a6d96a',  # Green
        '#66bd63',  # Darker green
        '#1a9850'   # Dark green (healthy, dense vegetation)
    ]
    return LinearSegmentedColormap.from_list('vegetation', colors, N=256)

# Helper function to apply colormap to NDVI/EVI values
def apply_colormap_to_image(index_image: np.ndarray, min_val: float = -1.0, max_val: float = 1.0):
    # Normalize the index values to 0-1 range
    norm_image = np.clip((index_image - min_val) / (max_val - min_val), 0, 1)
    
    # Get the colormap
    cmap = get_vegetation_colormap()
    
    # Convert normalized values to RGB (0-255 range)
    rgb = (cmap(norm_image)[:, :, :3] * 255).astype(np.uint8)  # Drop alpha channel, scale to 0-255
    
    return rgb  # Shape: (height, width, 3)

# Helper function to calculate NDVI
def calculate_ndvi(dataset, red_band: int, nir_band: int):
    try:
        if max(red_band, nir_band) > dataset.count:
            raise ValueError(f"File has {dataset.count} bands, but band {max(red_band, nir_band)} was requested.")
        
        red = dataset.read(red_band).astype(np.float32)
        nir = dataset.read(nir_band).astype(np.float32)
        
        denominator = nir + red
        ndvi = np.where(denominator > 0, (nir - red) / denominator, 0)
        return ndvi
    except Exception as e:
        raise ValueError(f"Failed to calculate NDVI: {str(e)}")

# Helper function to calculate EVI
def calculate_evi(dataset, red_band: int, nir_band: int, blue_band: int, G: float = 2.5, C1: float = 6.0, C2: float = 7.5, L: float = 1.0):
    try:
        if max(red_band, nir_band, blue_band) > dataset.count:
            raise ValueError(f"File has {dataset.count} bands, but band {max(red_band, nir_band, blue_band)} was requested.")
        
        red = dataset.read(red_band).astype(np.float32)
        nir = dataset.read(nir_band).astype(np.float32)
        blue = dataset.read(blue_band).astype(np.float32)
        
        denominator = nir + C1 * red - C2 * blue + L
        evi = np.where(denominator > 0, G * (nir - red) / denominator, 0)
        return evi
    except Exception as e:
        raise ValueError(f"Failed to calculate EVI: {str(e)}")

# Save processed image as GeoTIFF (single-band or RGB)
def save_tif(image_data: np.ndarray, input_dataset, output_path: str, is_rgb: bool = False):
    if is_rgb:
        # RGB image (3 bands)
        with rasterio.open(
            output_path,
            'w',
            driver='GTiff',
            height=image_data.shape[0],
            width=image_data.shape[1],
            count=3,  # 3 bands for RGB
            dtype=image_data.dtype,
            crs=input_dataset.crs,
            transform=input_dataset.transform,
        ) as dst:
            for i in range(3):
                dst.write(image_data[:, :, i], i + 1)
    else:
        # Single-band image
        with rasterio.open(
            output_path,
            'w',
            driver='GTiff',
            height=image_data.shape[0],
            width=image_data.shape[1],
            count=1,
            dtype=image_data.dtype,
            crs=input_dataset.crs,
            transform=input_dataset.transform,
        ) as dst:
            dst.write(image_data, 1)

# API Endpoint to process the image
@app.post("/calculate-index/")
async def calculate_index(
    file: UploadFile = File(...),
    index_type: str = Form(default="NDVI", regex="^(NDVI|EVI)$"),
    red_band: int = Form(default=4),
    nir_band: int = Form(default=5),
    blue_band: int = Form(default=2),
    evi_g: Optional[float] = Form(default=2.5),
    evi_c1: Optional[float] = Form(default=6.0),
    evi_c2: Optional[float] = Form(default=7.5),
    evi_l: Optional[float] = Form(default=1.0),
    apply_colormap: Optional[bool] = Form(default=False),
    min_display: Optional[float] = Form(default=-1.0),
    max_display: Optional[float] = Form(default=1.0)
):
    """
    Calculate vegetation index (NDVI or EVI) from a .tif image and optionally apply a colormap.
    Parameters:
    - file: Uploaded .tif image
    - index_type: "NDVI" or "EVI"
    - red_band: Band number for red (default: 4)
    - nir_band: Band number for NIR (default: 5)
    - blue_band: Band number for blue (default: 2, required for EVI)
    - evi_g, evi_c1, evi_c2, evi_l: EVI parameters (optional, defaults provided)
    - apply_colormap: If true, return a colorized RGB .tif (default: False)
    - min_display: Min value for colormap scaling (default: -1.0)
    - max_display: Max value for colormap scaling (default: 1.0)
    Returns:
    - Processed .tif file (single-band index or colorized RGB)
    """
    # Check file extension
    if not file.filename.lower().endswith(('.tif', '.tiff')):
        raise HTTPException(status_code=400, detail="Only .tif or .tiff files are supported.")

    # Use a temporary file to store the uploaded image
    with tempfile.NamedTemporaryFile(delete=False, suffix=".tif") as temp_input:
        temp_input.write(await file.read())
        temp_input_path = temp_input.name

    try:
        # Open the uploaded image
        with rasterio.open(temp_input_path) as dataset:
            # Calculate the requested index
            if index_type == "NDVI":
                index_image = calculate_ndvi(dataset, red_band, nir_band)
            else:  # EVI
                index_image = calculate_evi(dataset, red_band, nir_band, blue_band, evi_g, evi_c1, evi_c2, evi_l)

            # Save the result to a temporary output file
            with tempfile.NamedTemporaryFile(delete=False, suffix=".tif") as temp_output:
                output_path = temp_output.name
                
                if apply_colormap:
                    # Apply colormap and save as RGB
                    rgb_image = apply_colormap_to_image(index_image, min_display, max_display)
                    save_tif(rgb_image, dataset, output_path, is_rgb=True)
                    output_filename = f"{index_type}_colorized.tif"
                else:
                    # Save as single-band index
                    save_tif(index_image, dataset, output_path, is_rgb=False)
                    output_filename = f"{index_type}_result.tif"

        # Return the processed file
        return FileResponse(
            output_path,
            media_type="image/tiff",
            filename=output_filename,
            headers={"Content-Disposition": f"attachment; filename={output_filename}"}
        )
    except ValueError as ve:
        raise HTTPException(status_code=400, detail=str(ve))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Processing failed: {str(e)}")
    finally:
        # Clean up temporary input file
        if os.path.exists(temp_input_path):
            os.remove(temp_input_path)
        # Output file will be cleaned up by the OS after response

# Run the app (for local testing)
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)