package com.enit.satellite_platform.modules.resource_management.image_management.entities;

public enum ProcessingStatus {
    PENDING,    // Task received but not yet started
    PROCESSING, // Task is actively being processed
    COMPLETED,  // Task finished successfully
    FAILED,     // Task encountered an error
    UNKNOWN;   // Status is not known or not specified
}
