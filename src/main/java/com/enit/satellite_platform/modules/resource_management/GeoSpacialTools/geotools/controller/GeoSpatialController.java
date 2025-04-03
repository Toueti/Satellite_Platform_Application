package com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.geotools.controller;

import org.geotools.coverage.grid.GridCoverage2D;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.geotools.config.SensorCalibration;
import com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.geotools.service.GeoSpatialProcessingService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.gce.geotiff.GeoTiffWriteParams;


@RestController
@RequestMapping("/geospatial/geotools")
public class GeoSpatialController {

    @Autowired
    private final GeoSpatialProcessingService processingService;
    private final Path rootLocation = Paths.get("upload-dir");

    public GeoSpatialController(GeoSpatialProcessingService processingService) {
        this.processingService = processingService;
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    /**
     * Upload a GeoTIFF file
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> handleFileUpload(@RequestParam("file") MultipartFile file) {
        try {
            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Files.copy(file.getInputStream(), this.rootLocation.resolve(filename));
            
            Map<String, String> response = new HashMap<>();
            response.put("filename", filename);
            response.put("message", "File uploaded successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        }
    }

    /**
     * Calculate NDVI from uploaded files
     */
    @PostMapping("/ndvi")
    public ResponseEntity<byte[]> calculateNDVI(
            @RequestParam("redBand") String redBandFilename,
            @RequestParam("nirBand") String nirBandFilename) throws IOException {
        
        File redFile = rootLocation.resolve(redBandFilename).toFile();
        File nirFile = rootLocation.resolve(nirBandFilename).toFile();
        
        GridCoverage2D redBand = processingService.loadGeoTIFF(redFile.getAbsolutePath());
        GridCoverage2D nirBand = processingService.loadGeoTIFF(nirFile.getAbsolutePath());
        
        GridCoverage2D ndvi = processingService.calculateNDVI(redBand, nirBand);
        
        return createImageResponse(ndvi, "ndvi_result.tif");
    }

    /**
     * Calculate EVI from uploaded files
     */
    @PostMapping("/evi")
    public ResponseEntity<byte[]> calculateEVI(
            @RequestParam("redBand") String redBandFilename,
            @RequestParam("blueBand") String blueBandFilename,
            @RequestParam("nirBand") String nirBandFilename) throws IOException {
        
        File redFile = rootLocation.resolve(redBandFilename).toFile();
        File blueFile = rootLocation.resolve(blueBandFilename).toFile();
        File nirFile = rootLocation.resolve(nirBandFilename).toFile();
        
        GridCoverage2D redBand = processingService.loadGeoTIFF(redFile.getAbsolutePath());
        GridCoverage2D blueBand = processingService.loadGeoTIFF(blueFile.getAbsolutePath());
        GridCoverage2D nirBand = processingService.loadGeoTIFF(nirFile.getAbsolutePath());
        
        GridCoverage2D evi = processingService.calculateEVI(redBand, blueBand, nirBand);
        
        return createImageResponse(evi, "evi_result.tif");
    }

    /**
     * Advanced water detection
     */
    @PostMapping("/water-detection")
    public ResponseEntity<byte[]> detectWater(
            @RequestParam("blueBand") String blueBandFilename,
            @RequestParam("greenBand") String greenBandFilename,
            @RequestParam("redBand") String redBandFilename,
            @RequestParam("nirBand") String nirBandFilename,
            @RequestParam("swirBand") String swirBandFilename) throws IOException {
        
        GridCoverage2D blueBand = loadCoverage(blueBandFilename);
        GridCoverage2D greenBand = loadCoverage(greenBandFilename);
        GridCoverage2D redBand = loadCoverage(redBandFilename);
        GridCoverage2D nirBand = loadCoverage(nirBandFilename);
        GridCoverage2D swirBand = loadCoverage(swirBandFilename);
        
        GridCoverage2D waterMask = processingService.advancedWaterDetection(
            blueBand, greenBand, redBand, nirBand, swirBand);
        
        return createImageResponse(waterMask, "water_mask.tif");
    }

    /**
     * Advanced building detection
     */
    @PostMapping("/building-detection")
    public ResponseEntity<byte[]> detectBuildings(
            @RequestParam("panBand") String panBandFilename,
            @RequestParam("redBand") String redBandFilename,
            @RequestParam("nirBand") String nirBandFilename) throws IOException {
        
        GridCoverage2D panBand = loadCoverage(panBandFilename);
        GridCoverage2D redBand = loadCoverage(redBandFilename);
        GridCoverage2D nirBand = loadCoverage(nirBandFilename);
        
        // First calculate NDVI needed for building detection
        GridCoverage2D ndvi = processingService.calculateNDVI(redBand, nirBand);
        
        GridCoverage2D buildings = processingService.advancedBuildingDetection(panBand, redBand, nirBand, ndvi);
        
        return createImageResponse(buildings, "buildings.tif");
    }

    /**
     * Land cover classification
     */
    @PostMapping("/land-cover")
    public ResponseEntity<byte[]> classifyLandCover(
            @RequestParam("sensor") String sensorName,
            @RequestParam("band1") String band1Filename,
            @RequestParam("band2") String band2Filename,
            @RequestParam("band3") String band3Filename,
            @RequestParam("band4") String band4Filename,
            @RequestParam("band5") String band5Filename,
            @RequestParam("band6") String band6Filename,
            @RequestParam("band7") String band7Filename) throws IOException {
        
        GridCoverage2D[] bands = new GridCoverage2D[7];
        bands[0] = loadCoverage(band1Filename);
        bands[1] = loadCoverage(band2Filename);
        bands[2] = loadCoverage(band3Filename);
        bands[3] = loadCoverage(band4Filename);
        bands[4] = loadCoverage(band5Filename);
        bands[5] = loadCoverage(band6Filename);
        bands[6] = loadCoverage(band7Filename);
        
        GridCoverage2D landCover = processingService.classifyLandCover(bands, sensorName);
        
        return createImageResponse(landCover, "land_cover.tif");
    }

    /**
     * Configure sensor calibration
     */
    @PostMapping("/calibrate")
    public ResponseEntity<String> calibrateSensor(
            @RequestParam("sensor") String sensorName,
            @RequestBody SensorCalibration calibration) {
        
        processingService.calibrateSensor(sensorName, calibration);
        return ResponseEntity.ok("Calibration for " + sensorName + " updated successfully");
    }

    /**
     * Get sensor calibration
     */
    @GetMapping("/calibration/{sensorName}")
    public ResponseEntity<SensorCalibration> getCalibration(
            @PathVariable String sensorName) {
        
        SensorCalibration calibration = processingService.getSensorCalibration(sensorName);
        if (calibration == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(calibration);
    }

    // Helper method to load coverage from filename
    private GridCoverage2D loadCoverage(String filename) throws IOException {
        File file = rootLocation.resolve(filename).toFile();
        return processingService.loadGeoTIFF(file.getAbsolutePath());
    }

    // Helper method to create image response
    private ResponseEntity<byte[]> createImageResponse(GridCoverage2D coverage, String filename) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GeoTiffWriter writer = null;
        try {
            GeoTiffWriteParams writeParams = new GeoTiffWriteParams();
            writeParams.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
            writeParams.setCompressionType("LZW"); // Example compression

            // The write parameters are often configured internally or passed differently.
            // We'll pass null here as getParameters() is not available.
            writer = new GeoTiffWriter(baos);
            writer.write(coverage, null); // Pass null for parameters

            byte[] imageBytes = baos.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("image/tiff")); // Set correct MIME type
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(imageBytes.length);

            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);

        } finally {
            if (writer != null) {
                try {
                    writer.dispose(); // Clean up writer resources
                } catch (Exception e) {
                    // Log or handle disposal exception if necessary
                }
            }
            baos.close(); // Close the stream
        }
    }
}
