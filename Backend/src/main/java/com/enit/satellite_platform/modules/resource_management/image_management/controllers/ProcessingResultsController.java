package com.enit.satellite_platform.modules.resource_management.image_management.controllers;

import java.io.IOException; // Import IOException
import java.util.List;
import java.util.Map; // Import Map

import org.bson.types.ObjectId;
import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType; // Import MediaType
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile; // Import MultipartFile

import com.enit.satellite_platform.modules.resource_management.image_management.dto.resultsSaveRequest;
import com.enit.satellite_platform.modules.resource_management.image_management.entities.ProcessingResults;
import com.enit.satellite_platform.modules.resource_management.image_management.services.ProcessingResultsService;
import com.enit.satellite_platform.shared.dto.GenericResponse;
import com.fasterxml.jackson.core.type.TypeReference; // Import TypeReference
import com.fasterxml.jackson.databind.ObjectMapper; // Import ObjectMapper

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/geospatial/processing")
@Tag(name = "Processing Results Controller", description = "Endpoints for managing processing results, including optional file uploads") // Updated
                                                                                                                                         // Tag
                                                                                                                                         // name/desc
public class ProcessingResultsController {

        private static final Logger logger = LoggerFactory.getLogger(ProcessingResultsController.class); // Add logger

        @Autowired
        private ProcessingResultsService processingResultsService;

