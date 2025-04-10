package com.enit.satellite_platform.modules.resource_management.image_management.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.bson.types.ObjectId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.enit.satellite_platform.modules.resource_management.image_management.dto.resultsSaveRequest;
import com.enit.satellite_platform.modules.resource_management.image_management.entities.ProcessingResults;
import com.enit.satellite_platform.modules.resource_management.image_management.services.ProcessingResultsService;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/geospatial/processing")
@Tag(name = "processing Controller", description = "Endpoints for interacting with Google Earth Engine")
public class ProcessingResultsController {

        @Autowired
        private ProcessingResultsService processingResultsService;

        @PostMapping("/save")
        @Operation(summary = "Save processing results", description = "Saves the results of a processing analysis")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Results saved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<ProcessingResults> saveProcessingResults(
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "processing results to save", required = true) @RequestBody resultsSaveRequest processingSaveRequest) {
                ProcessingResults savedResults = processingResultsService.save(processingSaveRequest);
                return new ResponseEntity<>(savedResults, HttpStatus.CREATED);
        }

        @GetMapping("/{id}")
        @Operation(summary = "Get processing results by ID", description = "Retrieves processing results by their ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Results retrieved successfully"),
                        @ApiResponse(responseCode = "404", description = "Results not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<ProcessingResults> getProcessingResultsById(
                        @Parameter(description = "processing results ID", required = true) @PathVariable String id) {
                ProcessingResults ProcessingResults = processingResultsService.getProcessingResultsById(new ObjectId(id));
                return ResponseEntity.ok(ProcessingResults);
        }

        @GetMapping("/image/{imageId}")
        @Operation(summary = "Get processing results by image ID", description = "Retrieves processing results associated with a specific image")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Results retrieved successfully"),
                        @ApiResponse(responseCode = "404", description = "Results not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<List<ProcessingResults>> getProcessingResultsByImageId(
                        @Parameter(description = "Image ID", required = true) @PathVariable String imageId) {
                List<ProcessingResults> ProcessingResults = processingResultsService.getProcessingResultsByImageId(imageId);
                return ResponseEntity.ok(ProcessingResults);
        }

        @GetMapping
        @Operation(summary = "Get all processing results", description = "Retrieves all processing results")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Results retrieved successfully"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<Page<ProcessingResults>> getAllProcessingResults(
                        @Parameter(description = "Pagination information", required = false) @PageableDefault(size = 10, sort = "id") Pageable pageable) {
                Page<ProcessingResults> ProcessingResults = processingResultsService.getAllProcessingResults(pageable);
                return ResponseEntity.ok(ProcessingResults);
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "Delete processing results by ID", description = "Deletes processing results by their ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Results deleted successfully"),
                        @ApiResponse(responseCode = "404", description = "Results not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<Void> deleteProcessingResultsById(
                        @Parameter(description = "processing results ID", required = true) @PathVariable String id) {
                processingResultsService.deleteProcessingResultsById(new ObjectId(id));
                return ResponseEntity.noContent().build();
        }

        @DeleteMapping("/image/{imageId}/{id}")
        @Operation(summary = "Delete processing results by image ID and results ID", description = "Deletes processing results associated with a specific image and results ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Results deleted successfully"),
                        @ApiResponse(responseCode = "404", description = "Results not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<Void> deleteProcessingResultsByImageId(
                        @Parameter(description = "Image ID", required = true) @PathVariable String imageId,
                        @Parameter(description = "processing results ID", required = true) @PathVariable String id) {
                processingResultsService.deleteByImage_ImageIdAndId(imageId, new ObjectId(id));
                return ResponseEntity.noContent().build();
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
        public ResponseEntity<ProcessingResults> updateProcessingResults(
                        @Parameter(description = "processing results ID", required = true) @PathVariable String id,
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated processing results details", required = true) @Valid @RequestBody resultsSaveRequest updateRequest) {
                ProcessingResults updatedResults = processingResultsService.updateProcessingResults(new ObjectId(id), updateRequest);
                return ResponseEntity.ok(updatedResults);
        }

        // NEW: Bulk Save processing Results
        @PostMapping("/bulk-save")
        @Operation(summary = "Bulk save processing results", description = "Saves multiple processing results in a single request")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Results saved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<List<ProcessingResults>> bulkSaveProcessingResults(
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "List of processing results to save", required = true) @Valid @RequestBody List<resultsSaveRequest> processingSaveRequests) {
                List<ProcessingResults> savedResults = processingResultsService.bulkSave(processingSaveRequests);
                return new ResponseEntity<>(savedResults, HttpStatus.CREATED);
        }

}
