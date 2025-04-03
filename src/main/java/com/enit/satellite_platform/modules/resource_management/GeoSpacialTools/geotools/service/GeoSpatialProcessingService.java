package com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.geotools.service;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.raster.ConvolveCoverageProcess;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.stereotype.Service;

import com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.geotools.config.SensorCalibration;

import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.media.jai.KernelJAI;

@Service
public class GeoSpatialProcessingService {
    
    private Map<String, SensorCalibration> sensorCalibrations = new HashMap<>();
    private Map<String, Map<String, double[]>> spectralLibraries = new HashMap<>();
    
    /**
     * Initialize spectral libraries for different land cover types
     */
    public GeoSpatialProcessingService() {
        initializeSpectralLibraries();
    }
    
    private void initializeSpectralLibraries() {
        // Standard spectral signatures for common land cover types
        Map<String, double[]> landsat8Signatures = new HashMap<>();
        landsat8Signatures.put("water", new double[]{0.02, 0.03, 0.05, 0.02, 0.01, 0.01, 0.01});
        landsat8Signatures.put("vegetation", new double[]{0.05, 0.07, 0.25, 0.30, 0.45, 0.30, 0.15});
        landsat8Signatures.put("urban", new double[]{0.25, 0.30, 0.35, 0.40, 0.35, 0.30, 0.25});
        landsat8Signatures.put("soil", new double[]{0.25, 0.30, 0.35, 0.40, 0.35, 0.30, 0.25});
        
        spectralLibraries.put("Landsat8", landsat8Signatures);
        
        Map<String, double[]> sentinel2Signatures = new HashMap<>();
        sentinel2Signatures.put("water", new double[]{0.02, 0.03, 0.04, 0.03, 0.02, 0.01, 0.01, 0.01, 0.01});
        sentinel2Signatures.put("vegetation", new double[]{0.05, 0.07, 0.25, 0.30, 0.45, 0.50, 0.40, 0.30, 0.20});
        sentinel2Signatures.put("urban", new double[]{0.30, 0.35, 0.40, 0.45, 0.40, 0.35, 0.30, 0.25, 0.20});
        
        spectralLibraries.put("Sentinel2", sentinel2Signatures);
    }

