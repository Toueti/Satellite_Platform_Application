# Dockerfile
# Use a slim version of Python 3.13 as the base image
FROM python:3.13-slim
# Avoid prompts during install
ENV DEBIAN_FRONTEND=noninteractive

# Install dependencies for GDAL, rasterio, etc.
RUN apt-get update && apt-get install -y \
    gdal-bin \
    libgdal-dev \
    python3-dev \
    build-essential \
    && apt-get clean

# Set GDAL config path
ENV CPLUS_INCLUDE_PATH=/usr/include/gdal
ENV C_INCLUDE_PATH=/usr/include/gdal

# Set working directory
WORKDIR /app

# Copy files
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY ./app /app

# Run the FastAPI app
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
