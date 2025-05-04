package com.enit.satellite_platform.modules.resource_management.image_management.exceptions;

/**
 * Base exception class for image-related errors
 */
public class ImageException extends RuntimeException {
    public ImageException(String message) {
        super(message);
    }

    public ImageException(String message, Throwable cause) {
        super(message, cause);
    }
}
