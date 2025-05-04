package com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.openCV.vegetation_Index_calculation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.enit.satellite_platform.config.cache_handler.VegetationIndexResultCacheHandler;
import com.enit.satellite_platform.config.cache_handler.util.FileHashingUtil;
import org.apache.commons.io.IOUtils;
import com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.openCV.vegetation_Index_calculation.dto.VegetationIndexRequest;
import com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.openCV.vegetation_Index_calculation.dto.VegetationIndexResult;
import com.enit.satellite_platform.modules.resource_management.utils.communication_management.CommunicationManager;
import com.enit.satellite_platform.modules.resource_management.utils.communication_management.MultipartResponseWrapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Example service demonstrating how to use the generic communication system
 * to interact with the Python vegetation index calculator.
 */
@Service
public class VegetationIndexService {
    private static final Logger logger = LoggerFactory.getLogger(VegetationIndexService.class);

    private final CommunicationManager communicationManager;
    private final VegetationIndexResultCacheHandler vegetationIndexCacheHandler;
    private final FileHashingUtil fileHashingUtil;

    public VegetationIndexService(
            CommunicationManager communicationManager,
            VegetationIndexResultCacheHandler vegetationIndexCacheHandler,
            FileHashingUtil fileHashingUtil) {
        this.communicationManager = communicationManager;
        this.vegetationIndexCacheHandler = vegetationIndexCacheHandler;
        this.fileHashingUtil = fileHashingUtil;
    }

    /**
     * Constructs an endpoint URL by appending additional path segments.
     *
     * @param additionalPath Optional path segments to append to the base URL.
     * @return A string representing the constructed endpoint URL.
     */
    private String getEndpointUrl(String... additionalPath) {
        // Construct the endpoint URL based on the index type and additional path
        StringBuilder url = new StringBuilder("");
        if (additionalPath != null && additionalPath.length > 0) {
            for (String path : additionalPath) {
                url.append("/").append(path);
            }
        }
        return url.toString();

    }

    /**
     * Calculate a vegetation index using the Python script.
     *
     * @param request   The vegetation index request parameters
     * @param imageFile The satellite image file
     * @param authToken Optional authentication token
     * @return The calculation result
     */
    public VegetationIndexResult calculateIndex(VegetationIndexRequest request, File imageFile, String authToken) {
        logger.info("Calculating {} index for image: {}", request.getIndexType(), imageFile.getName());

        // Set custom headers if needed
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Index-Type", request.getIndexType());
        headers.put("X-Path", getEndpointUrl("calculate", request.getIndexType()));
        headers.put("Accept", "application/json");

        try {
            // Calculate hash of the image file content using utility class
            String imageHash = fileHashingUtil.calculateFileHash(imageFile);

            // Create a composite key for caching using the hash and request parameters
            Map<String, Object> cacheKeyComponents = new HashMap<>();
            cacheKeyComponents.put("request", request);
            cacheKeyComponents.put("imageHash", imageHash);

            // 1. Check cache first
            Optional<VegetationIndexResult> cachedResult = vegetationIndexCacheHandler
                    .getResourceData(cacheKeyComponents);

            if (cachedResult.isPresent()) {
                logger.info("Cache hit for {}. Returning cached result for image: {}", request.getIndexType(),
                        imageFile.getName());
                return cachedResult.get();
            }

            // 2. Cache miss: Execute the original logic
            logger.info("Cache miss for {}. Calling external service for image: {}", request.getIndexType(),
                    imageFile.getName());
            MultipartResponseWrapper<VegetationIndexResult> responseWrapper = communicationManager.sendDataSync(
                    request, // Input type T (VegetationIndexRequest)
                    imageFile, // File to send
                    VegetationIndexResult.class, // Expected metadata type R
                    authToken, // Auth token
                    headers // Custom headers - includes path info
            );

            if (responseWrapper == null) {
                logger.error("Received null response wrapper from communication manager for index {}",
                        request.getIndexType());
                throw new RuntimeException("Communication failed: No response received");
            }

            VegetationIndexResult computedResult = responseWrapper.getMetadata();
            byte[] processedImageData;
            try (InputStream imageStream = responseWrapper.getFileContent()) {
                processedImageData = IOUtils.toByteArray(imageStream);
            } catch (IOException e) {
                logger.error("Error reading processed image data: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to read processed image data", e);
            }

            if (computedResult != null) {
                // Include the processed image data in the result
                computedResult.setProcessedImage(processedImageData);
                logger.info("Received metadata and image result for {} index calculation: {}", request.getIndexType(),
                        computedResult);
            } else {
                logger.warn("Received null metadata in response wrapper for index {}", request.getIndexType());
                throw new RuntimeException("No metadata received from processing service");
            }

            // 3. Store the complete result (including image) in cache before returning
            if (computedResult != null) {
                vegetationIndexCacheHandler.storeResourceData(computedResult, cacheKeyComponents); // Persist=false by
                                                                                                   // default
                logger.info("Stored computed result with image in cache for {}", request.getIndexType());
            }

            return computedResult; // Return the newly computed result
            // } // This closing brace was misplaced, removed it from here

        } catch (Exception e) {
            // Log the error with context
            logger.error("Error calculating {} index (cache check/computation) for image {}: {}",
                    request.getIndexType(), imageFile.getName(), e.getMessage(), e);
            // Consider specific exception handling or re-throwing based on requirements
            throw new RuntimeException("Error calculating vegetation index for " + imageFile.getName(), e);
        }
        // No finally block needed as temporary file handling is managed elsewhere
    } // Correct placement of the method's closing brace