        @PostMapping(value = "/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) // Consume multipart
        @Operation(summary = "Save processing results", description = "Saves the results of a processing analysis, optionally including a result file.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Results saved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request (invalid data or file issue)"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<GenericResponse<?>> saveProcessingResults(
                        @Parameter(description = "Processing results metadata (JSON)", required = true) @RequestPart("metadata") @Valid resultsSaveRequest processingSaveRequest,
                        @Parameter(description = "Optional result file to store") @RequestPart(value = "file", required = false) MultipartFile file) { // Use
                                                                                                                                                       // @RequestPart

                try {
                        // Set the file on the DTO if it's provided
                        if (file != null && !file.isEmpty()) {
                                processingSaveRequest.setFile(file);
                        }

                        ProcessingResults savedResults = processingResultsService.save(processingSaveRequest);
                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(new GenericResponse<>("SUCCESS", "Processing results saved successfully.",
                                                        savedResults)); // Updated message
                } catch (IllegalArgumentException e) {
                        logger.error("Invalid data for saving processing results: {}", e.getMessage(), e); // Improved
                                                                                                           // logging
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(new GenericResponse<>("FAILURE", "Invalid data: " + e.getMessage(),
                                                        null)); // More specific message
                } catch (Exception e) {
                        logger.error("Error saving processing results: {}", e.getMessage(), e); // Improved logging
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new GenericResponse<>("FAILURE",
                                                        "Error saving processing results: " + e.getMessage(), null)); // More
                                                                                                                      // specific
                                                                                                                      // message
                }
        }

        @GetMapping("/{id}")
        @Operation(summary = "Get processing results by ID", description = "Retrieves processing results by their ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Results retrieved successfully"),
                        @ApiResponse(responseCode = "404", description = "Results not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<GenericResponse<?>> getProcessingResultsById( // Return GenericResponse
                        @Parameter(description = "processing results ID", required = true) @PathVariable String id) {
                try {
                        ProcessingResults processingResults = processingResultsService
                                        .getProcessingResultsById(new ObjectId(id));
                        return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Results retrieved successfully.",
                                        processingResults));
                } catch (IllegalArgumentException e) { // Catch potential ObjectId format error or not found from
                                                       // service
                        logger.warn("Processing results not found or invalid ID format for ID {}: {}", id,
                                        e.getMessage());
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(new GenericResponse<>("FAILURE",
                                                        "Processing results not found or invalid ID.", null));
                } catch (Exception e) {
                        logger.error("Error retrieving processing results by ID {}: {}", id, e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new GenericResponse<>("FAILURE", "Error retrieving processing results.",
                                                        null));
                }
        }

        @GetMapping("/{id}/file")
        @Operation(summary = "Get processing result file by ID", description = "Downloads the file associated with a specific processing result ID.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
                        @ApiResponse(responseCode = "404", description = "Processing result or associated file not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error retrieving file")
        })
        public ResponseEntity<?> getProcessingResultFile( // Keep return type flexible (Object/byte[] for success,
                                                          // GenericResponse for error)
                        @Parameter(description = "Processing results ID", required = true) @PathVariable String id) {
                try {
                        MultipartFile fileData = processingResultsService.getProcessingResultFile(new ObjectId(id));

                        // Determine content type and filename
                        String contentTypeStr = fileData.getContentType();
                        MediaType contentType = contentTypeStr != null ? MediaType.parseMediaType(contentTypeStr)
                                        : MediaType.APPLICATION_OCTET_STREAM;
                        String filename = fileData.getOriginalFilename() != null ? fileData.getOriginalFilename()
                                        : id + "_result_file";

                        return ResponseEntity.ok()
                                        .contentType(contentType)
                                        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                                                        "attachment; filename=\"" + filename + "\"")
                                        .body(fileData.getBytes());

                } catch (IllegalArgumentException e) { // Catches ID format errors or "not found" from service
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(new GenericResponse<>("FAILURE",
                                                        "Processing result not found: " + e.getMessage(), null)); // Use
                                                                                                                  // GenericResponse
                } catch (RuntimeException e) { // Catches "no file associated" or storage retrieval errors
                        if (e.getMessage() != null && e.getMessage().contains("No stored file associated")) {
                                logger.warn("No file associated with processing result ID {}", id);
                                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                                .body(new GenericResponse<>("FAILURE", e.getMessage(), null)); // Use
                                                                                                               // GenericResponse
                        }
                        logger.error("Error retrieving processing result file for ID {}: {}", id, e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new GenericResponse<>("FAILURE",
                                                        "Error retrieving file: " + e.getMessage(), null)); // Use
                                                                                                            // GenericResponse
                } catch (IOException e) { // Catch getBytes() error
                        logger.error("Error reading file bytes for processing result ID {}: {}", id, e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new GenericResponse<>("FAILURE",
                                                        "Error reading file data: " + e.getMessage(), null)); // Use
                                                                                                              // GenericResponse
                }
        }

        @GetMapping("/image/{imageId}")
        @Operation(summary = "Get processing results by image ID", description = "Retrieves processing results associated with a specific image")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Results retrieved successfully"),
                        @ApiResponse(responseCode = "404", description = "Results not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<GenericResponse<?>> getProcessingResultsByImageId( // Return GenericResponse
                        @Parameter(description = "Image ID", required = true) @PathVariable String imageId) {
                try {
                        List<ProcessingResults> processingResults = processingResultsService
                                        .getProcessingResultsByImageId(imageId);
                        // Consider if empty list should be 404 or 200 with empty data
                        if (processingResults.isEmpty()) {
                                logger.info("No processing results found for image ID {}", imageId);
                                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(new GenericResponse<>("FAILURE", "No processing results found for this image.", null));
                        }
                        return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Results retrieved successfully.",
                                        processingResults));
                } catch (Exception e) {
                        logger.error("Error retrieving processing results for image ID {}: {}", imageId, e.getMessage(),
                                        e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new GenericResponse<>("FAILURE", "Error retrieving processing results.",
                                                        null));
                }
        }

        @GetMapping("/project/{projectId}")
        @Operation(summary = "Get processing results by project ID", description = "Retrieves all non-deleted processing results associated with a specific project ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Results retrieved successfully"),
                        @ApiResponse(responseCode = "404", description = "No results found for this project"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<GenericResponse<List<resultsSaveRequest>>> getResultsByProjectId(
                        @Parameter(description = "Project ID", required = true) @PathVariable String projectId) {
                try {
                        List<resultsSaveRequest> results = processingResultsService.getResultsByProjectId(projectId);
                        if (results.isEmpty()) {
                                logger.info("No processing results found for project ID {}", projectId);
                                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(new GenericResponse<>("FAILURE", "No processing results found for this project.", null));
                        }
                        return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Results retrieved successfully.", results));
                } catch (IllegalArgumentException e) { // Catch potential validation errors from service (e.g., invalid ID format if validation added there)
                        logger.warn("Invalid request for project ID {}: {}", projectId, e.getMessage());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(new GenericResponse<>("FAILURE", "Invalid project ID format or request.", null));
                } catch (Exception e) {
                        logger.error("Error retrieving processing results for project ID {}: {}", projectId, e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new GenericResponse<>("FAILURE", "Error retrieving processing results.", null));
                }
        }


        @GetMapping
        @Operation(summary = "Get all processing results", description = "Retrieves all processing results")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Results retrieved successfully"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<GenericResponse<?>> getAllProcessingResults( // Return GenericResponse
                        @Parameter(description = "Pagination information", required = false) @PageableDefault(size = 10, sort = "id") Pageable pageable) {
                try {
                        Page<ProcessingResults> processingResultsPage = processingResultsService
                                        .getAllProcessingResults(pageable);
                        return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Results retrieved successfully.",
                                        processingResultsPage));
                } catch (Exception e) {
                        logger.error("Error retrieving all processing results: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new GenericResponse<>("FAILURE", "Error retrieving processing results.",
                                                        null));
                }
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "Soft delete processing results by ID", description = "Marks processing results as deleted by their ID. Does not delete associated files.") // Updated
                                                                                                                                                                         // description
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Results soft deleted successfully"), // Use
                                                                                                               // 200 OK
                        @ApiResponse(responseCode = "404", description = "Results not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<GenericResponse<?>> deleteProcessingResultsById( // Return GenericResponse
                        @Parameter(description = "processing results ID", required = true) @PathVariable String id) {
                try {
                        processingResultsService.deleteProcessingResultsById(new ObjectId(id));
                        return ResponseEntity.ok(
                                        new GenericResponse<>("SUCCESS", "Results soft deleted successfully.", null)); // Use
                                                                                                                       // 200
                                                                                                                       // OK
                                                                                                                       // with
                                                                                                                       // message
                } catch (IllegalArgumentException e) { // Catches ID format errors or "not found" from service
                        logger.warn("Processing results not found or invalid ID format for soft delete: ID {}", id, e);
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(new GenericResponse<>("FAILURE",
                                                        "Processing results not found or invalid ID.", null));
                } catch (Exception e) {
                        logger.error("Error soft deleting processing results by ID {}: {}", id, e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new GenericResponse<>("FAILURE",
                                                        "Error soft deleting processing results.", null));
                }
        }

        @DeleteMapping("/image/{imageId}/{id}")
        @Operation(summary = "Soft delete processing results by image ID and results ID", description = "Marks processing results associated with a specific image and results ID as deleted. Does not delete associated files.") // Updated
                                                                                                                                                                                                                                  // description
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Results soft deleted successfully"), // Use
                                                                                                               // 200 OK
                        @ApiResponse(responseCode = "404", description = "Results not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<GenericResponse<?>> deleteProcessingResultsByImageId( // Return GenericResponse
                        @Parameter(description = "Image ID", required = true) @PathVariable String imageId,
                        @Parameter(description = "processing results ID", required = true) @PathVariable String id) {
                try {
                        processingResultsService.deleteByImage_ImageIdAndId(imageId, new ObjectId(id));
                        return ResponseEntity.ok(
                                        new GenericResponse<>("SUCCESS", "Results soft deleted successfully.", null)); // Use
                                                                                                                       // 200
                                                                                                                       // OK
                                                                                                                       // with
                                                                                                                       // message
                } catch (IllegalArgumentException e) { // Catches ID format errors or "not found" from service
                        logger.warn("Processing results not found or invalid ID format for soft delete: ImageID {}, ResultID {}",
                                        imageId, id, e);
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(new GenericResponse<>("FAILURE",
                                                        "Processing results not found or invalid ID.", null));
                } catch (Exception e) {
                        logger.error("Error soft deleting processing results by ImageID {} and ResultID {}: {}",
                                        imageId, id, e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new GenericResponse<>("FAILURE",
                                                        "Error soft deleting processing results.", null));
                }
        }

        // NEW: Update processing Results
        @PutMapping("/{id}")
        @Operation(summary = "Update processing results", description = "Updates existing processing results by their ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Results updated successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "404", description = "Results not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<GenericResponse<?>> updateProcessingResults( // Return GenericResponse
                        @Parameter(description = "processing results ID", required = true) @PathVariable String id,
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated processing results details", required = true) @Valid @RequestBody resultsSaveRequest updateRequest) {
                try {
                        ProcessingResults updatedResults = processingResultsService.updateProcessingResults(
                                        new ObjectId(id),
                                        updateRequest);
                        return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Results updated successfully.",
                                        updatedResults));
                } catch (IllegalArgumentException e) { // Catches ID format errors or "not found" from service
                        logger.warn("Processing results not found or invalid ID format for update: ID {}", id, e);
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(new GenericResponse<>("FAILURE",
                                                        "Processing results not found or invalid ID.", null));
                } catch (Exception e) { // Catch other potential errors like validation within service
                        logger.error("Error updating processing results for ID {}: {}", id, e.getMessage(), e);
                        // Consider returning 400 Bad Request for certain types of exceptions if
                        // applicable
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new GenericResponse<>("FAILURE",
                                                        "Error updating processing results: " + e.getMessage(), null));
                }
        }

        // Updated: Bulk Save processing Results with optional files
        @PostMapping(value = "/bulk-save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @Operation(summary = "Bulk save processing results", description = "Saves multiple processing results, optionally including associated files. Files should be named 'file_0', 'file_1', etc., corresponding to the index of the metadata object in the 'metadataList' part.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Results saved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request (invalid JSON, file naming mismatch, etc.)"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<GenericResponse<?>> bulkSaveProcessingResults( // Return GenericResponse
                        @Parameter(description = "JSON array of processing results metadata", required = true) @RequestPart("metadataList") String metadataListJson,
                        @Parameter(description = "Map of files keyed by 'file_index' (e.g., 'file_0', 'file_1')") @RequestPart(required = false) Map<String, MultipartFile> files) {

                ObjectMapper mapper = new ObjectMapper();
                List<resultsSaveRequest> processingSaveRequests;
                try {
                        // Deserialize the JSON list of metadata
                        processingSaveRequests = mapper.readValue(metadataListJson,
                                        new TypeReference<List<resultsSaveRequest>>() {
                                        });
                } catch (IOException e) { // Catch JSON parsing errors
                        logger.error("Invalid JSON format for metadataList: {}", e.getMessage(), e);
                        return ResponseEntity.badRequest()
                                        .body(new GenericResponse<>("FAILURE",
                                                        "Invalid format for metadataList JSON: " + e.getMessage(),
                                                        null));
                }

                // Basic validation: Ensure DTO list is not empty
                if (processingSaveRequests == null || processingSaveRequests.isEmpty()) {
                        logger.warn("Attempted bulk save with empty metadataList.");
                        return ResponseEntity.badRequest()
                                        .body(new GenericResponse<>("FAILURE", "metadataList cannot be empty.", null));
                }

                // Pass both the DTO list and the file map to the service
                try {
                        List<ProcessingResults> savedResults = processingResultsService.bulkSave(processingSaveRequests,
                                        files != null ? files : Map.of()); // Pass empty map if no files part
                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(new GenericResponse<>("SUCCESS", "Bulk results saved successfully.",
                                                        savedResults));
                } catch (IllegalArgumentException e) { // Catch validation/argument errors from service
                        logger.error("Error during bulk save (Bad Request): {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(new GenericResponse<>("FAILURE",
                                                        "Error during bulk save: " + e.getMessage(), null));
                } catch (Exception e) { // Catch other runtime exceptions (including potentially wrapped IOExceptions
                                        // from service)
                        logger.error("Internal server error during bulk save: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new GenericResponse<>("FAILURE",
                                                        "Internal server error during bulk save: " + e.getMessage(),
                                                        null));
                }
        }

        @PostMapping("/{id}/restore")
        @Operation(summary = "Restore soft-deleted processing results", description = "Restores a processing result record that was previously soft-deleted.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Results restored successfully"),
                        @ApiResponse(responseCode = "404", description = "Results not found"),
                        @ApiResponse(responseCode = "400", description = "Results not deleted or other error"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<GenericResponse<?>> restoreProcessingResults( // Return GenericResponse
                        @Parameter(description = "ID of the processing results to restore", required = true) @PathVariable String id) {
                try {
                        ProcessingResults restoredResult = processingResultsService
                                        .restoreProcessingResults(new ObjectId(id));
                        return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Results restored successfully.",
                                        restoredResult));
                } catch (IllegalArgumentException e) { // Catches ID format errors or "not found"
                        logger.warn("Processing results not found or invalid ID format for restore: ID {}", id, e);
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(new GenericResponse<>("FAILURE",
                                                        "Processing result not found: " + e.getMessage(), null));
                } catch (IllegalStateException e) { // Catches "not deleted" error
                        logger.warn("Attempted to restore processing result that was not deleted: ID {}", id, e);
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
                } catch (Exception e) {
                        logger.error("Error restoring processing result with ID {}: {}", id, e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new GenericResponse<>("FAILURE",
                                                        "Error restoring result: " + e.getMessage(), null));
                }
        }

        @DeleteMapping("/{id}/force")
        @Operation(summary = "Force permanent deletion of processing results", description = "Permanently deletes a processing result record and its associated file (if any), bypassing soft-delete.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Results permanently deleted successfully"), // Use
                                                                                                                      // 200
                                                                                                                      // OK
                        @ApiResponse(responseCode = "404", description = "Results not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error during deletion")
        })
        public ResponseEntity<GenericResponse<?>> forceDeleteProcessingResults( // Return GenericResponse
                        @Parameter(description = "ID of the processing results to force delete", required = true) @PathVariable String id) {
                try {
                        processingResultsService.forceDeleteProcessingResults(new ObjectId(id));
                        return ResponseEntity.ok(new GenericResponse<>("SUCCESS",
                                        "Results permanently deleted successfully.", null)); // Use 200 OK
                } catch (IllegalArgumentException e) { // Catches ID format errors or "not found"
                        logger.warn("Processing results not found or invalid ID format for force delete: ID {}", id, e);
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(new GenericResponse<>("FAILURE",
                                                        "Processing results not found or invalid ID.", null));
                } catch (Exception e) {
                        logger.error("Error force deleting processing result with ID {}: {}", id, e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new GenericResponse<>("FAILURE", "Error during permanent deletion.",
                                                        null));
                }
        }

        @GetMapping("/deleted")
        @Operation(summary = "Get soft-deleted processing results", description = "Retrieves a paginated list of processing results that have been soft-deleted.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Deleted results retrieved successfully"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<GenericResponse<?>> getDeletedProcessingResults( // Return GenericResponse
                        @Parameter(description = "Pagination information", required = false) @PageableDefault(size = 10, sort = "deletedAt") Pageable pageable) {
                try {
                        Page<ProcessingResults> deletedResults = processingResultsService
                                        .getDeletedProcessingResults(pageable);
                        return ResponseEntity.ok(new GenericResponse<>("SUCCESS",
                                        "Deleted results retrieved successfully.", deletedResults));
                } catch (Exception e) {
                        logger.error("Error retrieving soft-deleted processing results: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new GenericResponse<>("FAILURE", "Error retrieving soft-deleted results.",
                                                        null));
                }
        }
}
