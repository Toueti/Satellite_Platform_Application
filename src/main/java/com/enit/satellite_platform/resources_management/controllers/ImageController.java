package com.enit.satellite_platform.resources_management.controllers;

import com.enit.satellite_platform.resources_management.dto.ImageDTO;
import com.enit.satellite_platform.resources_management.models.Image;
import com.enit.satellite_platform.resources_management.services.ImageService;
import com.enit.satellite_platform.exception.DuplicateNameException;
import com.enit.satellite_platform.project_management.dto.GenericResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/thematician/images")
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
  @PostMapping("/add")
  public ResponseEntity<GenericResponse<?>> addImage(@RequestBody ImageDTO imageDTO) {
    logger.info("Received request to add image: {}", imageDTO);
    try {
      ImageDTO addedImage = imageService.addImage(imageDTO);
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(new GenericResponse<>("SUCCESS", "Image added successfully", addedImage));
    } catch (IllegalArgumentException e) {
      logger.error("Invalid image data: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
    } catch (RuntimeException e) {
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
    } catch (DuplicateNameException e) {
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
}