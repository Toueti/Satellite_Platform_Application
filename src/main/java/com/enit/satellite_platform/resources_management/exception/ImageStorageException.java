package com.enit.satellite_platform.resources_management.exception;

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
