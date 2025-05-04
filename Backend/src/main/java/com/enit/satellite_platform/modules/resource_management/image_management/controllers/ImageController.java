package com.enit.satellite_platform.modules.resource_management.image_management.controllers;

import com.enit.satellite_platform.exceptions.DuplicationException;
import com.enit.satellite_platform.exceptions.ResourceNotFoundException;
import com.enit.satellite_platform.modules.resource_management.dto.ImageImportRequest;
import com.enit.satellite_platform.modules.resource_management.image_management.dto.ImageDTO;
import com.enit.satellite_platform.modules.resource_management.image_management.entities.Image;
import com.enit.satellite_platform.modules.resource_management.image_management.services.ImageService;
import com.enit.satellite_platform.shared.dto.GenericResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/geospatial/images")
@CrossOrigin(origins = "*")
public class ImageController {

    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);

    @Autowired
    private ImageService imageService;

    @Operation(summary = "Add a new image to a project")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Image added successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid image data or storage type"),
        @ApiResponse(responseCode = "404", description = "Project not found"),
        @ApiResponse(responseCode = "500", description = "Error adding image")
    })
    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GenericResponse<?>> addImage(
        @RequestPart("projectId") String projectId,
        @RequestPart("imageName") String imageName,
        @RequestPart(value = "metadata", required = false) String metadataJson,
        @RequestPart("file") MultipartFile file) {
            // Removed storageType @RequestPart
        logger.info("Received request to add image: projectId={}, imageName={}, metadata={}, file={}",
            projectId, imageName, metadataJson, file.getOriginalFilename()); // Removed storageType from log

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> metadata = metadataJson != null
                ? mapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {})
                : null;

            ImageDTO dto = new ImageDTO();
            dto.setProjectId(projectId);
            dto.setImageName(imageName);
            dto.setMetadata(metadata);
            dto.setFile(file);
            dto.setFileSize(file.getSize());

            // Call service without storageType parameter
            ImageDTO addedImage = imageService.addImage(dto); // Corrected: Removed second argument

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new GenericResponse<>("SUCCESS", "Image added successfully", addedImage));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid image data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (IOException e) {
            logger.error("Error processing metadata or file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericResponse<>("FAILURE", "Error processing request: " + e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error adding image: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GenericResponse<>("FAILURE", "Error adding image: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Rename an image")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Image renamed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or duplicate name"),
        @ApiResponse(responseCode = "500", description = "Error renaming image")
    })
    @PutMapping("/{id}/rename")
    public ResponseEntity<?> renameImage(
        @PathVariable String id,
        @RequestParam String newName,
        @RequestParam String projectId) {
        logger.info("Received request to rename image: id={}, newName={}, projectId={}", id, newName, projectId);
        try {
            Image image = imageService.renameImage(id, newName, new ObjectId(projectId));
            return ResponseEntity.ok(image);
        } catch (DuplicationException e) {
            logger.warn("Duplicate name error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error renaming image: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error renaming image: " + e.getMessage());
        }
    }

    @Operation(summary = "Get all images with pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Images retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
        @ApiResponse(responseCode = "500", description = "Error retrieving images")
    })
    @GetMapping
    public ResponseEntity<GenericResponse<?>> getAllImages(Pageable pageable) {
        logger.info("Received request to fetch all images with pagination: {}", pageable);
        try {
            Page<ImageDTO> images = imageService.getAllImages(pageable);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Images retrieved successfully", images));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid pagination parameters: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (RuntimeException e) {
            logger.error("Error retrieving images: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GenericResponse<>("FAILURE", "Error retrieving images: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Get image by name and project ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Image retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Image not found"),
        @ApiResponse(responseCode = "400", description = "Invalid project ID or name"),
        @ApiResponse(responseCode = "500", description = "Error retrieving image")
    })
    @GetMapping("/name/{projectId}/{name}")
    public ResponseEntity<GenericResponse<ImageDTO>> getImageByName(
        @Parameter(description = "Project ID") @PathVariable String projectId,
        @Parameter(description = "Image name") @PathVariable String name) {
        logger.info("Received request to fetch image with name: {} for project ID: {}", name, projectId);
        try {
            ObjectId projectObjectId = new ObjectId(projectId);
            Optional<ImageDTO> image = imageService.getImageByName(name, projectObjectId);
            return image
                .map(value -> ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Image retrieved successfully", value)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", "Image not found with name: " + name + " in project: " + projectId, null)));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid project ID or name: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (RuntimeException e) {
            logger.error("Error retrieving image: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GenericResponse<>("FAILURE", "Error retrieving image: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Get raw image data by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Image data retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Image or image data not found"),
        @ApiResponse(responseCode = "400", description = "Invalid image ID"),
        @ApiResponse(responseCode = "500", description = "Error retrieving image data")
    })
    @GetMapping("/{id}/data")
    public ResponseEntity<?> getImageData(
        @Parameter(description = "Image ID") @PathVariable String id) {
        logger.info("Received request to fetch image data for ID: {}", id);
        try {
            MultipartFile imageData = imageService.getImageData(id);

            if (imageData == null) {
                logger.warn("No image data found for ID: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", "No image data associated with ID: " + id, null));
            }

            String contentTypeStr = imageData.getContentType();
            MediaType contentType = contentTypeStr != null ? MediaType.parseMediaType(contentTypeStr)
                : MediaType.APPLICATION_OCTET_STREAM;

            String filename = imageData.getOriginalFilename() != null ? imageData.getOriginalFilename() : id + "_data";

            return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(imageData.getBytes());

        } catch (ResourceNotFoundException e) {
            logger.error("Image or data not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid image ID format: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericResponse<>("FAILURE", "Invalid image ID format: " + e.getMessage(), null));
        } catch (IOException e) {
            logger.error("Error reading image data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GenericResponse<>("FAILURE", "Error reading image data: " + e.getMessage(), null));
        } catch (RuntimeException e) {
            logger.error("Error retrieving image data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GenericResponse<>("FAILURE", "Error retrieving image data: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Get image by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Image retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Image not found"),
        @ApiResponse(responseCode = "400", description = "Invalid image ID"),
        @ApiResponse(responseCode = "500", description = "Error retrieving image")
    })
    @GetMapping("/{id}")
    public ResponseEntity<GenericResponse<?>> getImageById(
        @Parameter(description = "Image ID") @PathVariable String id) {
        logger.info("Received request to fetch image with ID: {}", id);
        try {
            ImageDTO image = imageService.getImageById(id);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Image retrieved successfully", image));
        } catch (IllegalArgumentException e) {
            logger.error("Image not found or invalid ID: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (RuntimeException e) {
            logger.error("Error retrieving image: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GenericResponse<>("FAILURE", "Error retrieving image: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Delete an image by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Image deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Image not found"),
        @ApiResponse(responseCode = "400", description = "Invalid image ID"),
        @ApiResponse(responseCode = "500", description = "Error deleting image")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<GenericResponse<?>> deleteImage(
        @Parameter(description = "Image ID") @PathVariable String id) {
        logger.info("Received request to delete image with ID: {}", id);
        try {
            imageService.deleteImage(id);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Image deleted successfully"));
        } catch (IllegalArgumentException e) {
            logger.error("Image not found or invalid ID: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (RuntimeException e) {
            logger.error("Error deleting image: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GenericResponse<>("FAILURE", "Error deleting image: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Get all images for a project")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Images retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Project not found or no images"),
        @ApiResponse(responseCode = "400", description = "Invalid project ID"),
        @ApiResponse(responseCode = "500", description = "Error retrieving images")
    })
    @GetMapping("/by-project/{projectId}")
    public ResponseEntity<GenericResponse<?>> getImagesByProject(
        @Parameter(description = "Project ID") @PathVariable String projectId,
        Pageable pageable) {
        logger.info("Received request to fetch images for project ID: {} with pageable: {}", projectId, pageable);
        try {
            ObjectId projectObjectId = new ObjectId(projectId);
            Page<ImageDTO> images = imageService.getImagesByProject(projectObjectId, pageable);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Images retrieved successfully", images));
        } catch (IllegalArgumentException e) {
            logger.error("Project not found or invalid ID: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (RuntimeException e) {
            logger.error("Error retrieving images: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GenericResponse<>("FAILURE", "Error retrieving images: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Delete all images for a project")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Images deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Project not found"),
        @ApiResponse(responseCode = "400", description = "Invalid project ID"),
        @ApiResponse(responseCode = "500", description = "Error deleting images")
    })
    @DeleteMapping("/by-project/{projectId}")
    public ResponseEntity<GenericResponse<?>> deleteAllImagesByProject(
        @Parameter(description = "Project ID") @PathVariable String projectId) {
        logger.info("Received request to soft delete all images for project ID: {}", projectId); // Updated log message
        try {
            ObjectId projectObjectId = new ObjectId(projectId);
            imageService.softDeleteAllImagesByProject(projectObjectId); // Correct method called
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "All images soft deleted successfully"));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid project ID: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (RuntimeException e) {
            logger.error("Error deleting images: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GenericResponse<>("FAILURE", "Error deleting images: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Get image by ID and project ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Image retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Image not found"),
        @ApiResponse(responseCode = "400", description = "Invalid image or project ID"),
        @ApiResponse(responseCode = "500", description = "Error retrieving image")
    })
    @GetMapping("/{imageId}/project/{projectId}")
    public ResponseEntity<GenericResponse<ImageDTO>> getImageByImageIdAndProject(
        @Parameter(description = "Image ID") @PathVariable String imageId,
        @Parameter(description = "Project ID") @PathVariable String projectId) {
        logger.info("Received request to fetch image with ID: {} for project ID: {}", imageId, projectId);
        try {
            ObjectId projectObjectId = new ObjectId(projectId);
            Optional<ImageDTO> image = imageService.getImageByImageIdAndProject(imageId, projectObjectId);
            return image
                .map(value -> ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Image retrieved successfully", value)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", "Image not found with ID: " + imageId + " in project: " + projectId, null)));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid image or project ID: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (RuntimeException e) {
            logger.error("Error retrieving image: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GenericResponse<>("FAILURE", "Error retrieving image: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Delete image by ID and project ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Image deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Image not found"),
        @ApiResponse(responseCode = "400", description = "Invalid image or project ID"),
        @ApiResponse(responseCode = "500", description = "Error deleting image")
    })
    @DeleteMapping("/{imageId}/project/{projectId}")
    public ResponseEntity<GenericResponse<?>> deleteImageByProject(
        @Parameter(description = "Image ID") @PathVariable String imageId,
        @Parameter(description = "Project ID") @PathVariable String projectId) {
        logger.info("Received request to delete image with ID: {} from project ID: {}", imageId, projectId);
        try {
            ObjectId projectObjectId = new ObjectId(projectId);
            imageService.deleteImageByProject(imageId, projectObjectId);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Image deleted successfully"));
        } catch (IllegalArgumentException e) {
            logger.error("Image not found or invalid ID: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (RuntimeException e) {
            logger.error("Error deleting image: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GenericResponse<>("FAILURE", "Error deleting image: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Bulk delete images by IDs")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Images deleted successfully"),
        @ApiResponse(responseCode = "404", description = "One or more images not found"),
        @ApiResponse(responseCode = "400", description = "Invalid image IDs"),
        @ApiResponse(responseCode = "500", description = "Error deleting images")
    })
    @DeleteMapping("/bulk-delete")
    public ResponseEntity<GenericResponse<?>> bulkDeleteImages(@RequestBody List<String> imageIds) {
        logger.info("Received request to bulk delete images with IDs: {}", imageIds);
        try {
            imageService.bulkDeleteImages(imageIds);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Images deleted successfully"));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid image IDs or not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (RuntimeException e) {
            logger.error("Error deleting images: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GenericResponse<>("FAILURE", "Error deleting images: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Count images in a project")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Image count retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Project not found"),
        @ApiResponse(responseCode = "400", description = "Invalid project ID"),
        @ApiResponse(responseCode = "500", description = "Error counting images")
    })
    @GetMapping("/count/{projectId}")
    public ResponseEntity<GenericResponse<?>> countImagesByProject(
        @Parameter(description = "Project ID") @PathVariable String projectId) {
        logger.info("Received request to count images for project ID: {}", projectId);
        try {
            ObjectId projectObjectId = new ObjectId(projectId);
            long count = imageService.countImagesByProject(projectObjectId);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Image count retrieved successfully", count));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid project ID: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (RuntimeException e) {
            logger.error("Error counting images: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GenericResponse<>("FAILURE", "Error counting images: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Import images from another project")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Images imported successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data (IDs, project IDs)"),
        @ApiResponse(responseCode = "403", description = "User lacks permissions for source or target project"),
        @ApiResponse(responseCode = "404", description = "Source/Target project or source images not found"),
        @ApiResponse(responseCode = "500", description = "Error importing images")
    })
    @PostMapping("/import")
    public ResponseEntity<GenericResponse<?>> importImages(@RequestBody ImageImportRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new GenericResponse<>("FAILURE", "User not authenticated", null));
        }
        String userEmail = authentication.getName();

        logger.info("Received request to import images by user {}: {}", userEmail, request);

        try {
            List<ImageDTO> importedImages = imageService.importImagesFromProject(
                request.getSourceProjectId(),
                request.getTargetProjectId(),
                request.getImageIds(),
                userEmail);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Images imported successfully", importedImages));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid import request data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (ResourceNotFoundException
                 | com.enit.satellite_platform.modules.project_management.exceptions.ProjectNotFoundException e) {
            logger.error("Resource not found during import: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (AccessDeniedException e) {
            logger.warn("Access denied during import for user {}: {}", userEmail, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (RuntimeException e) {
            logger.error("Error importing images: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                 .body(new GenericResponse<>("FAILURE", "Error importing images: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Restore a soft-deleted image")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Image restored successfully"),
        @ApiResponse(responseCode = "404", description = "Image not found or not deleted"),
        @ApiResponse(responseCode = "400", description = "Image is not deleted"),
        @ApiResponse(responseCode = "500", description = "Error restoring image")
    })
    @PostMapping("/{id}/restore")
    public ResponseEntity<GenericResponse<?>> restoreImage(
        @Parameter(description = "Image ID") @PathVariable String id) {
        logger.info("Received request to restore image with ID: {}", id);
        try {
            // TODO: Add permission check if needed (e.g., only project owner/editor?)
            ImageDTO restoredImage = imageService.restoreImage(id);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Image restored successfully", restoredImage));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error restoring image: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GenericResponse<>("FAILURE", "Error restoring image: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Force permanent deletion of a soft-deleted image")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Image permanently deleted successfully"),
        @ApiResponse(responseCode = "403", description = "User not authorized (not project owner)"),
        @ApiResponse(responseCode = "404", description = "Image not found"),
        @ApiResponse(responseCode = "500", description = "Error force deleting image")
    })
    @DeleteMapping("/{id}/force")
    public ResponseEntity<GenericResponse<?>> forceDeleteImage(
        @Parameter(description = "Image ID") @PathVariable String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
         if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new GenericResponse<>("FAILURE", "User not authenticated", null));
        }
        String userEmail = authentication.getName();
        logger.info("Received request to force delete image with ID: {} by user {}", id, userEmail);

        try {
            imageService.forceDeleteImage(id, userEmail);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Image permanently deleted successfully"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (AccessDeniedException e) {
             return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (IllegalStateException e) { // Catch if trying to force delete non-deleted image
             return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error force deleting image: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GenericResponse<>("FAILURE", "Error force deleting image: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Get soft-deleted images (paginated)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Deleted images retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Error retrieving deleted images")
    })
    @GetMapping("/deleted")
    public ResponseEntity<GenericResponse<?>> getDeletedImages(Pageable pageable) {
        logger.info("Received request to fetch soft-deleted images with pagination: {}", pageable);
        try {
            // Need to add getDeletedImages method to ImageService and ImageRepository
            Page<ImageDTO> images = imageService.getDeletedImages(pageable);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Deleted images retrieved successfully", images));
        } catch (Exception e) {
            logger.error("Error retrieving deleted images: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GenericResponse<>("FAILURE", "Error retrieving deleted images: " + e.getMessage(), null));
        }
    }

}

/* package com.enit.satellite_platform.modules.resource_management.image_management.controllers;

import com.enit.satellite_platform.exceptions.DuplicationException;
import com.enit.satellite_platform.exceptions.ResourceNotFoundException;
import com.enit.satellite_platform.modules.resource_management.dto.ImageImportRequest;
import com.enit.satellite_platform.modules.resource_management.image_management.dto.ImageDTO;
import com.enit.satellite_platform.modules.resource_management.image_management.entities.Image;
import com.enit.satellite_platform.modules.resource_management.image_management.services.ImageService;
import com.enit.satellite_platform.shared.dto.GenericResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/geospatial/images")
@CrossOrigin(origins = "*")
public class ImageController {

  private static final Logger logger = LoggerFactory.getLogger(ImageController.class);

  @Autowired
  private ImageService imageService;

  @Operation(summary = "Add a new image to a project")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Image added successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid image data"),
      @ApiResponse(responseCode = "404", description = "Project not found"),
      @ApiResponse(responseCode = "500", description = "Error adding image")
  })
  @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<GenericResponse<?>> addImage(
      @RequestPart("projectId") String projectId,
      @RequestPart("imageName") String imageName,
      @RequestPart("metadata") String metadataJson,
      @RequestPart("file") MultipartFile file) {
    logger.info("Received request to add image: projectId={}, imageName={}, metadata={}, file={}",
        projectId, imageName, metadataJson, file.getOriginalFilename());

    try {
      ObjectMapper mapper = new ObjectMapper();
      Map<String, Object> metadata = metadataJson != null
          ? mapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {
          })
          : null;

      ImageDTO dto = new ImageDTO();
      dto.setProjectId(projectId);
      dto.setImageName(imageName);
      dto.setMetadata(metadata);
      dto.setFile(file);
      dto.setFileSize(file.getSize());

      ImageDTO addedImage = imageService.addImage(dto);

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(new GenericResponse<>("SUCCESS", "Image added successfully", addedImage));
    } catch (IllegalArgumentException e) {
      logger.error("Invalid image data: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
    } catch (Exception e) {
      logger.error("Error adding image: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new GenericResponse<>("FAILURE", "Error adding image: " + e.getMessage(), null));
    }
  }

  @PutMapping("/{id}/rename")
  public ResponseEntity<?> renameImage(@PathVariable String id, @RequestParam String newName,
      @RequestParam String projectId) {
    try {
      Image image = imageService.renameImage(id, newName, new ObjectId(projectId));
      return ResponseEntity.ok(image);
    } catch (DuplicationException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error renaming image: " + e.getMessage());
    }
  }

  @Operation(summary = "Get all images with pagination")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Images retrieved successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
      @ApiResponse(responseCode = "500", description = "Error retrieving images")
  })
  @GetMapping
  public ResponseEntity<GenericResponse<?>> getAllImages(Pageable pageable) {
    logger.info("Received request to fetch all images with pagination: {}", pageable);
    try {
      Page<ImageDTO> images = imageService.getAllImages(pageable);
      return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Images retrieved successfully", images));
    } catch (IllegalArgumentException e) {
      logger.error("Invalid pagination parameters: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
    } catch (RuntimeException e) {
      logger.error("Error retrieving images: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new GenericResponse<>("FAILURE", "Error retrieving images: " + e.getMessage(), null));
    }
  }

  @Operation(summary = "Get image by name and project ID")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Image retrieved successfully"),
      @ApiResponse(responseCode = "404", description = "Image not found"),
      @ApiResponse(responseCode = "400", description = "Invalid project ID or name"),
      @ApiResponse(responseCode = "500", description = "Error retrieving image")
  })
  @GetMapping("/name/{projectId}/{name}")
  public ResponseEntity<GenericResponse<ImageDTO>> getImageByName(
      @Parameter(description = "Project ID") @PathVariable String projectId,
      @Parameter(description = "Image name") @PathVariable String name) {
    logger.info("Received request to fetch image with name: {} for project ID: {}", name, projectId);
    try {
      ObjectId projectObjectId = new ObjectId(projectId); // Validate ObjectId format
      Optional<ImageDTO> image = imageService.getImageByName(name, projectObjectId);
      return image
          .map(value -> ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Image retrieved successfully", value)))
          .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
              .body(new GenericResponse<>("FAILURE", "Image not found with name: " + name + " in project: " + projectId,
                  null)));
    } catch (IllegalArgumentException e) {
      logger.error("Invalid project ID or name: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
    } catch (RuntimeException e) {
      logger.error("Error retrieving image: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new GenericResponse<>("FAILURE", "Error retrieving image: " + e.getMessage(), null));
    }
  }

  @Operation(summary = "Get raw image data by ID")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Image data retrieved successfully"),
      @ApiResponse(responseCode = "404", description = "Image or image data not found"),
      @ApiResponse(responseCode = "400", description = "Invalid image ID"),
      @ApiResponse(responseCode = "500", description = "Error retrieving image data")
  })
  @GetMapping("/{id}/data")
  public ResponseEntity<?> getImageData(
      @Parameter(description = "Image ID") @PathVariable String id) {
    logger.info("Received request to fetch image data for ID: {}", id);
    try {
      MultipartFile imageData = imageService.getImageData(id);

      if (imageData == null) {
        logger.warn("No image data found for ID: {}", id);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new GenericResponse<>("FAILURE", "No image data associated with ID: " + id, null));
      }

      // Get content type from MultipartFile, fallback to octet-stream
      String contentTypeStr = imageData.getContentType();
      MediaType contentType = contentTypeStr != null ? MediaType.parseMediaType(contentTypeStr)
          : MediaType.APPLICATION_OCTET_STREAM;

      // Use original filename if available, otherwise construct one
      String filename = imageData.getOriginalFilename() != null ? imageData.getOriginalFilename() : id + "_data";

      return ResponseEntity.ok()
          .contentType(contentType)
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
          .body(imageData.getBytes());

    } catch (ResourceNotFoundException e) {
      logger.error("Image or data not found: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
    } catch (IllegalArgumentException e) {
      logger.error("Invalid image ID format: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(new GenericResponse<>("FAILURE", "Invalid image ID format: " + e.getMessage(), null));
    } catch (IOException e) {
      logger.error("Error reading image data: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new GenericResponse<>("FAILURE", "Error reading image data: " + e.getMessage(), null));
    } catch (RuntimeException e) {
      logger.error("Error retrieving image data: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new GenericResponse<>("FAILURE", "Error retrieving image data: " + e.getMessage(), null));
    }
  }

  @Operation(summary = "Get image by ID")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Image retrieved successfully"),
      @ApiResponse(responseCode = "404", description = "Image not found"),
      @ApiResponse(responseCode = "400", description = "Invalid image ID"),
      @ApiResponse(responseCode = "500", description = "Error retrieving image")
  })
  @GetMapping("/{id}")
  public ResponseEntity<GenericResponse<?>> getImageById(
      @Parameter(description = "Image ID") @PathVariable String id) {
    logger.info("Received request to fetch image with ID: {}", id);
    try {
      ImageDTO image = imageService.getImageById(id);
      return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Image retrieved successfully", image));
    } catch (IllegalArgumentException e) {
      logger.error("Image not found or invalid ID: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
    } catch (RuntimeException e) {
      logger.error("Error retrieving image: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new GenericResponse<>("FAILURE", "Error retrieving image: " + e.getMessage(), null));
    }
  }

  @Operation(summary = "Delete an image by ID")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Image deleted successfully"),
      @ApiResponse(responseCode = "404", description = "Image not found"),
      @ApiResponse(responseCode = "400", description = "Invalid image ID"),
      @ApiResponse(responseCode = "500", description = "Error deleting image")
  })
  @DeleteMapping("/{id}")
  public ResponseEntity<GenericResponse<?>> deleteImage(
      @Parameter(description = "Image ID") @PathVariable String id) {
    logger.info("Received request to delete image with ID: {}", id);
    try {
      imageService.deleteImage(id);
      return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Image deleted successfully"));
    } catch (IllegalArgumentException e) {
      logger.error("Image not found or invalid ID: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
    } catch (RuntimeException e) {
      logger.error("Error deleting image: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new GenericResponse<>("FAILURE", "Error deleting image: " + e.getMessage(), null));
    }
  }

  @Operation(summary = "Get all images for a project")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Images retrieved successfully"),
      @ApiResponse(responseCode = "404", description = "Project not found or no images"),
      @ApiResponse(responseCode = "400", description = "Invalid project ID"),
      @ApiResponse(responseCode = "500", description = "Error retrieving images")
  })
  @GetMapping("/by-project/{projectId}")
  public ResponseEntity<GenericResponse<?>> getImagesByProject(
      @Parameter(description = "Project ID") @PathVariable String projectId) {
    logger.info("Received request to fetch images for project ID: {}", projectId);
    try {
      ObjectId projectObjectId = new ObjectId(projectId); // Validate ObjectId format
      List<ImageDTO> images = imageService.getImagesByProject(projectObjectId);
      return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Images retrieved successfully", images));
    } catch (IllegalArgumentException e) {
      logger.error("Project not found or no images: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
    } catch (RuntimeException e) {
      logger.error("Error retrieving images: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new GenericResponse<>("FAILURE", "Error retrieving images: " + e.getMessage(), null));
    }
  }

  @Operation(summary = "Delete all images for a project")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Images deleted successfully"),
      @ApiResponse(responseCode = "404", description = "Project not found"),
      @ApiResponse(responseCode = "400", description = "Invalid project ID"),
      @ApiResponse(responseCode = "500", description = "Error deleting images")
  })
  @DeleteMapping("/by-project/{projectId}")
  public ResponseEntity<GenericResponse<?>> deleteAllImagesByProject(
      @Parameter(description = "Project ID") @PathVariable String projectId) {
    logger.info("Received request to delete all images for project ID: {}", projectId);
    try {
      ObjectId projectObjectId = new ObjectId(projectId); // Validate ObjectId format
      imageService.deleteAllImagesByProject(projectObjectId);
      return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "All images deleted successfully"));
    } catch (IllegalArgumentException e) {
      logger.error("Invalid project ID: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
    } catch (RuntimeException e) {
      logger.error("Error deleting images: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new GenericResponse<>("FAILURE", "Error deleting images: " + e.getMessage(), null));
    }
  }

  @Operation(summary = "Get image by ID and project ID")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Image retrieved successfully"),
      @ApiResponse(responseCode = "404", description = "Image not found"),
      @ApiResponse(responseCode = "400", description = "Invalid image or project ID"),
      @ApiResponse(responseCode = "500", description = "Error retrieving image")
  })
  @GetMapping("/{imageId}/project/{projectId}")
  public ResponseEntity<GenericResponse<ImageDTO>> getImageByImageIdAndProject(
      @Parameter(description = "Image ID") @PathVariable String imageId,
      @Parameter(description = "Project ID") @PathVariable String projectId) {
    logger.info("Received request to fetch image with ID: {} for project ID: {}", imageId, projectId);
    try {
      ObjectId projectObjectId = new ObjectId(projectId); // Validate ObjectId format
      Optional<ImageDTO> image = imageService.getImageByImageIdAndProject(imageId, projectObjectId);
      return image
          .map(value -> ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Image retrieved successfully", value)))
          .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
              .body(new GenericResponse<>("FAILURE",
                  "Image not found with ID: " + imageId + " in project: " + projectId, null)));
    } catch (IllegalArgumentException e) {
      logger.error("Invalid image or project ID: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
    } catch (RuntimeException e) {
      logger.error("Error retrieving image: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new GenericResponse<>("FAILURE", "Error retrieving image: " + e.getMessage(), null));
    }
  }

  @Operation(summary = "Delete image by ID and project ID")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Image deleted successfully"),
      @ApiResponse(responseCode = "404", description = "Image not found"),
      @ApiResponse(responseCode = "400", description = "Invalid image or project ID"),
      @ApiResponse(responseCode = "500", description = "Error deleting image")
  })
  @DeleteMapping("/{imageId}/project/{projectId}")
  public ResponseEntity<GenericResponse<?>> deleteImageByProject(
      @Parameter(description = "Image ID") @PathVariable String imageId,
      @Parameter(description = "Project ID") @PathVariable String projectId) {
    logger.info("Received request to delete image with ID: {} from project ID: {}", imageId, projectId);
    try {
      ObjectId projectObjectId = new ObjectId(projectId); // Validate ObjectId format
      imageService.deleteImageByProject(imageId, projectObjectId);
      return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Image deleted successfully"));
    } catch (IllegalArgumentException e) {
      logger.error("Image not found or invalid ID: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
    } catch (RuntimeException e) {
      logger.error("Error deleting image: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new GenericResponse<>("FAILURE", "Error deleting image: " + e.getMessage(), null));
    }
  }

  @Operation(summary = "Bulk delete images by IDs")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Images deleted successfully"),
      @ApiResponse(responseCode = "404", description = "One or more images not found"),
      @ApiResponse(responseCode = "400", description = "Invalid image IDs"),
      @ApiResponse(responseCode = "500", description = "Error deleting images")
  })
  @DeleteMapping("/bulk-delete")
  public ResponseEntity<GenericResponse<?>> bulkDeleteImages(@RequestBody List<String> imageIds) {
    logger.info("Received request to bulk delete images with IDs: {}", imageIds);
    try {
      imageService.bulkDeleteImages(imageIds);
      return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Images deleted successfully"));
    } catch (IllegalArgumentException e) {
      logger.error("Invalid image IDs or not found: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
    } catch (RuntimeException e) {
      logger.error("Error deleting images: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new GenericResponse<>("FAILURE", "Error deleting images: " + e.getMessage(), null));
    }
  }

  @Operation(summary = "Count images in a project")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Image count retrieved successfully"),
      @ApiResponse(responseCode = "404", description = "Project not found"),
      @ApiResponse(responseCode = "400", description = "Invalid project ID"),
      @ApiResponse(responseCode = "500", description = "Error counting images")
  })
  @GetMapping("/count/{projectId}")
  public ResponseEntity<GenericResponse<?>> countImagesByProject(
      @Parameter(description = "Project ID") @PathVariable String projectId) {
    logger.info("Received request to count images for project ID: {}", projectId);
    try {
      ObjectId projectObjectId = new ObjectId(projectId); // Validate ObjectId format
      long count = imageService.countImagesByProject(projectObjectId);
      return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Image count retrieved successfully", count));
    } catch (IllegalArgumentException e) {
      logger.error("Invalid project ID: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
    } catch (RuntimeException e) {
      logger.error("Error counting images: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new GenericResponse<>("FAILURE", "Error counting images: " + e.getMessage(), null));
    }
  }

  @Operation(summary = "Import images from another project")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Images imported successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid request data (IDs, project IDs)"),
      @ApiResponse(responseCode = "403", description = "User lacks permissions for source or target project"),
      @ApiResponse(responseCode = "404", description = "Source/Target project or source images not found"),
      @ApiResponse(responseCode = "500", description = "Error importing images")
  })
  @PostMapping("/import")
  public ResponseEntity<GenericResponse<?>> importImages(@RequestBody ImageImportRequest request) {
    // Get current user's email from security context
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(new GenericResponse<>("FAILURE", "User not authenticated", null));
    }
    String userEmail = authentication.getName(); // Assumes email is used as username

    logger.info("Received request to import images by user {}: {}", userEmail, request);

    try {
      List<ImageDTO> importedImages = imageService.importImagesFromProject(
          request.getSourceProjectId(),
          request.getTargetProjectId(),
          request.getImageIds(),
          userEmail);
      return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Images imported successfully", importedImages));
    } catch (IllegalArgumentException e) {
      logger.error("Invalid import request data: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
    } catch (ResourceNotFoundException
        | com.enit.satellite_platform.modules.project_management.exceptions.ProjectNotFoundException e) {
      // Catching specific not found exceptions from service
      logger.error("Resource not found during import: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
    } catch (AccessDeniedException e) {
      logger.warn("Access denied during import for user {}: {}", userEmail, e.getMessage());
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
    } catch (RuntimeException e) {
      logger.error("Error importing images: {}", e.getMessage(), e); // Log stack trace for runtime errors
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new GenericResponse<>("FAILURE", "Error importing images: " + e.getMessage(), null));
    }
  }
}
 */
