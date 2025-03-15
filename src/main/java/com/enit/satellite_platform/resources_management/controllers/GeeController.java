package com.enit.satellite_platform.resources_management.controllers;

import com.enit.satellite_platform.resources_management.dto.*;
import com.enit.satellite_platform.resources_management.models.GeeResults;
import com.enit.satellite_platform.resources_management.services.GeeService;
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
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/api/thematician/gee")
@Tag(name = "GEE Controller", description = "Endpoints for interacting with Google Earth Engine")
public class GeeController {

        @Autowired
        private GeeService geeService;

        @PostMapping("/service")
        @Operation(summary = "Provide a service", description = "Provide a service based on the provided request")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Analysis performed successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<GeeResponse> performAnalysis(
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Analysis request details", required = true) @Valid @RequestBody GeeRequest requestDto) {
                GeeResponse response = geeService.performService(requestDto);
                return ResponseEntity.ok(response);
        }

       

        @PostMapping("/save")
        @Operation(summary = "Save GEE results", description = "Saves the results of a GEE analysis")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Results saved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<GeeResults> saveGeeResults(
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "GEE results to save", required = true) @RequestBody GeeSaveRequest geeSaveRequest) {
                GeeResults savedResults = geeService.save(geeSaveRequest);
                return new ResponseEntity<>(savedResults, HttpStatus.CREATED);
        }

        @GetMapping("/{id}")
        @Operation(summary = "Get GEE results by ID", description = "Retrieves GEE results by their ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Results retrieved successfully"),
                        @ApiResponse(responseCode = "404", description = "Results not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<GeeResults> getGeeResultsById(
                        @Parameter(description = "GEE results ID", required = true) @PathVariable String id) {
                GeeResults geeResults = geeService.getGeeResultsById(new ObjectId(id));
                return ResponseEntity.ok(geeResults);
        }

        @GetMapping("/image/{imageId}")
        @Operation(summary = "Get GEE results by image ID", description = "Retrieves GEE results associated with a specific image")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Results retrieved successfully"),
                        @ApiResponse(responseCode = "404", description = "Results not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<List<GeeResults>> getGeeResultsByImageId(
                        @Parameter(description = "Image ID", required = true) @PathVariable String imageId) {
                List<GeeResults> geeResults = geeService.getGeeResultsByImageId(imageId);
                return ResponseEntity.ok(geeResults);
        }

        @GetMapping
        @Operation(summary = "Get all GEE results", description = "Retrieves all GEE results")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Results retrieved successfully"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<Page<GeeResults>> getAllGeeResults(
                        @Parameter(description = "Pagination information", required = false) @PageableDefault(size = 10, sort = "id") Pageable pageable) {
                Page<GeeResults> geeResults = geeService.getAllGeeResults(pageable);
                return ResponseEntity.ok(geeResults);
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "Delete GEE results by ID", description = "Deletes GEE results by their ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Results deleted successfully"),
                        @ApiResponse(responseCode = "404", description = "Results not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<Void> deleteGeeResultsById(
                        @Parameter(description = "GEE results ID", required = true) @PathVariable String id) {
                geeService.deleteGeeResultsById(new ObjectId(id));
                return ResponseEntity.noContent().build();
        }

        @DeleteMapping("/image/{imageId}/{id}")
        @Operation(summary = "Delete GEE results by image ID and results ID", description = "Deletes GEE results associated with a specific image and results ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Results deleted successfully"),
                        @ApiResponse(responseCode = "404", description = "Results not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<Void> deleteGeeResultsByImageId(
                        @Parameter(description = "Image ID", required = true) @PathVariable String imageId,
                        @Parameter(description = "GEE results ID", required = true) @PathVariable String id) {
                geeService.deleteByImage_ImageIdAndId(imageId, new ObjectId(id));
                return ResponseEntity.noContent().build();
        }

        // NEW: Update GEE Results
        @PutMapping("/{id}")
        @Operation(summary = "Update GEE results", description = "Updates existing GEE results by their ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Results updated successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "404", description = "Results not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<GeeResults> updateGeeResults(
                        @Parameter(description = "GEE results ID", required = true) @PathVariable String id,
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated GEE results details", required = true) @Valid @RequestBody GeeSaveRequest updateRequest) {
                GeeResults updatedResults = geeService.updateGeeResults(new ObjectId(id), updateRequest);
                return ResponseEntity.ok(updatedResults);
        }

        // NEW: Bulk Save GEE Results
        @PostMapping("/bulk-save")
        @Operation(summary = "Bulk save GEE results", description = "Saves multiple GEE results in a single request")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Results saved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<List<GeeResults>> bulkSaveGeeResults(
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "List of GEE results to save", required = true) @Valid @RequestBody List<GeeSaveRequest> geeSaveRequests) {
                List<GeeResults> savedResults = geeService.bulkSave(geeSaveRequests);
                return new ResponseEntity<>(savedResults, HttpStatus.CREATED);
        }

        
}