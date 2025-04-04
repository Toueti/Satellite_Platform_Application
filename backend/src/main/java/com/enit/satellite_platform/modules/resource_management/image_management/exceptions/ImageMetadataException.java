package com.enit.satellite_platform.modules.resource_management.image_management.exceptions;

/**
 * Thrown when there are issues with image metadata
 */
public class ImageMetadataException extends ImageException {
    public ImageMetadataException(String message) {
        super(message);
    }

    public ImageMetadataException(String message, Throwable cause) {
        super(message, cause);
    }
}