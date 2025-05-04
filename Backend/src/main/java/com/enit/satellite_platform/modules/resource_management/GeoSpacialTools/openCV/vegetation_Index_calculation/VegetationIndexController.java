package com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.openCV.vegetation_Index_calculation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.openCV.vegetation_Index_calculation.dto.VegetationIndexRequest;
import com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.openCV.vegetation_Index_calculation.dto.VegetationIndexResult;
import com.enit.satellite_platform.shared.dto.GenericResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/vegetation-indices")
@Tag(name = "Vegetation Indices", description = "Endpoints for calculating vegetation indices from satellite images")
@SecurityRequirement(name = "bearerAuth")
public class VegetationIndexController {
    
    private static final Logger logger = LoggerFactory.getLogger(VegetationIndexController.class);
    private final VegetationIndexService vegetationIndexService;

    public VegetationIndexController(VegetationIndexService vegetationIndexService) {
        this.vegetationIndexService = vegetationIndexService;
    }

    @PostMapping(value = "/ndvi", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Calculate NDVI index", 
              description = "Calculate NDVI index from a satellite image")
    //@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<GenericResponse<VegetationIndexResult>> calculateNDVI(
            @RequestPart("metadata") VegetationIndexRequest vegetationIndexRequest,
            @RequestPart("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authToken) {
        
        try {
            // Convert MultipartFile to File
            File tempFile = File.createTempFile("upload_", "_" + file.getOriginalFilename());
            file.transferTo(tempFile);

            try {
                VegetationIndexResult result = vegetationIndexService.calculateNDVI(
                    tempFile,
                    vegetationIndexRequest,
                    authToken
                );

                return ResponseEntity.ok(new GenericResponse<>(
                    "SUCCESS",
                    "Successfully calculated NDVI",
                    result
                ));
            } finally {
                Files.deleteIfExists(tempFile.toPath());
            }
        } catch (IOException e) {
            logger.error("Error processing file upload", e);
            return ResponseEntity.badRequest().body(new GenericResponse<>(
                "ERROR",
                "Error processing file upload: " + e.getMessage(),
                null
            ));
        } catch (Exception e) {
            logger.error("Error calculating NDVI", e);
            return ResponseEntity.internalServerError().body(new GenericResponse<>(
                "ERROR",
                "Error calculating NDVI: " + e.getMessage(),
                null
            ));
        }
    }

    @PostMapping(value = "/evi", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Calculate EVI index", 
              description = "Calculate EVI index from a satellite image")
    //@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<GenericResponse<VegetationIndexResult>> calculateEVI(
            @RequestPart("metadata") VegetationIndexRequest vegetationIndexRequest,
            @RequestPart("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authToken) {
        
        try {
            // Convert MultipartFile to File
            File tempFile = File.createTempFile("upload_", "_" + file.getOriginalFilename());
            file.transferTo(tempFile);

            try {
                VegetationIndexResult result = vegetationIndexService.calculateEVI(
                    tempFile,
                    vegetationIndexRequest,
                    authToken
                );

                return ResponseEntity.ok(new GenericResponse<>(
                    "SUCCESS",
                    "Successfully calculated EVI",
                    result
                ));
            } finally {
                Files.deleteIfExists(tempFile.toPath());
            }
        } catch (IOException e) {
            logger.error("Error processing file upload", e);
            return ResponseEntity.badRequest().body(new GenericResponse<>(
                "ERROR",
                "Error processing file upload: " + e.getMessage(),
                null
            ));
        } catch (Exception e) {
            logger.error("Error calculating EVI", e);
            return ResponseEntity.internalServerError().body(new GenericResponse<>(
                "ERROR",
                "Error calculating EVI: " + e.getMessage(),
                null
            ));
        }
    }
}
