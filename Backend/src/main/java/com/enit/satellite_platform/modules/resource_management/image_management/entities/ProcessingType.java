package com.enit.satellite_platform.modules.resource_management.image_management.entities;

import com.fasterxml.jackson.annotation.JsonCreator; // Import Jackson annotation
import java.util.stream.Stream;
import java.util.Arrays; // Import Arrays for error message

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

    @JsonCreator // Annotation to tell Jackson to use this method for deserialization
    public static ProcessingType fromString(String value) {
        if (value == null) {
            return null; // Or throw exception, depending on desired behavior for null input
        }

        // Handle specific alias: map "VEGETATION_INDEX" to NDVI
        if ("VEGETATION_INDEX".equalsIgnoreCase(value)) {
             return NDVI;
        }

        // Standard case-insensitive matching for defined enum names
        return Stream.of(ProcessingType.values())
                .filter(type -> type.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown ProcessingType value: '" + value + 
                                   "'. Accepted values (case-insensitive) are: " + 
                                   Arrays.toString(ProcessingType.values())));
    }
}
