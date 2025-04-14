package com.enit.satellite_platform.modules.resource_management.image_management.entities;

/**
 * Enum representing different types of image processing operations.
 * Each type has a description for documentation and UI display purposes.
 */
public enum ProcessingType {
    NDVI("Normalized Difference Vegetation Index"),
    EVI("Enhanced Vegetation Index"),
    SAVI("Soil Adjusted Vegetation Index"),
    NDWI("Normalized Difference Water Index"),
    GEE_COMPOSITE("Google Earth Engine Composite"),
    GEE_CLASSIFICATION("Google Earth Engine Classification");

    private final String description;
    
    ProcessingType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
