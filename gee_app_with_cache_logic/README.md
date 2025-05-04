# Satellite Image Processing Backend

Python backend for satellite image processing using Google Earth Engine (GEE), integrated with Spring Boot.

## 🚀 Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-repo/satellite-python-backend.git
   cd satellite-python-backend
   ```
2. Create a virtual environment:
   ```bash
   python -m venv venv
   ```
3. Activate the virtual environment:
   - On Windows:
     ```bash
     venv\Scripts\activate
     ```
   - On macOS/Linux:
     ```bash
     source venv/bin/activate
     ```

2. **Install dependencies**
   ```bash
   pip install -r requirements.txt
   ```

3. **GEE Credentials Setup**
   - Create a [Google Cloud Service Account](https://cloud.google.com/iam/docs/service-accounts) with Earth Engine access
   - Download the JSON key file
   - Place it in `config/` directory
   - Update `config.py`:
     ```python
     GEE_SERVICE_ACCOUNT = 'your-service-account@project.iam.gserviceaccount.com'
     GEE_KEY_FILE = r'config/your-key-file.json'  # Raw string for Windows
     ```
    - Create `.env` file:
         ```env
      GEE_SERVICE_ACCOUNT=your-service-account@project.iam.gserviceaccount.com
      GEE_KEY_FILE=config/your-key-file.json
## 🔒 Security
- Never commit your GEE key file (`*.json`) to version control
- Add `config/` to `.gitignore`

## Running the Server

```bash
python gee_app/app.py  !
uvicorn app.main:app --reload
```

The server will be available at `http://localhost:8000`


## 🌐 API Endpoints

### POST `/api/gee/fetch-images`
Fetch satellite images metadata within specified region/dates.

**Request:**
```json
{
  "region": {
    "type": "Polygon",
    "coordinates": [[
      [10.0, 20.0],
      [30.0, 40.0],
      [50.0, 60.0],
      [10.0, 20.0]
    ]]
  },
  "startDate": "2023-01-01",
  "endDate": "2023-12-31",
  "maxCloudCover": 20.0,
  "processingParameters": {}
}
```

**Response:**
```json
{
  "images": [
    {
      "id": "COPERNICUS/S2/20230101...",
      "date": "2023-01-01",
      "cloudCover": 5.2,
      "bounds": {"type": "Polygon", "coordinates": [...]},
      "previewUrl": "https://..."
    }
  ],
  "totalResults": 15
}
```

### POST `/api/gee/process-image`
Process an image with specified parameters.

**Request:**
```json
{
  "lat": 34.05,
  "lon": -118.25,
  "startDate": "2023-01-01",
  "endDate": "2023-01-10",
  "collection": "COPERNICUS/S2",
  "bands": ["B8", "B4"],
  "scale": 10
}

**Response:**
```json
{
  "imageUrl": "https://earthengine.googleapis.com/...",
  "metadata": {
    "bands": ["B8", "B4"],
    "scale": 10
  }
}
```

### POST `/api/gee/calculate-ndvi`
Calculate NDVI for an image.

**Request:**
```json
{
  "imageId": "COPERNICUS/S2/20230101...",
  "config": {
    "bands": ["B8", "B4"],
    "scale": 10
  }
}
```

### POST `/api/gee/get-download-url`
Get download URL for processed image.

**Request:** Same as process-image

**Response:**
{
  "downloadUrl": "https://earthengine.googleapis.com/...",
  "status": "success"
}

---

## 🧪 Testing

```bash
# Fetch images
curl -X POST "http://localhost:8000/api/gee/fetch-images" \
-H "Content-Type: application/json" \
-d '{
  "region": {
    "type": "Point",
    "coordinates": [10.0, 20.0]
  },
  "startDate": "2023-01-01",
  "endDate": "2023-12-31",
  "maxCloudCover": 20.0
}'

# Get download URL
curl -X POST "http://localhost:8000/api/gee/get-download-url" \
-H "Content-Type: application/json" \
-d '{
  "lat": 34.05,
  "lon": -118.25,
  "startDate": "2023-01-01",
  "endDate": "2023-01-10"
}'
```

---

## 🔧 Running the Server
```bash
uvicorn app.main:app --reload --port 8000
```

Access docs: http://localhost:8000/docs
