package com.enit.satellite_platform.resources_management.exception;

/**
 * Thrown when an image is not found in the system
 */
public class ImageNotFoundException extends ImageException {
    public ImageNotFoundException(String message) {
        super(message);
    }

    public ImageNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
