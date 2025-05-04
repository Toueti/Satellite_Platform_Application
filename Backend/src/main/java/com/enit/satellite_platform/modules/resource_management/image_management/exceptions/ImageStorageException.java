package com.enit.satellite_platform.modules.resource_management.image_management.exceptions;

/**
 * Thrown when there are issues with image storage operations
 */
public class ImageStorageException extends ImageException {
    public ImageStorageException(String message) {
        super(message);
    }

    public ImageStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