    /**
     * Calculate Normalized Difference Vegetation Index (NDVI)
     * 
     * @param redBand Red band raster
     * @param nirBand Near-Infrared band raster
     * @return NDVI raster
     */
    public GridCoverage2D calculateNDVI(GridCoverage2D redBand, GridCoverage2D nirBand) {
        Raster redRaster = redBand.getRenderedImage().getData();
        Raster nirRaster = nirBand.getRenderedImage().getData();
        
        int width = redRaster.getWidth();
        int height = redRaster.getHeight();
        
        WritableRaster ndviRaster = redRaster.createCompatibleWritableRaster();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double redValue = redRaster.getSampleDouble(x, y, 0);
                double nirValue = nirRaster.getSampleDouble(x, y, 0);
                
                // NDVI calculation: (NIR - Red) / (NIR + Red)
                double ndvi = (nirValue - redValue) / (nirValue + redValue);
                ndviRaster.setSample(x, y, 0, ndvi);
            }
        }
        
        return createGridCoverage(ndviRaster, "NDVI", redBand.getCoordinateReferenceSystem());
    }
    
    /**
     * Calculate Enhanced Vegetation Index (EVI)
     * 
     * @param redBand Red band raster
     * @param blueBand Blue band raster
     * @param nirBand Near-Infrared band raster
     * @return EVI raster
     */
    public GridCoverage2D calculateEVI(GridCoverage2D redBand, GridCoverage2D blueBand, GridCoverage2D nirBand) {
        // EVI constants
        final double G = 2.5;  // Gain factor
        final double C1 = 6.0; // Coefficient 1 for aerosol resistance
        final double C2 = 7.5; // Coefficient 2 for aerosol resistance
        final double L = 1.0;  // Canopy background adjustment
        
        Raster redRaster = redBand.getRenderedImage().getData();
        Raster blueRaster = blueBand.getRenderedImage().getData();
        Raster nirRaster = nirBand.getRenderedImage().getData();
        
        int width = redRaster.getWidth();
        int height = redRaster.getHeight();
        
        WritableRaster eviRaster = redRaster.createCompatibleWritableRaster();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double redValue = redRaster.getSampleDouble(x, y, 0);
                double blueValue = blueRaster.getSampleDouble(x, y, 0);
                double nirValue = nirRaster.getSampleDouble(x, y, 0);
                
                // EVI calculation
                double evi = G * ((nirValue - redValue) / 
                    (nirValue + C1 * redValue - C2 * blueValue + L));
                
                eviRaster.setSample(x, y, 0, evi);
            }
        }
        
        return createGridCoverage(eviRaster, "EVI", redBand.getCoordinateReferenceSystem());
    }
    
    /**
     * Calculate Soil Adjusted Vegetation Index (SAVI)
     * 
     * @param redBand Red band raster
     * @param nirBand Near-Infrared band raster
     * @param L Canopy background adjustment factor (typically 0.5)
     * @return SAVI raster
     */
    public GridCoverage2D calculateSAVI(GridCoverage2D redBand, GridCoverage2D nirBand, double L) {
        Raster redRaster = redBand.getRenderedImage().getData();
        Raster nirRaster = nirBand.getRenderedImage().getData();
        
        int width = redRaster.getWidth();
        int height = redRaster.getHeight();
        
        WritableRaster saviRaster = redRaster.createCompatibleWritableRaster();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double redValue = redRaster.getSampleDouble(x, y, 0);
                double nirValue = nirRaster.getSampleDouble(x, y, 0);
                
                // SAVI calculation
                double savi = ((nirValue - redValue) / (nirValue + redValue + L)) * (1 + L);
                
                saviRaster.setSample(x, y, 0, savi);
            }
        }
        
        return createGridCoverage(saviRaster, "SAVI", redBand.getCoordinateReferenceSystem());
    }
    
    /**
     * Perform image differencing with edge detection enhancement
     * 
     * @param image1 First image
     * @param image2 Second image
     * @return Difference image with edge enhancement
     */
    public GridCoverage2D enhancedDifferenceImages(GridCoverage2D image1, GridCoverage2D image2) {
        // Ensure images are of the same size
        if (image1.getRenderedImage().getWidth() != image2.getRenderedImage().getWidth() ||
            image1.getRenderedImage().getHeight() != image2.getRenderedImage().getHeight()) {
            throw new IllegalArgumentException("Images must be of the same dimensions");
        }
        
        Raster raster1 = image1.getRenderedImage().getData();
        Raster raster2 = image2.getRenderedImage().getData();
        
        int width = raster1.getWidth();
        int height = raster1.getHeight();
        
        WritableRaster diffRaster = raster1.createCompatibleWritableRaster();
        
        // First pass: simple difference
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double value1 = raster1.getSampleDouble(x, y, 0);
                double value2 = raster2.getSampleDouble(x, y, 0);
                
                diffRaster.setSample(x, y, 0, Math.abs(value1 - value2));
            }
        }
        
        // Second pass: edge enhancement
        GridCoverage2D diffCoverage = createGridCoverage(diffRaster, "TempDiff", image1.getCoordinateReferenceSystem());
        GridCoverage2D edgeEnhanced = applySobelEdgeDetection(diffCoverage);
        
        return edgeEnhanced;
    }
    
    /**
     * Advanced water detection using multiple spectral indices
     * 
     * @param blueBand Blue band
     * @param greenBand Green band
     * @param redBand Red band
     * @param nirBand Near-Infrared band
     * @param swirBand Short-Wave Infrared band
     * @return Water mask raster with confidence levels (0-1)
     */
    public GridCoverage2D advancedWaterDetection(GridCoverage2D blueBand, GridCoverage2D greenBand, 
                                               GridCoverage2D redBand, GridCoverage2D nirBand, 
                                               GridCoverage2D swirBand) {
        // Calculate multiple water indices
        GridCoverage2D ndwi = calculateNDWI(greenBand, nirBand);
        GridCoverage2D mndwi = calculateMNDWI(greenBand, swirBand);
        GridCoverage2D awei = calculateAWEI(blueBand, greenBand, nirBand, swirBand);
        
        Raster ndwiRaster = ndwi.getRenderedImage().getData();
        Raster mndwiRaster = mndwi.getRenderedImage().getData();
        Raster aweiRaster = awei.getRenderedImage().getData();
        
        int width = ndwiRaster.getWidth();
        int height = ndwiRaster.getHeight();
        
        WritableRaster waterConfidence = ndwiRaster.createCompatibleWritableRaster();
        
        // Combine indices with weighted confidence
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double ndwiVal = ndwiRaster.getSampleDouble(x, y, 0);
                double mndwiVal = mndwiRaster.getSampleDouble(x, y, 0);
                double aweiVal = aweiRaster.getSampleDouble(x, y, 0);
                
                // Apply thresholds and weights
                double confidence = 0;
                if (ndwiVal > 0.1) confidence += 0.4;
                if (mndwiVal > 0.2) confidence += 0.3;
                if (aweiVal > 0.1) confidence += 0.3;
                
                // Normalize to 0-1 range
                confidence = Math.min(1.0, Math.max(0.0, confidence));
                
                waterConfidence.setSample(x, y, 0, confidence);
            }
        }
        
        return createGridCoverage(waterConfidence, "WaterConfidence", blueBand.getCoordinateReferenceSystem());
    }
    
    /**
     * Calculate Normalized Difference Water Index (NDWI)
     */
    private GridCoverage2D calculateNDWI(GridCoverage2D greenBand, GridCoverage2D nirBand) {
        Raster greenRaster = greenBand.getRenderedImage().getData();
        Raster nirRaster = nirBand.getRenderedImage().getData();
        
        int width = greenRaster.getWidth();
        int height = greenRaster.getHeight();
        
        WritableRaster ndwiRaster = greenRaster.createCompatibleWritableRaster();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double greenValue = greenRaster.getSampleDouble(x, y, 0);
                double nirValue = nirRaster.getSampleDouble(x, y, 0);
                
                double ndwi = (greenValue - nirValue) / (greenValue + nirValue);
                ndwiRaster.setSample(x, y, 0, ndwi);
            }
        }
        
        return createGridCoverage(ndwiRaster, "NDWI", greenBand.getCoordinateReferenceSystem());
    }
    
    /**
     * Calculate Modified Normalized Difference Water Index (MNDWI)
     */
    private GridCoverage2D calculateMNDWI(GridCoverage2D greenBand, GridCoverage2D swirBand) {
        Raster greenRaster = greenBand.getRenderedImage().getData();
        Raster swirRaster = swirBand.getRenderedImage().getData();
        
        int width = greenRaster.getWidth();
        int height = greenRaster.getHeight();
        
        WritableRaster mndwiRaster = greenRaster.createCompatibleWritableRaster();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double greenValue = greenRaster.getSampleDouble(x, y, 0);
                double swirValue = swirRaster.getSampleDouble(x, y, 0);
                
                double mndwi = (greenValue - swirValue) / (greenValue + swirValue);
                mndwiRaster.setSample(x, y, 0, mndwi);
            }
        }
        
        return createGridCoverage(mndwiRaster, "MNDWI", greenBand.getCoordinateReferenceSystem());
    }
    
    /**
     * Calculate Automated Water Extraction Index (AWEI)
     */
    private GridCoverage2D calculateAWEI(GridCoverage2D blueBand, GridCoverage2D greenBand, 
                                       GridCoverage2D nirBand, GridCoverage2D swirBand) {
        Raster blueRaster = blueBand.getRenderedImage().getData();
        Raster greenRaster = greenBand.getRenderedImage().getData();
        Raster nirRaster = nirBand.getRenderedImage().getData();
        Raster swirRaster = swirBand.getRenderedImage().getData();
        
        int width = blueRaster.getWidth();
        int height = blueRaster.getHeight();
        
        WritableRaster aweiRaster = blueRaster.createCompatibleWritableRaster();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double blueValue = blueRaster.getSampleDouble(x, y, 0);
                double greenValue = greenRaster.getSampleDouble(x, y, 0);
                double nirValue = nirRaster.getSampleDouble(x, y, 0);
                double swirValue = swirRaster.getSampleDouble(x, y, 0);
                
                double awei = 4 * (greenValue - swirValue) - 
                            (0.25 * nirValue + 2.75 * blueValue);
                aweiRaster.setSample(x, y, 0, awei);
            }
        }
        
        return createGridCoverage(aweiRaster, "AWEI", blueBand.getCoordinateReferenceSystem());
    }
    
    /**
     * Advanced building detection using spectral, texture and morphological analysis
     * 
     * @param panBand Panchromatic band (high resolution)
     * @param redBand Red band
     * @param nirBand Near-Infrared band
     * @param ndvi NDVI raster
     * @return Building mask with confidence levels (0-1)
     */
    public GridCoverage2D advancedBuildingDetection(GridCoverage2D panBand, GridCoverage2D redBand, 
                                                  GridCoverage2D nirBand, GridCoverage2D ndvi) {
        // Step 1: Spectral analysis
        GridCoverage2D spectralMask = spectralBuildingDetection(redBand, nirBand, ndvi);
        
        // Step 2: Texture analysis
        GridCoverage2D textureFeatures = calculateTextureFeatures(panBand);
        
        // Step 3: Combine results
        Raster spectralRaster = spectralMask.getRenderedImage().getData();
        Raster textureRaster = textureFeatures.getRenderedImage().getData();
        
        int width = spectralRaster.getWidth();
        int height = spectralRaster.getHeight();
        
        WritableRaster buildingConfidence = spectralRaster.createCompatibleWritableRaster();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double spectralValue = spectralRaster.getSampleDouble(x, y, 0);
                double textureValue = textureRaster.getSampleDouble(x, y, 0);
                
                // Combine with weights (can be calibrated per sensor)
                double confidence = (0.6 * spectralValue) + (0.4 * textureValue);
                buildingConfidence.setSample(x, y, 0, confidence);
            }
        }
        
        // Step 4: Apply morphological operations to clean up
        GridCoverage2D confidenceCoverage = createGridCoverage(buildingConfidence, 
            "BuildingConfidence", panBand.getCoordinateReferenceSystem());
        
        return applyMorphologicalOperations(confidenceCoverage);
    }
    
    /**
     * Spectral-based building detection
     */
    private GridCoverage2D spectralBuildingDetection(GridCoverage2D redBand, GridCoverage2D nirBand, 
                                                   GridCoverage2D ndvi) {
        Raster redRaster = redBand.getRenderedImage().getData();
        Raster nirRaster = nirBand.getRenderedImage().getData();
        Raster ndviRaster = ndvi.getRenderedImage().getData();
        
        int width = redRaster.getWidth();
        int height = redRaster.getHeight();
        
        WritableRaster buildingMask = redRaster.createCompatibleWritableRaster();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double redValue = redRaster.getSampleDouble(x, y, 0);
                double nirValue = nirRaster.getSampleDouble(x, y, 0);
                double ndviValue = ndviRaster.getSampleDouble(x, y, 0);
                
                // Building detection rules
                double confidence = 0;
                
                // Rule 1: Low vegetation (NDVI < 0.2)
                if (ndviValue < 0.2) confidence += 0.3;
                
                // Rule 2: High reflectance in red and NIR
                if (redValue > 0.3 && nirValue > 0.3) confidence += 0.4;
                
                // Rule 3: Red/NIR ratio
                if ((redValue / (nirValue + 0.0001)) > 0.8) confidence += 0.3;
                
                confidence = Math.min(1.0, confidence);
                buildingMask.setSample(x, y, 0, confidence);
            }
        }
        
        return createGridCoverage(buildingMask, "SpectralBuilding", redBand.getCoordinateReferenceSystem());
    }
    
    /**
     * Calculate texture features for building detection
     */
    private GridCoverage2D calculateTextureFeatures(GridCoverage2D panBand) {
        // Step 1: Apply Sobel edge detection
        GridCoverage2D edgeImage = applySobelEdgeDetection(panBand);
        
        // Step 2: Calculate local variance
        GridCoverage2D varianceImage = calculateLocalVariance(panBand, 3);
        
        // Combine texture features
        Raster edgeRaster = edgeImage.getRenderedImage().getData();
        Raster varRaster = varianceImage.getRenderedImage().getData();
        
        int width = edgeRaster.getWidth();
        int height = edgeRaster.getHeight();
        
        WritableRaster textureRaster = edgeRaster.createCompatibleWritableRaster();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double edgeValue = edgeRaster.getSampleDouble(x, y, 0);
                double varValue = varRaster.getSampleDouble(x, y, 0);
                
                // Normalize and combine
                double texture = (0.6 * edgeValue) + (0.4 * (varValue / 0.1)); // assuming 0.1 is max expected variance
                texture = Math.min(1.0, texture);
                
                textureRaster.setSample(x, y, 0, texture);
            }
        }
        
        return createGridCoverage(textureRaster, "TextureFeatures", panBand.getCoordinateReferenceSystem());
    }
    
    /**
     * Apply Sobel edge detection
     */
    private GridCoverage2D applySobelEdgeDetection(GridCoverage2D coverage) {
        // Sobel kernels for x and y directions
        float[] xKernel = {
            -1, 0, 1,
            -2, 0, 2,
            -1, 0, 1
        };
        
        float[] yKernel = {
            -1, -2, -1,
             0,  0,  0,
             1,  2,  1
        };
        
        // Apply both kernels
        GridCoverage2D xGradient = applyConvolution(coverage, xKernel, 3, 3);
        GridCoverage2D yGradient = applyConvolution(coverage, yKernel, 3, 3);
        
        // Combine gradients
        Raster xRaster = xGradient.getRenderedImage().getData();
        Raster yRaster = yGradient.getRenderedImage().getData();
        
        WritableRaster edgeRaster = xRaster.createCompatibleWritableRaster();
        
        int width = xRaster.getWidth();
        int height = xRaster.getHeight();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double gx = xRaster.getSampleDouble(x, y, 0);
                double gy = yRaster.getSampleDouble(x, y, 0);
                double magnitude = Math.sqrt(gx * gx + gy * gy);
                edgeRaster.setSample(x, y, 0, magnitude);
            }
        }
        
        return createGridCoverage(edgeRaster, "SobelEdges", coverage.getCoordinateReferenceSystem());
    }
    /**
     * Calculate local variance
     */
    private GridCoverage2D calculateLocalVariance(GridCoverage2D coverage, int windowSize) {
        Raster raster = coverage.getRenderedImage().getData();
        int width = raster.getWidth();
        int height = raster.getHeight();
        
        WritableRaster varRaster = raster.createCompatibleWritableRaster();
        
        int halfWindow = windowSize / 2;
        
        for (int y = halfWindow; y < height - halfWindow; y++) {
            for (int x = halfWindow; x < width - halfWindow; x++) {
                // Calculate mean
                double sum = 0;
                int count = 0;
                
                for (int ky = -halfWindow; ky <= halfWindow; ky++) {
                    for (int kx = -halfWindow; kx <= halfWindow; kx++) {
                        sum += raster.getSampleDouble(x + kx, y + ky, 0);
                        count++;
                    }
                }
                
                double mean = sum / count;
                
                // Calculate variance
                double varSum = 0;
                for (int ky = -halfWindow; ky <= halfWindow; ky++) {
                    for (int kx = -halfWindow; kx <= halfWindow; kx++) {
                        double diff = raster.getSampleDouble(x + kx, y + ky, 0) - mean;
                        varSum += diff * diff;
                    }
                }
                
                double variance = varSum / count;
                varRaster.setSample(x, y, 0, variance);
            }
        }
        
        // Handle borders by replicating edge values
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (y < halfWindow || y >= height - halfWindow || 
                    x < halfWindow || x >= width - halfWindow) {
                    int xClamp = Math.min(width - halfWindow - 1, Math.max(halfWindow, x));
                    int yClamp = Math.min(height - halfWindow - 1, Math.max(halfWindow, y));
                    double val = varRaster.getSampleDouble(xClamp, yClamp, 0);
                    varRaster.setSample(x, y, 0, val);
                }
            }
        }
        
        return createGridCoverage(varRaster, "LocalVariance", coverage.getCoordinateReferenceSystem());
    }
    
    /**
     * Apply morphological operations (dilation + erosion)
     */
    private GridCoverage2D applyMorphologicalOperations(GridCoverage2D coverage) {
        // Simple implementation - in production you'd use a proper image processing library
        Raster raster = coverage.getRenderedImage().getData();
        int width = raster.getWidth();
        int height = raster.getHeight();
        
        WritableRaster processed = raster.createCompatibleWritableRaster();
        
        // First pass: dilation
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                double max = 0;
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        max = Math.max(max, raster.getSampleDouble(x + kx, y + ky, 0));
                    }
                }
                processed.setSample(x, y, 0, max);
            }
        }
        
        // Second pass: erosion
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                double min = 1;
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        min = Math.min(min, processed.getSampleDouble(x + kx, y + ky, 0));
                    }
                }
                processed.setSample(x, y, 0, min);
            }
        }
        
        return createGridCoverage(processed, "MorphProcessed", coverage.getCoordinateReferenceSystem());
    }
    
    /**
     * Advanced road detection using line detection and connectivity analysis
     * 
     * @param panBand Panchromatic band (high resolution)
     * @param ndvi NDVI raster
     * @param buildingMask Building confidence raster
     * @return Road network raster
     */
    public GridCoverage2D advancedRoadDetection(GridCoverage2D panBand, GridCoverage2D ndvi, 
                                              GridCoverage2D buildingMask) {
        // Step 1: Apply line detection
        GridCoverage2D lineImage = applyLineDetection(panBand);
        
        // Step 2: Filter out vegetation and building areas
        Raster lineRaster = lineImage.getRenderedImage().getData();
        Raster ndviRaster = ndvi.getRenderedImage().getData();
        Raster buildingRaster = buildingMask.getRenderedImage().getData();
        
        int width = lineRaster.getWidth();
        int height = lineRaster.getHeight();
        
        WritableRaster roadRaster = lineRaster.createCompatibleWritableRaster();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double lineValue = lineRaster.getSampleDouble(x, y, 0);
                double ndviValue = ndviRaster.getSampleDouble(x, y, 0);
                double buildingValue = buildingRaster.getSampleDouble(x, y, 0);
                
                // Filter conditions
                if (ndviValue > 0.3 || buildingValue > 0.5) {
                    roadRaster.setSample(x, y, 0, 0);
                } else {
                    roadRaster.setSample(x, y, 0, lineValue);
                }
            }
        }
        
        // Step 3: Apply connectivity analysis
        GridCoverage2D roadCoverage = createGridCoverage(roadRaster, "RoadNetwork", 
            panBand.getCoordinateReferenceSystem());
        
        return applyConnectivityAnalysis(roadCoverage);
    }
    
    /**
     * Apply line detection using directional filters
     */
    private GridCoverage2D applyLineDetection(GridCoverage2D coverage) {
        // Create horizontal and vertical line detection kernels
        float[] horizontalKernel = {
            -1, -1, -1,
             2,  2,  2,
            -1, -1, -1
        };
        
        float[] verticalKernel = {
            -1, 2, -1,
            -1, 2, -1,
            -1, 2, -1
        };
        
        // Apply both kernels and combine results
        GridCoverage2D horizontalLines = applyConvolution(coverage, horizontalKernel, 3, 3);
        GridCoverage2D verticalLines = applyConvolution(coverage, verticalKernel, 3, 3);
        
        Raster horRaster = horizontalLines.getRenderedImage().getData();
        Raster verRaster = verticalLines.getRenderedImage().getData();
        
        int width = horRaster.getWidth();
        int height = horRaster.getHeight();
        
        WritableRaster lineRaster = horRaster.createCompatibleWritableRaster();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double horValue = horRaster.getSampleDouble(x, y, 0);
                double verValue = verRaster.getSampleDouble(x, y, 0);
                
                // Combine using maximum response
                lineRaster.setSample(x, y, 0, Math.max(horValue, verValue));
            }
        }
        
        return createGridCoverage(lineRaster, "LineDetection", coverage.getCoordinateReferenceSystem());
    }
    
    /**
     * Apply convolution operation
     */
    private GridCoverage2D applyConvolution(GridCoverage2D coverage, float[] kernel, 
                                          int kernelWidth, int kernelHeight) {
        try {
            KernelJAI convKernel = new KernelJAI(kernelWidth, kernelHeight, kernel);
            ConvolveCoverageProcess convolve = new ConvolveCoverageProcess();
            return convolve.execute(coverage, convKernel, 3, 3, 1);
        } catch (Exception e) {
            throw new RuntimeException("Failed to apply convolution", e);
        }
    }
    
    /**
     * Simple connectivity analysis to enhance road network
     */
    private GridCoverage2D applyConnectivityAnalysis(GridCoverage2D coverage) {
        Raster raster = coverage.getRenderedImage().getData();
        int width = raster.getWidth();
        int height = raster.getHeight();
        
        WritableRaster connected = raster.createCompatibleWritableRaster();
        
        // First pass: identify connected components
        int[][] labels = new int[width][height];
        int currentLabel = 1;
        
        // Simple connected components labeling (4-connectivity)
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (raster.getSampleDouble(x, y, 0) > 0.5) {
                    int minNeighborLabel = Integer.MAX_VALUE;
                    
                    // Check 4 neighbors
                    if (labels[x-1][y] > 0) minNeighborLabel = Math.min(minNeighborLabel, labels[x-1][y]);
                    if (labels[x][y-1] > 0) minNeighborLabel = Math.min(minNeighborLabel, labels[x][y-1]);
                    if (labels[x+1][y] > 0) minNeighborLabel = Math.min(minNeighborLabel, labels[x+1][y]);
                    if (labels[x][y+1] > 0) minNeighborLabel = Math.min(minNeighborLabel, labels[x][y+1]);
                    
                    if (minNeighborLabel == Integer.MAX_VALUE) {
                        labels[x][y] = currentLabel++;
                    } else {
                        labels[x][y] = minNeighborLabel;
                    }
                }
            }
        }
        
        // Second pass: count component sizes
        int[] componentSizes = new int[currentLabel];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (labels[x][y] > 0) {
                    componentSizes[labels[x][y]]++;
                }
            }
        }
        
        // Third pass: filter small components
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int label = labels[x][y];
                if (label > 0 && componentSizes[label] > 10) { // threshold = 10 pixels
                    connected.setSample(x, y, 0, raster.getSampleDouble(x, y, 0));
                } else {
                    connected.setSample(x, y, 0, 0);
                }
            }
        }
        
        return createGridCoverage(connected, "ConnectedRoads", coverage.getCoordinateReferenceSystem());
    }
    
    /**
     * Perform land cover classification using spectral angle mapping
     * 
     * @param bands Array of bands (order must match spectral library)
     * @param sensorName Name of the sensor (must match spectral library)
     * @return Classification raster with class IDs
     */
    public GridCoverage2D classifyLandCover(GridCoverage2D[] bands, String sensorName) {
        if (!spectralLibraries.containsKey(sensorName)) {
            throw new IllegalArgumentException("No spectral library found for sensor: " + sensorName);
        }
        
        Map<String, double[]> signatures = spectralLibraries.get(sensorName);
        int numBands = bands.length;
        
        // Prepare rasters
        Raster[] bandRasters = new Raster[numBands];
        for (int i = 0; i < numBands; i++) {
            bandRasters[i] = bands[i].getRenderedImage().getData();
        }
        
        int width = bandRasters[0].getWidth();
        int height = bandRasters[0].getHeight();
        
        WritableRaster classRaster = bandRasters[0].createCompatibleWritableRaster();
        
        // Perform classification for each pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Get pixel vector
                double[] pixel = new double[numBands];
                for (int b = 0; b < numBands; b++) {
                    pixel[b] = bandRasters[b].getSampleDouble(x, y, 0);
                }
                
                // Normalize pixel vector
                double pixelNorm = vectorNorm(pixel);
                if (pixelNorm > 0) {
                    for (int b = 0; b < numBands; b++) {
                        pixel[b] /= pixelNorm;
                    }
                }
                
                // Find best matching class
                String bestClass = "unknown";
                double minAngle = Double.MAX_VALUE;
                
                for (Map.Entry<String, double[]> entry : signatures.entrySet()) {
                    String className = entry.getKey();
                    double[] classSig = entry.getValue();
                    
                    // Calculate spectral angle
                    double angle = spectralAngle(pixel, classSig);
                    
                    if (angle < minAngle) {
                        minAngle = angle;
                        bestClass = className;
                    }
                }
                
                // Set class ID (could use enum or mapping)
                int classId = 0;
                switch (bestClass) {
                    case "water": classId = 1; break;
                    case "vegetation": classId = 2; break;
                    case "urban": classId = 3; break;
                    case "soil": classId = 4; break;
                    default: classId = 0;
                }
                
                classRaster.setSample(x, y, 0, classId);
            }
        }
        
        return createGridCoverage(classRaster, "LandCover", bands[0].getCoordinateReferenceSystem());
    }
    
    /**
     * Calculate spectral angle between two vectors
     */
    private double spectralAngle(double[] v1, double[] v2) {
        double dotProduct = 0;
        double v1Norm = vectorNorm(v1);
        double v2Norm = vectorNorm(v2);
        
        for (int i = 0; i < v1.length; i++) {
            dotProduct += v1[i] * v2[i];
        }
        
        return Math.acos(dotProduct / (v1Norm * v2Norm));
    }
    
    /**
     * Calculate vector norm
     */
    private double vectorNorm(double[] v) {
        double sum = 0;
        for (double d : v) {
            sum += d * d;
        }
        return Math.sqrt(sum);
    }
    
    /**
     * Helper method to create GridCoverage2D from a WritableRaster with CRS
     */
    private GridCoverage2D createGridCoverage(WritableRaster raster, String name, CoordinateReferenceSystem crs) {
        GridCoverageFactory factory = new GridCoverageFactory();
        ReferencedEnvelope envelope = new ReferencedEnvelope(
            0, raster.getWidth(), 0, raster.getHeight(), crs);
        return factory.create(name, raster, envelope);
    }
    
    /**
     * Calibrate sensor with advanced parameters
     * @param sensorName Name of the sensor
     * @param calibration Complete calibration object
     */
    public void calibrateSensor(String sensorName, SensorCalibration calibration) {
        // Store the complete calibration object
        sensorCalibrations.put(sensorName, calibration);
        
        // Optionally update spectral library if new signatures are provided
        if (calibration.getSpectralSignatures() != null) {
            spectralLibraries.put(sensorName, calibration.getSpectralSignatures());
        }
    }
    
    /**
     * Get current calibration for a sensor
     * @param sensorName Name of the sensor
     * @return SensorCalibration object or null if not found
     */
    public SensorCalibration getSensorCalibration(String sensorName) {
        return sensorCalibrations.get(sensorName);
    }

     /**
     * Recalibrate NDVI calculation based on sensor-specific parameters
     * @param redBand Red band raster
     * @param nirBand Near-Infrared band raster
     * @param sensorName Name of the sensor
     * @return Calibrated NDVI raster
     */
    public GridCoverage2D calculateCalibratedNDVI(GridCoverage2D redBand, GridCoverage2D nirBand, String sensorName) {
        // Retrieve sensor-specific calibration
        SensorCalibration calibration = new SensorCalibration(); // In real scenario, retrieve from stored calibrations
        Map<String, Double> ndviParams = calibration.getNDVIParameters(sensorName);
        
        Raster redRaster = redBand.getRenderedImage().getData();
        Raster nirRaster = nirBand.getRenderedImage().getData();
        
        int width = redRaster.getWidth();
        int height = redRaster.getHeight();
        
        WritableRaster ndviRaster = redRaster.createCompatibleWritableRaster();
        
        double lowerBound = ndviParams.get("lower");
        double upperBound = ndviParams.get("upper");
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double redValue = redRaster.getSampleDouble(x, y, 0);
                double nirValue = nirRaster.getSampleDouble(x, y, 0);
                
                // NDVI calculation with sensor-specific bounds
                double ndvi = (nirValue - redValue) / (nirValue + redValue);
                
                // Apply custom thresholding
                ndvi = Math.max(lowerBound, Math.min(ndvi, upperBound));
                
                ndviRaster.setSample(x, y, 0, ndvi);
            }
        }
        
        return createGridCoverage(ndviRaster, "CalibratedNDVI", redBand.getCoordinateReferenceSystem());
    }

    
    /**
     * Advanced calibrated NDVI calculation with atmospheric correction
     * @param redBand Red band
     * @param nirBand NIR band
     * @param sensorName Sensor name for calibration
     * @return Calibrated NDVI with atmospheric correction
     */
    public GridCoverage2D calculateAdvancedCalibratedNDVI(GridCoverage2D redBand, GridCoverage2D nirBand, 
                                                         String sensorName) {
        SensorCalibration calibration = sensorCalibrations.get(sensorName);
        if (calibration == null) {
            throw new IllegalArgumentException("No calibration found for sensor: " + sensorName);
        }
        
        // Apply atmospheric correction if parameters are available
        GridCoverage2D correctedRed = applyAtmosphericCorrection(redBand, 
            calibration.getRedBandCorrection());
        GridCoverage2D correctedNir = applyAtmosphericCorrection(nirBand, 
            calibration.getNirBandCorrection());
        
        // Calculate NDVI with sensor-specific parameters
        return calculateCalibratedNDVI(correctedRed, correctedNir, sensorName);
    }
    
    /**
     * Apply atmospheric correction to a band
     */
    private GridCoverage2D applyAtmosphericCorrection(GridCoverage2D band, 
                                                    SensorCalibration.AtmosphericCorrection params) {
        if (params == null) {
            return band; // No correction parameters available
        }
        
        Raster raster = band.getRenderedImage().getData();
        int width = raster.getWidth();
        int height = raster.getHeight();
        
        WritableRaster corrected = raster.createCompatibleWritableRaster();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double value = raster.getSampleDouble(x, y, 0);
                
                // Simple dark object subtraction with gain/offset
                double correctedValue = params.getGain() * (value - params.getOffset()) 
                    - params.getDarkObjectValue();
                
                corrected.setSample(x, y, 0, Math.max(0, correctedValue));
            }
        }
        
        return createGridCoverage(corrected, "AtmCorrected", band.getCoordinateReferenceSystem());
    }
    
    /**
     * Load raster from GeoTIFF file
     */
    public GridCoverage2D loadGeoTIFF(String filePath) throws IOException {
        File file = new File(filePath);
        GeoTiffReader reader = new GeoTiffReader(file);
        return reader.read(null);
    }
    
    // /**
    //  * Extract subset using CQL filter
    //  */
    // public GridCoverage2D extractSubset(GridCoverage2D coverage, String cqlFilter) throws CQLException {
    //     ReferencedEnvelope envelope = new ReferencedEnvelope(
    //         coverage.getCoordinateReferenceSystem());
        
    //     // This is a simplified implementation - actual subsetting would require more complex logic
    //     return coverage.view(envelope);
    // }
}