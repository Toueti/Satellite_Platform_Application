# Satellite Platform

A Spring Boot application for managing satellite imagery data with user authentication and project management capabilities.

## Features
- **Role-Based Access Control**: 
  - User roles (Admin, Thematician) with different permissions
  - JWT authentication with refresh token support
- **User Management**: 
  - Secure signup/login with password hashing
  - Profile updates and account deletion with cascading project removal
- **Project Management**: 
  - Create projects with unique names per user
  - Rename/delete projects with ownership validation
  - Project sharing capabilities (via ProjectSharingRequest DTO)
- **Image Management**:
  - Add satellite images with metadata storage
  - Unique image names within projects
  - Integration with Google Earth Engine (GeeController)
  - Image processing and thumbnail generation
- **Security**:
  - Spring Security with JWT token authentication
  - Rate limiting (RedisRateLimiter)
  - Request validation and sanitization
- **Data Management**:
  - MongoDB compound indexes for data integrity
  - Automatic cleanup of orphaned resources
  - Database migration support
- **Monitoring**:
  - SLF4J logging with Spring AOP
  - Project statistics tracking (ProjectStatisticsDto)

## Technologies Used
- Java 17
- Spring Boot 3.x
- MongoDB
- Spring Security
- JSON Web Tokens (JWT)
- Lombok
- SLF4J logging
- Maven

## Prerequisites
- Java 17 JDK
- MongoDB 5.0+
- Maven 3.8+

## Installation
1. Clone repository:
```bash
git clone https://github.com/yourusername/satellite-platform.git
cd satellite-platform
```

2. Configure MongoDB:
```bash
mongod --dbpath=/path/to/data/directory
```

3. Configure application:
```properties
# src/main/resources/application.properties
spring.data.mongodb.uri=mongodb://localhost:27017/satellite_db
jwt.secret=your-512-bit-secret-key
jwt.expiration=86400000 # 24 hours
```

## Running the Application
```bash
mvn spring-boot:run
```
Application will be available at `http://localhost:8080`

## API Endpoints

### Authentication
| Method | Path        | Description          | Request Body                                  | Success Response                          |
|--------|-------------|----------------------|-----------------------------------------------|-------------------------------------------|
| POST   | /api/signup | Register new user    | `{"username":"email","password":"str","role":"ENUM"}` | 201 Created with JWT token               |
| POST   | /api/signin | Authenticate user    | `{"username":"email","password":"str"}`       | 200 OK with JWT token and refresh token   |

Example Error Response:
```json
{
  "timestamp": "2025-03-15T01:56:12.461+00:00",
  "status": 409,
  "error": "Conflict",
  "message": "Username already exists"
}
```

### Project Management
| Method | Path                | Description              | Parameters                   | Required Headers          |
|--------|---------------------|--------------------------|------------------------------|---------------------------|
| POST   | /api/projects       | Create new project       | `CreateProjectRequest` DTO   | Authorization: Bearer JWT |
| PUT    | /api/projects/{id}  | Rename project           | `projectName` in body        | Authorization: Bearer JWT |
| DELETE | /api/projects/{id}  | Delete project           | -                            | Authorization: Bearer JWT |

Example Project Response:
```json
{
  "id": "65f2b8a6d94f8e1d4c8b4567",
  "projectName": "Urban Growth Analysis",
  "owner": "user@example.com",
  "createdAt": "2025-03-15T01:56:12.461Z",
  "imageCount": 12
}
```

### Image Management
| Method | Path                        | Description          | Request Body                          | Valid Content Types              |
|--------|-----------------------------|----------------------|---------------------------------------|-----------------------------------|
| POST   | /api/projects/{id}/images   | Add image to project | Multipart file + `ImageDTO` metadata  | image/png, image/jpeg, image/tiff |
| DELETE | /api/images/{imageId}       | Remove image         | -                                     | -                                 |

Example Image Metadata:
```json
{
  "imageName": "2025-urban-sprawl",
  "coverageArea": "POLYGON((...))",
  "acquisitionDate": "2025-01-15",
  "resolution": 0.5,
  "sensorType": "Sentinel-2"
}
```

## Project Structure
```
src/main/java/com/enit/satellite_platform/
├── user_management/       # Authentication & user operations
├── project_management/    # Project CRUD operations  
├── resources_management/  # Image handling and processing
├── config/                # Security and application config
└── exception/             # Custom exception handlers
```
## Usage
 Typical Workflow

**Register new user**

curl -X POST http://localhost:8080/api/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"analyst@geo.com","password":"SecurePass123!","role":"THEMATICIAN"}'

**Authenticate**

curl -X POST http://localhost:8080/api/signin \
  -H "Content-Type: application/json" \
  -d '{"username":"analyst@geo.com","password":"SecurePass123!"}'

**Project Creation**

curl -X POST http://localhost:8080/api/projects \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"projectName":"Coastal Erosion","description":"Monitoring shoreline changes"}' 

**Image Upload**

curl -X POST http://localhost:8080/api/projects/65f2b8a6d94f8e1d4c8b4567/images \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@coastal_2025.tiff" \
  -F "metadata='{\"imageName\":\"january-shoreline\",\"resolution\":0.3,\"sensorType\":\"Landsat-9\"}'"

**Data Analysis**

curl -X POST http://localhost:8080/api/gee/process \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "projectId": "65f2b8a6d94f8e1d4c8b4567",
    "operation": "NDWI",
    "parameters": {
      "band1": "B3",
      "band2": "B8"
    }
  }'

## Troubleshooting
**MongoDB Connection Issues**
- Verify MongoDB is running
- Check connection string in application.properties

**Duplicate Key Errors**
- Ensure unique project names per user
- Unique image names within projects

**JWT Errors**
- Verify secret key configuration
- Check token expiration settings

## Contributing
Contributions are welcome! Please follow these steps:
1. Fork the repository
2. Create your feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m 'Add some feature'`
4. Push to the branch: `git push origin feature/your-feature`
5. Open a Pull Request

## License
MIT License
