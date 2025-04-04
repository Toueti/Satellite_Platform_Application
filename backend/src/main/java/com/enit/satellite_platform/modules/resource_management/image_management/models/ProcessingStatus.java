package com.enit.satellite_platform.modules.resource_management.image_management.models;

public enum ProcessingStatus {
    PENDING,    // Task received but not yet started
    PROCESSING, // Task is actively being processed
    COMPLETED,  // Task finished successfully
    FAILED      // Task encountered an error
}