    /**
     * Calculate NDVI index.
     *
     * @param imageFile The satellite image file
     * @param redBand   The red band number (default 1)
     * @param nirBand   The NIR band number (default 2)
     * @param authToken Optional authentication token
     * @return The NDVI calculation result
     */
    public VegetationIndexResult calculateNDVI(File imageFile, VegetationIndexRequest vegetationIndexRequest,
            String authToken) {
        vegetationIndexRequest.setIndexType("NDVI");
        if (imageFile == null || !imageFile.exists()) {
            throw new IllegalArgumentException("Image file must not be null and must exist.");
        }
        try {
            logger.info("Starting NDVI calculation for image: {}", imageFile.getName()); 
            VegetationIndexResult result = calculateIndex(vegetationIndexRequest, imageFile, authToken);
            logger.debug("NDVI calculation result: {}", result);
            if (result == null) {
                throw new RuntimeException("No result returned from NDVI calculation");
            }
            return result;
        } catch (Exception e) {
            logger.error("Error in NDVI calculation: {}", e.getMessage(), e);
            throw e;
        } finally {
            logger.info("Completed NDVI calculation for image: {}", imageFile.getName()); 
        }
    }

    /**
     * Calculate EVI index.
     *
     * @param imageFile The satellite image file
     * @param redBand   The red band number
     * @param nirBand   The NIR band number
     * @param blueBand  The blue band number
     * @param authToken Optional authentication token
     * @return The EVI calculation result
     */
    public VegetationIndexResult calculateEVI(
            File imageFile,
            VegetationIndexRequest vegetationIndexRequest,
            String authToken) {

        if (imageFile == null || !imageFile.exists()) {
            throw new IllegalArgumentException("Image file must not be null and must exist.");
        }
        if (vegetationIndexRequest == null) {
            throw new IllegalArgumentException("VegetationIndexRequest must not be null.");
        }
        vegetationIndexRequest.setIndexType("EVI"); 

        logger.info("Starting EVI calculation for image: {}", imageFile.getName()); 
        try {
            VegetationIndexResult result = calculateIndex(vegetationIndexRequest, imageFile, authToken);
            logger.debug("EVI calculation result: {}", result);
            if (result == null) {
                throw new RuntimeException("No result returned from EVI calculation");
            }
            return result;
        } catch (Exception e) {
            logger.error("Error in EVI calculation: {}", e.getMessage(), e);
            throw e;
        } finally {
            logger.info("Completed EVI calculation for image: {}", imageFile.getName()); 
        }
    }

    /**
     * Calculate Soil Adjusted Vegetation Index (SAVI)
     * 
     * @param imageFile The satellite image file
     * @param redBand   The red band number
     * @param nirBand   The NIR band number
     * @param L         Canopy background adjustment factor (typically 0.5)
     * @param authToken Optional authentication token
     * @return The SAVI calculation result
     */
    public VegetationIndexResult calculateSAVI(
            File imageFile,
            VegetationIndexRequest vegetationIndexRequest,
            String authToken) {
        if (imageFile == null || !imageFile.exists()) {
            throw new IllegalArgumentException("Image file must not be null and must exist.");
        }
        if (vegetationIndexRequest == null) {
            throw new IllegalArgumentException("VegetationIndexRequest must not be null.");
        }
        return calculateIndex(vegetationIndexRequest, imageFile, authToken);

    }

    /**
     * Calculate Normalized Difference Water Index (NDWI)
     * 
     * @param imageFile The satellite image file
     * @param greenBand The green band number
     * @param nirBand   The NIR band number
     * @param authToken Optional authentication token
     * @return The NDWI calculation result
     */
    public VegetationIndexResult calculateNDWI(
            File imageFile,
            int greenBand,
            int nirBand,
            String authToken) {
        VegetationIndexRequest request = new VegetationIndexRequest("NDWI", greenBand, nirBand);
        return calculateIndex(request, imageFile, authToken);
    }

}
