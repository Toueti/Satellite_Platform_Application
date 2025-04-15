package com.enit.satellite_platform.modules.resource_management.image_management.entities;

/**
 * Enum representing the status of an image processing operation.
 * Each status has a description for documentation and UI display purposes.
 */
public enum ProcessingStatus {
    PENDING("Processing request is queued"),
    RUNNING("Processing is in progress"),
    COMPLETED("Processing completed successfully"),
    FAILED("Processing failed"),
    CANCELLED("Processing was cancelled");

    private final String description;
    
    ProcessingStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
